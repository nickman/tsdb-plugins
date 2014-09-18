//========================================================================
//  MetaMetrics Remoting Definitions
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


define([],    // place holder. no dependencies yet.
function () {
  var remote = $mm.remote  = $mm.remote || {};

  
  // ==========================================================================================================================================
  // WebSocket remote
  // ==========================================================================================================================================
  remote.websocket = function websocket(props) {
    var events = ['close', 'open', 'message', 'error'];
    var readystates = {
      0: 'CONNECTING',
      1: 'OPEN',
      2: 'CLOSING',
      3: 'CLOSED'
    };
    
    var self = this;
    props = props || {};
    
    // If the caller has not defined a WS URL, then try to sniff one out
    if(props.remoteUrl==null) {
      if(document!=null) {
        this.remoteUrl = props.remoteUrl || 'ws://' + document.location.host + '/ws'; 
      } else if (self != null && self.location != null ) {
        this.remoteUrl = props.remoteUrl || 'ws://' + self.location.host + '/ws'; 
      } else {
        throw new Error("Failed to autolocate WebSocket URL. No document, no worker global scope. Please specify 'remoteUrl' in passed props");
      }      
    } else {
      this.remoteUrl = props.remoteUrl;
    }

    this.clientid = props.clientid;
    this.onsession = props.onsession;


    
    // ======================================
    // Define event handler arrays
    // ======================================
    this.onclose = []; if(props.onclose!=null) this.onclose.push([].concat(props.onclose));
    this.onopen = []; if(props.onopen!=null) this.onopen.push([].concat(props.onopen));
    this.onerror = []; if(props.onerror!=null) this.onerror.push([].concat(props.onerror));
    this.onmessage = []; if(props.onmessage!=null) this.onmessage.push([].concat(props.onmessage));
    
    // ======================================
    // Create the websocket
    // ======================================
    this.ws = new WebSocket(this.remoteUrl);
    
    // ======================================
    // Attach the defined event listeners
    // ======================================
    this.ws.onclose = function(evt) {
      console.info("ws closed");
      for(index in self.onclose) {
	      self.onclose[index](evt, self);
      }
    }
    this.ws.onerror = function(evt) {
      console.error("ws error:", evt);
      for(index in self.onerror) {
	      self.onerror[index](evt, self);
      }
    }
    this.ws.onopen = function(evt) {
      console.info("ws open");
      for(index in self.onopen) {
	      self.onopen[index](evt, self);
      }		
    }
    this.ws.onmessage = function(evt) {
      console.info("ws message: [%s]", evt.data);
      try {
        var parsed = JSON.parse(evt.data);
        if(parsed.sessionid) {
          props.onsession(parsed.sessionid, props.clientid); 
        }
      } catch (e) {}

      for(index in self.onmessage) {
	      self.onmessage[index](evt, self);
      }				
    }
    
    // ======================================
    // Add new event listeners
    // ======================================
    this.addListener = function(event, listener) {
      if(event != null && listener != null) {
        if(this.events.indexOf(event)==-1) throw new Error("Invalid event type: [" + event + "]. Valid events are: [" + this.events.join(", ") + "]");
        var listeners = [].concat(listener);
        for(index in listeners) {
          if(listeners[index] != null && listeners[index] instanceof Function) {
            this[event] = listeners[index];
          }
        }
      }
    }

    // ======================================
    // Remove registered event listeners
    // ======================================
    this.removeListener = function(event, listener) {
      if(event != null && listener != null) {
	if(this.events.indexOf(event)==-1) throw new Error("Invalid event type: [" + event + "]. Valid events are: [" + this.events.join(", ") + "]");
	var listeners = [].concat(listener);
	for(index in listeners) {
	  var idx = this[event].indexOf(listeners[index]);
	  if(idx > -1) {
	    this[event].splice(idx, 1);
	  }
	}
      }
    }
    
    // ======================================
    // Other ops
    // ======================================
    this.send = function(msg) {
      this.ws.send(msg);
    }
    
    this.close = function() {
      this.ws.close();
    }
    
    this.state = function() {
      return this.ws.readyState;
    }
    
    this.toString = function() {
      return "[remote:websocket]:" + this.remoteUrl + "  [" + readystates[this.ws.readyState] + "]";
    }
    
  }
  
  $mm.remote.remotes = [];

  for(var p in $mm.remote) {
    if($mm.remote[p] instanceof Function) {
      $mm.remote.remotes.push($mm.remote[p].name);
    }
  }
  
  
  console.info("Available Remotes: [%s]", $mm.remote.remotes.join(", "));
  	
  
  
});