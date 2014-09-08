
//========================================================================
//   QueryContext definition
//========================================================================

function QueryContext(props) { 
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.pageSize = (props && props.pageSize) ? props.pageSize : 100;
	this.maxSize = (props && props.maxSize) ? props.maxSize : 5000;
	this.timeout = (props && props.timeout) ? props.timeout : 3000;
	this.continuous = (props && props.continuous) ? props.continuous : false;	
	this.format = (props && props.format) ? props.format : "DEFAULT";	
	this.exhausted = false;
	this.cummulative = 0;
	this.elapsed = -1;
	this.expired = false;			
}

QueryContext.newContext = function(props) {
	return new QueryContext(props);
}

QueryContext.readContext = function(props) {
	return new QueryContext(props);	
}

QueryContext.prototype.shouldContinue = function() {
	console.info("Max: c:[%s], m:[%s]", this.cummulative , this.maxSize)
	return this.continuous && this.nextIndex != null && !this.isExpired() && !this.isExhausted() && (this.cummulative < this.maxSize);
}

QueryContext.prototype.clone = function() {
    try {
    	return JSON.parse(JSON.stringify(this));
    } catch (e) {
    	throw new Error("Unable to copy this! Its type isn't supported.");
    }
}


QueryContext.prototype.getFormat = function() {
	return this.format;
};

QueryContext.prototype.format = function(format) {
	var f = format.replace(/ /g, "").toUpperCase();
	this.format = f;
	return this;
};


QueryContext.prototype.getElapsed = function() {
	return this.elapsed;
};

QueryContext.prototype.getNextIndex = function() {
	return this.nextIndex;
};

QueryContext.prototype.isContinuous = function() {
	return this.continuous;
};


QueryContext.prototype.nextIndex = function(index) {
	this.nextIndex = index;
	return this;
};


QueryContext.prototype.getCummulative = function() {
	return this.cummulative;
};

QueryContext.prototype.getPageSize = function() {
	return this.pageSize;
};

QueryContext.prototype.pageSize = function(size) {
	this.pageSize = size;
	return this;
};


QueryContext.prototype.getMaxSize = function() {
	return this.maxSize;
};

QueryContext.prototype.maxSize = function(size) {
	this.maxSize = size;
	return this;
};

QueryContext.prototype.continuous = function(enabled) {
	return this.continuous = enabled;
	return this;
};


QueryContext.prototype.isExhausted = function() {
	return this.exhausted;
};

QueryContext.prototype.isExpired = function() {
	return this.expired;
};

QueryContext.prototype.getTimeout = function() {
	return this.timeout;
};

QueryContext.prototype.timeout = function(time) {
	this.timeout = time;
	return this;
};

QueryContext.getCtx = function(ctx) {
	var sortable = [];
	for (var item in ctx) {
	    sortable.push([item, ctx[item]])
	}
	sortable.sort(function(a, b) {return a[1] - b[1]});
	console.group("======================  CTX ======================");
	var prior = -1,	elapsed = 0, total = 0;
	for(var i = 0, x = sortable.length; i < x; i++) {
		if(prior != -1) {
			elapsed = sortable[i][1] - prior;			
		}
		prior = sortable[i][1];
		total += elapsed;
		console.info("Step: [%s] -- [%s] -- [%s]   ----- Total: [%s]", sortable[i][0], prior, elapsed, total);
	}
	console.groupEnd();
	return sortable;
}



QueryContext.prototype.refresh = function(props) {
	if(props!=null) {
		this.nextIndex = props.nextIndex!=null ? props.nextIndex : null;
		this.exhausted = props.exhausted!=null ? props.exhausted : false;
		this.cummulative = props.cummulative!=null ? props.cummulative : 0;
		this.elapsed = props.elapsed!=null ? props.elapsed : -1;
		console.info("ReadContext Elapsed: [%s]", this.elapsed);
		this.expired = props.expired!=null ? props.expired : false;
		this.format = props.format!=null ? props.formar : "DEFAULT";	
		if(props.timeout!=null) this.timeout = props.timeout;
	}
}

QueryContext.prototype.toString = function() {
	return "{timeout:" + this.timeout + ", nextIndex:" + this.nextIndex + ", pageSize:" + this.pageSize + ", maxSize:" + this.maxSize + ", exhausted:" + this.exhausted + ", cummulative:" + this.cummulative + ", expired:" + this.expired + ", elapsed:" + this.elapsed + "}";
};

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

Array.prototype.last = function() {
	if(this.length>0) {
		return this[this.length-1];
	} else {
		return null;
	}
}


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


function testSub() {
	//var expr = $('#exprfield').val() + ",*";
	var expr = "sys*:dc=dc1,host=WebServer1,*"
	console.info("Subscribing to expression [%s]", expr);
	ws.subscribe(expr);
	// .then(
	// 	function(data) {
	// 		console.info("SUB TERM: [%O]", data);
	// 	},
	// 	function(data) {
	// 		console.error("SUB ERROR: [%O]", arguments);
	// 	},
	// 	function(data) {
	// 		console.info("SUB EVENT: [%O]", data);
	// 	}
	// );
}

/*
	SUB STARTED:
	============
	id: 1486981487
	msg: Object
	subId: 1
	__proto__: Object
	op: "subst"
	rerid: 1
	t: "resp"

	SUB EVENT:
	==========
	metric: "sys.cpu"
	subid: 2
	tags: Object
	cpu: "1"
	dc: "dc4"
	host: "WebServer1"
	type: "sys"
	__proto__: Object
	ts: 1409783673
	type: "l"
	value: 17


	SUB TERM:
	=========	
*/


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

function testIncremental(startingKey) {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({timeout: 10000});

	var root = MetaTree.newInstance(startingKey);	

	var onKey = function(data, tree, state) {
		console.group("================ onKey ================");
		try {
			if(data==null || data.data==null || data.data.length==0) {
				console.info("Key Exhausted at [%O]", state);
				return;
			}

			var key = data.data.last().name;
			state.keys.push(key);
			//console.info("Last Key: [%s]", key);
			var nextTree = tree.tag(tree, key);
			ws.getTagValues(QueryContext.newContext({timeout: 10000}), 'sys.cpu', key, state.tags).then(function(data) { onValue(data, nextTree, state); });	
		} finally {
			console.groupEnd();
		}
		//ws.getTagKeys(q, 'sys.cpu', ['dc']).then(	
	}
	var onValue = function(data, tree, state) {
		//ws.getTagValues(q, 'sys.cpu', 'type', {host:'*Server*', cpu:'*'}).then(
		//data.data[x].name 
		console.group("================ onValue ================");
		//console.debug("Data: [%O], Tree: [%O], State: [%O]", data, tree, state);
		try {
			jQuery.each(data.data, function(index, uid){
				//console.info("value: [%s]", uid.name);
				var nextTree = tree.tag(tree, uid.name);
				state.tags[state.keys.last()];
				ws.getTagKeys(QueryContext.newContext({timeout: 10000}), 'sys.cpu', state.keys).then(function(data) { onKey(data, nextTree, state.clone()); });
			});
		} finally {
			console.groupEnd();
		}
	}
	var state = {
		tags: {},
		keys: [startingKey]
	}
	ws.getTagValues(QueryContext.newContext({timeout: 10000}), 'sys.cpu', startingKey, {}).then(function(data) { onValue(data, root, state); });


}

var ANNOTATION_URL = 'http://' + document.location.host + '/api/annotation'; 

function postGlobalAnnotations() {
	var callback = function(result) {
		console.info("Global Annotation Post Result: [%O]", result);
	}
	for(var i = 0; i < 100; i++) {
		var data = {
			startTime : new Date().getTime() - (i * 1000),
		  	description : "Test Annotation #" + i,
		  	notes : "This is a test annotation, with id [" + i + "]",
		  	custom : {
		    	type : "Global"
		  	}
		};
		$.post(ANNOTATION_URL, JSON.stringify(data), callback, "json");
	}
}

function postTSUIDAnnotations() {
	var callback = function(result) {
		console.info("TSUID Annotation Post Result: [%O]", result);
	}
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({pageSize: 100, maxSize: 100, timeout: 10000});	
	ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer*").then(
		function(result) {
			console.info("TSUID: [%O]", result);
			for(var i = 0, x = result.data.length; i < x; i++) {
				var t = result.data[i];
				var data = {
					startTime : new Date().getTime() - (i * 1000),
				  	description : "Test TSUID Annotation #" + i,
				  	notes : "This is a test annotation, with id [" + i + "]",
				  	tsuid : t.tsuid,
				  	custom : {
				    	type : "TSUID",
				    	tsuid : t.tsuid,
				    	name: t.name
				  	}
				};
				$.post(ANNOTATION_URL, JSON.stringify(data), callback, "json");
			}
		}
	);
}


function testAll() {
	if(ws==null) {
		ws = new WebSocketAPIClient();
	}
	var q = null;
	ws.findUids(q, "TAGV", "Web*").then(
		function(result) { console.info("findUids Result: [%O]", result); },
		function() { console.error("findUids Failed: [%O]", arguments);}
	);
	// ws.getAnnotations(q, "sys*:dc=dc1,host=WebServer1|WebServer5", 0).then(
	// 	function(result) { console.info("globalAnnotations Result: [%O]", result); },
	// 	function() { console.error("globalAnnotations Failed: [%O]", arguments);}
	// );

	

	// ws.getMetricNamesByKeys(q, ['host', 'type', 'cpu']).then(
	// 	function(result) { console.info("MetricNamesByKeys Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("MetricNamesByKeys Failed: [%O]", arguments);}
	// );
	// ws.getMetricNamesByTags(q, {host:'WebServer1'}).then(
	// 	function(result) { console.info("MetricNamesByTags Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("MetricNamesByTags Failed: [%O]", arguments);}
	// )
	// ws.getTSMetas(QueryContext.newContext({continuous:true, pageSize: 50, timeout: 60000}), 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
	// ws.getTSMetas(QueryContext.newContext({continuous:true, pageSize: 500, timeout: 60000, maxSize: 40000}), '*', {dc:'*'}).then(		
	// 	function(result) { console.info("TSMetas FINAL Result: [%O]  --  cummulative: %s", result, result.q.cummulative);},
	// 	function() { console.error("TSMetas Failed: [%O]", arguments);},
	// 	function(result) { console.info("TSMetas INTERRIM Result: [%O]", result);}
	// );
	// ws.getTagKeys(q, 'sys.cpu', ['dc', 'host', 'cpu']).then(
	// 	function(result) { console.info("TagKeys Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("TagKeys Failed: [%O]", arguments);}
	// );
	// ws.getTagValues(q, 'sys.cpu', 'type', {host:'*Server*', cpu:'*'}).then(
	// 	function(result) { console.info("TagValues Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("TagValues Failed: [%O]", arguments);}
	// );
	// ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer1|WebServer5").then(
	// 	function(result) { console.info("resolveTSMetas Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("resolveTSMetas Failed: [%O]", arguments);}
	// );

};

var EXPR = "*:dc=*";

function testTree(expr) {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({pageSize: 20000, maxSize: 20000, timeout: 10000});
	console.info("QC: %s", q.toString());
	if(EXPR == null) EXPR = "*:dc=*";

	// ws.d3TSMetas(q, "sys.cpu:dc=dc1,host=WebServer1|WebServer5").then(
	ws.d3TSMetas(q, expr || EXPR).then(	
		function(d3Data) {
			console.info("D3 Data: [%O]", d3Data.data);
			dgo(d3Data.data);
			console.info("QC: %O", q);
		}
	);
}

function dgo(root) {

  var nodes = cluster.nodes(root),
      links = cluster.links(nodes);

  var link = svg.selectAll(".link")
      .data(links)
    .enter().append("path")
      .attr("class", "link")
      .attr("d", diagonal);

  var node = svg.selectAll(".node")
      .data(nodes)
    .enter().append("g")
      .attr("class", "node")
      .attr("transform", function(d) { 
      	//console.info("Transform: [%O]", d);
      	return "translate(" + d.y + "," + d.x + ")"; 
      })

  node.append("circle")
      .attr("r", 4.5);

  node.append("text")
      .attr("dx", function(d) { return d.children ? -8 : 8; })
      .attr("dy", 3)
      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
      .text(function(d) { return d.name; });
	
}


/*

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.getMetricNames(q, ['host', 'type', 'cpu']);

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.serviceRequest("meta", "metricnames", {q: q, keys : ['host', 'type', 'cpu']}) 

function dgo(root) {

  var nodes = cluster.nodes(root),
      links = cluster.links(nodes);

  var link = svg.selectAll(".link")
      .data(links)
    .enter().append("path")
      .attr("class", "link")
      .attr("d", diagonal);

  var node = svg.selectAll(".node")
      .data(nodes)
    .enter().append("g")
      .attr("class", "node")
      .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })

  node.append("circle")
      .attr("r", 4.5);

  node.append("text")
      .attr("dx", function(d) { return d.children ? -8 : 8; })
      .attr("dy", 3)
      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
      .text(function(d) { return d.name; });
	
}


var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.getTSMetas(q, 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
	function(result) { console.info("TSMetas Result: [%O]", result); 

		var d3Data = {
			root: {
				children : [],
				name : "org"
			}
		}
		var root = d3Data.root;

		try {
			dgo(root);
		}  catch (e) {
			console.error(e);
		}
	},
	function() { console.error("TSMetas Failed: [%O]", arguments);}
);




 */


function flare() {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext();
	ws.getTSMetas(q, 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
		function(result) { console.info("TSMetas Result: [%O]", result); 

			var d3Data = {
				root: {
					children : [],
					name : "org"
				}
			}
			var root = d3Data.root;
			
			try {
				dgo(root);
			}  catch (e) {
				console.error(e);
			}
		},
		function() { console.error("TSMetas Failed: [%O]", arguments);}
	);

}

//========================================================================
// jQuery init
//========================================================================

(function( $ ){
	$(function(){
		// jQuery Init Stuff Here.
		console.info("Initialized jQuery");
	});				
})( jQuery );	


function syntaxHighlight(json) {
    if (typeof json != 'string') {
         json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}


/*

var obj = {a:1, 'b':'foo', c:[false,'false',null, 'null', {d:{e:1.3e5,f:'1.3e5'}}]};
var str = JSON.stringify(obj, undefined, 4);

output(str);
output(syntaxHighlight(str));

=================== CSS  =====================

pre {outline: 1px solid #ccc; padding: 5px; margin: 5px; }
.string { color: green; }
.number { color: darkorange; }
.boolean { color: blue; }
.null { color: magenta; }
.key { color: red; }




 */