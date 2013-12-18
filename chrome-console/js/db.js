/*
 * IndexedDB Interface
 * Whitehead, 2014
 */ 
/*
db init
  create db
  create stores
  create keys/indexes
  
get version
open next version
get all
save all
create new
*/

var dbname = "opentsdb";
var idb = null;
var stores = {
  connections: { schema: {keyPath: 'name'}, indexes: {name: {unique: true, keyPath: 'name'}}, defaultData: [
    {name: 'Default', url: 'ws://localhost:4243/ws', type: 'websocket'}
  ] }
}



function opendb() {
  var dbRequest = window.indexedDB.open(dbname);
  dbRequest.onerror = function(event) {
    console.error("Failed to open indexeddb: %o", event.target.error);
    throw "Failed to open indexeddb";    
  };
  dbRequest.onsuccess = function (evt) {
    idb = evt.target.result;
    console.info("Database Opened. Version:%s", idb.version);
    if(idb.version==1) {
      install();
    }
  };  
}

function deletedb() {
  if(idb) idb.close();
  var dbRequest = window.indexedDB.deleteDatabase(dbname);
  dbRequest.onsuccess = function(evt){ console.info("Deleted DB:%o", evt); }
  dbRequest.onerror = function(evt){ console.error("Failed to delete db: %o", evt.target.error); }  
}

function allData(ostore, callback) {
    if(idb==null) throw "No Database Connection";
    if(!idb.objectStoreNames.contains(ostore)) throw "No ObjectStore named [" + ostore + "] in Database";
    var dbObjectStore = idb.transaction([ostore]).objectStore(ostore);
    var dbCursorRequest = dbObjectStore.openCursor();
    var results = [];
		dbCursorRequest.onsuccess = function (evt) {
			var curCursor = evt.target.result;
			if(curCursor) {
				results.push(curCursor.value);
				curCursor.continue();
			}
			console.info("Post Cursor: %s", results.length);
		};
		console.info("Post Cursor Success: %s", results.length);
		dbCursorRequest.onerror = function (evt) {
				console.error("Failed to read all from store [%s]-->[%o]", ostore, evt.target.error);
				throw evt.target.error;
		};
}


function install() {
  if(idb) idb.close();
  console.info("Installing Schemas");
  var dbRequest = window.indexedDB.open(dbname, 2);
  dbRequest.onerror = function(event) {
    console.error("Failed to install indexeddb schema : %o", event.target.error);
    throw "Failed to install indexeddb schema";    
  };

  dbRequest.onupgradeneeded = function (evt) {
    console.info("Connected for Upgrade");
    idb = evt.target.result;
    $.each(stores, function(name, spec) {	
	var objectStore = idb.createObjectStore(name, spec.schema);
	spec.store = objectStore;
	console.info("Created Object Store [%s]", name);
	if(spec.indexes) {
	  $.each(spec.indexes, function(iname, ispec) {
	      var kp = ispec.keyPath;
	      delete ispec.keyPath;
	      objectStore.createIndex(iname, kp, ispec);
	      console.info("Created Object Store  Index [%s]->[%s]", name, iname);
	  });	  
	}
    });
    // connections: { schema: {keyPath: 'name'}, indexes: {name: {unique: true, keyPath: 'name'}} }
    
  }
    
  dbRequest.onsuccess = function (evt) {
    console.info("DB Upgraded:%s", idb.version);
    $.each(stores, function(name, spec) {	
	if(spec.defaultData) {
	  var dbTrans = idb.transaction([name], 'readwrite');
	  var objectStore = dbTrans.objectStore(name);
	  $.each(spec.defaultData, function(index, data) {
	    var dbAddRequest = objectStore.add(data);
	    dbAddRequest.onsuccess = function (evt) { console.info("Saved [%s] Default Data Item:[%o]", name, data); }
	    dbAddRequest.onerror = function (evt) { console.error("Failed to save [%s] Default Data Item:[%o]-->%o", name, data, evt.target.error); }
	  });
	}      
    });
  }
  
  
}




