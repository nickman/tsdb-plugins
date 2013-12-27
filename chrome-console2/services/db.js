/*
 * IndexedDB Interface
 * Whitehead, 2014
 */ 

chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
	var OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
	console.info("%s init.js  <------------", OP[0]);
	var DBService = Class.extend({
		me : null,
	    /**
	     * Constructor for Db Service. 
	     */
	    init: function(){
	    	this.me = this;
	    	console.info("Creating DBService");
	    },    
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
	 				},
	 				url: {
	 					unique: true, 
	 					keyPath: 'url'	    	 					
	 				}
	 			}, 
	 			defaultData: [
	 			{name: 'Default', auto: false, url: 'ws://localhost:4243/ws', type: 'websocket', permission: false, permission_pattern: ''},
	 			{name: 'DefaultTCP', auto: false, url: 'tcp://localhost:4242', type: 'tcp', permission: false, permission_pattern: ''},
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
	 		if(this.idb) this.idb.close();
	 		var dbRequest = window.indexedDB.deleteDatabase(this.dbname);
	 		dbRequest.onsuccess = function(evt){ console.info("Deleted DB:%o", evt); }
	 		dbRequest.onerror = function(evt){ console.error("Failed to delete db: %o", evt.target.error); }  
	 	},
	 	// Example:  opentsdb.services.db.getByIndex("connections", true, "url", "ws://localhost:4243/ws").then(function(ok) { console.info("OK:", ok); }, function(err) { console.error("ERR:", err); });
	 	getByIndex: function(ostore, failNotFound, indexName, indexValues) {
	 		var d = $.Deferred();	 
	 		var me = this;	
	 		var _keys = [];
	 		var results = {
	 			count : 0
	 		};
	 		var xcount = 0;
	 		var broken = false;
	 		if(arguments.length < 4) {
	 			d.resolve(results);
	 			return d.promise();
	 		} 
	 		if(!me.idb.objectStoreNames.contains(ostore)) throw "No ObjectStore named [" + ostore + "] in Database";
	 		for(var i = 3, l = arguments.length; i < l; i++) { 
	 			var item = arguments[i];
		 		if($.isArray(item)) {
		 			$.each(item, function(index, value){
		 				_keys.push(value);
		 			});
		 		} else {
		 			_keys.push(item);
		 		}
	 		}
			this.opendb().then(
	 			function() {
	 				try {	 					
	 					var transaction = me.idb.transaction([ostore], "readonly");
	 					var objectStore = transaction.objectStore(ostore);	 	
	 					var keyId = objectStore.keyPath;
	 					var dbIndex = objectStore.index(indexName);
	 					console.info("Index: [%o]", index);
	 					for(var index = 0, l = _keys.length; index < l; index++) {
	 						var value = _keys[index];				
	 						var ob = dbIndex.get(value);
		 					ob.onsuccess = function(evt) {
		 						xcount++;
		 						if(broken) return;
		 						var row = evt.target.result;
		 						if(row==null) {
		 							if(failNotFound) {
		 								console.error("getByIndex [%s] value not found, Event:[%o]", indexName, evt);
		 								d.reject(evt);
		 								broken = true;
		 							}
		 						} else {
		 							row.rownum = results.count;
		 							results.count++;
		 							results[row[keyId]] = row;		 						
		 						}
			 					if(xcount==l) {
				 					if(d.state() != "rejected") {
				 						d.resolve(results);
				 					}
			 					}
		 					}
		 					ob.onerror = function(evt) {
		 						xcount++;;
		 						if(broken) return;
		 						if(failNotFound) {
		 							console.error("getByIndex [%s] failed, Error:[%o]", indexName, evt.target.error);
		 							d.reject(evt.target.error);
		 							broken = true;
		 						}		 						
			 					if(xcount==l) {
				 					if(d.state() != "rejected") {
				 						d.resolve(results);
				 					}
			 					}
		 					}
		 					if(failNotFound && d.state=="rejected") break; 
	 					};
	 				} catch (e) {
	 					console.error("getByIndex [%s] failure:[%o]", indexName, e);
	 					d.reject(e);
	 				}
	 			},
	 			function(err) {
	 					console.error("opendb failure:[%o]", e);
	 					d.reject(e);
	 			}
	 		);
	 		return d.promise();
	 	},
	 	getByKey: function(ostore, failNotFound, keys) {
	 		var d = $.Deferred();	 
	 		var me = this;	
	 		var _keys = [];
	 		var results = {
	 			count : 0
	 		};
	 		var xcount = 0;
	 		var broken = false;
	 		if(arguments.length < 3) {
	 			d.resolve(results);
	 			return d.promise();
	 		} 
	 		if(!me.idb.objectStoreNames.contains(ostore)) throw "No ObjectStore named [" + ostore + "] in Database";
	 		for(var i = 2, l = arguments.length; i < l; i++) { 
	 			var item = arguments[i];
		 		if($.isArray(item)) {
		 			$.each(item, function(index, value){
		 				_keys.push(value);
		 			});
		 		} else {
		 			_keys.push(item);
		 		}
	 		}
			this.opendb().then(
	 			function() {
	 				try {	 					
	 					var transaction = me.idb.transaction([ostore], "readonly");
	 					var objectStore = transaction.objectStore(ostore);	 	
	 					for(var index = 0, l = _keys.length; index < l; index++) {
	 						var value = _keys[index];				
	 						var keyId = objectStore.keyPath;
		 					var ob = objectStore.get(value);
		 					ob.onsuccess = function(evt) {
		 						xcount++;
		 						if(broken) return;
		 						var row = evt.target.result;
		 						if(row==null) {
		 							if(failNotFound) {
		 								console.error("getByKey value not found, Event:[%o]", evt);
		 								d.reject(evt);
		 								broken = true;
		 							}
		 						} else {
		 							row.rownum = results.count;
		 							results.count++;
		 							results[row[keyId]] = row;		 						
		 						}
			 					if(xcount==l) {
				 					if(d.state() != "rejected") {
				 						d.resolve(results);
				 					}
			 					}
		 					}
		 					ob.onerror = function(evt) {
		 						xcount++;;
		 						if(broken) return;
		 						if(failNotFound) {
		 							console.error("getByKey failed, Error:[%o]", evt.target.error);
		 							d.reject(evt.target.error);
		 							broken = true;
		 						}		 						
			 					if(xcount==l) {
				 					if(d.state() != "rejected") {
				 						d.resolve(results);
				 					}
			 					}
		 					}
		 					if(failNotFound && d.state=="rejected") break; 
	 					};
	 				} catch (e) {
	 					console.error("getByKey failure:[%o]", e);
	 					d.reject(e);
	 				}
	 			},
	 			function(err) {
	 					console.error("opendb failure:[%o]", e);
	 					d.reject(e);
	 			}
	 		);
	 		return d.promise();
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
	 					console.error("allData failure:[%o]", e);
	 					d.reject(e);
	 				} 				 				
	 			},
	 			function(err) {
	 				console.error("allData failure:[%o]", err);
	 				d.reject(err);
	 			}
	 		);
	 		var p = d.promise();
	 		return p;		
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
	});  // end of DBService definition
	window.opentsdb.types.DBService = DBService;
	var db = new DBService();
	window.opentsdb.services.db = db;	
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	db.opendb().then(
		function() {			
			window.opentsdb.dependencies['db'].resolve(db);		
			console.info("------------> [%s] db.js", OP[1]);
		},
		function(evt) {
			window.opentsdb.services['db'].reject(evt);			
		}
	);  	
});

















