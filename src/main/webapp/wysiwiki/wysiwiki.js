
window.addEventListener('message', function(e) {
    // message that was passed from iframe page
    const message = e.data;
    const resource = message.resource;
    for (let iframe of document.querySelectorAll(`iframe[src=\"${resource}\"]`)) {    
        iframe.style.height = message.height + 'px';
    }
} , false);
