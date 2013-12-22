/**
 * The service background script that provides services to all UI and process pages
 * Whitehead, 2014
 */




/**
 * Listener registration
 */
chrome.app.runtime.onLaunched.addListener(function() { 
  console.info("App Runtime Started");
  chrome.runtime.onMessage.addListener(handleRequest);
  console.info("Request Listener Registered");
  chrome.runtime.onConnect.addListener(acceptConnect);
  console.info("Port Listener Registered");
});

/**
 * Message handler for one time requests
 * @param request The incoming request
 * @param sender The sender of the request
 * @param response The response channel
 */
function handleRequest(request, sender, response) {
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

/**
 * Accept handler for persisten connections
 * @param port The connection port
 */
function acceptConnect(port) {
  console.info("Server accepted connect. Port:[%o]", port);
}
