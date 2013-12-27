/*
 * OpenTSDB namespace initializer
 * Whitehead, 2014
 */ 

chrome.app.runtime.onLaunched.addListener(function serviceInitializer(launchData) {  
	var OP = launchData==null ? ["Starting", "Started"] : ["Restarting", "Restarted"];
	console.info("%s nsinit.js  <------------", OP[0]);
	var ctx = window;
	if(window.opentsdb==null) {
		window.opentsdb = {};
	}
	if(launchData!=null) {
		window.opentsdb.launchData = launchData;
	}	
	if(window.opentsdb.services==null) {
		window.opentsdb.services = {}; 
	}
	if(window.opentsdb.dependencies==null) {
		window.opentsdb.dependencies = {}; 
	}
	if(window.opentsdb.types==null) {
		window.opentsdb.types = {}; 
	}
	// ========================================================================================================
	//		Base service class definition
	// ========================================================================================================
	/* Simple JavaScript Inheritance
	 * By John Resig http://ejohn.org/
	 * MIT Licensed.
	 */
	// Inspired by base2 and Prototype
	window.opentsdb.types.initClass = function() {
		console.info("Building Base Class [%o]", this);
		var initializing = false, fnTest = /xyz/.test(function(){xyz;}) ? /\b_super\b/ : /.*/;

	  // The base Class implementation (does nothing)
	  this.Class = function(){};

	  // Create a new Class that inherits from this class
	  Class.extend = function(prop) {
	  	var _super = this.prototype;

	    // Instantiate a base class (but only create the instance,
	    // don't run the init constructor)
		initializing = true;
		var prototype = new this();
		initializing = false;

	    // Copy the properties over onto the new prototype
	    for (var name in prop) {
	      // Check if we're overwriting an existing function
	      prototype[name] = typeof prop[name] == "function" &&
	      typeof _super[name] == "function" && fnTest.test(prop[name]) ?
	      (function(name, fn){
	      	return function() {
	      		var tmp = this._super;

	            // Add a new ._super() method that is the same method
	            // but on the super-class
	            this._super = _super[name];

	            // The method only need to be bound temporarily, so we
	            // remove it when we're done executing
	            var ret = fn.apply(this, arguments);        
	            this._super = tmp;

	            return ret;
	        };
	    })(name, prop[name]) :
	    prop[name];
	}

	    // The dummy class constructor
	    function Class() {
	      // All construction is actually done in the init method
	      if ( !initializing && this.init )
	      	this.init.apply(this, arguments);
	  }

	    // Populate our constructed prototype object
	    Class.prototype = prototype;

	    // Enforce the constructor to be what we expect
	    Class.prototype.constructor = Class;

	    // And make this class extendable
	    Class.extend = arguments.callee;

	    return Class;
	};
		// ========================================================================================================
		//		Class extension functions
		// ========================================================================================================
		Class.prototype.sendRequest = $.proxy(function(request) {
			if(request==null) {
				throw "Passed request was null";
			}
			var deferred = $.Deferred();
			console.info("ServiceClient Sending Request: [%o]", request);

			chrome.runtime.sendMessage(request, function(resp) {
				if(resp==null && chrome.runtime.lastError!=null) {
					console.error("SendMessage Failed: [%o]", chrome.runtime.lastError);
					deferred.reject(chrome.runtime.lastError);	
					return false;
				}
				console.info("ServiceClient Received Response: [%o]", resp);
				deferred.resolve(resp);
				return true;
			});

			return deferred.promise();
		}, ctx);
		Class.prototype.startsWith = function(str, pattern) {
			if(str==null || (typeof str != "string")) throw "The passed value [" + str + "] was not a string";
			if(pattern==null) throw "The passed pattern was null";
			return str.indexOf(pattern.toString())==0;
		};
		Class.prototype.startsWithAny = function(str, pattern) {
			if(str==null || (typeof str != "string")) throw "The passed value [" + str + "] was not a string";
			if(arguments.length<2) return false;
			for(var i = 1, il = arguments.length; i < il; i++) {
				if(str.indexOf(arguments[i].toString())==0) return true;
			}
			return false;
		};

		Class.prototype.contains = function(str, pattern) {
			if(str==null || (typeof str != "string")) throw "The passed value [" + str + "] was not a string";
			if(pattern==null) throw "The passed pattern was null";
			return str.indexOf(pattern.toString())!=0;
		};
		Class.prototype.endsWith = function(str, pattern) {
			if(str==null || (typeof str != "string")) throw "The passed value [" + str + "] was not a string";
			if(pattern==null) throw "The passed pattern was null";
			spattern = pattern.toString();
			slength = spattern.length;
			xlength = str.length - slength;
			return str.indexOf(spattern)==xlength;
		};
		

	}






	// ========================================================================================================
	window.opentsdb.types.initClass.apply(window);
	console.info("OpenTSDB namespace initialized");
	console.group("Service Scripts");
	$.each(chrome.runtime.getManifest().app.background.scripts, function(index, script) {
		if(script.indexOf("/services/")==0) {
			var svcName = script.replace("/services/", "").replace(".js", "");
			console.info("Service Script: [%o]", svcName);
			window.opentsdb.dependencies[svcName] = $.Deferred();
		}
	});
	console.groupEnd();
	chrome.app.runtime.onRestarted.addListener(serviceInitializer);
	console.info("OpenTSDB Service dependencies created");	
	console.info("------------> [%s] nsinit.js", OP[1]);
});  

