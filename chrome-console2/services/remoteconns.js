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
	    getConnection : function(conn, nested) {
	    	if(conn==null) throw "Connection request was null";
	    	var d = nested==null ? $.Deferred() : nested;	 
	    	if(d.state()!="pending") return;
	    	var ctx = this.me;
	    	if(!isNaN(conn)) {
	    		this.sendRequest({port:'db', name:'getByKey', 'args': ['connections', true, conn]}).then(
	    			function(data) {
	    				ctx.getConnection(data, d);
	    			},
	    			function(err) {
	    				d.reject(err);
	    				ctx.getConnection(data, d);	
	    			}
	    		);
	    	} else {
	    		if(conn.type=="websocket") {
	    			console.info("Connecting to [%s]", conn.url);
	    		} else {
	    			console.error("The remote connection type [%s] has not been implemented", conn.type);
	    		}
	    	}
	    }

	});  // end of RConnService definition
	window.opentsdb.types.RConnService = RConnService;
	var rcon = new RConnService();
	window.opentsdb.services.rcon = rcon;	
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	window.opentsdb.dependencies['remoteconns'].resolve(rcon);		
	console.info("------------> [%s] remoteconns.js", OP[1]);
});
