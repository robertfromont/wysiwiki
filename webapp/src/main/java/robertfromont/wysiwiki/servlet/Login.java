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
 * This is a protected resource, so provokes a login form.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet({"/login"})
public class Login extends HttpServlet {
  /**
   * GET handler simply redirects the request, given they're logged in by the time it's invoked.
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    // we got this far, so they've logged in
    try {
      User user = (User)request.getUserPrincipal();
      log("user: " + user);
      
      if (user != null) {
        String allow = "OPTIONS, GET";
        Context initCtx = new InitialContext();
        UserDatabase users = (UserDatabase)initCtx.lookup("java:comp/env/wysiwiki-users");
        Role author = users.findRole("author");
        if (author != null) {
          if (user.isInRole(author)) {
            allow = "OPTIONS, GET, PUT, POST, DELETE";
          }
        }
        // set the header in this response
        response.addHeader("Allow", allow);
        // and save it in the session
      } // there is a user
    } catch (Exception x) {
      log("ERROR: Login.doGet: " + x);
    }

    // if we got this far, they successfully logged in

    // TODO redirect to url parameter
    // redirect to referrer if any
    if (request.getHeader("Referer") != null) {
      response.sendRedirect(request.getHeader("Referer"));
    } else {      
      // now redirect them back to home
      response.sendRedirect(getServletContext().getContextPath()+"/home.html");
    }
  }  
} // end of class ContentServlet
