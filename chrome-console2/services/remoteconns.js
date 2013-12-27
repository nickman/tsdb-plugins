/*
 * Remote Connection Manager
 * Whitehead, 2014
 */ 

chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
	OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
	console.info("%s remoteconns.js  <------------", OP[0]);	
	var RConnService = Class.extend({
		me: this,
	    /**
	     * Constructor for Db Service. 
	     */
	    init: function(){
	    	this.me = this;
	    	var _me = this;
	    	this.addGlobalListener({onConnect: function(conn){
	    		console.info("RConnService adding connected [%o]", conn);
	    		_me[conn.connection.URL] = conn;
	    	}});
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
	    /** Embeds the listener into the passed filter */
	    filterize : function(listener, filter) {
	    	if(filter==null || filter.filter=="undefined" || !$.isFunction(filter.filter)) return listener;
	    	return function(event) {
	    		if(filter.filter(event)) {
	    			listener(event);
	    		};
	    	};
	    },
		/**  Registers global connection listeners, registered with all created connections */
		addGlobalListener : function(globalListener, filter) {
			if(globalListener!=null) {
				if(globalListener.onConnect && $.isFunction(globalListener.onConnect)) {
					this.connectionListeners.connect.push(this.filterize(globalListener.onConnect, filter));					
				}
				if(globalListener.onClose && $.isFunction(globalListener.onClose)) {
					this.connectionListeners.close.push(this.filterize(globalListener.onClose, filter));					
				}
				if(globalListener.onError && $.isFunction(globalListener.onError)) {
					this.connectionListeners.error.push(this.filterize(globalListener.onError, filter));					
				}
				if(globalListener.onData && $.isFunction(globalListener.onData)) {
					this.connectionListeners.data.push(this.filterize(globalListener.onData, filter));					
				}
				if(globalListener.onAny && $.isFunction(globalListener.onAny)) {
					this.connectionListeners.connect.push(this.filterize(globalListener.onAny, filter));
					this.connectionListeners.close.push(this.filterize(globalListener.onAny, filter));
					this.connectionListeners.error.push(this.filterize(globalListener.onAny, filter));
					this.connectionListeners.data.push(this.filterize(globalListener.onAny, filter));
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
	    			var websock = new RConnService.WebSocketConnection(conn.url, this);
	    			console.info("Created WebSock Instance: [%o]", websock);
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
	    init: function(rconservice){
	    	this.me = this;	    
	    	this.rconservice = rconservice;	
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
    				this.listeners.push(connectListener);
    			}
    		}
    	},
    	onClose : {
    		listeners : [],
    		addListener : function(closeListener) {
    			if(closeListener!=null) {
    				this.listeners.push(closeListener);
    			}
    		}
    	},
    	onError : {
    		listeners : [],
    		addListener : function(errorListener) {
    			if(errorListener!=null) {
    				this.listeners.push(errorListener);
    			}
    		}
    	},
    	onIncomingData : {
    		listeners : [],
    		addListener : function(dataListener) {
    			if(dataListener!=null) {
    				this.listeners.push(dataListener);
    			}
    		}
    	}
    });
	    //====================================================
	    //	WebSocket Connection Type
	    //====================================================
    RConnService.WebSocketConnection = RConnService.Connection.extend({
	    init: function(webSocketUrl, rconservice){
	    	this._super( rconservice );
	    	this.webSocketUrl = webSocketUrl;
	    	this.webSocket = new WebSocket(this.webSocketUrl);
	    	console.debug("onConnect Stuff: [%o]", this.onConnect);
	    	var rcon = this;
	    	$.each(rconservice.connectionListeners.connect, function(i,x) {rcon.onConnect.addListener(x);});
	    	$.each(rconservice.connectionListeners.close, function(i,x) {rcon.onClose.addListener(x);});
	    	$.each(rconservice.connectionListeners.error, function(i,x) {rcon.onError.addListener(x);});
	    	$.each(rconservice.connectionListeners.data, function(i,x) {rcon.onIncomingData.addListener(x);});
	    	
	    	this.webSocket.onopen = function() {
	    		console.info("Connected WebSocket -- [%o]", this);
	    		this.connection = rcon;
	    		var x = rcon.onConnect.listeners;
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			x[i](this);
	    		}
	    	};
	    	this.webSocket.onclose = function(event) {
	    		console.info("Closed WebSocket, Event:[%o] -- [%o]", event, this);
	    		var x = rcon.onClose.listeners;
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			x[i](event, this);
	    		}
	    	};
	    	this.webSocket.onerror = function(error) {
	    		console.info("WebSocket Error, Error:[%o] -- [%o]", error, this);
	    		var x = rcon.onError.listeners;
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			x[i](error, this);
	    		}
	    	};
	    	this.webSocket.onmessage = function(message) {
	    		console.info("WebSocket Data, Message:[%o] -- [%o]", message, this);	    		
	    		var x = rcon.onIncomingData.listeners;
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			x[i](message, this);
	    		}	    		
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
