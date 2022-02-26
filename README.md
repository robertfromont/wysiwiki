# wysiwiki

A very simple site management system with indexed static content.

Features:

- Creating pages wiki-style - i.e. navigating to a non-existing page allows its creation
- Page editing is WYSIWYG (what you see is what you get) via CKeditor 5
- Pages are automatically indexed
- The index is a hyperlinked, navigable component available on all pages
- All content is stored as plain files on the file system
- All pages function (albeit read-only) when loaded directly from the file-system - i.e.
  double-click on the file on disk, and you get the same navigable view you would get
  via a web server

Create/Read/Update/Delete operations and indexing are managed on the back end with a Java servlet.

## Building

1. Ensure you've got Mavin installed.
1. Run the following command in the base directory:  
   `mvn package`

This will build a web application archive: `target/wysiwiki.war`

## Installation

1. Drop *wysiwiki.war* into Tomcat's webapps directory.
1. Edit Tomcat's *server.xml*, inserting the following into the `<GlobalNamingResources>` tag:
```
    <Resource name="wysiwiki-users" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/wysiwiki-users.xml"
	      readonly="false"/>
```
1. Restart Tomcat.
1. Browse to `http://${hostname}/wysiwiki`

The default author username is `wysiwiki` with a password `wysiwiki`.

