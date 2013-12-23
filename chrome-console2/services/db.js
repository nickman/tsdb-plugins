/*
 * IndexedDB Interface
 * Whitehead, 2014
 */ 


dbService = function(){
 	dbname : "opentsdb",
 	me : dbService,
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
 		var d = $.Deferred();
 		if(this.idb!=null) {
 			d.resolve(this.idb);
 			return d;
 		}
 		var dbRequest = window.indexedDB.open(this.dbname);
 		var me = this;
 		dbRequest.onerror = function(event) {
 			console.error("Failed to open indexeddb: %o", event.target.error);
 			d.reject(event.target.error);
 		};
 		dbRequest.onsuccess = function (evt) {
 			me.idb = evt.target.result;
 			console.info("Database Opened. Version:%s", me.idb.version);
 			if(me.idb.version==1) {
 				me.install();
 			}
 			d.resolve(me.idb);	
 		};
 		return d.promise();
 	},
 	deletedb : function() {
 		if(me.idb) me.idb.close();
 		var dbRequest = window.indexedDB.deleteDatabase(me.dbname);
 		dbRequest.onsuccess = function(evt){ console.info("Deleted DB:%o", evt); }
 		dbRequest.onerror = function(evt){ console.error("Failed to delete db: %o", evt.target.error); }  
 	},
 	allData : function(ostore) {
 		var d = $.Deferred();
 		var me = this;
 		this.opendb().then(
 			function() {
 				try {
 					if(!me.idb.objectStoreNames.contains(ostore)) throw "No ObjectStore named [" + ostore + "] in Database";
 					var dbObjectStore = me.idb.transaction([ostore]).objectStore(ostore);
 					var dbCursorRequest = dbObjectStore.openCursor();
 					var results = [];
 					dbCursorRequest.onsuccess = function (evt) {
 						var curCursor = evt.target.result;
 						if(curCursor) {
 							results.push(curCursor.value);
 							curCursor.continue();
 						} else {
 							d.resolve(results);
 						}
 					};
 					dbCursorRequest.onerror = function (evt) {
 						console.error("Failed to read all from store [%s]-->[%o]", ostore, evt.target.error);
 						deferred.fail(evt.target.error);
 					};
 				} catch (e) {
 					d.reject(e);
 				} 				 				
 			},
 			function(err) {
 				d.reject(err);
 			}
 			);
 		return d.promise(); 		
 	},

 	install : function() {
 		var me = this;
 		if(me.idb) me.idb.close();
 		console.info("Installing Schemas");
 		var dbRequest = window.indexedDB.open(me.dbname, 2);
 		dbRequest.onerror = function(event) {
 			console.error("Failed to install indexeddb schema : %o", event.target.error);
 			throw "Failed to install indexeddb schema";    
 		};

 		dbRequest.onupgradeneeded = function (evt) {
 			console.info("Connected for Upgrade");
 			me.idb = evt.target.result;
 			$.each(me.stores, function(name, spec) {	
 				var objectStore = me.idb.createObjectStore(name, spec.schema);
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
		}

		dbRequest.onsuccess = function (evt) {
			console.info("DB Upgraded:%s", me.idb.version);
			$.each(me.stores, function(name, spec) {	
				if(spec.defaultData) {
					var dbTrans = me.idb.transaction([name], 'readwrite');
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




chrome.app.runtime.onLaunched.addListener(function() { 
	console.info("Starting db.js  <------------");
	window.opentsdb.db = new dbService();
	window.opentsdb.db.opendb().then(
		function() {
			window.opentsdb.services['db'] = window.opentsdb.db;
			window.opentsdb.dependencies['db'].resolve(window.opentsdb.db);		
			console.info("------------> Started db.js")
		},
		function(evt) {
			window.opentsdb.services['db'].reject(evt);			
		}
	);  
});
















