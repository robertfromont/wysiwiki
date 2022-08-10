//
// (c) 2022 Robert Fromont - robert@fromont.nz
//
//    This file is part of WysiWiki.
//
//    WysiWiki is free software; you can redistribute it and/or modify
//    it under the terms of the GNU Affero General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    WysiWiki is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this module; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package robertfromont.wysiwiki.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Iterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import robertfromont.wysiwiki.service.ContentManager;

/**
 * Serves content, and handle content update requests.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet(urlPatterns={"/*"}, loadOnStartup=1)
public class ContentServlet extends HttpServlet {

  ContentManager content;
  
  /**
   * Default constructor.
   */
  public ContentServlet() {
  } // end of constructor

  /** 
   * Initialise the servlet.
   */
  public void init() {
    try {
      log("Initializing...");

      String root = getServletContext().getRealPath("/"); // TODO make this configurable

      Path rootPath = FileSystems.getDefault().getPath(root).normalize();
      log("Document root: " + rootPath);

      content = new ContentManager().setRoot(rootPath);

      log("Ready");
    } catch (Exception x) {
      log("failed", x);
    } 
  }

  /**
   * GET handler: Return the given resource. If the resource is a .html document that
   * doesn't exist yet, a blank template is returned with the 404 response.
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    if (request.getPathInfo() == null
        || request.getPathInfo().equals("/")) { // root directory with no slash
      // redirect to the slash-ending version
      response.sendRedirect(getServletContext().getContextPath()+"/home.html");
      return;
    }
    if (request.getPathInfo().endsWith("/")) { // request ends with a slash
      // redirect to the document representing the directory
      response.sendRedirect(
        request.getContextPath()
        +request.getPathInfo().substring(0, request.getPathInfo().length()-1) + ".html");
      return;
    }
    if (request.getPathInfo().indexOf(".") < 0) { // no dot, maybe a directory name?
      File realPath = new File(request.getRealPath(request.getPathInfo()));
      if (!realPath.exists() || realPath.isDirectory()) {
        // redirect to the document representing the directory
        response.sendRedirect(request.getContextPath() +request.getPathInfo() + ".html");
        return;
      }
    }    

    InputStream contentStream = null;
    try {
      contentStream = content.read(request.getPathInfo());
    } catch (FileNotFoundException notFound) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      if (request.getPathInfo().endsWith(".html")) {
        // return 404, but also a template for creating a new document

        // if it's not a top-level document we need to edit the <base> tag on the way through
        if (request.getPathInfo().indexOf('/', 1) >= 0) {
          // for each slash in the path, we need to add "../" to the base href
          int slashCount = request.getPathInfo().substring(1).split("/").length - 1;
          String href = "";
          for (int level = 0; level < slashCount; level++) href += "../";
          String baseTag = "<base href=\""+href+"\">";
          
          // read the document into a string, converting the base tag, and write out the result 
          StringBuilder template = new StringBuilder();
          BufferedReader reader = new BufferedReader(
            new InputStreamReader(content.read("/template.html")));
          String line = reader.readLine();
          while(line != null) {
            template.append(line.replaceAll("<base href=\"\\./\">", baseTag));
            template.append("\n");
            line = reader.readLine();
          } // next line
          reader.close();
          contentStream = new ByteArrayInputStream(template.toString().getBytes());
        } else { // top level document, so just copy through the template as-is
          contentStream = content.read("/template.html");
        }
      }
    }
    
    if (request.getPathInfo().endsWith(".html")) {
      response.setCharacterEncoding("UTF-8");
    }
    String contentType = URLConnection.guessContentTypeFromName(request.getPathInfo());
    if ((contentType == null || contentType.length() == 0)
        && request.getPathInfo().endsWith(".js")) {
      // for some reason guessContentTypeFromName doesn't get .js right
      contentType = "application/javascript";
    }
    response.setContentType(contentType);
    response.setDateHeader( // expires in a week
      "Expires", new java.util.Date().getTime() + (1000*60*60*24*7));
    
    byte[] buf = new byte[8192];
    int length;
    OutputStream responseBody = response.getOutputStream();
    while ((length = contentStream.read(buf)) > 0) {
      responseBody.write(buf, 0, length);
    }
    contentStream.close();
    responseBody.close();
  }  

  /**
   * PUT handler: Adds or updates an HTML document, or if the "move" parameter is specified,
   * the document's entry is moved in the index (in which case the HTML document itself is
   * not updated).
   */
  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    try {

      if (request.getPathInfo() == null || !request.getPathInfo().endsWith(".html")) {
        // can only PUT html documents
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      String moveWhere = request.getParameter("move");
      if (moveWhere == null) { // PUT full content                
        // back up the old version
        //TODO backup(html);
        try {
          content.update(request.getPathInfo(), request.getInputStream());
        } catch(NoSuchFileException exception) {
          content.create(request.getPathInfo(), request.getInputStream());
        }
        response.getWriter().write("OK");
      } else { // move request - only edit the position in the index
        boolean moved = content.move(request.getPathInfo(), moveWhere);
        if (moved) {
          response.getWriter().write("OK");
        } else {
          response.getWriter().write("Could not move " + request.getPathInfo() + " " + moveWhere);
        }
      } // move request
      
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(x.toString());
    }
  }

  /**
   * DELETE handler: Delete the given HTML document.
   */
  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      content.delete(request.getPathInfo());
      response.getWriter().write("OK");
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(x.toString());
    }
  }
  
  /**
   * POST handler: for saving images and other assets.
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

    try {
      if (ServletFileUpload.isMultipartContent(request)) { // file being uploaded
        
        // take the first file we find
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        List items = upload.parseRequest(request);
        Iterator it = items.iterator();
        FileItem fileItem = null;
        while (it.hasNext()) {
          FileItem item = (FileItem) it.next();
          if (!item.isFormField()) {
            fileItem = item;
            break;
          }            
        } // next part
        if (fileItem == null) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("No file received.");
        } else { // file found
          
          File file = new File(getServletContext().getRealPath(request.getPathInfo()));
          if (!file.getParentFile().exists()) {
            Files.createDirectories(file.getParentFile().toPath());
          }
          if (file.exists()) {
            // get a non-existent file name
            File dir = file.getParentFile();
            String name = file.getName().replaceAll("\\.[^.]*$","");
            String ext = file.getName().replaceAll(".*\\.([^.]*)$","$1");
            int i = 0;
            do {
              file = new File(dir, name + "-" + (++i) + "." + ext);
            } while(file.exists());
          }
          fileItem.write(file);

          response.getWriter().write(
            "."+request.getPathInfo().replaceAll("[^/]+$", file.getName()));
        } // file found
      } // file being uploaded
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getOutputStream().write(x.getMessage().getBytes());
    }
  }

  @Override
  /**
   * OPTIONS handler: specifies what HTML methods are allowed, depending on the user access.
   */
  protected void doOptions(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    String allow = "OPTIONS, GET";
    try {
      String username = request.getRemoteUser();
      if (username == null) { // look for Authorization header
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic ")) {
          String encodedAuth = authorization.substring("Basic ".length());
          String decodedAuth = new String(Base64.getDecoder().decode(encodedAuth.getBytes()));
          username = decodedAuth.split(":")[0];
        }        
      }
      
      if (username != null) {
        Context initCtx = new InitialContext();
        UserDatabase users = (UserDatabase)initCtx.lookup("java:comp/env/wysiwiki-users");
        User user = users.findUser(username);
        Role write = users.findRole("write");
        if (write != null) {
          if (user.isInRole(write)) {
            allow += ", PUT, POST, DELETE";
          }
        }
      } else { // not logged in
        // LOGIN isn't a valid method, but we use this so the caller can know whether to present
        // a login link or not
        allow += ", LOGIN";
      }
    } catch (Exception x) {
      log("doOptions ERROR: " + x);
    }
    response.addHeader("Allow", allow);
  }

} // end of class ContentServlet
