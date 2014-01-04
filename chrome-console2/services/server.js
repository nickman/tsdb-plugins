/**
 * The service background script that provides services to all UI and process pages
 * Whitehead, 2014
 */


chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
  var OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
  console.info("%s server.js  <------------", OP[0]);
  window.opentsdb.types.Server = Class.extend({
    _self : this,
    /**
     * Constructor for Server. Initializes the listener.
     */
    init: function(){
      var _me = this;
      var handlerWrapper = function(request, sender, response) {
        _me.handleRequest.apply(_me, arguments);
        return true;
      };
      chrome.runtime.onMessage.addListener(
        handlerWrapper
      );  
    },    
    /**
     * Indicates if the passed object is a jQuery promise
     * @param value The object to test
     * @return true if the passed object is a jQuery promise, false otherwise
     */    
    isPromise : function(value) {
        if (typeof value.then !== "function") {
            return false;
        }
        var promiseThenSrc = String($.Deferred().then);
        var valueThenSrc = String(value.then);
        return promiseThenSrc === valueThenSrc;
    },
    /**
     * Message handler for one time requests
     * @param request The incoming request
     * @param sender The sender of the request
     * @param response The response channel
     */
    handleRequest : function(request, sender, response) {
        console.info("Server received request [%o] from sender [%o]", request, sender);
        console.info("Invoke Request:%o", request);
        var port = window.opentsdb.services[request.port];
        var func = port[request.name];
        var resp = null;
        try {
          resp = func.apply(port, request.args);
          if(!this.isPromise(resp)) {
            response(resp);  
          } else {  
            resp.then(
              function(result) {      
                var result_copy = result;
                response(result_copy);
                console.info("Promise Resolved. Returning result: [%o]", result_copy);
              },
              function(evt) {              
                response(evt);
                console.info("Promise Failed. Returning error: [%o]", evt);
              },
              function(progress) {
                console.info("Promise Progress. Value: [%o]", progress);
              }            
            );
          }            
        } catch (e) {
          console.error("Request Error:%o", e);
          resp = e;
        }      
        return true;
    },
    portServices: {},
    registerServicePortHandler: function(serviceName, handler) {

    },
    handleServicePortConnect: function(port) {

    },
    // opentsdb.services.server.startServicePort("SystemStatus", {onMessage: function() { console.info("SystemStatus OnMessage:%o, handler:%o", arguments, this); }});
    startServicePort: function(serviceName, handler) {
      if(serviceName==null) throw "Service Name was Null";
      if(this.openPorts[serviceName] != null) {
        console.info("ServicePort handler for [%s] already installed", serviceName);  
        return;
      }
      if(handler==null) throw "Service Handler was Null";
      if(handler.onMessage==null || !$.isFunction(handler.onMessage)) throw "Service Handler had no valid onMessage function";
      console.info("Installing ServicePort handler for [%s]", serviceName);
      this.openPorts[serviceName] = {
        handler: handler,
        port : null
      };
      var _openPorts = this.openPorts;
      chrome.runtime.onConnect.addListener(function(port){
        console.info("onConnect event.Port:%o", port);
        port.onMessage.addListener(_openPorts[serviceName].handler);
        _openPorts[serviceName].handler.port = port;
        _openPorts[serviceName].port = port;
        console.info("Started ServicePort [%s]/[%s] -- [%o]", serviceName, port.name, _openPorts[serviceName]);
        return true;
      });
      console.info("ServicePort handler for [%s] Installed", serviceName);
    },
    initializeSubscriptionPorts: function(rconservice, url) {
      console.info("Calling internal for services catalog");
      _wconn._internalRequest(url, {svc:'router', op:'services', rid: rcon.ridCounter++, t:'req'}).then(
        function(message) {
          var jsonMsg = JSON.parse(message.data);
          console.info("Processing service catalog for [%s] -- [%o]", webSocketUrl, jsonMsg);
        },
        function(err) {
          console.error("Failed to get service catalog for [%s] -- [%o]", webSocketUrl, err);
        }
      );
    }    
  }); // end of Server definition
  var server = new window.opentsdb.types.Server();

  window.opentsdb.services.server = server; 
  window.opentsdb.dependencies['server'].resolve(server);   
  console.info("------------> [%s] server.js", OP[1]);
});

/*
msg: Object
services: Object
router: Object
sqlcatalog: Object
system: Object
desc: "Some generic system services"
ops: Object
subs: Object
subsysstat: "Subscribes the calling channel to system status messages"
*/






