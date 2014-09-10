importScripts("bacon.js");
self.bus = new Bacon.Bus();
self.onmessage = function (evt) {
	//self.postMessage(“Text received from the UI: “ + evt.data);
	self.bus.push(evt);
}
self.onerror = function(err) {
	self.bus.error(err);	
}
bus.onValue(function(event){
	try {
		self.postMessage(event);
	} catch (e) {
		console.error("Failed to post message [" + event + "], ex: " + e);
		throw e;
	}
});

console.info("[dedicatedworker] Dedicated Worker Thread Initialized. Location: [" + location + "]");