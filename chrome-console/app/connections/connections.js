/**
 * Console OpenTSDB Server Connection Manager
 * Whitehead, 2014
 */ 

var _db = null;
var _connectionStore = null;
var _connections = {};

function loadConnections() {
  console.info("Opening DB");
  $.indexedDB("opentsdb", {
    "upgrade" : function(tx) {
	console.info("Upgrade TX:[%o]", tx);
    },
    "schema" : {
      "1" :  function(tx) {
	console.info("Schema 1 TX:[%o]", tx);
	tx.createObjectStore("connections");
      }
    }
    
  }).done(onOpen).fail(onOpenFail);
}

function onOpen(db, event) {
    _db = db;
    console.info("Done DB:[%o]", _db);
    var tx = _db.transaction("connections", "READ_WRITE");
    console.info("OnOpen TX:[%o]", tx);
    //_db.createObjectStore("connections");
    //console.info("CStore:[%o]", _connectionStore);
    
}

function onOpenFail(error, event) {
    console.error("Failed to open DB:[%o]", error);
}


$(document).ready(function() { 
    console.info('Connections App Loaded');
    loadConnections();
});




