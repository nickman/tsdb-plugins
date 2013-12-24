/*
 * Chrome Console background script app initializer
 * Whitehead, 2014
 */

function bootBackground() {
	var _opentsdb = {
		manifest : null,
		services : {

		},
		dependencies: null,
		mainWindow : null,
		boot : {		
			waitForServices : function() {
				opentsdb.manifest = chrome.runtime.getManifest();
				console.info("Manifest:[%o]", opentsdb.manifest);
				console.info("Registered Services:[%o]", opentsdb.services);


				var readyPromises = [];
				$.each(opentsdb.manifest.app.background.scripts, function(index, script) {
					if(script.indexOf("/services/")==0) {
						var svcName = script.replace("/services/", "").replace(".js", "");
						readyPromises.push(opentsdb.dependencies[svcName]);
						console.info("Acquired startup promise for [%s]", svcName);
					}
				});
				console.info("Ready Promises [%o]", readyPromises);
				$.when(readyPromises).then(
					function() {
						opentsdb.boot.init();
					}
				);
			},
			init : function() {
				chrome.app.window.create('appstart.html', {
			  		'id' : 'mainWindow',
			  		'state' : 'normal',
			    	'bounds': {
			      		'width': Math.round(window.screen.availWidth*0.95),
			      		'height': Math.round(window.screen.availHeight*0.8)
			    	}    
			  	},
			  	function(createdWindow) {
					opentsdb.mainWindow = createdWindow;
					console.info("Created Main Window [%o]", opentsdb.mainWindow);  		
					console.info("------------> Started background.js");
			  	}
			  );
			}
		}
	};
	$.each(_opentsdb, function(key, value){
		var inm = opentsdb[key] != null;
		if(!inm) {
			opentsdb[key] = value;
		}
	});
	console.info("OpenTSDB Namespace:[%o]", opentsdb);
}


chrome.app.runtime.onLaunched.addListener(function() {  
	console.info("Starting background.js  <------------");
	bootBackground();
	opentsdb.boot.waitForServices();
});  

chrome.app.runtime.onRestarted.addListener(function() {
	console.info("Starting background.js  <------------");  
	bootBackground();
	opentsdb.boot.waitForServices();
});  





