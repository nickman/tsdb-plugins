//========================================================================
//   Meta Tree definition
//========================================================================

function MetaTree(name, path) { 
	if(name==null) throw new Error("The passed name was null");
	this.name = name;
	this.path = (path==null || ""==path) ? name : (path + "/" + name);
	this.children = [];
	this.metrics = null; // []
};

MetaTree.newInstance = function(name, path) {
	return new MetaTree(name, path);
}

MetaTree.prototype.getChild = function(name) {
    for(var i = 0, x = this.children.length; i < x; i++) {
        if (this.children[i].name == name) return this.children[i];
    }
    return null;
}

MetaTree.prototype.getRoot = function() {
	if(this.depth && this.depth==0) return this;
	if(this.parent==null) return null;
	var d = -1;
	var startingParent = this.parent;
	while(true) {
		if(startingParent.depth==null) return null;
		d = startingParent.depth;
		if(d==0) return startingParent;
		if(!startingParent.parent) return null;
		startingParent = startingParent.parent;
	}
}


MetaTree.prototype.addChild = function(name, path) {
	if(name==null) throw new Error("The passed name was null");
	var child = null;
	if(this.children==null) {
		this.children = [];
	} else {
		child = this.getChild(name);
	}	
	if(child!=null) return child;
	if(path==null) path = name;
	this.children.push(new MetaTree(name, path));
};

MetaTree.prototype.addMetrics = function() {
	if(this.metrics==null) this.metrics = [];
	for(var i = 0, x = arguments.length; i < x; i++) {
		this.metrics.concat(new MetaTree(arguments[i], this.path));
	}
};

MetaTree.prototype.isLeaf = function() {
	return this.metrics != null;
};

MetaTree.prototype.isBranch = function() {
	return this.metrics == null;
};

MetaTree.prototype.tag = function(parent, name) {
	var mt = this.getChild(name);
	if(mt==null) {
		mt = new MetaTree(name, parent.path)
		this.children.push(mt);
	}
	return mt;
};

MetaTree.prototype.get = function(path) {
	if(path==null || ""==path) return null;
	if(this.path==path) return this;
	for(var i = 0, x = this.children.length; i < x; i++) {
		if(this.children[i].path==path) return this.children[i];
	}
	var pathTree = null;
	for(var i = 0, x = this.children.length; i < x; i++) {
		pathTree = this.children[i].get(path);
		if(pathTree!=null) return pathTree;		
	}
	return pathTree;
};

MetaTree.mapTags = function(tsmeta) {
	var map = {};
	var key = null, value = null;
	for(var i = 0, x = tsmeta.tags.length; i < x; i++) {
		var tag = tsmeta.tags[i];
		if(tag.type=="TAGK") key = tag.name;
		else {
			value = tag.name;
			map[key] = value;
		}
	}
	return map;
}

MetaTree.arrTags = function(tsmeta) {
	var arr = [];
	for(var i = 0, x = tsmeta.tags.length; i < x; i++) {
		arr.push(tsmeta.tags[i].name);
	}
	return arr;
}

MetaTree.import = function(rootName, tsmetas) {
	var root = MetaTree.newInstance(rootName, tsmetas);
	var current = root;
	for(var i = 0, x = tsmetas.length; i < x; i++) {
		var tsmeta = tsmetas[i];
		var arrTags = MetaTree.arrTags(tsmeta);			
		for(var a = 0, b = arrTags.length; a < b; a++) {
			current = current.tag(current, arrTags[a]);
		}		
		current.addMetrics(tsmeta.metric.name);
		current = root;
	}
	return root;
}


function testWsMetaTree() {
	// var ws = new WebSocketAPIClient();
	// var q = null;
	ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer1|WebServer5").then(
		function(result) { 
			console.info("resolveTSMetas Result: [%O]", result); 
			var mt = MetaTree.newInstance("org", result.data);
			console.info("resolveTSMetas MetaTree: [%O]", mt); 
			dgo(mt);
		},
		function() { console.error("resolveTSMetas Failed: [%O]", arguments);}
	);	
}


function testBasicMetaTree() {
	var root = MetaTree.newInstance("dc");
	var mt = root;
	var slots = ["dc1", "host", "WebServer1", "cpu", "0", "type", "combined"];
	for(var i = 0, x = slots.length; i < x; i++) {
		mt = mt.tag(mt, slots[i]);
	}
	console.dir(root);
	console.info('Fetching Path ["dc/dc1/host/WebServer1/cpu/0"] --> [%O]', root.get("dc/dc1/host/WebServer1/cpu/0"));
	dgo(root);
}
