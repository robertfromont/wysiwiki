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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import robertfromont.wysiwiki.service.ContentManager;

/**
 * Serves content, and handle content update requests.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet({"/*"})
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
        "../"+request.getPathInfo().substring(0, request.getPathInfo().length()-1) + ".html");
      return;
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
      String move = request.getParameter("move");
      if (move == null) { // PUT full content                
        // back up the old version
        //TODO backup(html);
        try {
          content.update(request.getPathInfo(), request.getInputStream());
        } catch(NoSuchFileException exception) {
          content.create(request.getPathInfo(), request.getInputStream());
        }
        response.getWriter().write("OK");
      } else { // move request - only edit the position in the index TODO
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Move not supported.");
      } // move request
      
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(x.toString());
    }
  }

} // end of class ContentServlet
