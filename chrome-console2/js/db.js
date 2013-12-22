/*
 * IndexedDB Interface
 * Whitehead, 2014
 */ 

var dbService = {
   dbname : "opentsdb",
   idb : null,
   stores : {
      connections: { 
	schema: {
	  keyPath: 'id', 
	  autoIncrement: true	  
	}, 
	indexes: {
	  name: {
	    unique: true, 
	    keyPath: 'name'	    
	  }	  
	}, 
	defaultData: [
	  {name: 'Default', auto: false, url: 'ws://localhost:4243/ws', type: 'websocket', permission: false, permission_pattern: ''},
	  {name: 'DefaultTCP', auto: false, url: 'localhost:4242', type: 'tcp', permission: false, permission_pattern: ''},
	  {name: 'DefaultHTTP', auto: false, url: 'http://localhost:4242', type: 'http', permission: false, permission_pattern: ''}
	]
      }
    },
   opendb : function() {
      var dbRequest = window.indexedDB.open(this.dbname);
      dbRequest.onerror = function(event) {
	console.error("Failed to open indexeddb: %o", event.target.error);
	throw "Failed to open indexeddb";    
      };
      dbRequest.onsuccess = function (evt) {
	this.idb = evt.target.result;
	console.info("Database Opened. Version:%s", this.idb.version);
	if(this.idb.version==1) {
	  this.install();
	}
      };       
   },
   deletedb : function() {
      if(this.idb) this.idb.close();
      var dbRequest = window.indexedDB.deleteDatabase(this.dbname);
      dbRequest.onsuccess = function(evt){ console.info("Deleted DB:%o", evt); }
      dbRequest.onerror = function(evt){ console.error("Failed to delete db: %o", evt.target.error); }  
   },
   allData : function(ostore) {
    if(this.idb==null) {
     
    }
    if(this.idb==null) throw "No Database Connection";
    if(!this.idb.objectStoreNames.contains(ostore)) throw "No ObjectStore named [" + ostore + "] in Database";
    var deferred = $.Deferred();
    var dbObjectStore = this.idb.transaction([ostore]).objectStore(ostore);
    var dbCursorRequest = dbObjectStore.openCursor();
    var results = [];
    dbCursorRequest.onsuccess = function (evt) {
	    var curCursor = evt.target.result;
	    if(curCursor) {
		    results.push(curCursor.value);
		    curCursor.continue();
	    } else {
		    deferred.resolve(results);
	    }
	    
    };
    return deferred.promise();
    dbCursorRequest.onerror = function (evt) {
		    console.error("Failed to read all from store [%s]-->[%o]", ostore, evt.target.error);
		    deferred.fail(evt);
    };
   },
   install : function() {
      if(this.idb) this.idb.close();
      console.info("Installing Schemas");
      var dbRequest = window.indexedDB.open(this.dbname, 2);
      dbRequest.onerror = function(event) {
	console.error("Failed to install indexeddb schema : %o", event.target.error);
	throw "Failed to install indexeddb schema";    
      };

      dbRequest.onupgradeneeded = function (evt) {
	console.info("Connected for Upgrade");
	this.idb = evt.target.result;
	$.each(stores, function(name, spec) {	
	    var objectStore = this.idb.createObjectStore(name, spec.schema);
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
	console.info("DB Upgraded:%s", this.idb.version);
	$.each(stores, function(name, spec) {	
	    if(spec.defaultData) {
	      var dbTrans = this.idb.transaction([name], 'readwrite');
	      var objectStore = dbTrans.objectStore(name);
	      $.each(spec.defaultData, function(index, data) {
		var dbAddRequest = objectStore.add(data);
		dbAddRequest.onsuccess = function (evt) { console.info("Saved [%s] Default Data Item:[%o], ID:[%o]", name, data, evt.target.result); }
		dbAddRequest.onerror = function (evt) { console.error("Failed to save [%s] Default Data Item:[%o]-->%o", name, data, evt.target.error); }
	      });
	    }      
	});
      }
   }
}

// dbService
chrome.app.runtime.onLaunched.addListener(function() { 
  console.info("Initializing Service db");
  dbService.opendb();
  window.opentsdb.db = dbService;
});












