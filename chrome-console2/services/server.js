/**
 * The service background script that provides services to all UI and process pages
 * Whitehead, 2014
 */



chrome.app.runtime.onLaunched.addListener(function() { 
  console.info("Starting server.js  <------------");
  window.opentsdb.server = {
    /**
     * Message handler for one time requests
     * @param request The incoming request
     * @param sender The sender of the request
     * @param response The response channel
     */
    handleRequest : function(request, sender, response) {
        console.info("Server received request [%o] from sender [%o]", request, sender);
        console.info("Invoke Request:%o", request);
        var port = window.opentsdb[request.port];
        var func = port[request.name];
        var resp = null;
        try {
          resp = func(request.args);
        } catch (e) {
          console.error("Request Error:%o", e);
          resp = e;
        }      
        console.info("Invoke Request:%o  RESPONSE:[%o]", request, resp);
        response(resp);
        // {port:'db', name:'allData', 'args':['connections]}      
    }
    
  };  
  chrome.runtime.onMessage.addListener(
    window.opentsdb.server.handleRequest
  );  
  window.opentsdb.services['server'] = window.opentsdb.server;
  window.opentsdb.dependencies['server'].resolve(window.opentsdb.server);  
  console.info("------------> Started server.js");
});








