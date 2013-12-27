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
	    //=========================================================================================================
	    //	Connection Types
	    //=========================================================================================================
	    Connection : Class.extend({
		    /**
		     * Constructor for Db Service. 
		     */
		    init: function(){
		    	this.me = this;
		    },
	    	name: null, 
	    	auto: false, 
	    	url: null, 
	    	type: null, 
	    	permission: false, 
	    	permission_pattern: '',

	    	onConnect : {
	    		listeners : [],
	    		addListener : function(connectListener) {
	    			if(connectListener!=null) {
	    				me.onConnect.listeners.push(connectListener);
	    			}
	    		}
	    	},
	    	onClose : {
	    		listeners : [],
	    		addListener : function(closeListener) {
	    			if(closeListener!=null) {
	    				me.onClose.listeners.push(closeListener);
	    			}
	    		}
	    	},
	    	onError : {
	    		listeners : [],
	    		addListener : function(errorListener) {
	    			if(errorListener!=null) {
	    				me.onError.listeners.push(errorListener);
	    			}
	    		}
	    	},
	    	onIncomingData : {
	    		listeners : [],
	    		addListener : function(dataListener) {
	    			if(dataListener!=null) {
	    				me.onIncomingData.listeners.push(dataListener);
	    			}
	    		}
	    	}
	    }),
	    WebSocketConnection : Connection.extend({
		    init: function(webSocketUrl){
		    	this._super( this );
		    	this.webSocketUrl = webSocketUrl;
		    	this.webSocket = new WebSocket(this.webSocketUrl);
		    	this.webSocket
		    },
	    	//this._super( false );

	    	// name: 'Default', auto: false, url: 'ws://localhost:4243/ws', type: 'websocket', permission: false, permission_pattern: ''
			// URL: "ws://localhost:4243/ws"
			// binaryType: "blob"
			// bufferedAmount: 0
			// extensions: ""
			// onclose: null
			// onerror: null
			// onmessage: null
			// onopen: null
			// protocol: ""
			// readyState: 1.	    	
	    	
	    }),

	    //=========================================================================================================
	    /** The connections being managed, keyed by type, then URL */    
	    connectionsByType : {
	    },
	    /** The connections being managed, keyed by URL, then type */    
	    connectionsByURL : {
	    },

	    /** The global connection listeners */    
	    connectionListeners : {
	    	connect: [],
	    	close: [],
	    	error: [],
	    	data: []
	    },
		/**  Registers global connection listeners, registered with all created connections */
		addGlobalListener : function(globalListener) {
			if(globalListener!=null) {
				if(globalListener.onConnect && $.isFunction(globalListener.onConnect)) {
					this.connectionListeners.connect.push(globalListener);					
				}
				if(globalListener.onClose && $.isFunction(globalListener.onConnect)) {
					this.connectionListeners.close.push(globalListener);					
				}
				if(globalListener.onError && $.isFunction(globalListener.onError)) {
					this.connectionListeners.error.push(globalListener);					
				}
				if(globalListener.onData && $.isFunction(globalListener.onData)) {
					this.connectionListeners.data.push(globalListener);					
				}
			}
		},

	    /** Acquires a connection when a request comes in for a connection we don't have */
	    getConnection : function(conn, nested) {
	    	if(conn==null) throw "Connection request was null";
	    	var d = nested==null ? $.Deferred() : nested;	 
	    	if(d.state()!="pending") return;
	    	var ctx = this.me;
	    	if(!isNaN(conn)) {
	    		window.opentsdb.services.db.getByKey('connections', true, conn).then(
	    		//this.sendRequest({port:'db', name:'getByKey', 'args': ['connections', true, conn]}).then(
	    			function(data) {
	    				if(data.count > 0) {
	    					ctx.getConnection(data[1], d);
	    				} else {
	    					d.reject("No records returned");
	    				}
	    				
	    			},
	    			function(err) {
	    				d.reject(err);	    				
	    			}
	    		);
	    	} else {
	    		console.info("Retrieved Connection: [%o]", conn);
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
