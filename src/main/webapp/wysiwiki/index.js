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
    // Expand to current page
    const currentUrl = document.referrer;
    console.log("index loaded by " + currentUrl);
    if (currentUrl) {
        const baseURL = document.baseURI.replace(/\/index\.html$/,"");
        const currentId = currentUrl.substring(baseURL.length)
              .replace(/\.html$/,"");
        const item = document.getElementById(currentId);
        if (item) item.classList.add("current");
        let expandId = currentId;
        let details = document.getElementById(expandId);
        while (details) {
            details.parentElement.setAttribute("open", true);
            expandId = expandId.replace(/\/[^\/]*$/,"");
            details = document.getElementById(expandId);
        }
    }
}, false);
