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
    }
  }); // end of Server definition
  var server = new window.opentsdb.types.Server();
  window.opentsdb.services.server = server; 
  window.opentsdb.dependencies['server'].resolve(server);   
  console.info("------------> [%s] server.js", OP[1]);
});








