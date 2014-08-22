//========================================================================
//   Meta Tree definition
//========================================================================

function MetaTree(name, path) { 
	this.name = name;
	this.path = path;
	this.children = [];

}

QueryContext.newContext = function(props) {
	return new QueryContext(props);
}

QueryContext.prototype.getElapsed = function() {
	return this.elapsed;
};
