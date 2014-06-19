

$(document).ready(function() { 
	console.info("Loading.........");
	window.addEventListener("message", function(e){e.source.postMessage("Pong", e.origin);});
	
});


