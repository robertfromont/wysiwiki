import InlineEditor from '@ckeditor/ckeditor5-editor-inline/src/inlineeditor';
import Essentials from '@ckeditor/ckeditor5-essentials/src/essentials';
import UploadAdapter from '@ckeditor/ckeditor5-adapter-ckfinder/src/uploadadapter';
import Autoformat from '@ckeditor/ckeditor5-autoformat/src/autoformat';
import Bold from '@ckeditor/ckeditor5-basic-styles/src/bold';
import Italic from '@ckeditor/ckeditor5-basic-styles/src/italic';
import Strikethrough from '@ckeditor/ckeditor5-basic-styles/src/strikethrough';
import Superscript from '@ckeditor/ckeditor5-basic-styles/src/superscript';
import Code from '@ckeditor/ckeditor5-basic-styles/src/code';
import HorizontalLine from '@ckeditor/ckeditor5-horizontal-line/src/horizontalline';
import BlockQuote from '@ckeditor/ckeditor5-block-quote/src/blockquote';
import CKFinder from '@ckeditor/ckeditor5-ckfinder/src/ckfinder';
import EasyImage from '@ckeditor/ckeditor5-easy-image/src/easyimage';
import Heading from '@ckeditor/ckeditor5-heading/src/heading';
import Image from '@ckeditor/ckeditor5-image/src/image';
import ImageCaption from '@ckeditor/ckeditor5-image/src/imagecaption';
import ImageStyle from '@ckeditor/ckeditor5-image/src/imagestyle';
import ImageToolbar from '@ckeditor/ckeditor5-image/src/imagetoolbar';
import ImageUpload from '@ckeditor/ckeditor5-image/src/imageupload';
import Indent from '@ckeditor/ckeditor5-indent/src/indent';
import Link from '@ckeditor/ckeditor5-link/src/link';
import List from '@ckeditor/ckeditor5-list/src/list';
import MediaEmbed from '@ckeditor/ckeditor5-media-embed/src/mediaembed';
import Paragraph from '@ckeditor/ckeditor5-paragraph/src/paragraph';
import PasteFromOffice from '@ckeditor/ckeditor5-paste-from-office/src/pastefromoffice';
import Table from '@ckeditor/ckeditor5-table/src/table';
import TableToolbar from '@ckeditor/ckeditor5-table/src/tabletoolbar';
import TextTransformation from '@ckeditor/ckeditor5-typing/src/texttransformation';
import CloudServices from '@ckeditor/ckeditor5-cloud-services/src/cloudservices';
import Video from './video/video';
import Alignment from '@ckeditor/ckeditor5-alignment/src/alignment';
import InsertFile from './insertfile/insertfile';

InlineEditor.builtinPlugins = [
    Essentials,
    UploadAdapter,
    Autoformat,
    Bold,
    Italic,
    Strikethrough,
    Superscript,
    Code,
    HorizontalLine,
    BlockQuote,
    CKFinder,
    CloudServices,
    EasyImage,
    Heading,
    Image,
    ImageCaption,
    ImageStyle,
    ImageToolbar,
    ImageUpload,
    Indent,
    Link,
    List,
    MediaEmbed,
    Paragraph,
    PasteFromOffice,
    Table,
    TableToolbar,
    TextTransformation,
    Video,
    InsertFile,
    Alignment
];
// Editor configuration.
InlineEditor.defaultConfig = {
    toolbar: {
	items: [
	    'heading',
	    '|',
	    'bold',
	    'italic',
	    'code',
	    'strikethrough',
	    'superscript',
	    'link',
	    'bulletedList',
	    'numberedList',
            'alignment',
            'horizontalLine',
	    '|',
	    'outdent',
	    'indent',
	    '|',
	    'uploadImage',
            'uploadVideo',
            'insertFile',
	    'blockQuote',
	    'insertTable',
	    'mediaEmbed',
	    'undo',
	    'redo'
	]
    },
    image: {
        upload: {
	    types: [ 'jpeg', 'png', 'gif', 'bmp', 'svg+xml' ]
        },
	toolbar: [
	    'imageStyle:inline',
	    'imageStyle:block',
	    'imageStyle:side',
	    '|',
	    'toggleImageCaption',
	    'imageTextAlternative'
	]
    },
    video: {
        upload: {
	    types: [ 'mp4', 'webm' ]
        },
    },
    table: {
	contentToolbar: [
	    'tableColumn',
	    'tableRow',
	    'mergeTableCells'
	]
    },
    alignment: {
        options: [ 'left', 'center', 'right' ]
    },
    // This value must be kept in sync with the language defined in webpack.config.js.
    language: 'en'
};

let editable = false;
let creating = false;
let editButton = null;
let deleteButton = null;
let postButton = null;
let articleEditor = null;
let loginButton = null;
const editLabel = "✏️";
const createLabel = "➕";
const saveLabel = "💾";
const postLabel = "📅";
const deleteLabel = "❌";
const loginLabel = "🔓";

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
    if (articleEditor) {
        articleEditor.disableReadOnlyMode('wysiwiki');
        editingEnabled();
    } else { // not already created
        if (creating) {
            // add some default content
            let title = decodeURI(document.URL) // file name by default
                .replace(/.*\//, "")
                .replace(/\.html$/, "");
            // if there's a hash, assume it's intended to be the title
            if (document.location.hash && document.location.hash != "#") {
                title = decodeURI( // Convert %20 to space, etc.
                    document.location.hash
                        .replace(/^#/,"")); // remove initial #
            }

            if (/\t/.test(title)) { // title with subtitle
                const titles = title.split("\t");
                document.querySelector("#main>article").innerHTML
                    = `<h2>${titles[0]}</h2><h3>${titles[1]}</h3><p></p>`;
            } else {
                document.querySelector("#main>article").innerHTML
                    = `<h2>${title}</h2><p></p>`;
            }
            document.querySelector("title").innerText = title;
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
            articleEditor = editor;
            editingEnabled();
        });
    } // not already created
}

function editingEnabled() {
    editButton.innerHTML = creating?createLabel:saveLabel;
    editButton.title = creating?"Create Page":"Save Page";
    editButton.onclick = savePage;
    // ensure the user can start typing immediately
    articleEditor.focus();
    // put cursor at the end
    articleEditor.model.change( writer => {
        writer.setSelection(
            writer.createPositionAt(
                articleEditor.model.document.getRoot(), 'end' ) );
    });
}

function savePage() {
    // get template
    let oReq = new XMLHttpRequest();
    oReq.open("GET", "template.html");
    oReq.addEventListener("load", function(e) {
        let html = this.responseText;
        
        // determine the document title
        let title = decodeURI( // Convert %20 to space, etc.
            document.URL) // file name by default
            .replace(/.*\//, "")
            .replace(/\.html$/, "");
        let heading = document.querySelector("#main>article>h1")
            || document.querySelector("#main>article>h2");
        if (heading) title = heading.innerText; 
        title = title.replace(/^\*+/,""); // remove leading stars

        // determine the href
        const href = document.querySelector("base").getAttribute("href");
        
        // get the article content
        const article = articleEditor.getData()
        // add newlines to make HTML source easier to diff (in case we're using git, etc.)
              .replace(/<h(\d)?/g,"\n<h$1") // no indent
              .replace(/<(p|ul|ol|dl|div|figure|tr)([ >])?/g,"\n <$1$2") // one space indent
              .replace(/<(li|dt|dd|br)([ >])?/g,"\n  <$1$2");
        
        // insert the article content into it
        html = html
            .replace(/<article>.*<\/article>/, "<article>"+article+"</article>")
        // set the title
            .replace(/<title>[^<]*<\/title>/, "<title>"+title+"</title>")
        // set the base href
            .replace(/<base[^>]*>/, `<base href="${href}">`);

        oReq = new XMLHttpRequest();
        oReq.addEventListener("load", function(e) {
            // reload navigation
            window.nav.location.reload(true);
            // show buttons in index too, giving time for it to reload
            setTimeout(()=>{
                window.nav.postMessage("editable", "*");
            }, 1500);
            
            // set the title locally
            document.querySelector("title").innerText = title;
            
            // stop editing
            articleEditor.enableReadOnlyMode('wysiwiki');
            editButton.innerHTML = editLabel;
            editButton.title = "Edit Page";
            editButton.onclick = editPage;
            
            // show delete and post buttons
            deleteButton.className = "";
            postButton.className = "";

            creating = false;
        });
        oReq.addEventListener("error", function(r) {
            console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
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
            console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
        });
        oReq.open("DELETE", document.URL);
        oReq.setRequestHeader("Content-Type", "text/html");
        oReq.send()
    } // are you sure?
}

function newPost() {
    let title = prompt("Title of new post");
    if (title == null) return; // cancelled
    
    const postParent = document.URL
          .replace(/#.*/,"") // get rid of hash
          .replace(/[0-9/]+\/[^/]*$/,"") // look back through the URL for a non-digit parent
          .replace(/\.html/,"") // if we're on the parent now, remove the .html
          .replace(/\/home/,"") // if we're on the home page, use the base URL as the parnt
    const now = new Date()
    const dateParts = now.toDateString().split(" ");

    const ensureYearPageExists = new Promise((resolve, reject) => {
        // posts are created under yyyy/mm/... so we make sure that the year directory exists
        const yearPath = now.toISOString().substring(0,4) + ".html";
        const yearUrl = `${postParent}/${yearPath}`;
        let oYearReq = new XMLHttpRequest();
        oYearReq.open("GET", yearUrl);
        oYearReq.addEventListener("load", function(e) {
            if (this.status == 404) { // page doesn't exist yet
                // create the page
                let yearHtml = this.responseText;
                // ...with just the year name as the title
                const yearName = now.toISOString().substring(0,4);
                yearHtml = yearHtml
                    .replace(/<article>.*<\/article>/, `<article><h2>${yearName}</h2></article>`)
                    .replace(/<title>[^<]*<\/title>/, `<title>${yearName}</title>`);
                // create the document TODO
                oYearReq = new XMLHttpRequest();
                oYearReq.open("PUT", yearUrl);
                oYearReq.addEventListener("load", function(e) {
                    if (this.status == 200) {
                        resolve();
                    } else {
                        reject();
                    }
                });
                oYearReq.send(yearHtml);
            } else {
                resolve();
            }
        });
        oYearReq.send();
    });
    const ensureMonthPageExists = new Promise((resolve, reject) => {
        // posts are created under yyyy/mm/... so we make sure that the mont directory exists
        const monthPath = now.toISOString().replace(/-/g,"/").substring(0,7) + ".html";
        const monthUrl = `${postParent}/${monthPath}`;
        let oMonthReq = new XMLHttpRequest();
        oMonthReq.open("GET", monthUrl);
        oMonthReq.addEventListener("load", function(e) {
            if (this.status == 404) { // page doesn't exist yet
                // create the page
                let monthHtml = this.responseText;
                // ...with just the month name as the title
                const monthName = dateParts[1];
                monthHtml = monthHtml
                    .replace(/<article>.*<\/article>/, `<article><h2>${monthName}</h2></article>`)
                    .replace(/<title>[^<]*<\/title>/, `<title>${monthName}</title>`);
                // create the document TODO
                oMonthReq = new XMLHttpRequest();
                oMonthReq.open("PUT", monthUrl);
                oMonthReq.addEventListener("load", function(e) {
                    if (this.status == 200) {
                        resolve();
                    } else {
                        reject();
                    }
                });
                oMonthReq.send(monthHtml);
            } else {
                resolve();
            }
        });
        oMonthReq.send();
    });
    
    // default path is yyyy/mm/dd-hh.mm
    let postPath = now
        .toISOString().replace(/-/g,"/").replace(/T/,"-").replace(/:/g,".").substring(0,16)
        + ".html";
    if (title) { // use title instead of time
        // i.e. yyyy/mm/dd-slugged-title
        postPath = now.toISOString().replace(/-/g,"/").substring(0,10)
            + "-" + slugify(title)
            + ".html";
    }
    
    const timeWithoutSeconds = now.toTimeString().replace(/:[^:]*$/,"");
    if (title) { // append timestamp, delimited by tab %09
         // "Mon 28 Feb 2022, 13:12" 
        title += `%09${dateParts[0]} ${dateParts[2]} ${dateParts[1]}  ${dateParts[3]}, ${timeWithoutSeconds}`;
    } else { // use timestamp as title
        // "Mon 28, 13:12" - short version because it appears in the index, long titles are annoying
        title = `${dateParts[0]} ${dateParts[2]}, ${timeWithoutSeconds}`; 
    }
    
    let url = `${postParent}/${postPath}`;
    if (title) {
        url += `#${title}`;
    }
    ensureYearPageExists.then(()=>{
        ensureMonthPageExists.then(()=>{
            window.top.location = url;
        });
    });
}

// Slugify a string
// thanks to https://lucidar.me/en/web-dev/how-to-slugify-a-string-in-javascript/
function slugify(str) {
    str = str.replace(/^\s+|\s+$/g, '');

    // Make the string lowercase
    str = str.toLowerCase();

    // Remove accents, swap ñ for n, etc
    var from = "ÁÄÂÀÃÅČÇĆĎÉĚËÈÊẼĔȆÍÌÎÏŇÑÓÖÒÔÕØŘŔŠŤÚŮÜÙÛÝŸŽáäâàãåčçćďéěëèêẽĕȇíìîïňñóöòôõøðřŕšťúůüùûýÿžþÞĐđßÆa·/_,:;";
    var to   = "AAAAAACCCDEEEEEEEEIIIINNOOOOOORRSTUUUUUYYZaaaaaacccdeeeeeeeeiiiinnooooooorrstuuuuuyyzbBDdBAa------";
    for (var i=0, l=from.length ; i<l ; i++) {
        str = str.replace(new RegExp(from.charAt(i), 'g'), to.charAt(i));
    }

    // Remove invalid chars
    str = str.replace(/[^a-z0-9 -]/g, '') 
    // Collapse whitespace and replace by -
    .replace(/\s+/g, '-') 
    // Collapse dashes
    .replace(/-+/g, '-'); 

    return str;
}

window.addEventListener("message", function(e) {
    // message that was passed from iframe page
    const message = e.data;
    if (message.resource) {
        const resource = message.resource;
        for (let iframe of document.querySelectorAll(`iframe[src=\"${resource}\"]`)) {    
            iframe.style.height = message.height + 'px';
        }
        if (editable) { // maybe we already know if the index is editable
            // let index know it can add buttons
            window.nav.postMessage("editable", "*");
        }
    }
}, false);

window.addEventListener("load", function(e) {
    if (/^file:/.test(window.location.href)) { // loading from file:// URL
        // ensure index can know the URL of its parent, for menu expansion
        document.getElementById("nav").src += `#${window.location.href}`;
    }
    
    // determine whether they can update the page by makeing an OPTIONS request
    let oReq = new XMLHttpRequest();
    oReq.addEventListener("load", function(e) {
        const aside = document.querySelector("aside");

        // can they update the page?
        if (oReq.getResponseHeader("Allow").includes("PUT")) {
            editable = true;

            // Add edit button
            editButton = document.createElement("button");
            editButton.id = "edit"
            editButton.innerHTML = editLabel;
            editButton.title = "Edit Page"
            editButton.onclick = editPage;
            aside.appendChild(editButton);
            
            creating = document.querySelector("title").innerText.startsWith("*");

            // let index know it can add buttons, if it's already loaded
            window.nav.postMessage("editable", "*");
        }

        // can they delete the page?
        if (oReq.getResponseHeader("Allow").includes("DELETE")) {
            // Add delete button
            deleteButton = document.createElement("button");
            deleteButton.id = "delete"
            if (creating) {
                deleteButton.className = "hidden";
            }
            deleteButton.innerHTML = deleteLabel;
            deleteButton.title = "Delete Page"
            deleteButton.onclick = deletePage;
            aside.appendChild(deleteButton);            
        }

        if (oReq.getResponseHeader("Allow").includes("PUT")) {
            // Create post button (after delete button)
            postButton = document.createElement("button");
            postButton.id = "post"
            if (creating) {
                postButton.className = "hidden";
            }
            postButton.innerHTML = postLabel;
            postButton.title = "New post for the current date/time";
            postButton.onclick = function() {
                newPost();
            };
            aside.appendChild(postButton);
        }

        // can they log in?
        if (oReq.getResponseHeader("Allow").includes("LOGIN")) {
            // Add log button
            loginButton = document.createElement("button");
            loginButton.id = "login"
            loginButton.innerHTML = loginLabel;
            loginButton.title = "Log In"
            loginButton.onclick = () => window.location.href = "./login";
            aside.appendChild(loginButton);
        }
        
        if (creating) {
            window.setTimeout( () => {
                editPage();
            }, 2000); // wait a couple of seconds to allow ckeditor5Script to load
        }
    }); // load OPTIONS request content
    oReq.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    oReq.open("OPTIONS", document.URL);
    oReq.send();
}, false);

// ensure they don't accidentally navigate away without saving
window.addEventListener("beforeunload", function(event) {
    if (articleEditor && !articleEditor.isReadOnly) {
        event.preventDefault();
        return "You have not saved your changes.";
    }
});
