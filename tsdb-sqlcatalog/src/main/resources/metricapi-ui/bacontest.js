// ================  bacon.test 
var filters = {
	reridFilter : function(rerid) {
		return function(event) {
			try {
				return (event.rerid!=null && event.rerid == rerid);
			} catch (e) { return false; }
		}
	}
};

var timeoutEvent = function(ms, cancelStream) {	
	var toh = null;
	var cb = Bacon.fromCallback(function(callback) {
	  toh = setTimeout(function() {
	  	if(cb.hasSubscribers()) {
	    	callback(new Bacon.Error("Timeout after [" + ms + "] ms."));
	    }
	  }, ms);
	});
	if(cancelStream && cancelStream.onEnd) {
		cancelStream.onEnd(function(){
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
		stream: function() { return cb; }
	}

	return ret;
};

var isEvent = function(e) {
	return (e!=null && (e.hasValue) && (e.value));
}

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





