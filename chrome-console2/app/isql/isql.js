/**
 * Console OpenTSDB Server, Metric Catalog SQL
 * Whitehead, 2014
 */ 

var isql = {
  columnAttributes : [
    //{id:'id', classes: "visible readonly"},
  ],
  sqlEditor: null,
  execSql: function() {
  	var sqlText = isql.sqlEditor.getValue();
  	console.info("Excuting SQL [%s]", sqlText);
  }
};

$(document).ready(function () {
	$('#go-btn').button({}).css({ width: '60px'})
		.click(function(e){
	    	isql.execSql();
	 	});

	isql.sqlEditor = CodeMirror.fromTextArea(sqleditor, {
    	mode: "text/x-sql",
    	extraKeys: {
    		"Ctrl-Enter" : function() {
    			isql.execSql();
    		}
    	}
  	});
	console.info("CodeMirror Loaded");
});