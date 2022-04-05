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
package robertfromont.wysiwiki.standalone;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.fileupload.MultipartStream;
import robertfromont.wysiwiki.service.ContentManager;

/**
 * Allows running WysiWiki from the command line.
 */
public class WebApp implements HttpHandler {
  
  /** Command-line entrypoint */
  public static void main(String argv[]) {
    WebApp webapp = new WebApp();
    if (argv.length > 0) {
      try {
        File root = new File(argv[0]);
        if (!root.isDirectory()) root = root.getParentFile();
        webapp.setRoot(root);
      } catch(Exception exception) {
        System.err.println("Cannot set root directory to \""+argv[0]+"\": "+exception);
      }
    }
    if (webapp.getRoot() == null) webapp.setRoot(new File("."));
    webapp.start();
  }
  
  /** HTTP server */
  protected HttpServer server;

  /** Wysiwiki Content Manager */
  protected ContentManager content;
  
  /**
   * Root directory of webapp.
   * @see #getRoot()
   * @see #setRoot(File)
   */
  protected File root;
  /**
   * Getter for {@link #root}: Root directory of webapp.
   * @return Root directory of webapp.
   */
  public File getRoot() { return root; }
  /**
   * Setter for {@link #root}: Root directory of webapp.
   * @param newRoot Root directory of webapp.
   */
  public WebApp setRoot(File newRoot) {
    root = newRoot;
    try {
      if (root != null) root = root.getCanonicalFile();
    } catch(IOException exception) {}
    return this;
  }
  
  /**
   * Server port to listen on.
   * @see #getPort()
   * @see #setPort(Integer)
   */
  protected Integer port = Integer.valueOf(3121);
  /**
   * Getter for {@link #port}: Server port to listen on.
    * @return Server port to listen on.
    */
  public Integer getPort() { return port; }
  /**
   * Setter for {@link #port}: Server port to listen on.
   * @param newPort Server port to listen on.
   */
  public WebApp setPort(Integer newPort) { port = newPort; return this; }
  
  /**
   * Determines the content-type for a given resource name, using the file extension.
   * @param name
   * @return A valid content-type
   */
  public static String ContentTypeForName(String name) {
    if (name.endsWith(".html"))
      return "text/html;charset=UTF-8";
    else if (name.endsWith(".js"))
      return "text/javascript;charset=UTF-8";
    else if (name.endsWith(".json"))
      return "application/json;charset=UTF-8";
    else if (name.endsWith(".css"))
      return "text/css;charset=UTF-8";
    else if (name.endsWith(".png"))
      return "image/png";
    else if (name.endsWith(".jpg"))
      return "image/jpeg";
    else if (name.endsWith(".svg"))
      return "image/svg+xml";
    else if (name.endsWith(".ico"))
      return "image/vnd.microsoft.icon";
    else
      return "application/octet-stream";
  } // end of ContentTypeForName()   

  /** HttpHandler method */
  public void handle(HttpExchange exchange) throws IOException {

    //Path path = Paths.get(urlPath.split("/"));
    if ("GET".equals(exchange.getRequestMethod())) {
      get(exchange);
    } else if ("PUT".equals(exchange.getRequestMethod())) {
      put(exchange);
    } else if ("PPOST".equals(exchange.getRequestMethod())) {
      post(exchange);
    } else if ("DELETE".equals(exchange.getRequestMethod())) {
      delete(exchange);
    } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
      options(exchange);
    } else {
      exchange.sendResponseHeaders(400, -1);
    }
  }
  
  /** Http GET */
  public void get(HttpExchange exchange) throws IOException {
    String urlPath = exchange.getRequestURI().getPath();
    InputStream contentStream = null;
    int responseCode = 200;
    try {
      contentStream = content.read(urlPath);
    } catch (FileNotFoundException notFound) {
      responseCode = 404;;
      if (urlPath.endsWith(".html")) {
        // return 404, but also a template for creating a new document
        
        // if it's not a top-level document we need to edit the <base> tag on the way through
        if (urlPath.indexOf('/', 1) >= 0) {
          // for each slash in the path, we need to add "../" to the base href
          int slashCount = urlPath.substring(1).split("/").length - 1;
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
    
    String contentType = URLConnection.guessContentTypeFromName(urlPath);
    if (contentType == null) contentType = ContentTypeForName(urlPath);    
    if (contentType != null) {
      exchange.getResponseHeaders().add("Content-Type", contentType);
    }
    if (urlPath.equals("/index.html")) { // make sure index is refreshable
      exchange.getResponseHeaders().add( // expires in a week
        "Cache-Control", "no-store");
    } else {
      exchange.getResponseHeaders().add( // expires in a week
        "Expires", new Date(new Date().getTime() + (1000*60*60*24*7)).toString());
    }
    
    exchange.sendResponseHeaders(responseCode, 0);
    
    byte[] buf = new byte[8192];
    int length;
    OutputStream responseBody = exchange.getResponseBody();
    while ((length = contentStream.read(buf)) > 0) {
      responseBody.write(buf, 0, length);
    }
    contentStream.close();
    responseBody.close();
  }

  /** Http PUT */
  public void put(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=UTF-8");
    String urlPath = exchange.getRequestURI().getPath();
    String body = "OK";
    int responseCode = 200;
    try {      
      if (urlPath == null || !urlPath.endsWith(".html")) {
        // can only PUT html documents
        responseCode = 403;
        body = "Forbidden";
      } else {
        String moveWhere = null;
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.startsWith("?move=")) {
          moveWhere = query.substring("?move=".length());
        }
        if (moveWhere == null) { // PUT full content                
          // back up the old version
          //TODO backup(html);
          try {
            content.update(urlPath, exchange.getRequestBody());
          } catch(NoSuchFileException exception) {
            content.create(urlPath, exchange.getRequestBody());
          }
        } else { // move request - only edit the position in the index
          boolean moved = content.move(urlPath, moveWhere);
          if (!moved) {
            body = "Could not move " + urlPath + " " + moveWhere;
          }
        } // move request
      }      
    } catch (Exception x) {
      x.printStackTrace(System.err);
      responseCode = 500;
      body = x.toString();
    }
    exchange.sendResponseHeaders(responseCode, body.length());
    exchange.getResponseBody().write(body.getBytes());
  }
  
  /** Http POST */
  public void post(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=UTF-8");
    String urlPath = exchange.getRequestURI().getPath();
    String body = "";
    int responseCode = 200;
    String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
    if (contentType == null || contentType.startsWith("multipart/form-data; boundary=")) {
      responseCode = 400;
      body = "Wrong content type: " + contentType;
    } else {
      
      Path rootPath = root.toPath();
      Path relativePath = rootPath.getFileSystem().getPath(".", urlPath.split("/"));
      Path path = rootPath.resolve(relativePath).normalize();
      if (!path.startsWith(rootPath)) {
        // can only access under root
        responseCode = 403;
        body = "Cannot access files outside root.";
      } else {
        
        File file = null;
        try {
          // parse the parts out of the request
          // take the first file we find
          String boundary = contentType.substring("multipart/form-data; boundary=".length());
          MultipartStream bodyParts = new MultipartStream(
            exchange.getRequestBody(), boundary.getBytes(), 1024, null);
          boolean nextPart = true;
          nextPart = bodyParts.skipPreamble();
          while (nextPart) {
            String headers = bodyParts.readHeaders();
            Matcher fileNameParser = Pattern.compile("filename=\"([^\"]+)\"").matcher(headers);
            if (fileNameParser.find()) {
              String fileName = fileNameParser.group(1);
              file = path.toFile();
              break;
            }
            nextPart = bodyParts.readBoundary();
          } // next part
          
          if (file == null) {
            responseCode = 400;
            body = "No file received.";
          } else { // file found
            
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
            FileOutputStream output = new FileOutputStream(file);
            bodyParts.readBodyData(output);
            
            body = "."+urlPath.replaceAll("[^/]+$", file.getName());
          } // file found
        } catch (Exception x) {
          x.printStackTrace(System.err);
          responseCode = 500;
          body = x.getMessage();
        }
      } // path ok
    } // content type ok
    exchange.sendResponseHeaders(responseCode, body.length());
    exchange.getResponseBody().write(body.getBytes());
  }
  
  /** Http DELETE */
  public void delete(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=UTF-8");
    String urlPath = exchange.getRequestURI().getPath();
    String body = "OK";
    int responseCode = 200;
    try {
      content.delete(urlPath);
      body = "OK";
    } catch (Exception x) {
      x.printStackTrace(System.err);
      responseCode = 500;
      body = x.toString();
    }
    exchange.sendResponseHeaders(responseCode, body.length());
    exchange.getResponseBody().write(body.getBytes());
  }
  
  /** Http OPTIONS */
  public void options(HttpExchange exchange) throws IOException {
    exchange.getResponseHeaders().add("Allow", "OPTIONS, GET, PUT, POST, DELETE");
    exchange.sendResponseHeaders(200, -1);
  }
  
  /**
   * Creates the web server.
   */
  protected void createServer() throws IOException {
    // create web server
    server = HttpServer.create(new InetSocketAddress(port), 0);
  } // end of createServer()
  
  /**
   * Creates the content manager.
   */
  protected void createContentManager() throws Exception {
    System.out.println("root: " + root.getPath());
    content = new ContentManager().setRoot(root);
  } // end of createContentManager()

  /**
   * Adds handlers to the web server.
   * <p> The default implementation adds handlers for all files under {@link #root}. 
   * Subclass can replace/augment this behaviour by overriding this method.
   */
  protected void addHandlers() throws IOException {
    if (server == null) createServer();
    server.createContext("/", this);
  } // end of addHandlers()
  
  /** Start handling requests  */
  public void start() {
    try {
      if (server == null) {
        createServer();
        createContentManager();
        addHandlers();
      }
      
      server.setExecutor(null);
      server.start();
      
      // open browser
      java.awt.Desktop.getDesktop().browse(new URI("http://localhost:" + port + "/home.html"));
      
    } catch(Exception exception) {
      System.err.println("ERROR: " + exception);
      exception.printStackTrace(System.err);
    }
  }
}
