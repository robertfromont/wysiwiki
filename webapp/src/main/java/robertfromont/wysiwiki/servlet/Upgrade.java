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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;

/**
 * Servlet that manages installation and upgrade.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet(urlPatterns = "/wysiwiki/admin/upgrade", loadOnStartup = 10)
public class Upgrade extends HttpServlet {
  
  /**
   * GET handler
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
        
    PrintWriter writer = response.getWriter();
    writer.println("<!DOCTYPE html>");
    writer.println("<html>");
    writer.println(" <head>");
    writer.println("  <title>WysiWiki Upgrade</title>");
    writer.println("  <link rel=\"shortcut icon\" href=\"../logo.png\" />");
    writer.println("  <link rel=\"stylesheet\" href=\"../css/install.css\" type=\"text/css\" />");
    writer.println(" </head>");
    writer.println(" <body>");
    writer.println("  <h1>WysiWiki Upgrade</h1>");
    writer.println("  <form method=\"POST\" enctype=\"multipart/form-data\"><table>");
        
    // WAR file
    writer.println("   <tr title=\"The new version of the web application archive (.war file)\">");
    writer.println("    <td><label for=\"war\">digit-triplets-test.war file</label></td>");
    writer.println("    <td><input id=\"war\" name=\"war\" type=\"file\""
                   +" onchange=\"if (!this.files[0].name.match('\\.war$'))"
                   +" { alert('Please choose a .war file'); this.value = null; }\""
                   +"/></td></tr>");
    
    writer.println("    <tr><td><input type=\"submit\" value=\"Upgrade\"></td></tr>");
    
    writer.println("  </table></form>");
    writer.println(" </body>");
    writer.println("</html>");
    writer.flush();
  }
  
  /**
   * POST handler for installer form.
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    
    File webappRoot = new File(getServletContext().getRealPath("/"));
    
    log("Upgrade from war...");
    PrintWriter writer = response.getWriter();
    writer.println("<!DOCTYPE html>");
    writer.println("<html>");
    writer.println(" <head>");
    writer.println("  <title>WysiWiki Upgrade</title>");
    writer.println("  <link rel=\"shortcut icon\" href=\"../logo.png\" />");
    writer.println("  <link rel=\"stylesheet\" href=\"../css/install.css\" type=\"text/css\" />");
    writer.println(" </head>");
    writer.println(" <body>");
    writer.println("  <h1>WysiWiki Upgrade</h1>");
    writer.println("  <pre>");
    boolean fileFound = false;
    ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
    try {
      List<FileItem> items = upload.parseRequest(request);
      for (FileItem item : items) {
        if (!item.isFormField()
            && item.getName() != null && item.getName().endsWith(".war")) { // it's a war file
          log("File: " + item.getName());
          writer.println("File: " + item.getName());
          fileFound = true;
              
          // save file
          File war = File.createTempFile(item.getName(), ".war");
          try {
            war.delete(); // (so that item.write() doesn't complain)
            item.write(war);
            log("Saved: " + war.getPath());
                          
            // unpack it
            JarFile jar = new JarFile(war);
            // Version is in:
            JarEntry pomPropertiesEntry = jar.getJarEntry(
              "META-INF/maven/robertfromont.wysiwiki/wysiwiki-webapp/pom.properties");
            if (pomPropertiesEntry == null) {
              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
              writer.println(
                "<span class=\"error\">No build version information found."
                +" Please upload a .war file built from the source code.</span>");
              log("ERROR: No build version information found."
                  +" Please upload a .war file built from the source code.");
            } else { // build version found
              Properties pomProperties = new Properties();
              pomProperties.load(jar.getInputStream(pomPropertiesEntry));
              String version = pomProperties.getProperty("version");
              writer.println("Build: " + version);
              log("Build " + version);
              
              Enumeration<JarEntry> entries = jar.entries();
              while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                // don't replace possibly customized config files
                if ("WEB-INF/web.xml".equals(entry.getName())) continue;
                if (entry.getName().startsWith("META-INF/")) continue;
                
                if (!entry.isDirectory()) {
                  
                  // unpack file 
                  File parent = webappRoot;
                  String sFileName = entry.getName();
                  writer.print("Unpacking: "+sFileName+" ...");
                  log("Unpacking: "+sFileName+" ...");
                  String[] pathParts = entry.getName().split("/");
                  for (int p = 0; p < pathParts.length - 1; p++) {
                    // ensure that the required directories exist
                    parent = new File(parent, pathParts[p]);
                    if (!parent.exists()) {
                      parent.mkdir();
                    }		     
                  } // next part
                  sFileName = pathParts[pathParts.length - 1];
                  File file = new File(parent, sFileName);
                  
                  // get input stream
                  InputStream in = jar.getInputStream(entry);
                  
                  // get output stream
                  FileOutputStream fout = new FileOutputStream(file);
                  
                  // pump data from one stream to the other
                  byte[] buffer = new byte[1024];
                  int bytesRead = in.read(buffer);
                  while(bytesRead >= 0) {
                    fout.write(buffer, 0, bytesRead);
                    bytesRead = in.read(buffer);
                  } // next chunk of data
                  
                  // close streams
                  in.close();
                  fout.close();
                  writer.println("OK");
                  
                } // not a directory
                
              } // next entry
            } // build version found
          } finally {
            war.delete();
          }
        } // .war file
      } // next item
      
      if (!fileFound) {
        writer.print("<span class=\"error\">No file uploaded.</span>");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        writer.println("</pre>");
        writer.println("<p><em>Upload complete</em></p>");
        writer.println("<p>The web-app should automatically reload and upgrade.</p>");
        writer.println("<p>Click <a href=\"../../home.html\">here</a> to continue...</p>");
        writer.print("<pre>");
      }
    } catch (Exception x) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log("ERROR: " + x);
      writer.println("\n<span class=\"error\">ERROR: " + x + "</span>");
      x.printStackTrace(writer);            
    } finally {
      writer.println("</pre></body></html>");
    }
  }
    
  private static final long serialVersionUID = 1;
} // end of class Upgrade
