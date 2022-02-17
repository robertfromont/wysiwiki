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

