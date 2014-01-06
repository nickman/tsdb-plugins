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
    subscriptionPorts: {
    },
    initializeSubscriptionPorts: function(rconservice, webSocketUrl) {
      console.info("Calling internal for services catalog");
      var subsInited = false;
      rconservice._internalRequest(webSocketUrl, {svc:'router', op:'services'}).then(
        function(message) {
          var jsonMsg = JSON.parse(message.data);
          console.info("Processing service catalog for [%s] -- [%o]", webSocketUrl, jsonMsg);
          console.group("Services");
          try {
            $.each(jsonMsg.msg.services, function(serviceName, service){
              console.info("Service:[%s]  :%s", serviceName, service.desc);
              if(service.subs!=null) {
                $.each(service.subs, function(description, ops){
                  console.info("Sub Service [%s]/[%s]:  %s", ops[0], ops[1], description);
                  var subPorts = window.opentsdb.services.server.subscriptionPorts;
                  var _instances = [];
                  subPorts[ops[0]] = {
                    instances: _instances,
                    onConnect: function(port) {
                      _instances.push(port);
                      console.info("Issuing Start Subscribe for [%s]/[%s]", serviceName, ops[0]);
                      var p = port;
                      window.opentsdb.services.rcon.sendRequest(webSocketUrl, {svc: serviceName, op:ops[0]}).then(
                        function() {        // Sub Closed
                          console.warn("Subscribe Done for [%s]/[%s]", serviceName, ops[0]);
                        },
                        function(err) {    // Sub Error
                          console.error("Subscribe Error for [%s]/[%s] -- [%o]", serviceName, ops[0], err);
                        },
                        function(msg) {    // Sub Received
                          console.info("Subscribe Receive for [%s]/[%s] -- [%o]", serviceName, ops[0], msg);
                          p.postMessage(msg.msg[0]);
                          $.each(_instances, function(index, portInstance){
                            portInstance.postMessage(msg.msg[1]);
                          });
                        }
                      );
                    },
                  };
                });
              }
            });
            console.info("Starting Port Connection Listener");
            var subPorts = window.opentsdb.services.server.subscriptionPorts;
            chrome.runtime.onConnect.addListener(function(port){
              console.info("Handling connect for port name [%s]", port.name);
              var subPort = subPorts[port.name];
              if(subPort==null) {
                console.error("No subscription port found for [%s]", port.name);
                throw "No subscription port found for [" + port.name + "]";            
              }
              subPort.onConnect(port);
            });
            subsInited = true;
          } catch (e) {
            console.error("Failed to initialize subscription ports: [%o]", e);
          }
          console.groupEnd();
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
  window.opentsdb.dependencies['remoteconns'].then(
    function(rcon) {
      server.initializeSubscriptionPorts(rcon, 'ws://localhost:4243/ws');
    },
    function(err) {
      console.error("Failed to acquire rcon dependency: [%o]", err);
    }
  );
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






