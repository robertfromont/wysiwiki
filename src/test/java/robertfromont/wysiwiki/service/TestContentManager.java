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

import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;

/**
 * Tests ContentManager.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TestContentManager {

  static ContentManager manager;

  @BeforeClass
  public static void createContentManager() throws Exception {
    File root = dir();
    // ensure generated files don't already exist
    File[] toDelete = {
      new File(root, "template.html"),
      new File(root, "header.html"),
      new File(root, "footer.html"),
      new File(root, "style.css"),
      new File(root, "index.html"),
      new File(new File(root, "test"), "test.html"),
      new File(new File(root, "test"), "peer.html"),
      new File(new File(new File(root, "test"), "test"), "test.html"),
      new File(new File(root, "test"), "test"),      
    };
    for (File f : toDelete) f.delete();
    // create manager
    manager = new ContentManager().setRoot(root);
  }

  public static File dir() throws Exception { 
    URL urlThisClass = ContentManager.class.getResource(
      TestContentManager.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass
      .getParentFile() // service
      .getParentFile() // wysiwiki
      .getParentFile() // robertfromont
      .getParentFile(); // test-classes
  }

  /** Index was created. */
  @Test public void indexHtml() throws Exception {
    String[] expectedContentArray = {
      "<!DOCTYPE html>",
      "<html>",
      "    <head>",
      "        <META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">",
      "        <meta content=\"text/html; charset=UTF-8\" http-equiv=\"content-type\"> </meta>",
      "        <meta content=\"width=device-width, initial-scale=1\" name=\"viewport\"> </meta>",
      "        <base target=\"_top\"> </base>",
      "        <title>Index</title>",
      "        <link href=\"wysiwiki/wysiwiki.css\" rel=\"stylesheet\" type=\"text/css\"> </link>",
      "        <link href=\"style.css\" rel=\"stylesheet\" type=\"text/css\"> </link>",
      "        <script src=\"wysiwiki/index.js\"></script>",
      "    </head>",
      "    <body class=\"resource index\">",
      "        <details open=\"true\">",
      "            <summary id=\"/\">",
      "                <a href=\"home.html\">home</a>",
      "            </summary>",
      "            <details>",
      "                <summary id=\"/subdir\">",
      "                    <a href=\"subdir.html\">subdir</a>",
      "                </summary>",
      "                <div id=\"/subdir/child\">",
      "                    <a href=\"subdir/child.html\">child</a>",
      "                </div>",
      "                <details>",
      "                    <summary id=\"/subdir/subsubdir\">subsubdir</summary>",
      "                    <div id=\"/subdir/subsubdir/grandchild\">",
      "                        <a href=\"subdir/subsubdir/grandchild.html\">grandchild</a>",
      "                    </div>",
      "                </details>",
      "            </details>",
      "        </details>",
      "    </body>",
      "</html>"
    };
    List<String> expectedContent = Arrays.asList(expectedContentArray);
    File indexHtml = new File(dir(), "index.html");
    assertTrue("index.html exists", indexHtml.exists());
    List<String> content = Files.readAllLines(indexHtml.toPath());
    Iterator<String> expected = expectedContent.iterator();
    Iterator<String> actual = content.iterator();
    int l = 0;
    while (expected.hasNext() && actual.hasNext()) {
      assertEquals("Line "+(++l)+" content correct", expected.next(), actual.next());
    }
    assertEquals("Correct number of lines", expectedContent.size(), content.size());
    
  }
  
  /** Ensure customizable files are created. */
  @Test public void customizableFilesCreated() throws Exception {
    File root = dir();
    File[] customizableFiles = {
      new File(root, "template.html"),
      new File(root, "header.html"),
      new File(root, "footer.html"),
      new File(root, "style.css")
    };
    for (File f : customizableFiles) {
      assertTrue(f.getName() + " exists", f.exists());
    }
  }

  /** Ordinary files, and wysiwiki files, can be read. */
  @Test public void readOk() {
    String[] paths = {
      "/template.html",
      "/home.html",
      "/subdir.html",
      "/subdir/child.html",
      "/wysiwiki/index.html",
      "/wysiwiki/resource.js",
      "/wysiwiki/wysiwiki.css",
      "/wysiwiki/wysiwiki.js"      
    };
    for (String path : paths) {
      try {        
        BufferedReader r = new BufferedReader(new InputStreamReader(
                                                manager.read(path), "UTF-8"));
        try {
          String line = r.readLine();
          assertEquals("Correct file", path, line);
        } finally {
          r.close();
        }
        
      } catch (Exception x) {
        fail("Could not read " + path + ": " + x);
      }

      String pathNoSlash = path.substring(1);
      try {        
        BufferedReader r = new BufferedReader(new InputStreamReader(
                                                manager.read(pathNoSlash), "UTF-8"));
        try {
          String line = r.readLine();
          assertEquals("Correct file", path, line);
        } finally {
          r.close();
        }
      } catch (Exception x) {
        fail("Could not read " + pathNoSlash + ": " + x);
      }
    } // next path
  }

  /** Webapp configuration files, and files outside root, can't be read. */
  @Test public void readForbidden() {
    String[] paths = {
      "/META-INF/context.xml",
      "/WEB-INF/web.xml",
      "/../../pom.xml",
      "/WEB-INF/../../../pom.xml"
    };
    for (String path : paths) {
      try {        
        manager.read(path).close();
        fail("Successfully read forbidden path " + path);
      } catch (Exception x) {
      }

      path = path.substring(1);
      try {        
        manager.read(path);
        fail("Successfully read forbidden path (no leading slash) " + path);
      } catch (Exception x) {
      }

    } // next path
  }
  
  /** Webapp configuration files, wysiwiki files, and files outside root, can't be read. */
  @Test public void writeForbidden() {
    String[] paths = {
      "/META-INF/context.xml",
      "/WEB-INF/web.xml",
      "../../pom.xml",
      "/WEB-INF/../../../pom.xml",
      "/wysiwiki/index.html",
      "/wysiwiki/iframe.js",
      "/wysiwiki/wysiwiki.css",
      "/wysiwiki/wysiwiki.js",
    };
    for (String path : paths) {
      try {        
        manager.update(path, new ByteArrayInputStream(path.getBytes()));
        fail("Successfully wrote forbidden path " + path);
      } catch (Exception x) {
      }
    } // next path
  }
  
  /** Normal documents can be written. */
  @Test public void writeOk() {
    String[] paths = {
      "/home.html",
      "/subdir.html",
      "/subdir/child.html",
      "/style.css",
    };
    for (String path : paths) {
      try {        
        manager.update(path, new ByteArrayInputStream(path.getBytes()));
      } catch (Exception x) {
        fail("Could not write " + path + ": " + x);
      }
      
      String pathNoSlash = path.substring(1);
      try {        
        manager.update(pathNoSlash, new ByteArrayInputStream(path.getBytes()));
      } catch (Exception x) {
        fail("Could not write " + pathNoSlash + ": " + x);
      }
    } // next path
  }
  
  /** Create/Read/Update/Delete lifecycle works, and all changes affect index. */
  @Test public void crudIndexing() throws Exception {
    String path =  "/test/test.html";
    String path2 = "/test/test/test.html";
    String path3 =  "/test/peer.html";
    // ensure file doesn't exist
    File file = new File(new File(dir(), "test"), "test.html");
    File file2 = new File(new File(new File(dir(), "test"), "test"), "test.html");
    File file3 = new File(new File(dir(), "test"), "peer.html");
    file.delete();
    file2.delete();
    file3.delete();

    // doesn't exist yet
    try {        
      manager.read(path).close();
      fail("Test file should not exist yet, but was read: " + path);
    } catch (Exception x) {
    }

    // isn't in index
    XPath xpath = XPathFactory.newInstance().newXPath();
    Element item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertNull("File isn't indexed yet", item);

    // can be created
    manager.create(path, new ByteArrayInputStream("<title>create</title>".getBytes()));    
    
    // can be read
    BufferedReader r = new BufferedReader(new InputStreamReader(
                                            manager.read(path), "UTF-8"));
    try {
      assertEquals("Created with correct content", "<title>create</title>", r.readLine());
    } finally {
      r.close();
    }

    // has been indexed
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertNotNull("File now indexed", item);
    assertEquals("Title in index", "create", item.getTextContent());
    
    // can't be created again
    try {
      manager.create(path, new ByteArrayInputStream("<title>create again</title>".getBytes()));
      fail("Test file was created, but it already exists");
    } catch (Exception x) {
    }

    // can be updated
    manager.update(path, new ByteArrayInputStream("<title>update</title>".getBytes()));

    // check it was updated
    r = new BufferedReader(new InputStreamReader(
                             manager.read(path), "UTF-8"));
    try {
      assertEquals("Updated to correct content", "<title>update</title>", r.readLine());
    } finally {
      r.close();
    }

    // index has been updated
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertNotNull("File still indexed", item);
    assertEquals("New title in index", "update", item.getTextContent());

    // can be updated again
    manager.update(path, new ByteArrayInputStream("<title>update 2</title>".getBytes()));

    // check it was updated<title>create</title>
    r = new BufferedReader(new InputStreamReader(
                             manager.read(path), "UTF-8"));
    try {
      assertEquals("Updated to correct content", "<title>update 2</title>", r.readLine());
    } finally {
      r.close();
    }
    
    // index has been updated again
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertNotNull("File still indexed", item);
    assertEquals("New title in index", "update 2", item.getTextContent());

    // convert into a directory
    assertEquals("Index item is div before adding child", "div", item.getTagName());
    manager.create(path2, new ByteArrayInputStream("child".getBytes()));    

    // index item changed
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertEquals("Index item is details after adding child", "summary", item.getTagName());

    // create peer in /test, so it's not deleted
    manager.create(path3, new ByteArrayInputStream("peer".getBytes()));    

    // can be deleted
    manager.delete(path);

    // can no longer be read
    try {        
      manager.read(path).close();
      fail("Test file should not exist any more, but was read: " + path);
    } catch (Exception x) {
    }

    // index item is still there, but named after directory
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertEquals("Directory name in index", "test", item.getTextContent());

    // delete child
    manager.delete(path2);
    item = (Element)xpath.evaluate(
      "//*[@id='/test/test']", manager.index, XPathConstants.NODE);
    assertNull("File no longer indexed", item);
    
    manager.delete(path3); // be tidy
    item = (Element)xpath.evaluate(
      "//*[@id='/test']", manager.index, XPathConstants.NODE);
    assertNull("Directory no longer indexed", item);
  }
}
