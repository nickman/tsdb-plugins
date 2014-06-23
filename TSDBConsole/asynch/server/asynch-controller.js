//document.domain = chrome.runtime.id;
var asynchWebview = null;
var asynchContentWindow = null;
var seq = 0;
var requests = {};
var ports = {};
var worker = null;
$(document).ready(function() { 
//(function() {	
	console.info("[Asynch-Controller] - Loading Asynch Controller....");
	worker = new SharedWorker("/asynch/server/sharedWorker.js");
	worker.port.onmessage = handleWorkerMessage;
	worker.port.start();
	console.info("[Asynch-Controller] - Started SharedWorker");
	
	chrome.runtime.onConnect.addListener(onConnect);
	console.info("[Asynch-Controller] - Added Connect Listener");
	window.addEventListener("message", onPhoneHome);
	//$('body').append($('<iframe id="asynch" name="asynch" src="/asynch/server/asynch.html" width="0" height="0"></iframe>'));
	//$('body').append($('<webview id="asynch" name="asynch-' + chrome.runtime.id + '" src="/asynch/server/asynch.html" style="width:0px; height:0px;" partition="servers"></webview>'));
	asynchContentWindow = document.getElementById("asynch").contentWindow;
	/*
	asynchWebview = document.getElementById("asynch");
	asynchWebview.addEventListener("close", onClose);
	asynchWebview.addEventListener("exit", onExit);
	asynchWebview.addEventListener("consolemessage", onConsoleMessage);
	asynchWebview.addEventListener("contentload", onContentLoad);
	asynchWebview.addEventListener("loadcommit", onLoadCommit);
	asynchWebview.addEventListener("loadredirect", onLoadRedirect);
	asynchWebview.addEventListener("loadstart", onLoadStart);
	asynchWebview.addEventListener("loadabort", onLoadAbort);
	asynchWebview.addEventListener("loadstop", onLoadStop);
	asynchWebview.addEventListener("unresponsive", onUnresponsive);
	asynchWebview.addEventListener("responsive", onResponsive);


	asynchWebview.addEventListener("loadcommit", function(e){
		asynchContentWindow = asynchWebview.contentWindow;
		console.info("[Asynch-Controller] Asynch Webview Loaded: [%O]", e);
	});
*/
});

function handleWorkerMessage(e) {
	console.info("Received Worker Message: [%O]", e);
}

function onConnect(port) {
	console.info("Accepted port connection: [%O]", port);
	ports[port.name] = port;
	port.onMessage.addListener(function(msg){
		var x = msg.seq;
		if(x==null) x = seq++;
		requests[x] = port;
		msg.seq = x;
		console.info("-->[Asynch-Controller] Posting Request #[%s]: [%O]", x, msg);
		if(msg.type=='img') {
			onImageRequest(msg, port);
		} else {
			asynchContentWindow.postMessage(msg, '*');
		}
		
	});	
	port.onDisconnect.addListener(function(event) {
		console.info("Processing Port DISCONNECT: [%O]", event);
		if(event.name!=null) {
			delete ports[port.name];
		}
	});
}

function handleRequest(msg, sender, callback) {
	var x = seq++;
	requests[x] = callback;
	msg.seq = x;
	console.info("-->[Asynch-Controller] Posting Request #[%s]: [%O]", x, msg);
	if(msg.type=='img') {
		onImageRequest(msg, callback);
	} else {
		asynchContentWindow.postMessage(msg, '*');
		//callback("POSTED !")
	}
}

function onPhoneHome(e) {
	console.info("[Asynch-Controller]<-- Received Phone Home From [%s]: [%O]", e.origin, e);
	if(e.data!=null && e.data.response!=null && e.data.seq!=null) {
		var callback = requests[e.data.seq];
		//delete requests[e.data.seq];
		console.info("[Asynch-Controller] Posting response to [" + callback + "]");
		callback.postMessage(e.data);
		console.info("[Asynch-Controller] Post Complete");
	}
}

function onImageRequest(msg, port) {
	//console.info("Processing Request:" + event.data.seq)
	var url = msg.url + ((/\?/).test(url) ? "&" : "?") + (new Date()).getTime();
    var xhr = new XMLHttpRequest();
    xhr.onerror = function(err) { 
    	console.error("Failed to get image [" + url + "]"); 
    	port.postMessage({type: "err-img", seq: msg.seq, error: err.error});
    },
    xhr.onload = function(data) { 
    	// eventPhase 
    	if(xhr.readyState==4) {
    		if("OK"==xhr.statusText) {
		    	var b = data.currentTarget.response;
		        console.info("Blob Retrieved: [" +  b + "]");
		        port.postMessage({type: "img", ts: data.timeStamp, seq: msg.seq, response: {blob: b}});
    		} else {
		    	console.error("Failed to get image [" + url + "] : [" + xhr.statusText + "]"); 
		    	port.postMessage({type: "err-img", seq: msg.seq, error: xhr.statusText});
    		}
	    }
    },
    xhr.open('GET', url, true);
    xhr.responseType = "blob";

    xhr.send();

}


function onClose() {
	console.error("[Asynch-Controller] Closed");
}

function onExit(s) {
	console.error("[Asynch-Controller] Exited. Reason: [%s]", e.reason);
}


function onConsoleMessage(e) {
	if(e!=null && e.message!=null) {
		console.info(e.message);
	}
}

function onContentLoad() {
	console.info("[Asynch-Controller] Load Event");	
}

function onLoadCommit() {
	console.info("[Asynch-Controller] Load Commit: [%O]", arguments);	
}

function onLoadRedirect() {
	console.info("[Asynch-Controller] Load Redirect: [%O]", arguments);	
}

function onLoadStart() {
	console.info("[Asynch-Controller] Load Start: [%O]", arguments);	
}

function onLoadAbort() {
	console.info("[Asynch-Controller] Load Abort: [%O]", arguments);	
}

function onLoadStop() {
	console.info("[Asynch-Controller] Load Stop: [%O]", arguments);	
}

function onUnresponsive() {
	console.warn("[Asynch-Controller] Became Unresponsive: [%O]", arguments);		
}


function onResponsive() {
	console.info("[Asynch-Controller] Resumed Responsiveness: [%O]", arguments);		
}
