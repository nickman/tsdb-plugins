/*
 * Remote Connection Manager
 * Whitehead, 2014
 */ 

chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
	var OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
	console.info("%s remoteconns.js  <------------", OP[0]);
	var RConnService = Class.extend({
	    /**
	     * Constructor for Db Service. 
	     */
	    init: function(){
	    	this.me = this;
	    	console.info("Creating RConnService");
	    },
	    /** The connections being managed */    
	    connections : {
	    },
	    /** The managed connection listeners */    
	    connectionListeners : [],

	    /** Acquires a connection when a request comes in for a connection we don't have */
	        




	});  // end of RConnService definition
	window.opentsdb.types.RConnService = RConnService;
	var rcon = new RConnService();
	window.opentsdb.services.rcon = rcon;	
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	db.opendb().then(
		function() {			
			window.opentsdb.dependencies['rcon'].resolve(db);		
			console.info("------------> [%s] remoteconns.js", OP[1]);
		},
		function(evt) {
			window.opentsdb.services['RConnService'].reject(evt);			
		}
	);  	

});
