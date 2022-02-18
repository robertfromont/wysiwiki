const baseURL = document.baseURI.replace(/\/index\.html$/,"");

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
        let currentId = currentUrl.substring(baseURL.length)
              .replace(/\.html$/,"");
        if (currentId == "/home") currentId = "/";
        const item = document.getElementById(currentId);
        
        // Expand the index tree to current page
        let expandId = currentId;
        let details = document.getElementById(expandId);
        while (expandId && expandId != "/") {
            details.parentElement.setAttribute("open", true);
            expandId = expandId.replace(/\/[^\/]*$/,"");
            details = document.getElementById(expandId);
        } // next ancestor in the tree

        if (item) { // the page exists
            
            // Mark the current page in the index
            item.classList.add("current");

            // Create peer page button
            const peerButton = document.createElement("button");
            peerButton.id = "peer"
            peerButton.innerHTML = "+"; // TODO something better
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

            if (currentId != "/") {
                // Create child page button
                const childButton = document.createElement("button");
                childButton.id = "child"
                childButton.innerHTML = "+"; // TODO something better
                childButton.title = "New child page";
                childButton.onclick = function() {
                    newPage(currentId);
                };
                if (item.tagName == "DIV") { // plain page
                    item.appendChild(childButton);
                } else { // directory
                    item.parentElement.appendChild(childButton);
                }
            }
        } // the page exists
    }
}, false);

function newPage(parentId) {
    const title = prompt("Page Title");
    if (title) {
        const documentName = slugify(title) + ".html";
        let url = `${baseURL}${parentId}/${documentName}`;
        if (title+".html" != documentName) url += `#${title}`;
        window.top.location = url;
    }
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
