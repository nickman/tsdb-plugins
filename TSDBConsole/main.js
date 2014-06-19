var openPorts = {};
function scheduleRecurring() {
  chrome.alarms.create("alarm1", {when: (Date.now() + 10000)});

}

// startServicePort("ImgDownload", {onMessage : function() { console.info("Service Callback: %O", arguments); }}); 

function startServicePort(serviceName, handler) {
    if(serviceName==null) throw "Service Name was Null";
    if(openPorts[serviceName] != null) {
      console.info("ServicePort handler for [%s] already installed", serviceName);  
      return;
    }
    if(handler==null) throw "Service Handler was Null";
    if(handler.onMessage==null) throw "Service Handler had no valid onMessage function";  //  || !$.isFunction(handler.onMessage)
    console.info("Installing ServicePort handler for [%s]", serviceName);
    openPorts[serviceName] = {
      handler: handler,
      port : null
    };
    var _openPorts = openPorts;
    chrome.runtime.onConnect.addListener(function(port){
      console.info("onConnect event.Port:%o", port);
      port.onMessage.addListener(_openPorts[serviceName].handler);
      _openPorts[serviceName].handler.port = port;
      _openPorts[serviceName].port = port;
      console.info("Started ServicePort [%s]/[%s] -- [%o]", serviceName, port.name, _openPorts[serviceName]);
      return true;
    });
    console.info("ServicePort handler for [%s] Installed", serviceName);
}

chrome.app.runtime.onLaunched.addListener(function() {
  chrome.app.window.create('main.html', {
	'id' : 'mainWindow',
	'state' : 'normal',
    'bounds' : {
		'width': Math.round(window.screen.availWidth*0.8),
        'height': Math.round(window.screen.availHeight*0.8),
        'left' : Math.round((window.screen.availWidth-(window.screen.availWidth*0.8))/2),
        'top' : Math.round((window.screen.availHeight-(window.screen.availHeight*0.8))/2)
    }
  });
  scheduleRecurring();
  chrome.alarms.onAlarm.addListener(function(alarm){
    scheduleRecurring();
    console.info("========= RING: Alarm. Time: [%s], Alarm Details: [%O]", new Date(), alarm);
  });
});



