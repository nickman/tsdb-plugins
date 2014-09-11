// ================  bacon.test 
var filters = {
	// e.g. jsonPath(a, '$[?(@.rerid == 23)]')
	jsonPathFilter: function(expression) {
		return function(event) {
			if(event==null)	return null;
			var target = null;
			try {
				if(isEvent(event)) {
					target = event.value;
				} else {
					target = event;
				}
				return jsonPath(target, expression)!=false;
			} catch (e) {
				return false;
			}
		}
	},
	allFilter: function() {
		return function() {
			return 	true;
		}
	},	
	reridFilter : function(rerid) {
		return function(event) {
			try {
				if(event==null)	return null;
				var target = null;
				if(isEvent(event)) {
					target = event.value;
				} else {
					target = event;
				}			
				return (target.rerid!=null && target.rerid == rerid);
			} catch (e) { return false; }
		}
	}
};

var timeoutCountingStream = function(ms, cancelStream) {	
	var toh = null;
	var errInvoker = null;
	var actual = 0;
	var cb = Bacon.fromCallback(function(callback) {
		errInvoker = callback;
	  	toh = setTimeout(function() {
	  		if(cb.hasSubscribers()) {
	    		callback(new Bacon.Error("Timeout after [" + ms + "] ms."));
	    	}
	  	}, ms);
	});
	if(cancelStream && cancelStream.onEnd) {
		cancelStream.endOnError(function(){
			clearTimeout(toh);
			console.info("Timeout [%s] cleared", toh);
		});
	}
	cb.onValue(function(){
				console.warn("Timeout [%s] fired after [%s] ms", toh, ms);
			}
	);
	var ret = {		
		cancel: function() { clearTimeout(toh); console.info("Timeout [%s] cleared", toh); },
		stream: function() { return cb; },
		reset: function() {
			clearTimeout(toh);
			console.info("Timeout [%s] cleared", toh);
			toh = setTimeout(function() {
	  				if(cb.hasSubscribers()) {
	    				errInvoker(new Bacon.Error("Timeout after [" + ms + "] ms."));
	    			}
			}, ms);
		}
	}

	return ret;
};

var isEvent = function(e) {
	if(e==null) return false;
	if(e instanceof Bacon.Next) return true;
	if(e instanceof Bacon.Error) return true;
	if(e instanceof Bacon.Initial) return true;
	if(e instanceof Bacon.End) return true;
	return false;
}

var timeoutBusSub = function(bus, opts) {
	opts = opts || {};	
	if(opts.timeout==null) opts.timeout = 1000;
	if(opts.resetting==null) opts.resetting = false;
	if(opts.expectedReturns==null) opts.expectedReturns = Infinity;
	if(opts.filter==null) opts.filter = filters.allFilter();
	if(opts.seperateErrStream==null) opts.seperateErrStream = false;
	var f = bus.filter(opts.filter);
	var t = timeoutCountingStream(opts.timeout, f);
	var stream = f.merge(t.stream()).endOnError().take(opts.expectedReturns);
	if(opts.resetting) {
		stream.endOnError(function(err){
			console.error("Sub got error. Stopping....", err);
		}).skipErrors().subscribe(function(v){		
			if(v.isEnd()) {
				console.info("End event. cancelling timer, v:[%s]", printEvent(v));				
				t.cancel();
			} else if(v.isInitial()) {
				console.info("Initial event. resetting timer, v:[%s]", printEvent(v));			
				t.reset();
			} else if(v.isNext()) {
				console.info("Next event. resetting timer, v:[%s]", printEvent(v));			
				t.reset();				
			}
		});
	}
	return stream;
}



var testWhile2 = function(filter, expected, actual) {
	if(filter==null) filter = filters.jsonPathFilter('$[?(@.rerid == 23)]');
	if(expected==null) expected = Math.round(Math.random()*1000)%10;
	if(actual==null) actual = Math.round(Math.random()*1000)%10;
	console.info("[testWhile2] expected: %s, actual: %s", expected, actual);
	var msgs = 0;
	var errcond = false;
	var stream = timeoutBusSub(ws.bus, {
		timeout : 5000,
		expectedReturns : expected,
		filter : filter,
		resetting: true
	}).endOnError(function(err){
		errcond = true;
		console.error("Result was error:", err);	
	}).subscribe(function(event){
		if(!errcond) {
			msgs++;
			if(!event.isEnd()) {
				console.info("Received Event #%s: [%O], Event ID: %s", msgs, event.value(), event.id);				
			} else {
				console.info("Received End Event");
			}
		}
	});
	pushTestData(actual, 100, function(){
		var data = [];
		for(var i = 0; i < 30; i++) {
			data.push({rerid: i})
		}
		return data;
	});
	// for(var i = 0; i < 2; i++) {
	// 	setTimeout(function(){
	// 		ws.bus.push({rerid:23}); 
	// 	}, (Math.round(Math.random() *10000)/2))
	// }
}

var pushTestData = function(count, delay, data) {
	if(data==null) {
		data = [];
	} else if(data instanceof Array) {
		// nuthin'
	} else if(data instanceof Function) {
		data = data();		
	} else {
		data = [data];
	}
	var darr = [];
	for(var i = 0; i < count; i++) {
		data.forEach(function(v){
			darr.push(v);
		});
		
	}
	Bacon.sequentially(delay, darr).onValue(function(d){
		ws.bus.push(d);
	});
}

var extractEvent = function(event) {
	var e = {};
	if(event==null) {
		e.id = "<null>";
	} else if(!isEvent(event)) {
		e.id = "<not event>";
	} else {
		e.id = event.id;
		if(event.isInitial()) {
			e.type = "initial";
		} else if(event.isEnd()) {
			e.type = "end";
		} else if(event.isNext()) {
			e.type = "next";
		} else {
			e.type = "unknown";
		}
		if(event.hasValue()) {
			e.value = event.value();
		} else {
			e.value = null;
		}
	}
	return e;
}

var printEvent = function(event) {
	var ev = extractEvent(event);
	try {
		return JSON.stringify(ev);	
	} catch (e) {
		ev.value = "<removed>";
		return JSON.stringify(ev);	
	}
}




/*
var testWhile = function() {
	var cancelTimeout = [];
	var f = ws.bus.filter(filters.reridFilter(23)).take(1);
	var t = timeoutEvent(1000, f);
	var cancel = f.merge(t.stream())
	.endOnError(function(err) {
		console.error("Result was error:", err);
	})
	.subscribe(function(event){		
		console.info("Received Event: [%O]", event.value());				
		console.info("RESULT Bus has subscribers: [%s]", ws.bus.hasSubscribers());		
	});

	ws.bus.push({rerid:21});
	ws.bus.push({rerid:22});
	// ws.bus.push({rerid:23});

	console.info("Bus has subscribers: [%s]", ws.bus.hasSubscribers());

}
*/

