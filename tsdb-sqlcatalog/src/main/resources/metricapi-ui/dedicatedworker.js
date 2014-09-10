importScripts("bacon.js", "clone.js", "jquery.hive.pollen.js", "websockapi.js");  // "jquery-2.1.1.min.js"
self.bus = new Bacon.Bus();
self.wsclient = null;
self.onmessage = function (evt) {
	//self.postMessage(“Text received from the UI: “ + evt.data);
	self.bus.push(evt);
}
self.onerror = function(err) {
	self.bus.error(err);	
}
self.bus.onValue(function(event){
	delete event.WorkerGlobalScope;
	var pevent = self.prepare(event);

	try {
		self.postMessage(pevent);
	} catch (e) {
		console.error("Failed to post message [" + pevent + "], ex: " + e);
		throw e;
	}
});

self.transferNoGos = ["currentTarget", "srcElement", "target"];

self.prepare = function(obj, tobj, sofar) {
	tobj = tobj || {};
	sofar = sofar || [];
	if(obj!=null) {		
		for(var p in obj) {
			if(self.transferNoGos.indexOf(p) != -1) continue;
			var prop = obj[p];
			switch(typeof(prop)) {
				case "undefined":
				case "boolean":
				case "number":
				case "string":
				case "symbol":					
					tobj[p] = prop;
					break;
				case "object":
					tobj[p] = clone(prop, true, 2);
					break;
			}
		}
		return tobj;
	} else {
		return null;
	}
}

console.info("[dedicatedworker] Dedicated Worker Thread Initialized. Location: [" + location + "]");