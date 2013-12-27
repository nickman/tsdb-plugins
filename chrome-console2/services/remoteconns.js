/*
 * Remote Connection Manager
 * Whitehead, 2014
 */ 

chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
	OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
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
	    /** type decodes */
	    typeDecodes : {
	    	"ws" : "websocket",
	    	"tcp" : "tcp",
	    	"http" : "http",
	    	"udp" : "udp",

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
		isConnectionUrl : function(connUrl) {
			try {
				return this.startsWithAny(connUrl, "http://", "tcp://", "ws://", "udp://");
			} catch (e) {
				return false;
			}						
		},
		getConnectionUrlType : function(connUrl) {
			if(!this.isConnectionUrl(connUrl)) throw "The passed value [" + connUrl + "] is not a supported URL";
			return this.typeDecodes[connUrl.substring(0, connUrl.indexOf(":"))];
		},
	    /** Acquires a connection when a request comes in for a connection we don't have */
	    // example:  opentsdb.services.rcon.getConnection("ws://localhost:4243/ws");
	    // example: opentsdb.services.rcon.getConnection(1);
	    getConnection : function(conn, nested) {
	    	if(conn==null) throw "Connection request was null";
	    	var d = nested==null ? $.Deferred() : nested;	 
	    	if(d.state()!="pending") return;
	    	var ctx = this.me;
	    	if(!isNaN(conn)) {
	    		window.opentsdb.services.db.getByKey('connections', true, conn).then(
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
	    	} else if (this.isConnectionUrl(conn)) {
	    		var _conn = {
	    			url: conn,
	    			type: ctx.getConnectionUrlType(conn)
	    		};
	    		ctx.getConnection(_conn, d);
	    	} else {	    		
	    		console.info("Retrieved Connection: [%o]", conn);
	    		if(conn.type=="websocket") {
	    			console.info("Connecting to [%s]", conn.url);
	    			var websock = new WebSocket(conn.url);
	    			console.info("WebSock for [%o]", websock);
	    		} else {
	    			console.error("The remote connection type [%s] has not been implemented", conn.type);
	    		}
	    	}
	    }

	});  // end of RConnService definition
    //=========================================================================================================
    //	Connection Types
    //=========================================================================================================
	    //====================================================
	    //	Base Connection Type
	    //====================================================
    RConnService.Connection = Class.extend({
    	me: null,
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
    });
	    //====================================================
	    //	WebSocket Connection Type
	    //====================================================
    RConnService.WebSocketConnection = RConnService.Connection.extend({
	    init: function(webSocketUrl){
	    	this._super( this );
	    	this.webSocketUrl = webSocketUrl;
	    	this.webSocket = new WebSocket(this.webSocketUrl);
	    	this.webSocket.onopen = function() {

	    	};
	    	this.webSocket.onclose = function(event) {
	    		var wasClean = event.wasClean;
	    	};
	    	this.webSocket.onerror = function(error) {
	    		console.error("WebSocket error on [%s]--> [%o]", me.webSocketUrl, error);
	    	};
	    	this.webSocket.onmessage = function(message) {
	    		console.debug("WebSocket message on [%s]--> [%o]", me.webSocketUrl, message);
	    	};


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
    	
    });

	var rcon = new RConnService();
	window.opentsdb.services.rcon = rcon;	
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	window.opentsdb.dependencies['remoteconns'].resolve(rcon);		
	console.info("------------> [%s] remoteconns.js", OP[1]);
});
