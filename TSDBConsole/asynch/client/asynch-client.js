/*
	asynch-client.js
	Whitehead, 2014
 */
var backgroundPage = null;
var targetOrigin = null;
var asynchPort = null;


/** Load background page on first call */
$(document).ready(function() { 
	targetOrigin = chrome.runtime.getURL("");
	chrome.runtime.getBackgroundPage(function(p){
		backgroundPage = p;
		console.info("Acquired BackgroundPage: [%s]", backgroundPage);
	})
	asynchPort = chrome.runtime.connect({name: "asynch-client"});	
	console.info("Connect to asynch-server. Port: [%O]", asynchPort);
	asynchPort.onMessage.addListener(function(msg) {
		console.info("Async-Server Response: [%O]", msg);
	});
});


function ping() {
	var d = $.Deferred();
	backgroundPage.postMessage("ping", targetOrigin);
	return d.promise();
}

function testImg() {
	
	asynchPort.postMessage({
		type: "img", 
		url: "http://opentsdb:8080/q?start=5m-ago&ignore=9&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&nokey&wxh=377x180&png"
	}, function(response) {
		console.info("[Asynch Client] TestImg Result: [%O]", response);
	});
}

