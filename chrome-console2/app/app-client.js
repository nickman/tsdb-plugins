/**
 * OpenTSDB Console Services Client
 * Whitehead, 2014
 */ 

$(document).ready(function() { 
    console.info("OpenTSDB Console Services Client");
    chrome.runtime.onMessage.addListener(function(p){
      console.info("App Client Received Message: [%o]", p);
    });
});


function sendRequest(request) {  
  if(request==null) {
    throw "Passed request was null";
  }
  var deferred = $.Deferred();
  var handler = function(resp) {
    var _response = resp;
    console.info("AppClient Received Response: [%o]", _response);
    deferred.resolve(_response);
  }
  console.info("AppClient Sending Request: [%o]", request);
  
  chrome.runtime.sendMessage(request, handler);

  return deferred.promise();
}

function sendRemoteRequest(url, request) {
  return sendRequest({
    port: 'rcon',
    name: 'sendRequest',
    args: [
      url,
      request
    ]
  });
}

function getServicePort(serviceName) {
  if(serviceName==null) {
    throw "Passed serviceName was null";
  }  
  var deferred = $.Deferred();
  var port = chrome.runtime.connect({name: serviceName});
  port.onMessage.addListener(function(message) {
    deferred.notify(message);
  });
  port.onDisconnect.addListener(function() {
    deferred.done();
  });
  deferred.postMessage = function(message) {
    port.postMessage(message);
  }
  deferred.portName = function() {
   return port.name; 
  }
  deferred.disconnect = function() {
   port.disconnect(); 
  }
  return deferred;
}


/*
function asyncEvent() {
var dfd = new jQuery.Deferred();
// Resolve after a random interval
setTimeout(function() {
dfd.resolve( "hurray" );
}, Math.floor( 400 + Math.random() * 2000 ) );
// Reject after a random interval
setTimeout(function() {
dfd.reject( "sorry" );
}, Math.floor( 400 + Math.random() * 2000 ) );
// Show a "working..." message every half-second
setTimeout(function working() {
if ( dfd.state() === "pending" ) {
dfd.notify( "working... " );
setTimeout( working, 500 );
}
}, 1 );
// Return the Promise so caller can't change the Deferred
return dfd.promise();
}
// Attach a done, fail, and progress handler for the asyncEvent
$.when( asyncEvent() ).then(
function( status ) {
alert( status + ", things are going well" );
},
function( status ) {
alert( status + ", you fail this time" );
},
function( status ) {
$( "body" ).append( status );
}
); 
 */ 

