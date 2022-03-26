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
package robertfromont.wysiwiki.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.regex.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Manages updates of content and maintenance of indices.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ContentManager {  

  DocumentBuilderFactory documentBuilderFactory;
  TransformerFactory transformerFactory;
  XPath xpath;
  Document index;
  File indexHtml;
  
  /**
   * Root directory of all content.
   * @see #getRoot()
   * @see #setRoot(Path)
   */
  protected Path root;
  /**
   * Getter for {@link #root}: Root directory of all content.
   * @return Root directory of all content.
   */
  public Path getRoot() { return root; }
  /**
   * Setter for {@link #root}: Root directory of all content.
   * @param newRoot Root directory of all content.
   */
  public ContentManager setRoot(Path newRoot) throws Exception {
    root = newRoot.normalize();

    // forbidden path prefixes for reading
    readForbidden = new Vector<Path>();
    readForbidden.add(root.resolve("WEB-INF"));
    readForbidden.add(root.resolve("META-INF"));
    
    // forbidden path prefixes for writing
    writeForbidden = new Vector<Path>(readForbidden);
    writeForbidden.add(root.resolve("wysiwiki"));
    writeForbidden.add(root.resolve("index.html"));
    writeForbidden.add(root.resolve("rss.xml"));

    Path wysiwiki = root.resolve("wysiwiki");

    // ensure standard files exist
    String[] wysiwikiFiles = {
      "wysiwiki.js",
      "wysiwiki.css",
      "index.js",
      "resource.js",
      "header.html",
      "footer.html",
      "template.html",
      "style.css"
    };
    for (String wysiwikiFile : wysiwikiFiles) {      
      Path file = wysiwiki.resolve(wysiwikiFile);
      if (!Files.exists(file)) {
        // unzip it from our own jar file
        try {
          URL url = getClass().getResource("/wysiwiki/"+wysiwikiFile);
          Files.createDirectories(file.getParent());
          Files.copy(url.openStream(), file);
        } catch (Throwable t) {
          System.err.println("Couldn't extract /wysiwiki/" + wysiwikiFile + " : " + t);
        }
      } // doesn't exist
    } // next file

    // ensure customizable files are in place TODO
    String[] customizableFiles = {
      "template.html",
      "header.html",
      "footer.html",
      "style.css"
    };
    for (String name : customizableFiles) {
      Path file = root.resolve(name);
      if (!Files.exists(file)) {
        // copy the standard version
        Path standardVersion = wysiwiki.resolve(name);
        Files.copy(standardVersion, file);
      }
    } // next customizable file    

    loadIndex();
    return this;
  }
  /**
   * Setter for {@link #root}: Root directory of all content.
   * @param newRoot Root directory of all content.
   */
  public ContentManager setRoot(File newRoot) throws Exception {
    return setRoot(newRoot.toPath());
  }

  /** Path prefixes that are forbidden for reading */
  protected List<Path> readForbidden;
  
  /** Path prefixes that are forbidden for writing/deleting */
  protected List<Path> writeForbidden;
  
  /**
   * Default constructor.
   */
  public ContentManager() {
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    transformerFactory = TransformerFactory.newInstance();
    try {
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    } catch (Exception x) {
      System.err.println("ContentManager: " + x);
    }
    xpath = XPathFactory.newInstance().newXPath();
  } // end of constructor
  
  /**
   * Loads /index.html, creating it if required.
   * @throws Exception
   */
  protected void loadIndex() throws Exception {
    indexHtml = new File(root.toFile(), "index.html");
    if (!indexHtml.exists()) { // create index
      createIndex();
    }
    DocumentBuilder xmlParser = documentBuilderFactory.newDocumentBuilder();
    // the output may contain <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
    // which we strip out so the parser doesn't panic
    StringBuilder html = new StringBuilder();
    BufferedReader htmlReader = new BufferedReader(new FileReader(indexHtml));
    String line = htmlReader.readLine();
    String meta = "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">";
    while (line != null) {
      if (!line.trim().equals(meta)) {
        html.append(line);
      }
      line = htmlReader.readLine();
    } // next line
    index = xmlParser.parse(new ByteArrayInputStream(html.toString().getBytes()));
  } // end of loadIndex()
  
  /**
   * Creates the index.html file by traversing the root directory.
   * @throws Exception
   */
  protected void createIndex() throws Exception {

    // create the document and top node...
    DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
    index = docBuilder.newDocument();

    // create html preamble
    Element html = index.createElement("html");
    index.appendChild(html);
    Element head = index.createElement("head");
    html.appendChild(head);
    Element meta = index.createElement("meta");
    meta.setAttribute("http-equiv","content-type");
    meta.setAttribute("content","text/html; charset=UTF-8");
    meta.setTextContent(" ");
    head.appendChild(meta);
    meta = index.createElement("meta");
    meta.setAttribute("name","viewport");
    meta.setAttribute("content","width=device-width, initial-scale=1");
    meta.setTextContent(" ");
    head.appendChild(meta);
    Element base = index.createElement("base");
    base.setAttribute("target", "_top");
    base.setTextContent(" ");
    head.appendChild(base);
    Element title = index.createElement("title");
    title.setTextContent("Index"); // TODO title of home.hmtl?
    head.appendChild(title);
    Element link = index.createElement("link");
    link.setAttribute("rel","stylesheet");
    link.setAttribute("href","wysiwiki/wysiwiki.css");
    link.setAttribute("type","text/css");
    link.setTextContent(" ");
    head.appendChild(link);
    link = index.createElement("link");
    link.setAttribute("rel","stylesheet");
    link.setAttribute("href","style.css");
    link.setAttribute("type","text/css");
    link.setTextContent(" ");
    head.appendChild(link);
    Element script = index.createElement("script");
    script.setAttribute("src","wysiwiki/index.js");
    script.setTextContent("");
    head.appendChild(script);
    Element body = index.createElement("body");
    body.setAttribute("class","resource index");
    html.appendChild(body);
    
    // now the index body
    Element details = index.createElement("details");
    details.setAttribute("open", "true");
    File homePage = new File(root.toFile(), "home.html");
    Element a = index.createElement("a");
    a.setTextContent(title(homePage));
    a.setAttribute("href","home.html");
    Element summary = index.createElement("summary");
    summary.appendChild(a);
    summary.setAttribute("id", "/");
    details.appendChild(summary);
    body.appendChild(details);

    // traverse/index directory structure
    indexDirectory(details, root.toFile(), root.toFile(), "/");

    // write the file
    writeIndex();
    
  } // end of createIndex()
  
  /**
   * Write the index to index.html
   * @throws Exception
   */
  protected void writeIndex() throws Exception {
    DOMSource source = new DOMSource(index);
    PrintWriter indexWriter = new PrintWriter(new FileWriter(indexHtml));
    indexWriter.println("<!DOCTYPE html>");
    StreamResult result =  new StreamResult(indexWriter);
    Transformer xmlTransformer = transformerFactory.newTransformer();
    xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
    xmlTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    xmlTransformer.transform(source, result);
    indexWriter.close();
  } // end of writeIndex()
  
  /**
   * Indexes the given directory, inserting corresponding HTML into the given node.
   * @param parentDetails
   * @param dir
   * @throws Exception
   */
  protected void indexDirectory(Node parentDetails, File dir, File root, String idPrefix)
    throws Exception {
    File[] children = dir.listFiles(
      f -> f.getName().endsWith(".html")
      || (f.isDirectory() // directory 
          && f.listFiles(  // with html files
            ff -> ff.getName().endsWith(".html")).length > 0));
    Arrays.sort(children, Comparator.comparingLong(File::lastModified));
    for (File child : children) {
      if (dir.equals(root)
          // don't list the customizable files
          && (child.getName().equals("template.html")
              || child.getName().equals("header.html")
              || child.getName().equals("footer.html")
              // nor the home page
              || child.getName().equals("home.html")
              // nor the index
              || child.getName().equals("index.html")
              // nor the web app directories
              || child.getName().equals("WEB-INF")
              || child.getName().equals("META-INF")
              || child.getName().equals("wysiwiki"))) continue;
      addIndexItem(parentDetails, dir, child, idPrefix, root);
    } // next child
  } // end of indexDirectory()
  
  /**
   * Adds a file/directory item to the given node of the index.
   * @param parentDetails
   * @param dir
   * @param child
   */
  protected void addIndexItem(
    Node parentDetails, File dir, File child, String idPrefix, File root) throws Exception {
    if (child.isFile()) {
      // is there a directory with the name name?
      File dirWithSameName =  new File(dir, child.getName().replaceAll("\\.html$", ""));
      if (dirWithSameName.exists()) {
        return; // we'll process this when we process the directory
      }
      String url = idPrefix.replaceAll("^/","") + child.getName();
      String id = "/"+url.replaceAll("\\.html$", "");
      Element a = index.createElement("a");
      a.setTextContent(title(child));      
      a.setAttribute("href", url);
      Element div = index.createElement("div");
      div.appendChild(a);
      div.setAttribute("id", id);
      parentDetails.appendChild(div);
    } else if (child.isDirectory()) { // a directory
      String id = idPrefix + child.getName();
      Element details = index.createElement("details");
      File fileWithSameName = new File(dir, child.getName() + ".html");
      Element summary = index.createElement("summary");
      summary.setAttribute("id", id);
      if (fileWithSameName.exists()) { // use file title
        Element a = index.createElement("a");
        a.setTextContent(title(fileWithSameName));
        a.setAttribute("href", idPrefix.replaceAll("^/","") + fileWithSameName.getName());
        summary.appendChild(a);
      } else { // no file
        // show the directory name
        summary.appendChild(index.createTextNode(child.getName()));
        // but also include a link that would create the page
        Element a = index.createElement("a");
        a.setAttribute("class", "new-page"); // add a class so client can hide this if read-only
        a.setTextContent("+");
        a.setAttribute("href", idPrefix.replaceAll("^/","") + fileWithSameName.getName());
        summary.appendChild(a);
      }
      details.appendChild(summary);
      if (root != null) {
        indexDirectory(details, child, root, idPrefix + child.getName() + "/");
      }
      parentDetails.appendChild(details);
    } // a directory      
  } // end of addItem()
  
  /**
   * Adds/updates the given path to/in the index.
   * @param urlPath
   * @return true if the index was updated, false otherwise
   * @throws Exception
   */
  protected synchronized boolean indexPath(String urlPath) throws Exception {
    if (urlPath == null || urlPath.length() == 0) return false;
    if (urlPath.length() == 0 || urlPath.charAt(0) != '/') urlPath = "/"+urlPath;
    Path path = root.resolve(urlPath.replaceAll("^/","")).normalize();
    File child = path.toFile();
    boolean updated = false;
    String id = urlPath.replaceAll("\\.html$", "");
    if (id.equals("/home")) id = "/";
    Element item = (Element)xpath.evaluate(
      "//*[@id='"+id+"']", index, XPathConstants.NODE);
    if (item == null) { // item isn't there yet
      if (!child.exists()) return false; // no need to index it
      
      // add it
      String parentId = id.replaceAll("/[^/]*$","");
      if (parentId.length() == 0) { // we've reached root
        parentId = "/";
      }
      Element parentSummary = (Element)xpath.evaluate(
        "//*[@id='"+parentId+"']", index, XPathConstants.NODE);
      if (parentSummary == null) {
        indexPath(parentId);
        parentSummary = (Element)xpath.evaluate(
          "//*[@id='"+parentId+"']", index, XPathConstants.NODE);
      }
      Element parentDetails = (Element)parentSummary.getParentNode();
      if (parentSummary.getTagName().equals("div")) { // file entry is becoming a dir entry
        Element grandparentDetails = parentDetails;
        Element parentDiv = parentSummary;
        parentSummary = index.createElement("summary");
        Element a = (Element)xpath.evaluate("a", parentDiv, XPathConstants.NODE);
        parentDiv.removeChild(a);
        parentSummary.appendChild(a);
        parentDetails = index.createElement("details");
        parentDetails.appendChild(parentSummary);
        grandparentDetails.replaceChild(parentDetails, parentDiv);
        parentSummary.setAttribute("id", parentId);
      }
      if (parentId.equals("/")) parentId = "";
      addIndexItem(parentDetails, child.getParentFile(), child, parentId + "/", null);
      updated = true;
    } else { // item is already there
      // check it's the correct type and title...
      
      if (child.getName().endsWith(".html")) {
        Node parentDetails = item.getParentNode();
        String tagName = "div"; // should be a <div> tag
        // unless there's a non-empty directory with the same name
        File dirWithSameName = new File(
          child.getParentFile(), child.getName().replaceAll("\\.html$", ""));
        if (dirWithSameName.exists() && dirWithSameName.isDirectory()
            && dirWithSameName.listFiles(f -> f.getName().endsWith(".html")).length > 0) {
          // it should be a <details> tag
          tagName = "summary";
        }
        if (!tagName.equals(item.getTagName()) && !id.equals("/")) {
          // change tag name
          parentDetails.removeChild(item);
          if (!child.exists() && tagName.equals("div")) { // both dir and .html file deleted
            // delete the <details> tag              
            parentDetails.getParentNode().removeChild(parentDetails);
          } else {
            addIndexItem(
              parentDetails, child.getParentFile(), child, urlPath.replaceAll("[^/]+$",""),
              root.toFile());
          }
          updated = true;
        } else {
          if (child.exists()) {
            // check title
            String currentTitle = title(child);
            Element a = (Element)xpath.evaluate("a", item, XPathConstants.NODE);
            if (a != null && !currentTitle.equals(a.getTextContent())) {
              a.setTextContent(currentTitle);
              // ensure that if it was a new-page link before, it's not now
              if (a.getAttribute("class") != null) {
                a.removeAttribute("class");
                Node precedingText = a.getPreviousSibling();
                if (precedingText != null) {
                  item.removeChild(precedingText);
                }
              }
              updated = true;
            }
          } else { // doesn't exist
            if (tagName.equals("summary")) { // there's a dir with the same name
              Element a = (Element)xpath.evaluate("a", item, XPathConstants.NODE);
              if (a != null && !a.getTextContent().equals("+")) { // the summary is a link
                // replace link with the name of the directory
                item.setTextContent(dirWithSameName.getName());
                a = index.createElement("a");
                a.setAttribute("class", "new-page"); // add a class so client can hide this if read-only
                a.setTextContent("+");
                a.setAttribute("href", urlPath);
                item.appendChild(a);
                updated = true;
              }
            } else { // there's no dir with the same name
              // remove it from the index
              parentDetails.removeChild(item);
              updated = true;
            }
          }
        }
        if (updated) {
          // parent may also need indexing
          String parentUrl = urlPath.replaceAll("/[^/]+$",".html");
          if (!parentUrl.equals(".html")) {
            indexPath(parentUrl);
          }
        }
      } // .html file
    }
    return updated;
  } // end of indexPath()
  
  static final Pattern titlePattern = Pattern.compile(".*<title>(.*)</title>.*");
  /**
   * Gets the title of the given document file.
   * @param doc A .html document.
   * @return The contents of the &lt;title&gt; tag in the file, or the file name without
   * the suffix, if there is none.
   */
  public static String title(File html) {
    if (html.exists() && html.getName().endsWith(".html")) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(html));
        int l = 0;
        String title = null;
        String line = reader.readLine();
        while (line != null && ++l < 10 && title == null) { // read up to ten lines
          Matcher matcher = titlePattern.matcher(line);
          if (matcher.find()) {
            title = matcher.group(1);
            break;
          }
          line = reader.readLine();
        } // next line
        if (title != null && title.trim().length() > 0) {
          return title;
        }
      } catch(Exception exception) {
        //log("Doc.title("+html.getPath()+"): " + exception.toString());
      }
    } // file exists and is .html
    return html.getName().replaceAll("\\.html$", "");
  } // end of title()

  
  /**
   * Creates a file.
   * @param urlPath The slash-delimited path to the file.
   * @param content
   * @return The file created.
   * @throws IOException
   */
  public Path create(String urlPath, InputStream content) throws IOException {
    Path relativePath = root.getFileSystem().getPath(".", urlPath.split("/"));
    Path path = root.resolve(relativePath).normalize();
    if (!path.startsWith(root)) {
      // can only access under root
      throw new IOException("Cannot access files outside root.");
    }
    // check it's not forbidden
    for (Path forbidden : writeForbidden) {
      if (path.startsWith(forbidden)) {
        // can only access under root
        throw new IOException("Forbidden path: " + urlPath);
      }
    } // next forbidden path
    Files.createDirectories(path.getParent());
    Files.createFile(path);
    Files.copy(content, path, StandardCopyOption.REPLACE_EXISTING);
    
    if (urlPath.endsWith(".html")) {
      try {
        if (indexPath(urlPath)) {
          // write the file
          writeIndex();
        }
      } catch(Exception exception) {
        System.err.println("ContentManager.create: " + exception);
        exception.printStackTrace(System.err);
      }
    }
    
    return path;
  } // end of create()
  
  /**
   * Reads the content of a file.
   * @param urlPath The slash-delimited path to the file.
   * @return The content of the file.
   * @throws IOException
   */
  public InputStream read(String urlPath) throws IOException {
    Path relativePath = root.getFileSystem().getPath(".", urlPath.split("/"));
    Path path = root.resolve(relativePath).normalize();
    if (!path.startsWith(root)) {
      // can only access under root
      throw new IOException("Cannot access files outside root.");
    }
    // check it's not forbidden
    for (Path forbidden : readForbidden) {
      if (path.startsWith(forbidden)) {
        // can only access under root
        throw new IOException("Forbidden path: " + urlPath);
      }
    } // next forbidden path
    return new FileInputStream(path.toFile());
  } // end of read()
  
  /**
   * Updates a file.
   * @param urlPath The slash-delimited path to the file.
   * @param content
   * @return The file updated.
   * @throws IOException
   */
  public Path update(String urlPath, InputStream content) throws IOException {
    Path relativePath = root.getFileSystem().getPath(".", urlPath.split("/"));
    Path path = root.resolve(relativePath).normalize();
    if (!path.startsWith(root)) {
      // can only access under root
      throw new IOException("Cannot access files outside root.");
    }
    // check it's not forbidden
    for (Path forbidden : writeForbidden) {
      if (path.startsWith(forbidden)) {
        // can only access under root
        throw new IOException("Forbidden path: " + urlPath);
      }
    } // next forbidden path
    Files.copy(content, path, StandardCopyOption.REPLACE_EXISTING);
    
    if (urlPath.endsWith(".html")) {
      try {
        if (indexPath(urlPath)) {
          // write the file
          writeIndex();
        }
      } catch(Exception exception) {
        System.err.println("ContentManager.create: " + exception);
        exception.printStackTrace(System.err);
      }
    }
    
    return path;
  } // end of update()
  
  /**
   * Moves the given document in the index.
   * @param urlPath The slash-delimited path to the file, or its ID.
   * @param where Where to move the index item, "up" (before previous peer) or "down"
   * (after next peer).
   * @return true if the index location was moved, false otherwise.
   * @throws IOException
   */
  public boolean move(String urlPath, String where) {
    try {
      String id = urlPath.replaceAll("\\.html$", ""); // might be document instead of id
      if (!id.startsWith("/")) id = "/"+id;
      if (!id.equals("/")) { // not the home page
        Element item = (Element)xpath.evaluate(
          "//*[@id='"+id+"']", index, XPathConstants.NODE);
        if (item != null) {
          Element nodeToMove = item.getTagName().equals("div")?
            item // plain file - we just move the item itself
            : (Element)item.getParentNode(); // dir - summary, but we want to move the details
          Node parent = nodeToMove.getParentNode();
          Element toBeBefore = null;
          Element toBeAfter = null;          
          if ("up".equals(where)) { // move up
            toBeBefore = nodeToMove;
            // find the previous element, skipping text nodes
            Node sibling = nodeToMove.getPreviousSibling();
            while (sibling != null && !(sibling instanceof Element)) {
              sibling = sibling.getPreviousSibling();
            } // previous node
            if (sibling instanceof Element) toBeAfter = (Element)sibling;
          } else { // move down
            toBeAfter = nodeToMove;          
            // find the next element, skipping text nodes
            Node sibling = nodeToMove.getNextSibling();
            while (sibling != null && !(sibling instanceof Element)) {
              sibling = sibling.getNextSibling();
            } // previous node
            if (sibling instanceof Element) toBeBefore = (Element)sibling;
          }
          if (toBeBefore != null
              && toBeAfter != null
              && !toBeAfter.getTagName().equals("summary")) { // not the top of the directory
            parent.removeChild(toBeBefore);
            parent.insertBefore(toBeBefore, toBeAfter);
            writeIndex();
            return true;
          }
        } // id is in the index
      } // not home page
    } catch (Exception x) {
      System.err.println("ContentManager.move("+urlPath+", "+where+") : " + x);
    }
    return false;
  } // end of move()
  
  /**
   * Deletes a file.
   * @param urlPath The slash-delimited path to the file.
   * @return The file that was supposed to be deleted.
   * @throws IOException
   */
  public Path delete(String urlPath) throws IOException {
    Path relativePath = root.getFileSystem().getPath(".", urlPath.split("/"));
    Path path = root.resolve(relativePath).normalize();
    if (!path.startsWith(root)) {
      // can only access under root
      throw new IOException("Cannot access files outside root.");
    }
    // check it's not forbidden
    for (Path forbidden : writeForbidden) {
      if (path.startsWith(forbidden)) {
        // can only access under root
        throw new IOException("Forbidden path: " + urlPath);
      }
    } // next forbidden path
    Files.delete(path);
    
    if (urlPath.endsWith(".html")) {
      try {
        if (indexPath(urlPath)) {
          // write the file
          writeIndex();
        }
      } catch(Exception exception) {
        System.err.println("ContentManager.create: " + exception);
        exception.printStackTrace(System.err);
      }
    }
    return path;
  } // end of delete()

} // end of class ContentManager
