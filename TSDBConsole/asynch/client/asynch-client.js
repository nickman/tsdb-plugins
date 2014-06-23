/*
	asynch-client.js
	Whitehead, 2014
 */
var backgroundPage = null;
var targetOrigin = null;
var asynchPort = null;
var seq = 0;
var requests = {};



/** Load background page on first call */
$(document).ready(function() { 
	targetOrigin = chrome.runtime.getURL("");
	chrome.runtime.getBackgroundPage(function(p){
		backgroundPage = p;
		console.info("Acquired BackgroundPage: [%s]", backgroundPage);
	})
	asynchPort = chrome.runtime.connect({name: "asynch-client"});	
	console.info("Connect to asynch-server. Port: [%O]", asynchPort);
	asynchPort.onMessage.addListener(handleResponse);
});


function ping() {
	return asynchPort.postMessage("ping");
}

function loadExternalBlob(urlToLoad) {
	return postRequest({type: "img", url: urlToLoad});
}


function postRequest(request) {
	var d = $.Deferred();
	var p = d.promise();
	var x = seq++;
	requests[x] = d;
	request.seq = x;
	asynchPort.postMessage(request);
	return p;
}

function handleResponse(response) {
	try {
		var x = response.seq;
		var p = requests[x];
		if(p==null) throw ("No pending promise for seq [" + x + "]");
		p.resolve(response.response);
		delete requests[x];
	} catch (e) {
		console.error("Error processing asynch response: [%O]", e);
	}
}

function testImg() {	
	loadExternalBlob("http://localhost:8080/q?start=5m-ago&ignore=9&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&nokey&wxh=377x180&png")
		.then(
			function(data){
				console.info("Loaded Blob: [%O]", data);
			}
		);
}

