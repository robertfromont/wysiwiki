function reportDimensions() {
    let message = {
        resource: document.URL.replace(/.*\//,""),
        height: document.body.scrollHeight,
        width: document.body.scrollWidth
    };	
    // window.top refers to parent window
    window.top.postMessage(message, "*");
}
window.addEventListener("resize", reportDimensions);
reportDimensions();
