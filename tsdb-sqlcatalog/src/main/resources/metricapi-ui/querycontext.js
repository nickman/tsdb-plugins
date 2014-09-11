//========================================================================
//   QueryContext definition
//========================================================================

function QueryContext(props) { 
	this.nextIndex = (props && props.nextIndex) ? props.nextIndex : null; 
	this.pageSize = (props && props.pageSize) ? props.pageSize : 100;
	this.maxSize = (props && props.maxSize) ? props.maxSize : 5000;
	this.timeout = (props && props.timeout) ? props.timeout : 3000;
	this.continuous = (props && props.continuous) ? props.continuous : false;	
	this.format = (props && props.format) ? props.format : "DEFAULT";	
	this.exhausted = false;
	this.cummulative = 0;
	this.elapsed = -1;
	this.expired = false;			
}

QueryContext.newContext = function(props) {
	return new QueryContext(props);
}

QueryContext.readContext = function(props) {
	return new QueryContext(props);	
}

QueryContext.prototype.shouldContinue = function() {
	console.info("Max: c:[%s], m:[%s]", this.cummulative , this.maxSize)
	return this.continuous && this.nextIndex != null && !this.isExpired() && !this.isExhausted() && (this.cummulative < this.maxSize);
}

QueryContext.prototype.clone = function() {
    try {
    	return JSON.parse(JSON.stringify(this));
    } catch (e) {
    	throw new Error("Unable to copy this! Its type isn't supported.");
    }
}

QueryContext.prototype.export = function() {
	return {
		extIndex : this.nextIndex,
		pageSize : this.pageSize,
		maxSize : this.maxSize,
		timeout : this.timeout,
		continuous : this.continuous,
		format : this.format,
		exhausted : this.exhausted,
		cummulative : this.cummulative,
		elapsed : this.elapsed,
		expired : this.expired		
	}
}


QueryContext.prototype.getFormat = function() {
	return this.format;
};

QueryContext.prototype.format = function(format) {
	var f = format.replace(/ /g, "").toUpperCase();
	this.format = f;
	return this;
};


QueryContext.prototype.getElapsed = function() {
	return this.elapsed;
};

QueryContext.prototype.getNextIndex = function() {
	return this.nextIndex;
};

QueryContext.prototype.isContinuous = function() {
	return this.continuous;
};


QueryContext.prototype.nextIndex = function(index) {
	this.nextIndex = index;
	return this;
};


QueryContext.prototype.getCummulative = function() {
	return this.cummulative;
};

QueryContext.prototype.getPageSize = function() {
	return this.pageSize;
};

QueryContext.prototype.pageSize = function(size) {
	this.pageSize = size;
	return this;
};


QueryContext.prototype.getMaxSize = function() {
	return this.maxSize;
};

QueryContext.prototype.maxSize = function(size) {
	this.maxSize = size;
	return this;
};

QueryContext.prototype.continuous = function(enabled) {
	return this.continuous = enabled;
	return this;
};


QueryContext.prototype.isExhausted = function() {
	return this.exhausted;
};

QueryContext.prototype.isExpired = function() {
	return this.expired;
};

QueryContext.prototype.getTimeout = function() {
	return this.timeout;
};

QueryContext.prototype.timeout = function(time) {
	this.timeout = time;
	return this;
};

QueryContext.getCtx = function(ctx) {
	var sortable = [];
	for (var item in ctx) {
	    sortable.push([item, ctx[item]])
	}
	sortable.sort(function(a, b) {return a[1] - b[1]});
	console.group("======================  CTX ======================");
	var prior = -1,	elapsed = 0, total = 0;
	for(var i = 0, x = sortable.length; i < x; i++) {
		if(prior != -1) {
			elapsed = sortable[i][1] - prior;			
		}
		prior = sortable[i][1];
		total += elapsed;
		console.info("Step: [%s] -- [%s] -- [%s]   ----- Total: [%s]", sortable[i][0], prior, elapsed, total);
	}
	console.groupEnd();
	return sortable;
}



QueryContext.prototype.refresh = function(props) {
	if(props!=null) {
		this.nextIndex = props.nextIndex!=null ? props.nextIndex : null;
		this.exhausted = props.exhausted!=null ? props.exhausted : false;
		this.cummulative = props.cummulative!=null ? props.cummulative : 0;
		this.elapsed = props.elapsed!=null ? props.elapsed : -1;
		console.info("ReadContext Elapsed: [%s]", this.elapsed);
		this.expired = props.expired!=null ? props.expired : false;
		this.format = props.format!=null ? props.formar : "DEFAULT";	
		if(props.timeout!=null) this.timeout = props.timeout;
	}
}

QueryContext.prototype.toString = function() {
	return "{timeout:" + this.timeout + ", nextIndex:" + this.nextIndex + ", pageSize:" + this.pageSize + ", maxSize:" + this.maxSize + ", exhausted:" + this.exhausted + ", cummulative:" + this.cummulative + ", expired:" + this.expired + ", elapsed:" + this.elapsed + "}";
};

