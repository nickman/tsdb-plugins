
function QueryContext(props) { 
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.pageSize = (props && props.pageSize) ? props.pageSize : 100;
	this.maxSize = (props && props.maxSize) ? props.maxSize : 5000;
	this.exhausted = false;
}

QueryContext.prototype.getNextIndex = function() {
	return this.nextIndex;
};

QueryContext.prototype.getPageSize = function() {
	return this.pageSize;
};

QueryContext.prototype.getMaxSize = function() {
	return this.maxSize;
};

QueryContext.prototype.isExhausted = function() {
	return this.exhausted;
};

//Define a textual representation for a query context
QueryContext.prototype.toString = function() {
	return this.rank.toString() + " of " + this.suit.toString();
};