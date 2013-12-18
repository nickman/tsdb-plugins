/**
 * Console OpenTSDB Server Connection Manager
 * Whitehead, 2014
 */ 

document.domain = "anpdknjjbhaojaaiopefckeimcpdpnkc";
var _db = null;
var _connectionStore = null;
var _connections = {};

var READ_ONLY = 0;
var READ_WRITE = 1;

function loadConnections() {
  console.info("Opening DB");
  $.indexedDB("opentsdb", {
    "upgrade" : function(tx) {
	console.info("Upgrade TX:[%o]", tx);
    },
    "schema" : {
      "1" :  function(tx) {
				console.info("Schema 1 TX:[%o]", tx);
				tx.createObjectStore("connections").createIndex("name");
				console.info("Created connections ObjectStore");
      }
    }
    
  }).done(onOpen).fail(onOpenFail);
}


function onOpen(db, event) {
    _db = db;
    console.info("Done DB:[%o]", _db);
    console.info("Done Event:[%o]", event.srcElement.result);
    var tx = _db.transaction("connections", "readwrite");
    var cnt = 0;
    tx.objectStore("connections").each(function(item){
    	cnt++;
    	_connections[item.name] = item;	
    });
    console.info("Loaded %s Connections", cnt);
    if(cnt==0) {
    	var item = {
    		name: "Default",
    		url: "ws://localhost:4243/ws",
    		def: true
    	};
    	tx.objectStore("connections", "readwrite").put(item, item.name);
    	console.info("Stored Default [%o]", item);
    }
    
    //_db.createObjectStore("connections");
    //console.info("CStore:[%o]", _connectionStore);
    
}

function onOpenFail(error, event) {
    console.error("Failed to open DB:[%o]", error);
}


$(document).ready(function() { 
    console.info('Connections App Loaded');
    console.dir($('body'));
    //loadConnections();
});




