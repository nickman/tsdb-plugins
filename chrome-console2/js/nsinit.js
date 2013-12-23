/*
 * OpenTSDB namespace initializer
 * Whitehead, 2014
 */ 

 /*
 	services : {

	},
	dependencies: null,
*/

function initOpenTSDB() {
	if(window.opentsdb==null) {
		window.opentsdb = {};
		console.info("OpenTSDB namespace created [%o]", document.location);
		window.opentsdb.services = {};
		window.opentsdb.dependencies = {};

		console.group("Service Scripts");
		$.each(chrome.runtime.getManifest().app.background.scripts, function(index, script) {
			if(script.indexOf("/services/")==0) {
				var svcName = script.replace("/services/", "").replace(".js", "");
				console.info("Service Script: [%o]", svcName);
				window.opentsdb.dependencies[svcName] = $.Deferred();
			}
		});
		console.groupEnd();

		console.info("OpenTSDB Services namespace created [%o]", document.location);
	} else {
		console.info("OpenTSDB namespace exists");
	}
}

chrome.app.runtime.onLaunched.addListener(function() {  
	console.info("Starting init.js  <------------");
	initOpenTSDB();
});  

chrome.app.runtime.onRestarted.addListener(function() {  
	console.info("Starting init.js  <------------");
	initOpenTSDB();
});  
