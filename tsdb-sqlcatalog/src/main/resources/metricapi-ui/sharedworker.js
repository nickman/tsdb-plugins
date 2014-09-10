importScripts("bacon.js");
var connections = 0;
self.addEventListener("connect" function(e){
	var port = e.ports[0];
	connections++;
	port.start();
}, false);