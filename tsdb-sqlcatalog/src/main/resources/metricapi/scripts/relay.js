//========================================================================
//  MetaMetrics Local Relay 
//  Whitehead (nwhitehead AT heliosdev DOT org
//  2014
//========================================================================
// ==============================
// Set up dependencies
// ==============================

requirejs.config({
    baseUrl: 'lib',
    paths: {
        scripts: '../scripts'
    }
});

// The mm core namespace
var $mm = $mm || {};


define(['bacon', 'scripts/remote'],
function (Baconator, Remote) {
  var relay = $mm.relay  = $mm.relay || {};  
  // The central shared event bus
  $mm.bus = new Baconator.Bus();
    
  relay.local = function local(props) {
    props = props || {};
    var self = this;
    this.remote = null;
    
    
    
    // =======================================
    //  Remoting properties
    // =======================================
    this.remoteProps = {};
    this.remotetype = props.remote || 'websocket';    
    if($mm.remote[this.remotetype]==null) {
      throw new Error(
	"Invalid remote type [" + self.remotetype + "]"
      );
    }
    
    this.remoteProps.remoteUrl = props.url || null;
    // =======================================
    //  Remoting events
    // =======================================    
    this.remoteProps.onclose = props.onclose;
    this.remoteProps.onopen = props.onopen;
    this.remoteProps.onmessage = props.onmessage;
    this.remoteProps.onerror = props.onerror;
    
    // ======================================
    // Add new event listeners to remote
    // ======================================
    this.addListener = function(event, listener) {
      remote.addListener(event, listener);
    }

    // ======================================
    // Remove registered event listeners
    // ======================================
    this.removeListener = function(event, listener) {
	remote.removeListener(event, listener);
    }
    
    // =======================================
    //  Boot remote instance
    // =======================================    
    this.remote = new $mm.remote[this.remotetype](this.remoteProps);
    console.info("Remote Acquired: [%s]", this.remote);
    
    // ============================================================
    // Core internal functions
    // ============================================================
    var timeoutCountingStream = function(ms, cancelStream) {	
	var toh = null;
	var errInvoker = null;
	var actual = 0;
	var cb = Baconator.fromCallback(function(callback) {
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
    
    this.sendRequest = function _sendRequest_(payload, routing) {
      
    };
    
    
    return {
      id: -1,
      send: this.sendRequest 
    }

  }
  
  
  $mm.relay.relays = [];
  for(var p in $mm.relay) {
    if($mm.relay[p] instanceof Function) {
      $mm.relay.relays.push($mm.relay[p].name);
    }
  }  
  console.info("Available Relays: [%s]", $mm.relay.relays.join(", "));
  
  return {      
      local : function _local_(props) {
	return new relay.local(props);
      }
  }
});