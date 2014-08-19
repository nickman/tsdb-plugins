/**
 * APMRouter jQuery Plugin
 * Whitehead, Helios Development Group
 */

(function($){
	$.apmr = {};
	$.apmr.config =  {
		/** The websocket URL that connects back from whence the document came */
		wsUrl : 'ws://' + document.location.host + '/ws',
		/** The websocket instance */
		ws : false,
		/** The connecting state indicator  */
		connecting : false,
		/** A handle to the timeout set to timeout the connection attempt */
		connectTimeoutHandle : -1,
		/** A handle to the timeout for the reconnect attempt */
		reconnectTimeoutHandle : -1,
		
		/** The connection timeout in ms. */
		connectionTimeout : 5000,
		/** The pause time before attempting a reconnect in ms. */
		reconnectPauseTime : 3000,
		
		/** The session Id, set when connected, cleared when disconnected  */
		sessionId : "",
		/** The request ID counter */
		requestId : 0,
		/** A repository of subscriptions keyed by sub key */
		subsBySubKey: {},
		/** A repository of subscriptions keyed by req Id */
		subsByReqId: {},
		/** Clears all subs */
		resetSubs: function() {
			this.subsBySubKey = {};
			this.subsByReqId = {};
		},
		
		/** Indicates if down nodes (hosts and agents) should be loaded in the tree when expanding */
		loadDownNodes: true,
		/** Indicates if downed nodes (hosts and agents) should be removed from the tree when timed out */
		unloadDownNodes: false,
		/** The amount of time in ms. that downed nodes (hosts and agents) will linger in the tree before being unloaded */
		downedNodeLingerTime: 15000,
		/** The handle of the downed node reaper */
		downedNodeReaper: -1,
		/** The last sequence number received */
		lastSequence: -1
		
	},
	
	$.apmr.isReconnectScheduled = function() {
		return ($.apmr.config.reconnectTimeoutHandle != -1);
	},
	
	$.apmr.isConnectingOrConnected = function() {
		if($.apmr.config.ws) {
			var readyState = $.apmr.config.ws.readyState;
			if(readyState!=null && (readyState==0 || readyState==1)) return true;
		}
		return false;		
	},
	
	$.apmr.startReconnectLoop = function() {
		if($.apmr.isConnectingOrConnected() || $.apmr.isReconnectScheduled()) return;
		$.apmr.config.reconnectTimeoutHandle = setTimeout(function(){
			$.apmr.config.reconnectTimeoutHandle = -1;
			$(document).trigger('status.reconnect.attempt',[]);
			$.apmr.connect(function(){ // callback called when reconnect times out
				if($.apmr.isConnectingOrConnected()) return;
				if($.apmr.config.ws) {
					$.apmr.config.ws.close();
				}
				//$.apmr.config.reconnectTimeoutHandle = $.apmr.startReconnectLoop();
			});
		}, $.apmr.config.reconnectPauseTime);
	},
	
	$.apmr.connect = function(callback) {
		// send connecting event
		console.info("Connecting to [%s]", cli.config.wsUrl);
		$.apmr.config.connecting = true;		
		var cli = this;
		$.apmr.config.connectTimeoutHandle = setTimeout(function(){
			$.apmr.onConnectionTimeout();
			if(callback!=null) callback();
		},$.apmr.config.connectionTimeout);
		setTimeout(function(){
			cli.config.ws = new WebSocket(cli.config.wsUrl);
			cli.config.ws.c = cli;
			cli.config.ws.onopen = $.apmr.onOpen;
			cli.config.ws.onerror = $.apmr.onError;
			cli.config.ws.onclose = $.apmr.onClose;
			cli.config.ws.onmessage = $.apmr.onMessage;
		},1);
		console.info("Connecting.....");
	},
	
	/**
	 * Called when the connection attempt timesout
	 */
	$.apmr.onConnectionTimeout = function() {
		if($.apmr.isConnectingOrConnected() || $.apmr.isReconnectScheduled()) return;
		console.info("Connect timeout");
		// send connect timeout event
		$.apmr.config.connecting = false;
		$.apmr.config.connectTimeoutHandle = -1;
		if($.apmr.config.ws) $.apmr.config.ws.close();
	},
	
	
	$.apmr.onOpen = function() {
	    console.info("WebSocket Opened");
	    clearTimeout(this.c.config.connectTimeoutHandle);
	    clearTimeout($.apmr.config.reconnectTimeoutHandle);
	    this.c.config.connectTimeoutHandle = -1;
	    //this.c.sendWho();
	    $(document).trigger('status.connected',[true]);
	    $.apmr.subtreeOn();

	},
	$.apmr.onError = function(e) {
		console.info("WebSocket Error");
		console.dir(e);		
	},
	$.apmr.onClose = function() {
		$.apmr.config.ws = false;
		console.info("WebSocket Closed"); 
		$(document).trigger('status.connected',[false]);
		$.apmr.startReconnectLoop();
	},
	$.apmr.onMessage = function(event) {
		try {
			var json = JSON.parse(event.data);
			if(json.sessionid !=null) {
				this.c.sessionId = json.sessionid;
				console.info("Set SessionID [%s]", this.c.sessionId);
				$(document).trigger('connection.session',[this.c.sessionId]);
			} else {
				var topic = '/' + 'req' + '/' + json.rerid;
//				$.publish(topic, [json]);
			}
		} finally {
		}		
	},
	$.apmr.send = function(req, callback) {
		var rid = this.config.requestId++;
		if(callback!=null) {
			var topic = '/' + req.t + '/' + rid;
			$.oneTime(topic, callback);
			//console.info("Registered Callback for oneTime [%s]", topic);
		}
		req['rid']=rid;
		this.config.ws.send(JSON.stringify(req));
		return rid;
	}
	
})(jQuery);
