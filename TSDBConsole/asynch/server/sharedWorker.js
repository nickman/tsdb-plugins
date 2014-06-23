/*
	Shared Worker
	Whitehead, 2014
*/
var connections = 0;
self.onconnect = function(e){
	e.ports[0].onmessage = function(msg) {
		msg.target.postMessage("pong");
	}
}