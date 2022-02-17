let creating = false;
let editButton = null;
let articleEditor = null;
const editLabel = "Edit";
const createLabel = "Create";
const saveLabel = "Save";

function editPage() {
    creating = document.querySelector("title").innerText.startsWith("*");
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
        // editor.plugins.get( 'FileRepository' ).createUploadAdapter = ( loader ) => {
        //     return new LabbcatUploadAdapter( loader );
        // };        
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
        
        // update title in menu
        //TODO $(`#${idInMenu} > a`).html(title);
        
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
            //TODO $("#delete").show();
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

/*
function deletePage() {
    if (confirm("Are you sure you want to delete this page?")) {
        if (articleEditor) {
            articleEditor.destroy();
            articleEditor = null;
        }
        $.ajax({
            type: "DELETE",
            url: document.URL,
            contentType: "text/html"
        }).done(data=>{
            alert("Deleted");
            window.location.href = new URL(".", document.URL);
        }).fail(r=>{
            alert(`${r.status}: ${r.statusText}\n${r.responseText}`);
        });
    } // are you sure?
}
*/
// ensure they don't accidentally navigate away without saving
window.addEventListener("beforeunload", function() {
    if (articleEditor) {
        return "You have not saved your changes.";
    }
});

window.addEventListener("message", function(e) {
    // message that was passed from iframe page
    const message = e.data;
    const resource = message.resource;
    for (let iframe of document.querySelectorAll(`iframe[src=\"${resource}\"]`)) {    
        iframe.style.height = message.height + 'px';
    }
}, false);

window.addEventListener("load", function(e) {
    // Add edit button
    editButton = document.createElement("button");
    editButton.id = "edit"
    editButton.innerHTML = editLabel;
    editButton.onclick = editPage;
    const aside = document.querySelector("aside");
    aside.appendChild(editButton);
}, false);

