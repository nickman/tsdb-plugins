/*
 * IndexedDB interface js
 * Whitehead, 2014
 */
 
var tsdb_db = {
	dbname : null,
	db : null,
	last_error : null,
	onerror : function(err) {
		
	},
	open = function(dbname) {
  	var version = 1;
  	var request = indexedDB.open(dbname, version);

  	request.onsuccess = function(e) {
    	this.db = e.target.result;
  	};

  	request.onerror = this.onerror;
	}
};

