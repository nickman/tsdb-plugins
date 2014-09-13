//========================================================================
//  MetaMetrics JS API for OpenTSDB 
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
// The mm global req id counter
$mm.RID = function() {
  var rid = 0;
  return {
    nextrid : function() {
      return rid++;
    }
  }
}
// the standard bus selectors
$mm.selector = {
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
// the standard bus end event selectors
$mm.endselector = {
  onDataCount : function(count) {
    return function(data) {
      var cnt = 0;
      var onData = function _onData_(data) {
	cnt++;
	return cnt >= count;
      }
    }
  }
};
//  the $mm default config
$mm.defaults = {
    // the default request timeout
    requestTimeout : 1500
};


console.group("Initializing MetaMetrics....");


// ==============================
// Initialize main module
// ==============================
define(['bacon', 'scripts/relay', 'jsonpath'],
function (Baconator, r, jsonPath) {
  this.bus = new Baconator.Bus();
  this.relayFactory = r;
  var cbs = ['onstart', 'onend', 'data', 'error'];
  console.info("mbus: [%O]", this.bus);
  console.info("rfac: [%O]", this.relayFactory);
  // The central shared event bus
  $mm.bus = new Baconator.Bus();
  
  this.version = 1.0;
  this.name = "This is the OpenTSDB MetaMetrics JS API, version " + this.name;
  var self = this;
  var ridCounter = $mm.RID.nextrid;
  this.relay = null;
  this.MClient = function MClient(props) {
    props = props || {};
    if(props.relay==null) props.relay = 'local';
    this.relay = new self.relayFactory[props.relay](props);
    
    
    this.services = function services() {
      var response = this.wrapreq({svc:"router", op:"services", t: "req"});
	
    }
    
    return {
      id       : this.relay.id,
      services : this.services
       
    }
    
  }
  
  $mm.newClient = function newClient(props) {
     return self.MClient(props);
  }
  
  console.info("MetaMetrics Ready");
  console.groupEnd();
  
});

// =========  RELAY PROPS ================
/*
    this.remotetype = props.remote || 'websocket';    
    if($mm.remote[remotetype]==null) throw new Error("Invalid remote type [" + this.remotetype + "]");
    
    this.remoteProps.remoteUrl = props.url || null;
    // =======================================
    //  Remoting events
    // =======================================    
    this.remoteProps.onclose = props.onclose;
    this.remoteProps.onopen = props.onopen;
    this.remoteProps.onmessage = props.onmessage;
    this.remoteProps.onerror = props.onerror;
*/


//========================================================================
//  WebSock API
//========================================================================

// Sample Call
//	var ws = new WebSocketAPIClient();
//	var q = QueryContext.newContext();
//	ws.getMetricNames(q, ['host', 'type', 'cpu']);

