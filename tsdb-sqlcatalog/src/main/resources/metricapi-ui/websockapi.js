//========================================================================
//  WebSock API
//========================================================================

// Sample Call
//	var ws = new WebSocketAPIClient();
//	var q = QueryContext.newContext();
//	ws.getMetricNames(q, ['host', 'type', 'cpu']);

function WebSocketAPIClient(props) { 
	this.wsUrl = (props && props.wsUrl) ? props.wsUrl : 'ws://' + document.location.host + '/ws'; 
	this.onclose = (props && props.onclose) ? new Array(props.onclose).concat() : [];
	this.onerror = (props && props.onerror) ? new Array(props.onerror).concat() : [];
	this.onmessage = (props && props.onmessage) ? new Array(props.onmessage).concat() : [];
	this.onopen = (props && props.onopen) ? new Array(props.onopen).concat() : [];
	this.session = null;
	this.ridseq = 0;
	this.ws = new WebSocket(this.wsUrl);
	this.bus = new Bacon.Bus();
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

WebSocketAPIClient.newClient = function(props) {
	var d = jQuery.Deferred();
	// TODO:  implement a ctor like function that returns  an onConnect promise.
	return p.promise();
};

WebSocketAPIClient.filters = {
	reridFilter : function(rerid) {
		return function(event) {
			return (event.rerid!=null && event.rerid == rerid);
		}
	}
};

WebSocketAPIClient.defaultHandlers = {
		onclose: function(evt, client) { console.info("WebSocket Closed: [%O]", evt); client.session = null;},
		onerror: function(evt, client) { console.error("WebSocket Error: [%O]", evt); },
		onmessage: function(evt, client) { 
			// console.info("WebSocket Message: [%O]", evt);
			try {
				var result = JSON.parse(evt.data);
				client.bus.push(result);
				console.group("=================  Raw Result =================");
				console.dir(result);
				try {
					if(result.msg!=null && result.msg.q != null && result.msg.q.ctx)  {
						QueryContext.getCtx(result.msg.q.ctx);
					}
				} catch (e) {
					console.error("Failed to get context", e);
				}
				
				console.groupEnd();
				if(result.rerid) {
					var continuing = QueryContext.readContext(result.msg.q).shouldContinue();
					// console.info("Response: [%O]", result);
					var pendingRequest = continuing ? client.getPendingRequest(result.rerid) : client.completePendingRequest(result.rerid);
					if(pendingRequest!=null) {
						console.debug("Retrieved Pending Request XXX [%O]", pendingRequest);
						if(!pendingRequest.request || !pendingRequest.request.q) {
							pendingRequest.d.resolveWith(client,[evt.data]);
							return;
						}
						if(pendingRequest.cb) pendingRequest.cb(result);
						try {
							if(pendingRequest.d) {
								if(pendingRequest.request && pendingRequest.request.q) {
			 						pendingRequest.request.q.refresh(result.msg.q);
			 						if(!continuing) delete pendingRequest.request.q;	 
			 						else client.resetPendingRequest(result.rerid);
			 						var callback =  [{
										data: result.msg.results,
										q: result.msg.q,
										id: result.id,
										op: result.op,
										rerid: result.rerid,
										type: result.t,
										request: pendingRequest.request
									}];
									if(pendingRequest.d.startTime) {
										callback[0].q.elapsedTime = performance.now() - pendingRequest.d.startTime;
									}
									if(continuing) {
										pendingRequest.d.notifyWith(client,callback);
									} else {
										pendingRequest.d.resolveWith(client,callback);
									}
								} else {
									pendingRequest.d.resolveWith(client,[evt.data]);
								}								
							}
						} catch (e) {
							console.error("Failed to marshall response: [%O]", e);
						}
					}
				}
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

WebSocketAPIClient.prototype.pendingRequests = {};

WebSocketAPIClient.prototype.addPendingRequest = function(pr) {	
	var timeout = 1000;
	try {
		timeout = pr.q.getTimeout();
	} catch (e) {
		timeout = 1000;
	}
	if(pr!=null && pr.rid!=null) {
		var self = this;
		this.pendingRequests[pr.rid] = pr;		
		var thandle = setTimeout(function(){
			console.warn("Pending Request Timeout: [%O]", pr);
			delete self.pendingRequests[pr.rid];
			if(pr.d) {
				pr.d.rejectWith(self, ["timeout", pr]);
			}
		}, timeout);
		pr.th = thandle;
	} else {
		throw new Error("Invalid Pending Request: [" + ((pr==null) ? null : pr + "]")); // TODO: need a render for pendingRequest
	}
};

WebSocketAPIClient.prototype.completePendingRequest = function(rid) {
	var pendingRequest = this.pendingRequests[rid];
	if(pendingRequest!=null) {
		delete this.pendingRequests[rid];
		//console.debug("Deleted Pending Request for RID:%s", rid);
		try { 
			clearTimeout(pendingRequest.th);			
			//console.debug("Cleared Timeout for RID:%s", rid);
		} catch (e) {}
	}
	return pendingRequest;
}

WebSocketAPIClient.prototype.getPendingRequest = function(rid) {
	return this.pendingRequests[rid];
}

WebSocketAPIClient.prototype.resetPendingRequest = function(rid) {
	var pr = this.pendingRequests[rid];
	if(pr.th) {
		clearTimeout(pr.th);		
		console.debug("Cleared Timeout [%s]", pr.th);	
	}
	var timeout = 1000;
	try {
		timeout = pr.q.getTimeout();
	} catch (e) {
		timeout = 1000;
	}
	var self = this;	
	pr.th = setTimeout(function(){
		console.warn("Pending Request Timeout id [%s]: [%O]", pr.th,  pr);
		delete self.pendingRequests[pr.rid];
		if(pr.d) {
			pr.d.rejectWith(self, ["timeout", pr]);
		}
	}, timeout);	
	console.debug("Timeout Reset to [%s] for [%s]", timeout, pr.th);
}

WebSocketAPIClient.prototype.close = function() {
	if(this.ws) this.ws.close();
};


WebSocketAPIClient.prototype.isPromise = function(value) {
    if (typeof value.then !== "function") {
        return false;
    }
    var promiseThenSrc = String(jQuery.Deferred().then);
    var valueThenSrc = String(value.then);
    return promiseThenSrc === valueThenSrc;
}

/*
* Schedule an invocation or invocations of f() in the future.
* Wait start milliseconds, then call f() every interval milliseconds,
* stopping after a total of start+end milliseconds.
* If interval is specified but end is omitted, then never stop invoking f.
* If interval and end are omitted, then just invoke f once after start ms.
* If only f is specified, behave as if start was 0.
* Note that the call to invoke() does not block: it returns right away.
*/
WebSocketAPIClient.prototype.retry = function invoke(fx, args, start, interval, end) {
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
	
	// ["resp", "xmresp", "xmsub"]:  these types mean deregister
	
	var deferred = jQuery.Deferred();
	deferred.startTime = performance.now();
	if(this.ws.readyState!=1) {
		var self = this;
		var args = [];
		for(var i = 0, l = arguments.length; i < l; i++) {
			args.push(arguments[i]);
		}		
		args.push(deferred);
		try {
			var fx = function() {
				console.debug("Retrying svc:[%s], op:[%s], args ---> [%O]", service, opname, args);				
				//self.serviceRequest(service, opname, args);
				self.serviceRequest.apply(self, args);
			}
			this.retry(fx, args, 100);			
		} catch (e) {
			console.error("Delayed service request failed: svc:[%s], op:[%s], err:[%O]", service, opname, e);
		}
		return deferred.promise();
		//throw "Failed to call service. Socket in state: [" + this.ws.readyState + "]";
	}
	// {"t":"req", "rid":1, "svc":"meta", "op":"metricnames", "q": { "pageSize" : 10 }, "keys" : ["host", "type", "cpu"] }
	
	var minArgSize = 2;

	var RID = ++this.ridseq;	
	var obj = {
			t: "req",
			rid: RID,
			svc: service,
			op: opname			
	};
	if(arguments.length > minArgSize) {
		var args = [];
		for(var i = minArgSize; i < arguments.length; i++) {
			var payload = arguments[i];
			if(this.isPromise(arguments[i])) {
				console.debug("Replacing Deferred [%O] with [%O]", deferred, payload);
				deferred = payload;
				payload = null;
			}
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
	console.debug("Service Request: rid:[%s] svc:[%s], op:[%s], payload:[%O]", RID, service, opname, obj);
	var Q = null;
	try { Q = arguments[minArgSize].q; } catch (e) {}
	
	
	var pendingRequest = {
		rid: RID,
		th: -1,
		d: deferred,
		q: Q,
		self: this,
		request: obj,
		cb: null
		// function(result) {
		// 	console.info("THIS: ----> [%O]", this.self);
		// 	if(result.op=="ok") {
		// 		console.group("============ Call Successful ============");
		// 		console.info("Result: [%O]", result);
		// 		console.groupEnd();
		// 	} else {
		// 		console.group("============ Call Failed ============");
		// 		console.error("Code: [%s]", result.op);
		// 		console.groupEnd();
		// 	}
		// }
	};
	pendingRequest.self = pendingRequest;
	this.addPendingRequest(pendingRequest);
	console.group("Raw JSON Request");
	console.info(JSON.stringify(obj))
	console.groupEnd();	
	this.ws.send(JSON.stringify(obj));
	var promise = deferred.promise();
	promise.rid = RID;
	return promise;
	
};

WebSocketAPIClient.prototype.findUids= function(queryContext, type, name) {	
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "finduid", {q: queryContext, name: name, type: type});
};

WebSocketAPIClient.prototype.getAnnotations = function(queryContext, expression, startTime, endTime) {	
	if(queryContext==null) queryContext= QueryContext.newContext();
	var payload = {
		q: queryContext, 
		x: expression	
	}
	var range = [];
	if(startTime!=null) {
		range.push(startTime);
	}
	if(endTime!=null) {
		range.push(endTime);	
	}	
	payload.r = range;
	return this.serviceRequest("meta", "annotations", payload);
};

WebSocketAPIClient.prototype.getMetricNamesByKeys = function(queryContext, tagKeys) {
	if(tagKeys && (typeof tagKeys == 'object' && !Array.isArray(tagKeys))) throw new Error("The tagKeys argument must be an array of tag key values, or a single tag key value");
	if(queryContext==null) queryContext= QueryContext.newContext();
	var keys = new Array().concat(tagKeys);
	var prom = this.serviceRequest("meta", "metricnames", {q: queryContext, keys: keys});
	return prom;
};

WebSocketAPIClient.prototype.getMetricNamesByTags = function(queryContext, tags) {
	if(tags==null || !jQuery.isPlainObject(tags) || jQuery.isEmptyObject(tags) ) throw new Error("The tags argument must be map of key values");
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "metricswtags", {q: queryContext, tags: tags});	
};

WebSocketAPIClient.prototype.getTSMetas = function(queryContext, metricName, tags) {
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "tsmetas", {q: queryContext, tags: tags||{}, m: metricName||"*"});	
};

WebSocketAPIClient.prototype.getTagKeys = function(queryContext, metricName, tagKeys) {
	if(tagKeys && (typeof tagKeys == 'object' && !Array.isArray(tagKeys))) throw new Error("The tagKeys argument must be an array of tag key values, or a single tag key value");
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "tagkeys", {q: queryContext, keys: tagKeys||[], m: metricName||""});	
};

WebSocketAPIClient.prototype.getTagValues = function(queryContext, metricName, tagKey, tags) {   // || jQuery.isEmptyObject(tags)
	if(tags==null) tags = {};
	if(!jQuery.isPlainObject(tags)) throw new Error("The tags argument must be map of key values");
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "tagvalues", {q: queryContext, tags: tags||{}, m: metricName||"", k: tagKey});	
};

WebSocketAPIClient.prototype.resolveTSMetas = function(queryContext, expression) {
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "tsMetaEval", {q: queryContext, x: expression||"*:*"});	
};

WebSocketAPIClient.prototype.overlap = function(queryContext, expressionOne, expressionTwo) {
	if(queryContext==null) queryContext= QueryContext.newContext();
	var parms = {q: queryContext, x: expressionOne, y: expressionTwo};
	return this.serviceRequest("meta", "overlap", parms);	
};


WebSocketAPIClient.prototype.d3TSMetas = function(queryContext, expression) {
	if(queryContext==null) queryContext= QueryContext.newContext();
	return this.serviceRequest("meta", "d3tsmeta", {q: queryContext, x: expression||"*:*"});	
};

WebSocketAPIClient.prototype.subscribe = function(expression) {
	this.ws.send(JSON.stringify(
		{t: "sub", rid: 1, svc:'pubsub', op:'sub', x:expression}
	));
};

WebSocketAPIClient.prototype.services = function() {	
	var sr =  this.serviceRequest("router", "services");	
	var dispose = null;
	dispose = this.bus.filter(WebSocketAPIClient.filters.reridFilter(sr.rid)).onValue(function(svcResponse) {
		console.group("===== BUS EVENT =====");
		console.info("Services Response [%O]", svcResponse);
		console.info("Sub Handle [%O]", dispose);
		dispose();
		console.info("Disposed");		
		console.groupEnd();
	});
};

WebSocketAPIClient.mapTags = function(tsmeta) {
	var map = {};
	var key = null, value = null;
	for(var i = 0, x = tsmeta.tags.length; i < x; i++) {
		var tag = tsmeta.tags[i];
		if(tag.type=="TAGK") key = tag.name;
		else {
			value = tag.name;
			map[key] = value;
		}
	}
	return map;
}

WebSocketAPIClient.arrTags = function(tsmeta) {
	var arr = [];
	for(var i = 0, x = tsmeta.tags.length; i < x; i++) {
		push(tsmeta.tags[i].name);
	}
	return arr;
}
