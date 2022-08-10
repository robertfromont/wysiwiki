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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import robertfromont.wysiwiki.service.ContentManager;

/**
 * Serves content, and handle content update requests.
 * @author Robert Fromont robert@fromont.net.nz
 */
@WebServlet(urlPatterns="/wysiwiki/admin/users/*", loadOnStartup=20)
public class Users extends HttpServlet {

  UserDatabase users;
  DocumentBuilderFactory documentBuilderFactory;
  TransformerFactory transformerFactory;
  DocumentBuilder docBuilder;
  
  /**
   * Default constructor.
   */
  public Users() {
  } // end of constructor

  /** 
   * Initialise the servlet.
   */
  public void init() {
    try {
      log("Initializing user manager...");
      
      Context initCtx = new InitialContext();
      users = (UserDatabase)initCtx.lookup("java:comp/env/wysiwiki-users");

      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      transformerFactory = TransformerFactory.newInstance();
      docBuilder = documentBuilderFactory.newDocumentBuilder();

      log("User manager ready");
    } catch (Exception x) {
      log("User manager failed", x);
    } 
  }

  /**
   * GET handler: Return an XML-encoded list of users.
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/xml");
    response.setCharacterEncoding("UTF-8");
    // build a document listing the users
    Document usersDoc = docBuilder.newDocument();
    Element xmlUsers = usersDoc.createElement("users");
    usersDoc.appendChild(xmlUsers);
    Iterator<User> u = users.getUsers();
    while (u.hasNext()) {
      User user = u.next();
      Element xmlUser = usersDoc.createElement("user");
      xmlUsers.appendChild(xmlUser);
      xmlUser.setAttribute("username", user.getUsername());
      xmlUser.setAttribute("fullname", user.getFullName());
      Element xmlRoles = usersDoc.createElement("roles");
      xmlUser.appendChild(xmlRoles);
      Iterator<Role> r = user.getRoles();
      while (r.hasNext()) {
        Element xmlRole = usersDoc.createElement("role");
        xmlRole.setAttribute("rolename", r.next().getRolename());
        xmlRoles.appendChild(xmlRole);
      }
    }
    
    // output the document as XML 
    try {
      DOMSource source = new DOMSource(usersDoc);
      StreamResult result =  new StreamResult(response.getWriter());
      Transformer xmlTransformer = transformerFactory.newTransformer();
      xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
      xmlTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      xmlTransformer.transform(source, result);
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(x.toString());
    }
  }  

  /**
   * PUT handler: Adds or updates a user.
   * <p> The path trailing "/wysiwiki/admin/users/" determines the username, and
   * the body must be an XML-encoded document with a "password" element and/or a
   * "roles" element.
   */
  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");

    // infer the username from the URL
    if (request.getPathInfo() == null
        || request.getPathInfo().equals("/")) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("No username specified");
      return;
    }
    String username = request.getPathInfo().substring(1); // strip of leading slash
    if (username.trim().length() == 0) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("No username specified");
      return;
    }

    try {
      // parse the body as XML
      DocumentBuilder xmlParser = documentBuilderFactory.newDocumentBuilder();
      Document body = xmlParser.parse(request.getInputStream());
      
      // is there a password?
      String password = null;
      NodeList passwordNodes = body.getElementsByTagName("password");
      if (passwordNodes.getLength() > 0) {
        Node passwordNode = passwordNodes.item(0);
        password = passwordNode.getTextContent();
      }
      
      // are there roles?
      HashSet<String> newRoles = null;
      NodeList rolesNodes = body.getElementsByTagName("roles");
      if (rolesNodes.getLength() > 0) {
        Node rolesNode = rolesNodes.item(0);
        newRoles = new HashSet<String>();
        NodeList roleNodes = rolesNode.getChildNodes();
        for (int r = 0; r < roleNodes.getLength(); r++) {
          Element roleNode = (Element)roleNodes.item(r);
          newRoles.add(roleNode.getAttribute("rolename"));
        }
      }

      // does the user already exist?
      User user = users.findUser​(username);
      if (user == null) { // user doesn't exist yet
        // try to create them
        if (password == null || password.length() == 0) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("No password supplied for new user: " + username);
          return;
        }
        user = users.createUser(username, password, username);
      } else if (password != null && password.length() > 0) { // update password of existing user
        user.setPassword(password);
      }

      if (newRoles != null) {        
        // not allowed to edit themselves
        User thisUser = (User)request.getUserPrincipal();
        if (thisUser.getUsername().equals(username)) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("You cannot edit your own roles");
          return;
        }

        // delete/add roles to match the newRoles set
        HashSet<String> oldRoles = new HashSet<String>();
        Iterator<Role> userRoles = user.getRoles(); 
        while (userRoles.hasNext()) oldRoles.add(userRoles.next().getRolename());

        // add roles that aren't in oldRoles
        HashSet<String> rolesToAdd = new HashSet<String>(newRoles);
        rolesToAdd.removeAll(oldRoles);
        for (String rolename : rolesToAdd) {
          Role r = users.findRole(rolename);
          if (r != null) {
            user.addRole(r);
          } // valid role
        } // next role to add
        
        // remove roles that aren't in newRoles
        HashSet<String> rolesToRemove = new HashSet<String>(oldRoles);
        rolesToRemove.removeAll(newRoles);
        for (String rolename : rolesToRemove) {
          Role r = users.findRole(rolename);
          if (r != null) {
            user.removeRole(r);
          } // valid role
        } // next role to remove
      } // roles are specified
      
      // save changes to disk to ensure persistence
      users.save();
      
      response.getWriter().write("OK");
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write(x.toString());
    }
  }

  /**
   * DELETE handler: Deletes a user.
   */
  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    
    // infer the username from the URL
    if (request.getPathInfo() == null
        || request.getPathInfo().equals("/")) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("No username specified");
      return;
    }
    String username = request.getPathInfo().substring(1); // strip of leading slash

    try {
      // not allowed to delete themselves
      User thisUser = (User)request.getUserPrincipal();
      if (thisUser.getUsername().equals(username)) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("You cannot delete yourself");
        return;
      }

      // does the user exist?
      User user = users.findUser​(username);
      if (user == null) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("Not found: " + username);
        return;
      }

      users.removeUser(user);

      response.getWriter().write("OK");
    } catch (Exception x) {
      x.printStackTrace(System.err);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(x.toString());
    }
  }
  
} // end of class ContentServlet
