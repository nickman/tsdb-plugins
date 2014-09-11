



// inherit() returns a newly created object that inherits properties from the
// prototype object p. It uses the ECMAScript 5 function Object.create() if
// it is defined, and otherwise falls back to an older technique.
function inherit(p) {
	if (p == null) throw TypeError(); // p must be a non-null object
	if (Object.create) // If Object.create() is defined...
		return Object.create(p); // then just use it.
	var t = typeof p; // Otherwise do some more type checking
	if (t !== "object" && t !== "function") 
		throw TypeError();
	function f() {}; // Define a dummy constructor function.
	f.prototype = p; // Set its prototype property to p.
	return new f(); // Use f() to create an "heir" of p.
}




function testSub() {
	//var expr = $('#exprfield').val() + ",*";
	var expr = "sys*:dc=dc1,host=WebServer1,*"
	console.info("Subscribing to expression [%s]", expr);
	ws.subscribe(expr);
	// .then(
	// 	function(data) {
	// 		console.info("SUB TERM: [%O]", data);
	// 	},
	// 	function(data) {
	// 		console.error("SUB ERROR: [%O]", arguments);
	// 	},
	// 	function(data) {
	// 		console.info("SUB EVENT: [%O]", data);
	// 	}
	// );
}

/*
	SUB STARTED:
	============
	id: 1486981487
	msg: Object
	subId: 1
	__proto__: Object
	op: "subst"
	rerid: 1
	t: "resp"

	SUB EVENT:
	==========
	metric: "sys.cpu"
	subid: 2
	tags: Object
	cpu: "1"
	dc: "dc4"
	host: "WebServer1"
	type: "sys"
	__proto__: Object
	ts: 1409783673
	type: "l"
	value: 17


	SUB TERM:
	=========	
*/



function testIncremental(startingKey) {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({timeout: 10000});

	var root = MetaTree.newInstance(startingKey);	

	var onKey = function(data, tree, state) {
		console.group("================ onKey ================");
		try {
			if(data==null || data.data==null || data.data.length==0) {
				console.info("Key Exhausted at [%O]", state);
				return;
			}

			var key = data.data.last().name;
			state.keys.push(key);
			//console.info("Last Key: [%s]", key);
			var nextTree = tree.tag(tree, key);
			ws.getTagValues(QueryContext.newContext({timeout: 10000}), 'sys.cpu', key, state.tags).then(function(data) { onValue(data, nextTree, state); });	
		} finally {
			console.groupEnd();
		}
		//ws.getTagKeys(q, 'sys.cpu', ['dc']).then(	
	}
	var onValue = function(data, tree, state) {
		//ws.getTagValues(q, 'sys.cpu', 'type', {host:'*Server*', cpu:'*'}).then(
		//data.data[x].name 
		console.group("================ onValue ================");
		//console.debug("Data: [%O], Tree: [%O], State: [%O]", data, tree, state);
		try {
			jQuery.each(data.data, function(index, uid){
				//console.info("value: [%s]", uid.name);
				var nextTree = tree.tag(tree, uid.name);
				state.tags[state.keys.last()];
				ws.getTagKeys(QueryContext.newContext({timeout: 10000}), 'sys.cpu', state.keys).then(function(data) { onKey(data, nextTree, state.clone()); });
			});
		} finally {
			console.groupEnd();
		}
	}
	var state = {
		tags: {},
		keys: [startingKey]
	}
	ws.getTagValues(QueryContext.newContext({timeout: 10000}), 'sys.cpu', startingKey, {}).then(function(data) { onValue(data, root, state); });


}

var ANNOTATION_URL = 'http://' + document.location.host + '/api/annotation'; 

function postGlobalAnnotations() {
	var callback = function(result) {
		console.info("Global Annotation Post Result: [%O]", result);
	}
	for(var i = 0; i < 100; i++) {
		var data = {
			startTime : new Date().getTime() - (i * 1000),
		  	description : "Test Annotation #" + i,
		  	notes : "This is a test annotation, with id [" + i + "]",
		  	custom : {
		    	type : "Global"
		  	}
		};
		$.post(ANNOTATION_URL, JSON.stringify(data), callback, "json");
	}
}

function postTSUIDAnnotations() {
	var callback = function(result) {
		console.info("TSUID Annotation Post Result: [%O]", result);
	}
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({pageSize: 100, maxSize: 100, timeout: 10000});	
	ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer*").then(
		function(result) {
			console.info("TSUID: [%O]", result);
			for(var i = 0, x = result.data.length; i < x; i++) {
				var t = result.data[i];
				var data = {
					startTime : new Date().getTime() - (i * 1000),
				  	description : "Test TSUID Annotation #" + i,
				  	notes : "This is a test annotation, with id [" + i + "]",
				  	tsuid : t.tsuid,
				  	custom : {
				    	type : "TSUID",
				    	tsuid : t.tsuid,
				    	name: t.name
				  	}
				};
				$.post(ANNOTATION_URL, JSON.stringify(data), callback, "json");
			}
		}
	);
}


function testAll() {
	if(ws==null) {
		ws = new WebSocketAPIClient();
	}
	var q = null;
	ws.findUids(q, "TAGV", "Web*").then(
		function(result) { console.info("findUids Result: [%O]", result); },
		function() { console.error("findUids Failed: [%O]", arguments);}
	);
	// ws.getAnnotations(q, "sys*:dc=dc1,host=WebServer1|WebServer5", 0).then(
	// 	function(result) { console.info("globalAnnotations Result: [%O]", result); },
	// 	function() { console.error("globalAnnotations Failed: [%O]", arguments);}
	// );

	

	// ws.getMetricNamesByKeys(q, ['host', 'type', 'cpu']).then(
	// 	function(result) { console.info("MetricNamesByKeys Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("MetricNamesByKeys Failed: [%O]", arguments);}
	// );
	// ws.getMetricNamesByTags(q, {host:'WebServer1'}).then(
	// 	function(result) { console.info("MetricNamesByTags Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("MetricNamesByTags Failed: [%O]", arguments);}
	// )
	// ws.getTSMetas(QueryContext.newContext({continuous:true, pageSize: 50, timeout: 60000}), 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
	// ws.getTSMetas(QueryContext.newContext({continuous:true, pageSize: 500, timeout: 60000, maxSize: 40000}), '*', {dc:'*'}).then(		
	// 	function(result) { console.info("TSMetas FINAL Result: [%O]  --  cummulative: %s", result, result.q.cummulative);},
	// 	function() { console.error("TSMetas Failed: [%O]", arguments);},
	// 	function(result) { console.info("TSMetas INTERRIM Result: [%O]", result);}
	// );
	// ws.getTagKeys(q, 'sys.cpu', ['dc', 'host', 'cpu']).then(
	// 	function(result) { console.info("TagKeys Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("TagKeys Failed: [%O]", arguments);}
	// );
	// ws.getTagValues(q, 'sys.cpu', 'type', {host:'*Server*', cpu:'*'}).then(
	// 	function(result) { console.info("TagValues Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("TagValues Failed: [%O]", arguments);}
	// );
	// ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer1|WebServer5").then(
	// 	function(result) { console.info("resolveTSMetas Result: [%O]", result); 
	// 		//console.debug("JSON: [%s]", JSON.stringify(result));
	// 	},
	// 	function() { console.error("resolveTSMetas Failed: [%O]", arguments);}
	// );

};

var EXPR = "*:dc=*";

function testTree(expr) {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext({pageSize: 20000, maxSize: 20000, timeout: 10000});
	console.info("QC: %s", q.toString());
	if(EXPR == null) EXPR = "*:dc=*";

	// ws.d3TSMetas(q, "sys.cpu:dc=dc1,host=WebServer1|WebServer5").then(
	ws.d3TSMetas(q, expr || EXPR).then(	
		function(d3Data) {
			console.info("D3 Data: [%O]", d3Data.data);
			dgo(d3Data.data);
			console.info("QC: %O", q);
		}
	);
}

function dgo(root) {

  var nodes = cluster.nodes(root),
      links = cluster.links(nodes);

  var link = svg.selectAll(".link")
      .data(links)
    .enter().append("path")
      .attr("class", "link")
      .attr("d", diagonal);

  var node = svg.selectAll(".node")
      .data(nodes)
    .enter().append("g")
      .attr("class", "node")
      .attr("transform", function(d) { 
      	//console.info("Transform: [%O]", d);
      	return "translate(" + d.y + "," + d.x + ")"; 
      })

  node.append("circle")
      .attr("r", 4.5);

  node.append("text")
      .attr("dx", function(d) { return d.children ? -8 : 8; })
      .attr("dy", 3)
      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
      .text(function(d) { return d.name; });
	
}


/*

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.getMetricNames(q, ['host', 'type', 'cpu']);

var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.serviceRequest("meta", "metricnames", {q: q, keys : ['host', 'type', 'cpu']}) 

function dgo(root) {

  var nodes = cluster.nodes(root),
      links = cluster.links(nodes);

  var link = svg.selectAll(".link")
      .data(links)
    .enter().append("path")
      .attr("class", "link")
      .attr("d", diagonal);

  var node = svg.selectAll(".node")
      .data(nodes)
    .enter().append("g")
      .attr("class", "node")
      .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; })

  node.append("circle")
      .attr("r", 4.5);

  node.append("text")
      .attr("dx", function(d) { return d.children ? -8 : 8; })
      .attr("dy", 3)
      .style("text-anchor", function(d) { return d.children ? "end" : "start"; })
      .text(function(d) { return d.name; });
	
}


var ws = new WebSocketAPIClient();
var q = QueryContext.newContext();
ws.getTSMetas(q, 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
	function(result) { console.info("TSMetas Result: [%O]", result); 

		var d3Data = {
			root: {
				children : [],
				name : "org"
			}
		}
		var root = d3Data.root;

		try {
			dgo(root);
		}  catch (e) {
			console.error(e);
		}
	},
	function() { console.error("TSMetas Failed: [%O]", arguments);}
);




 */


function flare() {
	var ws = new WebSocketAPIClient();
	var q = QueryContext.newContext();
	ws.getTSMetas(q, 'sys.cpu', {host:'WebServer*', type:'combined', cpu:'*'}).then(
		function(result) { console.info("TSMetas Result: [%O]", result); 

			var d3Data = {
				root: {
					children : [],
					name : "org"
				}
			}
			var root = d3Data.root;
			
			try {
				dgo(root);
			}  catch (e) {
				console.error(e);
			}
		},
		function() { console.error("TSMetas Failed: [%O]", arguments);}
	);

}

//========================================================================
// jQuery init
//========================================================================

(function( $ ){
	$(function(){
		// jQuery Init Stuff Here.
		console.info("Initialized jQuery");
	});				
})( jQuery );	


function syntaxHighlight(json) {
    if (typeof json != 'string') {
         json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}


/*

var obj = {a:1, 'b':'foo', c:[false,'false',null, 'null', {d:{e:1.3e5,f:'1.3e5'}}]};
var str = JSON.stringify(obj, undefined, 4);

output(str);
output(syntaxHighlight(str));

=================== CSS  =====================

pre {outline: 1px solid #ccc; padding: 5px; margin: 5px; }
.string { color: green; }
.number { color: darkorange; }
.boolean { color: blue; }
.null { color: magenta; }
.key { color: red; }




 */