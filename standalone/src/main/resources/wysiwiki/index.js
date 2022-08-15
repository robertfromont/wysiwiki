const baseURL = document.baseURI.replace(/\/index\.html$/,"");
let currentId = null;
let peerButton = null;
let childButton = null;
let moveUpButton = null;
let moveDownButton = null;
const newPeerLabel = "＋";
const newChildLabel = "＋";
const moveUpLabel = "▲";
const moveDownLabel = "▼";

function reportDimensions() {
    let message = {
        resource: document.URL.replace(/.*\//,""),
        height: document.body.scrollHeight,
        width: document.body.scrollWidth
    };	
    
    // window.top refers to parent window
    window.top.postMessage(message, "*");
}

window.addEventListener("load", function(e) {
    const currentUrl = document.referrer;
    if (currentUrl) {
        currentId = currentUrl.substring(baseURL.length)
              .replace(/\.html$/,"");
        if (currentId == "/home") currentId = "/";
        let item = document.getElementById(currentId);
        
        // Expand the index tree to current page
        let expandId = currentId;
        let details = document.getElementById(expandId);
        while (expandId && expandId != "/") {
            if (details) {
                details.parentElement.setAttribute("open", true);
            }
            expandId = expandId.replace(/\/[^\/]*$/,"");
            details = document.getElementById(expandId);
        } // next ancestor in the tree

        if (item) { // the page exists            
            // Mark the current page in the index
            item.classList.add("current");
        } // the page exists
    }
    // resize when details expanded
    const collection = document.getElementsByTagName("details");
    for (let i = 0; i < collection.length; i++) {
        collection[i].addEventListener('click', e => {
            setTimeout(()=>{
                reportDimensions();
            }, 100);
        });
    }
    reportDimensions();
}, false);
window.addEventListener("resize", function(e) {
    reportDimensions();
});
window.addEventListener("message", function(e) {
    // message that was passed from iframe page
    const message = e.data;
    if (message == "editable") addButtons();
}, false);

function addButtons() {
    let item = document.getElementById(currentId);
    if (item) { // the page exists

        let somethingChanged = false;
        
        if (!peerButton) {
            // Create peer page button
            peerButton = document.createElement("button");
            peerButton.id = "peer"
            peerButton.innerHTML = newPeerLabel;
            peerButton.title = "New peer page";
            peerButton.onclick = function() {
                newPage(currentId.replace(/\/[^\/]*$/,""));
            };
            if (item.tagName == "DIV" // plain page
                || currentId == "/") { // home
                item.parentElement.appendChild(peerButton);
            } else { // directory
                item.parentElement.parentElement.appendChild(peerButton);
            }
            somethingChanged = true;
        }
        
        if (currentId != "/" && !childButton) {
            // Create child page button
            childButton = document.createElement("button");
            childButton.id = "child"
            childButton.innerHTML = newChildLabel;
            childButton.title = "New child page";
            childButton.onclick = function() {
                newPage(currentId);
            };
            if (item.tagName == "DIV") { // plain page
                item.appendChild(childButton);
            } else { // directory
                item.parentElement.appendChild(childButton);
            }
            somethingChanged = true;
        }

        if (!moveUpButton) {
            let elementToMove = item;
            if (item.tagName != "DIV") { // plain page
                elementToMove = item.parentElement;
            }
            if (elementToMove.previousElementSibling
                && elementToMove.previousElementSibling.tagName != "SUMMARY") {
                // Create move up button
                moveUpButton = document.createElement("button");
                moveUpButton.id = "moveUp"
                moveUpButton.innerHTML = moveUpLabel;
                moveUpButton.title = "Move page up";
                moveUpButton.onclick = function() {
                    movePage(item.id, "up", elementToMove);
                };
                item.appendChild(moveUpButton);
                somethingChanged = true;
            }
        }
        if (!moveDownButton) {
            let elementToMove = item;
            if (item.tagName != "DIV") { // plain page
                elementToMove = item.parentElement;
            }
            if (elementToMove.nextElementSibling
                && elementToMove.nextElementSibling.tagName != "BUTTON") {
                // Create move down button
                moveDownButton = document.createElement("button");
                moveDownButton.id = "moveDown"
                moveDownButton.innerHTML = moveDownLabel;
                moveDownButton.title = "Move page down";
                moveDownButton.onclick = function() {
                    movePage(item.id, "down", elementToMove);
                };
                item.appendChild(moveDownButton);
                somethingChanged = true;
            }
        }

        // our size may have changed
        if (somethingChanged) reportDimensions();
    } // the page exists
}

function newPage(parentId) {
    const title = prompt("Page Title");
    if (title) {
        const documentName = slugify(title) + ".html";
        let url = `${baseURL}${parentId}/${documentName}`;
        if (title+".html" != documentName) url += `#${title}`;
        window.top.location = url;
    }
}

function movePage(id, direction, elementToMove) {
    const xhr = new XMLHttpRequest();
    xhr.addEventListener("load", function(e) {
        if (this.responseText == "OK") {
            // we could rearrange the DOM, but the cache would load an old version later
            // so reload the index
            document.location.reload(true);
        }
    });
    xhr.addEventListener("error", function(r) {
        console.error(`${r.status}: ${r.statusText}\n${r.responseText}`);
    });
    xhr.open("PUT", `${baseURL}${id}.html?move=${direction}`);
    xhr.send()
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
