
//========================================================================
//   QueryContext definition
//========================================================================

function QueryContext(props) { 
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.pageSize = (props && props.pageSize) ? props.pageSize : 100;
	this.maxSize = (props && props.maxSize) ? props.maxSize : 5000;
	this.timeout = (props && props.timeout) ? props.timeout : 3000;
	this.continuous = (props && props.continuous) ? props.continuous : false;	
	this.exhausted = false;
	this.cummulative = 0;
	this.elapsed = -1;
	this.expired = false;	
	
}

QueryContext.newContext = function(props) {
	return new QueryContext(props);
}

QueryContext.prototype.shouldContinue = function() {
	return this.continuous && this.nextIndex != null && !this.isExpired() && !this.isExhausted() && (this.cummulative < this.maxSize);
}

QueryContext.prototype.clone = function() {
    try {
    	return JSON.parse(JSON.stringify(this));
    } catch (e) {
    	throw new Error("Unable to copy this! Its type isn't supported.");
    }
}


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


QueryContext.prototype.refresh = function(props) {
	if(props!=null) {
		this.nextIndex = props.nextIndex || null;
		this.exhausted = props.exhausted || false;
		this.cummulative = props.cummulative || 0;
		this.elapsed = props.elapsed || -1;
		this.expired = props.expired || false;
		if(props.timeout) this.timeout = props.timeout;
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

WebSocketAPIClient.defaultHandlers = {
		onclose: function(evt, client) { console.info("WebSocket Closed: [%O]", evt); client.session = null;},
		onerror: function(evt, client) { console.info("WebSocket Error: [%O]", evt); },
		onmessage: function(evt, client) { 
			// console.info("WebSocket Message: [%O]", evt);
			try {
				var result = JSON.parse(evt.data);
				if(result.rerid) {
					var continuing = QueryContext.newContext(result.msg[1]).shouldContinue();
					// console.info("Response: [%O]", result);
					var pendingRequest = continuing ? client.getPendingRequest(result.rerid) : client.completePendingRequest(result.rerid);
					if(pendingRequest!=null) {
						console.debug("Retrieved Pending Request XXX [%O]", pendingRequest);
						if(pendingRequest.cb) pendingRequest.cb(result);
						try {
							if(pendingRequest.d) {
		 						pendingRequest.request.q.refresh(result.msg[1]);
		 						if(!continuing) delete pendingRequest.request.q;	 						
		 						var callback =  [{
									data: result.msg[0],
									q: result.msg[1],
									id: result.id,
									op: result.op,
									rerid: result.rerid,
									type: result.t,
									request: pendingRequest.request
								}];
								if(continuing) {
									pendingRequest.d.notifyWith(client,callback);
								} else {
									pendingRequest.d.resolveWith(client,callback);
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
	if(pr!=null && pr.rid!=null && pr.q!=null) {
		var self = this;
		this.pendingRequests[pr.rid] = pr;		
		var thandle = setTimeout(function(){
			console.warn("Pending Request Timeout: [%O]", pr);
			delete self.pendingRequests[pr.rid];
			if(pr.d) {
				pr.d.rejectWith(self, ["timeout", pr]);
			}
		}, pr.q.getTimeout());
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
	var deferred = jQuery.Deferred();
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
	var Q = arguments[minArgSize].q;
	
	
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


WebSocketAPIClient.prototype.getMetricNamesByKeys = function(queryContext, tagKeys) {
	if(tagKeys && (typeof tagKeys == 'object' && !Array.isArray(tagKeys))) throw new Error("The tagKeys argument must be an array of tag key values, or a single tag key value");
	if(queryContext==null) queryContext= QueryContext.newContext();
	var keys = new Array().concat(tagKeys);
	var prom = this.serviceRequest("meta", "metricnames", {q: queryContext, keys: keys});
	console.debug("Returning async result promise [%O]", prom);
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
	return this.serviceRequest("meta", "tsmetaexpr", {q: queryContext, x: expression||"*:*"});	
};

WebSocketAPIClient.prototype.d3TSMetas = function(queryContext, expression) {
	if(queryContext==null) queryContext= QueryContext.newContext();
	var prom = this.serviceRequest("meta", "d3tsmeta", {q: queryContext, x: expression||"*:*"});	
	console.debug("Returning async result promise [%O]", prom);
	return prom;
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


function testAll() {
	var ws = new WebSocketAPIClient();
	var q = null;
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
	ws.getTSMetas(QueryContext.newContext({continuous:true, pageSize: 500, timeout: 60000}), 'sys.cpu', {host:'*'}).then(		
		function(result) { console.info("TSMetas FINAL Result: [%O]  --  cummulative: %s", result, result.q.cummulative);},
		function() { console.error("TSMetas Failed: [%O]", arguments);},
		function(result) { console.info("TSMetas INTERRIM Result: [%O]", result);}
	);
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