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
	    	this.addGlobalListener({
	    		onConnect: function _GLOBAL_ON_CONNECT_(conn){
	    			console.info("RConnService adding connected [%o]", conn);
	    			opentsdb.services.rcon.connectionsByURL[conn.connection.ID] = conn.connection;	    			
	    			console.info("RConnService added connected [%o]", conn);
	    		},
	    		onClose: function _GLOBAL_ON_CLOSE_(event, conn){
	    			console.info("RConnService removing closed [%o]", conn);
	    			delete opentsdb.services.rcon.connectionsByURL[conn.connection.ID];
	    		}	    		
	    	});
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
					console.info("Adding onConnect Global Listener [%s]", globalListener.onConnect.name);
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
	    			websock.onConnect.addListener(function _OTF_LISTENER_(){
	    				console.info("OTF: args:[%o]", arguments);
	    				d.resolve(arguments[0]);	    				
	    			});	    			
	    		} else {
	    			console.error("The remote connection type [%s] has not been implemented", conn.type);
	    			d.reject("The remote connection type [" + conn.type + "] has not been implemented");
	    		}
	    	}
	    	return d.promise();
	    },
	    inflight : {
	    	

	    },
	    ridCounter : 0,	    

	    // opentsdb.services.rcon.sendRequest('ws://localhost:4243/ws', {"svc": "system", "op": "sleep", "args": {"sleep":4000}});

	    sendRequest: function(url, request, nested) {
	    	var d = nested==null ? $.Deferred() : nested;
	    	if(this.connectionsByURL[url]==null) {
	    		if(nested!=null) {
	    			console.error("Received recursive request for connection [%s]. Not connected. Failing ... ", url);
	    			d.reject("Failed to get connection to [" + url + "]");
	    			return;
	    		} else {
	    			console.info("Received request for connection [%s]. Not connected. Connecting .... ", url);
	    			var _me = this;
	    			this.getConnection(url).then(
	    				function() {  // on success
	    					_me.sendRequest(url, request, d);
	    				},
	    				function(err) {  // on fail
	    					d.reject(err);	    			
	    				}
	    			);
	    		}
	    	} else {
	    		var _conn = this.connectionsByURL[url];
	    		console.info("Have connection [%o] for request [%o]", _conn, request);
	    		//======================================================================================================
	    		//		Set up response handler and timeout
	    		//======================================================================================================
	    		var rid = -1;
	    		var timeout = 5000;
	    		if(request.rid!=null && !isNaN(request.rid)) {
	    			rid = request.rid;
	    		} else {
	    			rid = this.ridCounter++;
	    			request.rid = rid;
	    		}
	    		if(request.t==null) {
	    			request.t = 'req';
	    		}
				if(request.timeout!=null && !isNaN(request.timeout) && (request.timeout > 0)) {
					timeout = request.timeout;
				}
				var alarmName = "rid-" + rid + "-alarm";
				var handler = function _OTF_DATA_HANDLER_(data){
					console.debug("_OTF_DATA_HANDLER_ Data Received:[%o]", data);
					var decoded = null;
					var msg = null;
					var rerid = -1;
					if(data.data != null) {
						try {
							decoded = JSON.parse(data.data);
							if(decoded.rerid!=null && !isNaN(decoded.rerid)) {
								rerid = decoded.rerid;
								if(decoded.msg!=null) {
									try {
										msg = JSON.parse(decoded.msg);
										decoded.msg = msg;
									} catch (e) {}

								}
							}
						} catch (e) {
							d.reject(e);
							return;
						}
					}
					// =================
					// PROBLEM:   First request comes out:  Processing Message with rerid:[-1] and rid:[0] 
					// =================
					
					console.debug("Processing Message with rerid:[%s] and rid:[%s]", rerid, rid);
					if(rerid==rid) {
						chrome.alarms.clear(alarmName);
						console.info("Received response on rid [%s] --> [%o]", rid, decoded);
						_conn.onIncomingData.removeListener(_OTF_DATA_HANDLER_);
						d.resolve(decoded);
					}
				};
				_conn.onIncomingData.addListener(handler);
	    		chrome.alarms.create(alarmName, {when: Date.now() + timeout});
	    		chrome.alarms.onAlarm.addListener(function(alarm){
	    			if(alarm.name==alarmName) {
	    				_conn.onIncomingData.removeListener(handler);
	    				console.warn("Request rid [%s] timed out", rid);
	    				d.reject("ERROR:Request rid [" + rid + "] timed out");
	    			}
	    		});
	    		_conn.send(request);
	    		//======================================================================================================
	    	}
	    	return d.promise();
	    //======================================================================================================
	    }, 			// END OF sendRequest
	    //======================================================================================================
	    subscribe: function(url, request, nested) {
	    	var d = nested==null ? $.Deferred() : nested;
	    	if(this.connectionsByURL[url]==null) {
	    		if(nested!=null) {
	    			console.error("Received recursive request for connection [%s]. Not connected. Failing ... ", url);
	    			d.reject("Failed to get connection to [" + url + "]");
	    			return;
	    		} else {
	    			console.info("Received request for connection [%s]. Not connected. Connecting .... ", url);
	    			var _me = this;
	    			this.getConnection(url).then(
	    				function() {  // on success
	    					_me.sendRequest(url, request, d);
	    				},
	    				function(err) {  // on fail
	    					d.reject(err);	    			
	    				}
	    			);
	    		}
	    	} else {
	    		var _conn = this.connectionsByURL[url];
	    		console.info("Have connection [%o] for subscription [%o]", _conn, request);
	    		//======================================================================================================
	    		//		Set up response handler and timeout
	    		//======================================================================================================
	    		var rid = -1;
	    		var timeout = 5000;
	    		if(request.rid!=null && !isNaN(request.rid)) {
	    			rid = request.rid;
	    		} else {
	    			rid = this.ridCounter++;
	    			request.rid = rid;
	    		}
	    		if(request.t==null) {
	    			request.t = 'sub';
	    		}
				if(request.timeout!=null && !isNaN(request.timeout) && (request.timeout > 0)) {
					timeout = request.timeout;
				}
				var alarmName = "rid-" + rid + "-alarm";
				// the handler gets back the subscription confirmation (or error)
				// so the handler's job is to initiate the routing of all messages
				// with the sub's RID to the promise.progress callback 

				var handler = function _OTF_SUB_HANDLER_(data){
					console.debug("_OTF_SUB_HANDLER_ Data Received:[%o]", data);
					var decoded = null;
					var msg = null;
					var rerid = -1;
					// if data is parsed, then
					// 		if msg.rerid==this.rerid  AND t=='subst'
					//  then out subscription is inited and we're good to go
					if(data.data != null) {
						try {
							decoded = JSON.parse(data.data);
							if(decoded.rerid!=null && !isNaN(decoded.rerid)) {
								rerid = decoded.rerid;
								if(decoded.msg!=null) {
									try {
										msg = JSON.parse(decoded.msg);
										decoded.msg = msg;
									} catch (e) {}

								}
							}
						} catch (e) {
							d.reject(e);
							return;
						}
					}
					// =================
					// PROBLEM:   First request comes out:  Processing Message with rerid:[-1] and rid:[0] 
					// =================
					
					console.debug("Processing Message with rerid:[%s] and rid:[%s]", rerid, rid);
					if(rerid==rid) {
						chrome.alarms.clear(alarmName);
						console.info("Received response on rid [%s] --> [%o]", rid, decoded);
						_conn.onIncomingData.removeListener(_OTF_DATA_HANDLER_);
						d.resolve(decoded);
					}
				};
				_conn.onIncomingData.addListener(handler);
	    		chrome.alarms.create(alarmName, {when: Date.now() + timeout});
	    		chrome.alarms.onAlarm.addListener(function(alarm){
	    			if(alarm.name==alarmName) {
	    				_conn.onIncomingData.removeListener(handler);
	    				console.warn("Request rid [%s] timed out", rid);
	    				d.reject("ERROR:Request rid [" + rid + "] timed out");
	    			}
	    		});
	    		_conn.send(request);
	    		//======================================================================================================
	    	}
	    	return d.promise();
	    //======================================================================================================
	    }, 			// END OF subscribe
	    //======================================================================================================	    
	    closeAll : function() {
	    	console.group("Closing All Connections");
	    	$.each(this.connectionsByURL, function(_url, _conn) {
	    		try {
	    			_conn.close();
	    			console.info("Closed [%s]", _url);
	    		} catch (e) {}
	    	});
			console.groupEnd();	    	
	    },
	    _internalRequest: function(url, request) {
	    	console.info("Internal call: [%s] -- [%s]", url, JSON.stringify(request));
	    	var _ws = null;
	    	var d = $.Deferred();
    		if(request.rid==null || !isNaN(request.rid)) {
    			request.rid = this.ridCounter++;
    		}
    		if(request.t==null) {
    			request.t = 'req';
    		}
    		_ws = new WebSocket(url);
    		_ws.onopen = function() {
    			_ws.send(JSON.stringify(request));
    		};
    		_ws.onerror = function(err) {
    			d.reject(err);
    			if(_ws!=null) try { _ws.close();} catch (e) {}
    		};
    		_ws.onmessage = function(msg) {
    			var jsonMsg = null;
    			if(msg.data!=null) {
    				jsonMsg = JSON.parse(msg.data);
    				if(jsonMsg.sessionid!=null) return;
    			}
    			d.resolve(msg);
    			if(_ws!=null) try { _ws.close();} catch (e) {}
    		};
    		return d.promise();
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
    		},
    		removeListener : $.proxy(function(listener) {
    			var index = this.listeners.indexOf(listener);
    			if(index>-1) {
    				this.listeners.splice(index, 1);
    			}
    		}, this.me)
    	},
    	onClose : {
    		listeners : [],
    		addListener : function(closeListener) {
    			if(closeListener!=null) {
    				this.listeners.push(closeListener);
    			}
    		},
    		removeListener : $.proxy(function(listener) {
    			var index = this.listeners.indexOf(listener);
    			if(index>-1) {
    				this.listeners.splice(index, 1);
    			}
    		}, this.me)
    	},
    	onError : {
    		listeners : [],
    		addListener : function(errorListener) {
    			if(errorListener!=null) {
    				this.listeners.push(errorListener);
    			}
    		},
    		removeListener : $.proxy(function(listener) {
    			var index = this.listeners.indexOf(listener);
    			if(index>-1) {
    				this.listeners.splice(index, 1);
    			}
    		}, this.me)
    	},
    	onIncomingData : {
    		listeners : [],
    		addListener : function(dataListener) {
    			if(dataListener!=null) {
    				this.listeners.push(dataListener);
    			}
    		},
    		removeListener : $.proxy(function(listener) {
    			var index = this.listeners.indexOf(listener);
    			if(index>-1) {
    				this.listeners.splice(index, 1);
    			}
    		}, this.me)
    	}
    });
	RConnService.Connection.name = "Connection";
	    //====================================================
	    //	WebSocket Connection Type
	    //====================================================
    RConnService.WebSocketConnection = RConnService.Connection.extend({
	    init: function(webSocketUrl, rconservice){
	    	this._super( rconservice );
	    	this.webSocketUrl = webSocketUrl;
	    	this.ID = webSocketUrl;	    	
	    	console.debug("onConnect Stuff: [%o]", this.onConnect);
	    	var rcon = this;	    	
	    	$.each(rconservice.connectionListeners.connect, function(i,x) {rcon.onConnect.addListener(x);});
	    	$.each(rconservice.connectionListeners.close, function(i,x) {rcon.onClose.addListener(x);});
	    	$.each(rconservice.connectionListeners.error, function(i,x) {rcon.onError.addListener(x);});
	    	$.each(rconservice.connectionListeners.data, function(i,x) {rcon.onIncomingData.addListener(x);});
	    	console.debug("Added Global Listeners");
	    	console.debug("Issuing connect for [%s]", this.webSocketUrl);
	    	this.webSocket = new WebSocket(this.webSocketUrl);
	    	var _wconn = this;
	    	this.webSocket.onopen = function() {
	    		console.info("Connected WebSocket -- [%o]", this);
	    		this.connection = rcon;
	    		rcon.close = function _CLOSE_WEBSOCK_() {
	    			if(rcon.webSocket != null && rcon.webSocket.readyState != null && rcon.webSocket.readyState!=3)  {
	    				rcon.webSocket.close();
	    			}
	    		};
	    		var x = rcon.onConnect.listeners;
	    		console.group("Calling onConnect Listeners");
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			console.info("Calling onConnect Listener [%s]", x[i]);
	    			x[i](this);
	    		}
	    		console.groupEnd();
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
	    		var x = [];
	    		$.each(rcon.onIncomingData.listeners, function(ind, val){
	    			x.push(val);
	    		});
	
	    		if(message.data==null || message.data=="") {
	    			console.warn("Received data was null or empty");
	    			return;
	    		}
	    		console.info("WebSocket Data, Message:[%o] -- [%o] -- Listeners: [%s]", message, this, x.length);	    
	    		var jsonMsg = JSON.parse(message.data);
	    		if(jsonMsg.sessionid != null) {
	    			rcon.sessionid = jsonMsg.sessionid;
	    			console.group("");
	    			console.info("===============================================");
					console.info("     WebSock Session ID: [%s]", rcon.sessionid);
	    			console.info("===============================================");
	    			console.groupEnd();
	    			return;
	    		}
	    		
	    		for(var i = 0, il = x.length; i < il; i++) {
	    			console.info("Calling Listener [%s], message[%o], jsonMsg:[%o], this:[%o]", i, message, jsonMsg, this);
	    			x[i](message, jsonMsg, this);
	    		}	    		
	    	};
	    },
	    send : function(data) {
	    	this.webSocket.send(JSON.stringify(data));
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
	RConnService.WebSocketConnection.name="WebSocketConnection";
	var rcon = new RConnService();
	window.opentsdb.services.rcon = rcon;	
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	window.opentsdb.dependencies['remoteconns'].resolve(rcon);		

	console.info("------------> [%s] remoteconns.js", OP[1]);
});

chrome.runtime.onSuspend.addListener(function(){
	opentsdb.services.rcon.closeAll();
});