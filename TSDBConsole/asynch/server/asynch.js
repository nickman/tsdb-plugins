var port = null;

$(document).ready(function() { 
	console.info("[Asynch-Sandbox] Loading Sandboxed Asynch Worker....");
	window.addEventListener("message", route);
	console.info("[Asynch-Sandbox] Window Name: [" + window.name + "], Document Domain: [" + document.domain + "]");
});

function route(e) {
	console.info("[Asynch-Sandbox] Processing Message from origin: [" + e.origin + "]");
	console.info("[Asynch-Sandbox] Data:" + JSON.stringify(e.data));
	if(e.data==null || e.data.type==null) {
		unhandled(event, "Null data or type");
		return;
	}
	if(e.data=="ping") {
		ping(e);
		return;
	}
	switch(e.data.type) {
		case "ping":
			ping(e);
			break;
		case "img":
			onImageRequest(e);
			break;
		default:
			unhandled(e, "Unrecognized Type [" + e.data + "]");
	}
}

function ping(event) {
	event.source.postMessage({name: n, type: "Pong", rt: e.origin}, e.origin)	
}

function unhandled(event, message) {
	console.error("Unhandled Event: " + message)
}


/**
	Requests an external URL as a blob.
	Request parameters:
	type: "img"
	url: the url of the image to get
	@TODO: timeout
*/
function onImageRequest(event) {
	//console.info("Processing Request:" + event.data.seq)
	var url = event.data.url; //"http://localhost:8080/q?start=5m-ago&ignore=9&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&nokey&wxh=377x180&png";
    var xhr = new XMLHttpRequest();
    xhr.onerror = function(err) { 
    	console.error("Failed to get image [" + url + "]"); 
    	event.source.postMessage({type: "err-img", seq: event.data.seq, error: err.error}, event.origin);
    },
    xhr.onload = function(data) { 
    	var b = data.currentTarget.response;
        console.info("Blob Retrieved: [" +  b + "]");
        if(event.data.callback!=null) {
        	event.data.callback(b);
        	return;
        }
        event.source.postMessage({type: "img", seq: event.data.seq, response: {blob: b}}, event.origin);
    },
    xhr.open('GET', url, true);
    xhr.responseType = "blob";
    xhr.send();

}

