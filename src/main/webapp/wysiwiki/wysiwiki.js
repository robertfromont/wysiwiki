let creating = false;
let editButton = null;
let deleteButton = null;
let articleEditor = null;
const editLabel = "Edit";
const createLabel = "Create";
const saveLabel = "Save";
const deleteLabel = "Delete";

class WysiwikiUploadAdapter {
    constructor( loader ) {
        // The file loader instance to use during the upload.
        this.loader = loader;
    }

    // Starts the upload process.
    upload() {
        return this.loader.file
            .then( file => new Promise( ( resolve, reject ) => {
                this._initRequest(file);
                this._initListeners( resolve, reject, file );
                this._sendRequest( file );
            } ) );
    }

    // Aborts the upload process.
    abort() {
        if ( this.xhr ) {
            this.xhr.abort();
        }
    }
        // Initializes the XMLHttpRequest object using the URL passed to the constructor.
    _initRequest(file) {
        const xhr = this.xhr = new XMLHttpRequest();
        
        // Note that your request may look different. It is up to you and your editor
        // integration to choose the right communication channel. This example uses
        // a POST request with JSON as a data structure but your configuration
        // could be different.
        xhr.open("POST", new URL(file.name, document.URL), true);
        //TODO delete xhr.open("POST", `${baseUrl}/../browser`, true);
        xhr.responseType = 'text';
	xhr.setRequestHeader("Accept", "text/plain");
    }
    
    // Initializes XMLHttpRequest listeners.
    _initListeners( resolve, reject, file ) {
        const xhr = this.xhr;
        const loader = this.loader;
        const genericErrorText = `Couldn't upload file: ${ file.name }.`;
        
        xhr.addEventListener( 'error', () => reject( xhr.responseText ) );
        xhr.addEventListener( 'abort', () => reject() );
        xhr.addEventListener( 'load', () => {
            console.log("upload: " + xhr.responseText);
            // This example assumes the XHR server's "response" object will come with
            // an "error" which has its own "message" that can be passed to reject()
            // in the upload promise.
            //
            // Your integration may handle upload errors in a different way so make sure
            // it is done properly. The reject() function must be called when the upload fails.
            // if ( !response || response.error ) {
            //     return reject( response && response.error ? response.error.message : genericErrorText );
            // }
            
            // If the upload is successful, resolve the upload promise with an object containing
            // at least the "default" URL, pointing to the image on the server.
            // This URL will be used to display the image in the content. Learn more in the
            // UploadAdapter#upload documentation.
            // TODO for some reason the "default" servlet takes time to realise the file is there - fix this!
            // window.setTimeout(()=>{ 
                resolve( {
                    default: xhr.responseText
                    //TODO delete                default: response.url
                } );
            // }, 5000);
        } );
        
        // Upload progress when it is supported. The file loader has the #uploadTotal and #uploaded
        // properties which are used e.g. to display the upload progress bar in the editor
        // user interface.
        if ( xhr.upload ) {
            xhr.upload.addEventListener( 'progress', evt => {
                if ( evt.lengthComputable ) {
                    loader.uploadTotal = evt.total;
                    loader.uploaded = evt.loaded;
                }
            } );
        }
    }
    
    // Prepares the data and sends the request.
    _sendRequest( file ) {
        // Prepare the form data.
        const data = new FormData();
        
        data.append( 'upload', file );
        
        // Important note: This is the right place to implement security mechanisms
        // like authentication and CSRF protection. For instance, you can use
        // XMLHttpRequest.setRequestHeader() to set the request headers containing
        // the CSRF token generated earlier by your application.
        
        // Send the request.
        this.xhr.send( data );
    }
}

function editPage() {
    if (creating) {
        // add some default content
        let title = document.URL // file name by default
            .replace(/.*\//, "")
            .replace(/\.html$/, "");
        // if there's a hash, assume it's intended to be the title
        if (document.location.hash && document.location.hash != "#") {
            title = decodeURI( // Convert %20 to space, etc.
                document.location.hash
                    .replace(/^#/,"")); // remove initial #            
        }

        document.querySelector("#main>article").innerHTML
            = `<h2>${title}</h2><p>Add page text here.</p>`;
        document.querySelector("title").innerText = title;
    } else {
        // show delete button
        deleteButton.style = "display: inherit;";
    }
    InlineEditor.create(document.querySelector("#main>article"), {
        //toolbar:
        link: {
            // Automatically add target="_blank" and rel="noopener noreferrer" to all external links.
            addTargetToExternalLinks: true,            
        //    // Let the users control the "download" attribute of each link.
        //    decorators: [{mode: 'manual', label: 'Downloadable', attributes: {download: 'download'}}]
        }
    }).catch( error => {
        console.error( error );
    }).then((editor) => {
        editor.plugins.get( 'FileRepository' ).createUploadAdapter = ( loader ) => {
            return new WysiwikiUploadAdapter( loader );
        };        
        editButton.innerHTML = creating?createLabel:saveLabel;
        editButton.onclick = savePage;
        editor.focus();
        articleEditor = editor;
    });
}

function savePage() {
    // get template
    let oReq = new XMLHttpRequest();
    oReq.open("GET", "template.html"); // TODO baseURL
    oReq.addEventListener("load", function(e) {
        let html = this.responseText;
        
        // determine the document title
        let title = document.URL // file name by default
            .replace(/.*\//, "")
            .replace(/\.html$/, "");
        let heading = document.querySelector("#main>article>h1")
            || document.querySelector("#main>article>h2");
        if (heading) title = heading.innerText; 
        title = title.replace(/^\*+/,""); // remove leading stars

        // determine the href
        const href = document.querySelector("base").getAttribute("href");
        
        // get the article content
        const article = articleEditor.getData();
        
        // insert the article content into it
        html = html
            .replace(/<article>.*<\/article>/, "<article>"+article+"</article>")
        // set the title
            .replace(/<title>[^<]*<\/title>/, "<title>"+title+"</title>")
        // set the base href
            .replace(/<base[^>]*>/, `<base href="${href}">`);

        oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function(e) {
            console.log("Response: " + this.responseText);
            // reload navigation
            window.frames["nav"].location.reload();
            
            // TODO if (creating) {
            //     alert("Created");
            //     loadNavigation();
            // } else {
            //     alert("Saved");
            // }
            // set the title locally
            document.querySelector("title").innerText = title;
            
            // stop editing
            articleEditor.destroy();
            articleEditor = null;            
            editButton.innerHTML = editLabel;
            editButton.onclick = editPage;
            
            // show delete button
            deleteButton.style = "display: inherit;";

            creating = false;
        });
        oReq.addEventListener("error", function(r) {
            alert(`${r.status}: ${r.statusText}\n${r.responseText}`);
        });
        oReq.open("PUT", document.URL);
        oReq.setRequestHeader("Content-Type", "text/html");
        oReq.send(html)
        
    });
    oReq.send();
}

function deletePage() {
    if (confirm("Are you sure you want to delete this page?")) {
        if (articleEditor) {
            articleEditor.destroy();
            articleEditor = null;
        }
        let oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function(e) {
            console.log("Response: " + this.responseText);
            alert("Deleted");
            window.location.href = new URL(".", document.URL);
        });
        oReq.addEventListener("error", function(r) {
            alert(`${r.status}: ${r.statusText}\n${r.responseText}`);
        });
        oReq.open("DELETE", document.URL);
        oReq.setRequestHeader("Content-Type", "text/html");
        oReq.send()
    } // are you sure?
}

window.addEventListener("message", function(e) {
    // message that was passed from iframe page
    const message = e.data;
    const resource = message.resource;
    for (let iframe of document.querySelectorAll(`iframe[src=\"${resource}\"]`)) {    
        iframe.style.height = message.height + 'px';
    }
}, false);

window.addEventListener("load", function(e) {
    const aside = document.querySelector("aside");
    
    // Add edit button
    editButton = document.createElement("button");
    editButton.id = "edit"
    editButton.innerHTML = editLabel;
    editButton.onclick = editPage;
    aside.appendChild(editButton);
    
    // Add delete button
    deleteButton = document.createElement("button");
    deleteButton.id = "delete"
    deleteButton.style = "display: none;";
    deleteButton.innerHTML = deleteLabel;
    deleteButton.onclick = deletePage;
    aside.appendChild(deleteButton);
    
    creating = document.querySelector("title").innerText.startsWith("*");
    if (creating) editPage();
}, false);

// ensure they don't accidentally navigate away without saving
window.addEventListener("beforeunload", function() {
    if (articleEditor) {
        return "You have not saved your changes.";
    }
});
