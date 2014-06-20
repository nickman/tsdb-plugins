

$(document).ready(function() { 
	console.info("Loading.........");
	window.addEventListener("message", route);
	
});

function route(e) {
	console.info("Processing Message from origin: [%s] ---> [%O]", e.origin, e);
}
