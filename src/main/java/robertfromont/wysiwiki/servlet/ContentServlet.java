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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import robertfromont.wysiwiki.service.ContentManager;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.net.URLConnection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        contentStream = content.read("/template.html"); // TODO edite <base> on the way through
      }
    }
    
    if (request.getPathInfo().endsWith(".html")) {
      response.setCharacterEncoding("UTF-8");
    }
    response.setContentType(
      URLConnection.guessContentTypeFromName(request.getPathInfo()));
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

} // end of class ContentServlet
