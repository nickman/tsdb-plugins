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
  if(this.rid==null) {
    this.rid = 0;
  }
  var self = this;
  return {
    nextrid : function() {
      self.rid++;
      return self.rid;
    }
  }
}
// The mm global client id to assign to MClients 
// to uniquely identify a client until the remote
// assigns a sessionid. $mm.CLIENTID().clientId()
$mm.CLIENTID = function _CLIENTID_() {
  if(this.rid==null) {
    this.rid = 0;
  }
  var self = this;
  return {
    clientId : function _clientId_() {
      self.rid++;
      return self.rid;
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
    sessionFilter: function(clientid) {
      return function(sevt) {
        return (sevt != null && sevt.clientid != null && sevt.clientid == clientid && sevt.type != null && sevt.type == 'assignsession');
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
function (Baconator, Relay, jsonPath) {  
  this.relayFactory = Relay;
  var cbs = ['onstart', 'onend', 'data', 'error'];
  // The central shared event bus
  if($mm.bus!=null) {
    $mm.bus = new Baconator.Bus();
  }
  console.info("mbus: [%O]", $mm.bus);
  console.info("rfac: [%O]", this.relayFactory);

  $mm.bus.onValue(function(evt) {
    console.group(" DEBUG BUS EVENT ");
    console.dir(evt);
    console.groupEnd();
  });
  
  this.version = 1.0;
  this.name = "This is the OpenTSDB MetaMetrics JS API, version " + this.name;

  var self = this;
  var ridCounter = $mm.RID.nextrid;
  this.relay = null;
  this.MClient = function MClient(props) {
    props = props || {};
    if(props.relay==null) props.relay = 'local';
    this.clientid = $mm.CLIENTID().clientId();
    var me = this;
    props.clientid = me.clientid;
    
    this.onsession = function _onsession_(r_clientid, r_sessionid) {
      if(r_clientid = me.clientid) {
        me.sessionid = r_sessionid;
        console.info("Acquired sessionid. Client: %s, Session: %s", me.clientid, me.sessionid);
      } else {
        console.debug("Ignoring Session Callback Mismatch. Provided: [session:%s, client:%s] but my clientid is [%s]", r_sessionid, r_clientid, me.clientid);
      }
    }
    $mm.bus.filter($mm.selector.sessionFilter(this.clientid)).onValue(this.onsession);
    this.relay = new self.relayFactory[props.relay](props);
      
    // $mm.RID.nextrid();

    this.wrapRequest = function _wrapRequest_(payload, routing) {
      var rid = $mm.RID().nextrid();
      payload.rid = rid;

      routing = routing || {};
      if(routing.timeout==null) routing.timeout = $mm.defaults.requestTimeout;
      if(routing.resetting==null) routing.resetting = false;
      if(routing.expectedReturns==null) routing.expectedReturns = Infinity;
      if(routing.filter==null) {
        if(routing.expectedReturns != Infinity) {
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
      var routeKey = {
        clientid: me.clientid,
        sessionid: me.sessionid,
        rid: payload.rid,
        t: payload.t,
        svc: payload.svc,
        op: payload.op
      }
      $mm.bus.push({ payload: payload, routing: routing, routekey: routeKey});

      //$mm.bus.push({uri: "/" + payload.t + "/" + payload.svc + "/" + payload.op, args:[payload, routing]});
      


    }

    this.services = function _services_() {
      var response = me.wrapRequest({svc:"router", op:"services", t: "req"}, {expectedReturns:1});
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

