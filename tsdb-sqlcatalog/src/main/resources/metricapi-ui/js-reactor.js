function $reactor () {

	var dispatcherTypes = ["single"];

	var active = {};

	var bus = new Bacon.Bus();

	// =======================================================================
	//	DEBUG
	// =======================================================================
	bus.onValue(function(event){
		console.debug("[reactor] Bus Event: [%O]", event);
	});
	// =======================================================================

	var websockurl = 'ws://' + document.location.host + '/ws';

	function Dispatcher(props) { 
		var opts = props || {};
		var type = opts.type || "dedicated";

		if(type=="dedicated") {
			this.gDedicatedWorker = new Worker("dedicatedworker.js");
			this.gDedicatedWorker.onerror = function(evtError) {
				console.error("[DedicatedWorker] Received Error From Worker: [%O]", evtError);
				bus.error(evtError);
			};
			this.gDedicatedWorker.onmessage = function(evtMsg) {				
				bus.push(evtMsg);
			}

			console.info("Dedicated Thread Dispatcher Created");
			console.dir(this.gDedicatedWorker);
			this.gDedicatedWorker.postMessage("init");
		} else if(type=="shared") {
			var cnt = opts.threadcount || 4;
			this.sharedworkers = [];
			for(var i = 0; i < cnt; i++) {
				var sharedWorker = new SharedWorker("sharedworker.js", "Worker" + i);
				sharedWorker.onerror = function(evtError) {
					bus.error(evtError);
				};
				sharedWorker.onmessage = function(evtMsg) {
					bus.push(evtMsg);
				}
				this.sharedworkers.push(sharedWorker);
			}
		}
	}

	function Reactor(props) {
		var opts = props || {};
		var dispatcher = new Dispatcher(opts);
	}

	var createReactor = function(args) {
		var opts = args || {};
		var name = opts.name || "some-name";
		var dispatcher = null;
		var r = new Reactor(args);
		active[name] = r;
		return r;
	}

	console.group("***************** Initing jsReactor *****************");
	if (window.Worker) {
		dispatcherTypes.push("dedicated");
		console.info("Dedicated Worker Threads Supported");
	} else {
		console.info("Dedicated Worker Threads NOT Supported");
	} 
	if (window.SharedWorker) {
		dispatcherTypes.push("shared");
		console.info("Shared Worker Threads Supported");
	} else {
		console.info("Shared Worker Threads NOT Supported");
	}

	console.info("Supported Dispatcher Types: %s", JSON.stringify(dispatcherTypes));

	console.groupEnd();

	this.createReactor = createReactor;

	return {
		create: createReactor
	}

};
$reactor();
