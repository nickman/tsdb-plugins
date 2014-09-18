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
      rid++;
      return rid;
    }
  }
}
// The mm global client id to assign to MClients 
// to uniquely identify a client until the remote
// assigns a sessionid. $mm.CLIENTID().clientId()
$mm.CLIENTID = function() {
  var clientid = 0;
  return {
    clientId : function() {
      clientid++;
      return clientid;
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
    this.clientId = $mm.CLIENTID().clientId();
    this.self = this;
    props.clientid = this.clientId;
    props.onsession = function _onsession_(r_sessionid, r_clientid) {
      if(r_clientid = self.clientid) {
        self.sessionid = sessionid;
        console.info("Acquired sessionid. Client: %s, Session: %s", self.clientId, self.sessionid);
      } else {
        console.debug("Ignoring Session Callback Mismatch. Provided: [session:%s, client:%s] but my clientid is [%s]", r_sessionid, r_clientid, self.clientid);
      }
    }
    this.relay = new self.relayFactory[props.relay](props);
      
    // $mm.RID.nextrid();

    this.wrapRequest = function _wrapRequest_(payload, routing) {
      var rid = $mm.RID.nextrid();
      payload.rid = rid;

      routing = routing || {};
      if(routing.timeout==null) routing.timeout = $mm.defaults.requestTimeout;
      if(routing.resetting==null) routing.resetting = false;
      if(routing.expectedReturns==null) routing.expectedReturns = Infinity;
      if(routing.filter==null) {
        if(routing.expectedReturns != Infinrouting.expectedReturnsity) {
          routing.filter = $mm.selector.reridFilter(rid);
        } else {
          routing.filter = $mm.selector.allFilter();
        }
      }
      if(routing.endSelector==null) {
        if(routing.expectedReturns != Infinity) {
          routing.endSelector = $mm.endselector.onDataCount(routing.expectedReturns);
        }
      }
      
      if(routing.seperateErrStream==null) routing.seperateErrStream = false;

      console.debug("Sending Request: rid: %s, payload: [%O], routing: [%O]", rid, payload, routing);
      return self.relay.sendRequest(payload, routing);


    }

    this.services = function _services_() {
      var response = self.wrapRequest({svc:"router", op:"services", t: "req"}, {expectedReturns:1});
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

