(function() {
	console.info("Starting....");


	var GETIMGSRC = function() {
		return $('img.gwt-Image').src;
	}
	var embedder = null;

	var bindEmbedder = function(e) {
	  embedder = e.source;
	};


	window.addEventListener('message', function(e) {
	    e.source.postMessage('message', '*');
	  }
	);
	  


	console.info("Finished");
}());