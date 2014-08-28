var ws = null;
var q = null;
var svgContext = {};

$( document ).ready(function() {
	$(window).resize(function() {
		$("#main").width($(window).width()-$("#leftbar").width() - 50);
		$("#jsonOutput").height($(window).height()-$("#outputQCForm").height() - 70);		
	});
	ws = new WebSocketAPIClient();
	q = QueryContext.newContext();
	$("button.runAction").click(function(e) {
	    goAction(e);		
	    return false;        
	});
	
	$("input.runAction").keypress(function(e) {
	    if(e.which == 13) {
		    goAction(e);
		    return false;
	    }
	    return true;
	});
	$("#main").width($(window).width()-$("#leftbar").width() - 50);
	$("#jsonOutput").height($(window).height()-$("#outputQCForm").height() - 70);
	console.info("Index.js Loaded");
});

var defaultErrorHandler = function() {
	console.error("Default Error Handler Called ---  [%O]", arguments);
}
var defaultJsonHandler = function(result) {
	updateOutputContext(result.q);
	$('#jsonOutput').empty();
	$('#jsonOutput').append("<pre>" + syntaxHighlight(result.data) + "</pre>");
}


function goAction(e) {	
	var command = $(e.currentTarget).parents('form').first().attr('id');
	var commandFx = window[command];
	if(commandFx==null || !jQuery.isFunction(commandFx)) {
		throw new Error("Command not recognized (" + command + ")");
	}
	if($('#clearqc').is(':checked')) {
		q = getInputContext();
	} 		
	try {
		commandFx.call();
	} finally {
		return false;
	}
};



function findTagValuesTest() {
	var selected = $('input[name=goaction]:checked', '#goAction').val();	
	if("json"!=selected) {
		 showError("Find Tag Values", "Find Tag Values can only be output to JSON");
		 return;
	}
	var metric = getText("tvMetric", "*");			// e.g. sys.cpu
	var tagPairs = getObject("tvTagsPairs", {});   // e.g.  {'host':'*Server*', 'cpu':'*'},   OR   host, *Server*, cpu, *
	var tagKey = getText("tvTagKey", "*");			// e.g. type
	ws.getTagValues(q, metric, tagKey, tagPairs).then(
		defaultJsonHandler, defaultErrorHandler, defaultJsonHandler
	);

}

function findMetricNamesTest() {
	var selected = $('input[name=goaction]:checked', '#goAction').val();	
	if("json"!=selected) {
		 showError("Find Tag Values", "Find Tag Values can only be output to JSON");
		 return;
	}
	var isTags = $('#fmByTags').is(':checked');							// e.g. [byKeys]: host, type, cpu   OR [byTags]:   host, WebServer1
	var x = isTags ? getObject("fmTags", {}) : getArr("fmTags", []);
	if(isTags) {
		ws.getMetricNamesByTags(q, x).then(
			defaultJsonHandler, defaultErrorHandler, defaultJsonHandler
		);
	} else {
		ws.getMetricNamesByKeys(q, x).then(
			defaultJsonHandler, defaultErrorHandler, defaultJsonHandler
		);
	}
}


function findTagKeysTest() {
	var selected = $('input[name=goaction]:checked', '#goAction').val();	
	if("json"!=selected) {
		 showError("Find Tag Keys", "Find Tag Keys can only be output to JSON");
		 return;
	}
	var metric = getText("tkMetric", "*");
	var tagKeys = getArr("tkTagsKeys", []);
	ws.getTagKeys(q, metric, tagKeys).then(
		defaultJsonHandler, defaultErrorHandler, defaultJsonHandler
	);
}



function findUIDTest() {
	var selected = $('input[name=goaction]:checked', '#goAction').val();	
	if("json"!=selected) {
		 showError("Find UID", "Find UID can only be output to JSON");
		 return;
	}
	var pattern = getText("uidwc", "*");	
	var type = $("#uidType").val();
	ws.findUids(q, type, pattern).then(
		defaultJsonHandler, defaultErrorHandler, defaultJsonHandler
	);
}


function metricExpressionTest() {
	var selected = $('input[name=goaction]:checked', '#goAction').val();
	console.info("GO!  (Action is [%s])", selected);
	$('#jsonOutput').empty();
	if("json"==selected) {
		doDisplayJson();
	} else if("fulltree"==selected) {
		doFullTree();	
	} else if("incrtree"==selected) {
		doIncrementalTree();	
	}
}

function printServices() {
	var handle = function(result) {
		console.group("=============== WebSock Services ===============");
		console.dir(result.data);
		console.groupEnd();
	}
	ws.services(handle, null, handle);
}


function doDisplayJson() {
	var expr = $('#exprfield').val();
	console.info("Executing fetch for expression [%s]", expr);
	var handle = function(result) {			
		updateOutputContext(result.q);
		if(!result.q.continuous) {
			$('#jsonOutput').empty();
			$('#jsonOutput').append("<pre>" + syntaxHighlight(result.data) + "</pre>");
		} else {
			responseCount++;
			var index = responseCount;
			var app = "<a id='link" + index + "'>Response #" + index + " -- Results:" + result.data.length + "&nbsp;<font size='-2'>(Click to Expand)</font>";
			app += "<div id='data" + index + "'><pre>";
			app += syntaxHighlight(result.data);
			app += "</pre></div></a><br>";
			$('#jsonOutput').append($(app));
			$('#data' + index).toggle();
			$('#link' + index).click(function(){
				
				$('#data' + index).toggle();
			});
		}
	};
	var handleError = function() {
		console.error("resolveTSMetas Failed: [%O]", arguments);
	}
 
	// Retrieve data using expr call.
	// write formatted data to jsonOutput
	var responseCount = 0;
	ws.resolveTSMetas(q, expr).then(
			handle, handleError, handle
	);
};

function updateOutputContext(qctx) {	
	var ctx = QueryContext.readContext(qctx);
	if(ctx.isExpired()|ctx.isExhausted()|ctx.getNextIndex()==null) {
		q = QueryContext.newContext({timeout: 10000});
	}	
	$('#qoutCummulative').val(ctx.getCummulative());
	$('#qoutElapsed').val(ctx.getElapsed());
	$('#qoutNextIndex').val(ctx.getNextIndex());
	$('#qoutExhausted').val(ctx.isExhausted());
	$('#qoutExpired').val(ctx.isExpired());	
	if(qctx.elapsedTime) {
		try {
			$('#qoutRTElapsed').val(Math.round(qctx.elapsedTime));
		} catch (e) {}
	}
};

function getInputContext() {
	return QueryContext.newContext({
		pageSize: parseInt($("#qinPageSize").val()),
		maxSize: parseInt($("#qinMaxSize").val()),
		timeout: parseInt($("#qinTimeout").val()),
		continuous: $('#qinContinuous').is(':checked'),
		format: $("#qinFormat").val()
	});
}

function updateSvgTree(root) {	
	 var i = svgContext.i;


  // Compute the new svgContext.tree layout.
  var nodes = svgContext.tree.nodes(root).reverse(),
   links = svgContext.tree.links(nodes);

  // Normalize for fixed-depth.
  nodes.forEach(function(d) { d.y = d.depth * 120; });

  // Declare the nodesâ€¦
  var node = svgContext.svg.selectAll("g.node")
   .data(nodes, function(d) { return d.id || (d.id = ++i); });

  // Enter the nodes.
  var nodeEnter = node.enter().append("g")
   .attr("class", "node")
   .attr("transform", function(d) { 
    	return "translate(" + d.y + "," + d.x + ")"; 
	}) .on("click", function(d){
   		click(d);
   	})


  nodeEnter.append("circle")
   .attr("r", 5)
   .style("fill", "#fff");

  nodeEnter.append("text")
   .attr("x", function(d) { 
    return d.children || d._children ? -13 : 13; })
   .attr("dy", ".35em")
   .attr("text-anchor", function(d) { 
    return d.children || d._children ? "end" : "start"; })
   .text(function(d) { return d.name; })
   .style("fill-opacity", 1);
   

  // Declare the linksâ€¦
  var link = svgContext.svg.selectAll("path.link")
   .data(links, function(d) { return d.target.id; });

  // Enter the links.
  link.enter().insert("path", "g")
   .attr("class", "link")
   .attr("d", svgContext.diagonal);

}


function doIncrementalTree() {
	$('#jsonOutput').empty();
	var mt = MetaTree.newInstance("org", []);
	mt.get("org").addChild("dc", "org");


	try {
		initTree(mt);
		// mt.update();
		updateSvgTree(mt) ;
		
	} catch (e) {
		console.error(e);
	}



	// var expr = $('#exprfield').val();
	// console.info("Executing fetch for expression [%s]", expr);
	// ws.resolveTSMetas(q, expr).then(
	// 	function(result) {
	// 		updateOutputContext(result.q);
	// 		var mt = MetaTree.newInstance("org", result.data);
			
	// 	}
	// );	
}



function initTree(root) {

	var canvasWidth = $('#jsonOutput').width(),
	canvasHeight = $('#jsonOutput').height(),
	margin = {top: 30, right: 30, bottom: 30, left: 30},
 	svgWidth = canvasWidth - margin.right - margin.left,
 	svgHeight = canvasHeight - margin.top - margin.bottom;

	var tree = d3.layout.tree()
	 .size([svgHeight, svgWidth]);

	var diagonal = d3.svg.diagonal()
	 .projection(function(d) { return [d.y, d.x]; });

	var svg = d3.select("#jsonOutput").append("svg")
	 .attr("width", svgWidth + margin.right + margin.left)
	 .attr("height", svgHeight + margin.top + margin.bottom)
	  .append("g")
	 .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

		
	$('svg g').attr('id', 'viewport');
	var treeZoom = d3.behavior.zoom();
	treeZoom.on("zoom", zoomed);
	d3.select("svg").call(treeZoom); 
	function zoomed() {
		var zoomTranslate = treeZoom.translate();
		d3.select("#viewport").attr("transform", "translate("+zoomTranslate[0]+","+zoomTranslate[1]+")");
	
	};

	//$('svg').svgPan('viewport', true, true, true);



	svgContext.tree = tree;
	svgContext.svg = svg;
	svgContext.diagonal = diagonal;
	svgContext.i = 0;
	
	
}

function click(d) {
	var isKey = d.depth%2!=0;
	console.info("Node Click:  type:[%s] [%O]", isKey,  d);
	if(isKey) {
		ws.getTagValues(q, "*", d.name, {}).then(
			function(result) {
				for(var i = 0, x = result.data.length; i < x; i++) {
					d.addChild(result.data[i].name, d.path);					
				}
				updateSvgTree(d.getRoot());
			}
		).then(
			function() {
				console.info("Updated (TagValues) d: [%O]", d);
			}
		)
	} else {
		ws.getTagKeys(q, "*", [d.parent.name]).then(
			function(result) {
				var last = result.data.last();
				d.addChild(last.name, d.path);					
				updateSvgTree(d.getRoot());
			}
		).then(
			function() {
				console.info("Updated (TagKeys) d: [%O]", d);
			}
		)
	}


	// 2014-08-27 15:23:16.918Node Click:  type:[true] [MetaTreedepth: 1id: 1metrics: nullname: "dc"parent: MetaTreepath: "org/dc"x: 159.703125y: 60

	// if (d.children) {
 // 		d._children = d.children;
 // 		d.children = null;
 //  	} else {
 // 		d.children = d._children;
 // 		d._children = null;
 //  	}	
 //  	update(d);
}






function doFullTree() {
	var expr = $('#exprfield').val();
	console.info("Executing fetch for expression [%s]", expr);
	// Retrieve data using expr call.
	// write formatted data to jsonOutput
	q.format = "D3";
	ws.resolveTSMetas(q, expr).then(
		function(result) {
			$('#jsonOutput').empty();
			updateOutputContext(result.q);
			//$('#jsonOutput').append("<pre>" + syntaxHighlight(result.data) + "</pre>");
			//$('#jsonOutput').json2html(convert('json',result.data,'open'),transforms.object);
			var cluster = d3.layout.cluster().size([$('#jsonOutput').height()*5, $('#jsonOutput').width()*5]);
			var diagonal = d3.svg.diagonal()
		    .projection(function(d) { 
		    	return [d.y, d.x]; 
		    });

			
			var svg = d3.select("div#jsonOutput").append("svg")
		    .attr("width", $('#jsonOutput').width()) // width + margin.right + margin.left)
		    .attr("height", $('#jsonOutput').height()) //height + margin.top + margin.bottom)
		    .append("g")
		    .attr("transform", "translate(10,3)");
			
			
		var nodes = cluster.nodes(result.data),
		links = cluster.links(nodes);
		nodes.forEach(function(d) { d.y = d.depth * 90; });

		  var link = svg.selectAll(".link")
		      .data(links)
		    .enter().append("path")
		      .attr("class", "link")
		      .attr("d", diagonal);

		  var node = svg.selectAll(".node")
		      .data(nodes)
		    .enter().append("g")
		      .attr("class", "node")
//		      .attr("transform", function(d) { return "rotate(" + (d.x - 90) + ")translate(" + d.y + ")"; })
		      .attr("transform", function(d) { 
		      	//console.info("Transform: [%O]", d);
		      	return "translate(" + d.y + "," + d.x + ")"; 
		      })

		  node.append("circle")
		      .attr("r", 4.5);

		  node.append("text")
		      .attr("dx", function(d) { return d.children ? -8 : 8; })
		      .attr("dy", 3)
//		      .attr("text-anchor", function(d) { return d.x < 180 ? "start" : "end"; })
		      .style("text-anchor", function(d) { 
		    	  return d.children ? "end" : "start"; }
		      )
		      .text(function(d) { return d.name; });
			
			
		}	
	).then(function(){
		$('svg g').attr('id', 'viewport');
		var treeZoom = d3.behavior.zoom();
		treeZoom.on("zoom", zoomed);
		d3.select("svg").call(treeZoom); 
		function zoomed() {
			var zoomTranslate = treeZoom.translate();
			d3.select("#viewport").attr("transform", "translate("+zoomTranslate[0]+","+zoomTranslate[1]+")");
		
		};
		//$('svg').svgPan('viewport', true, true, true);
	});
};


function showError(topic, message) {
	$('#jsonOutput').empty();
	$('#jsonOutput').append("<font color='red'><p>Error executing [" + topic + "]</p><p>" + message + "</p></font>");
}

function getText(key, defaultv) {
	var v = $('#' + key).val();
	if(v!=null) {
		v = v.replace(/ /g, '');
		if(v=='') v = defaultv;		
	} else {
		v = defaultv;
	}
	$('#' + key).val(v);		
	return v;
}

function getArr(key, defaultv) {
	var v = $('#' + key).val();
	if(v!=null) {
		v = v.replace(/ /g, '');
		if(v=='') v = defaultv;		
		return v.split(',');
	} else {
		return defaultv;
	}
}

function getObject(key, defaultv) {
	var v = $('#' + key).val();
	if(v!=null) {
		v = v.replace(/ /g, '').replace(/'/g, '"')
		if(v.indexOf("{")==0) {
			try {
				var o = JSON.parse(v);
				return o;
			} catch (e) {}
		}
		if(v=='') v = defaultv;		
		var arr = v.split(',');
		var x = arr.length;
		if(x%2 != 0) {			
			var err = Error("Invalid number of members in [" + v + "]. Must be an even number of values to create key value pair object");
			console.error(err);
			throw err;
		}
		var obj = {};
		for(var i = 0; i < x; i++) {
			var key = arr[i];
			i++;
			var value = arr[i];
			obj[key] = value;
		}
		return obj;
	} else {
		return defaultv;
	}
}