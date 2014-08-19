
//========================================================================
//   QueryContext definition
//========================================================================

function QueryContext(props) { 
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.pageSize = (props && props.pageSize) ? props.pageSize : 100;
	this.maxSize = (props && props.maxSize) ? props.maxSize : 5000;
	this.exhausted = false;
	this.cummulative = 0;
}

QueryContext.newContext = function(props) {
	return new QueryContext(props);
}


QueryContext.prototype.getNextIndex = function() {
	return this.nextIndex;
};

QueryContext.prototype.getCummulative = function() {
	return this.cummulative;
};

QueryContext.prototype.getPageSize = function() {
	return this.pageSize;
};

QueryContext.prototype.getMaxSize = function() {
	return this.maxSize;
};

QueryContext.prototype.isExhausted = function() {
	return this.exhausted;
};

QueryContext.prototype.update = function(props) {
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.exhausted = (props && props.exhausted) ? props.exhausted : false;
	this.cummulative = (props && props.cummulative) ? props.exhausted : 0;	
}

QueryContext.prototype.toString = function() {
	return "{nextIndex:" + this.nextIndex + ", pageSize:" + this.pageSize + ", maxSize:" + this.maxSize + ", exhausted:" + this.exhausted + ", cummulative:" + this.cummulative + "}";
};

//========================================================================
//  WebSock API
//========================================================================

function WebSocketAPIClient(props) { 
	this.wsUrl = (props && props.wsUrl) ? props.wsUrl : 'ws://' + document.location.host + '/ws'; 
	this.onclose = (props && props.onclose) ? new Array(props.onclose).concat() : [];
	this.onerror = (props && props.onerror) ? new Array(props.onerror).concat() : [];
	this.onmessage = (props && props.onmessage) ? new Array(props.onmessage).concat() : [];
	this.onopen = (props && props.onopen) ? new Array(props.onopen).concat() : [];
	this.session = null;
	this.ridseq = 0;
	this.ws = new WebSocket(this.wsUrl);
	var self = this;
	this.ws.onclose = function(evt) {
		WebSocketAPIClient.defaultHandlers.onclose(evt, self);
		for(index in self.onclose) {
			self.onclose[index](evt, self);
		}
	}
	this.ws.onerror = function(evt) {
		WebSocketAPIClient.defaultHandlers.onerror(evt, self);
		for(index in self.onerror) {
			self.onerror[index](evt, self);
		}
	}
	this.ws.onopen = function(evt) {
		WebSocketAPIClient.defaultHandlers.onopen(evt, self);
		for(index in self.onopen) {
			self.onopen[index](evt, self);
		}		
	}
	this.ws.onmessage = function(evt) {
		WebSocketAPIClient.defaultHandlers.onmessage(evt, self);
		for(index in self.onmessage) {
			self.onmessage[index](evt, self);
		}				
	}
};

WebSocketAPIClient.defaultHandlers = {
		onclose: function(evt, client) { console.info("WebSocket Closed: [%O]", evt); client.session = null;},
		onerror: function(evt, client) { console.info("WebSocket Error: [%O]", evt); },
		onmessage: function(evt, client) { 
			console.info("WebSocket Message: [%O]", evt);
			try {
				console.info("Response: [%O]", JSON.parse(evt.data));
			} catch (e) {
				console.error(e);
			}

			if(client.session==null) {
				var data = JSON.parse(evt.data);
				if(data.sessionid) {
					client.session = data.sessionid;
					console.info("Session ID: [%s]", client.session);
				} else {
				}
			}
		},
		onopen: function(evt, client) { console.info("WebSocket Opened: [%O]", evt); }
};


WebSocketAPIClient.prototype.close = function() {
	if(this.ws) this.ws.close();
};

/*
* Schedule an invocation or invocations of f() in the future.
* Wait start milliseconds, then call f() every interval milliseconds,
* stopping after a total of start+end milliseconds.
* If interval is specified but end is omitted, then never stop invoking f.
* If interval and end are omitted, then just invoke f once after start ms.
* If only f is specified, behave as if start was 0.
* Note that the call to invoke() does not block: it returns right away.
*/
WebSocketAPIClient.prototype.invokeOnOpen = function invoke(fx, args, start, interval, end) {
	if (!start) start = 0;
	// Default to 0 ms
	if (arguments.length <= 3) {
		// Single-invocation case
		setTimeout(fx, start, args);
	} else {
		setTimeout(repeat, start, args); // Repetitions begin in start ms
		function repeat() {
			// Invoked by the timeout above
			var h = setInterval(fx, interval, args); // Invoke f every interval ms.
			// And stop invoking after end ms, if end is defined
			if (end) setTimeout(function() { clearInterval(h); }, end);
		}		
	}
};


WebSocketAPIClient.prototype.serviceRequest = function(service, opname) {
	if(this.ws.readyState!=1) {
		invoke(serviceRequest, arguments, 100);
		//throw "Failed to call service. Socket in state: [" + this.ws.readyState + "]";
	}
	// {"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 10 }, "keys" : ["host", "type", "cpu"] }
	this.ridseq++;
	var obj = {
			t: "req",
			rid: this.ridseq,
			svc: service,
			op: opname			
	};
	if(arguments.length > 2) {
		var args = [];
		for(var i = 2; i < arguments.length; i++) {
			var payload = arguments[i];
			if(payload!=null) {
				if(typeof payload === 'object') {
					for(index in payload) {
						obj[index] = payload[index];
					}
				} else {
					args = args.concat(payload);
				}
			}
		}
		if(args.length>0) {
			obj.args = args;
		}
	}	
	console.debug("Service Request: svc:[%s], op:[%s], payload:[%O]", service, opname, obj);
	this.ws.send(JSON.stringify(obj));
};

WebSocketAPIClient.prototype.getMetricNames = function(queryContext, tagKeys) {
	if(tagKeys && (typeof tagKeys == 'object' && !Array.isArray(tagKeys))) throw "The tagKeys argument must be an array of tag key values, or a single tag key value";
	var q = queryContext | QueryContext.newContext();
	var keys = new Array().concat(tagKeys);
	this.serviceRequest("meta", "metricnames", {q: queryContext, keys: keys});
};

/*

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.getMetricNames(q, ['host', 'type', 'cpu']);

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.serviceRequest("meta", "metricnames", {q: q, keys : ['host', 'type', 'cpu']}) 


 */


//========================================================================
// jQuery init
//========================================================================

(function( $ ){
	$(function(){
		// jQuery Init Stuff Here.
		console.info("Initialized jQuery");
	});				
})( jQuery );	
