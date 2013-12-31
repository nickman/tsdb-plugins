/**
 * Console OpenTSDB Server, Metric Catalog SQL
 * Whitehead, 2014
 */ 

var isql = {
  columnAttributes : [
    //{id:'id', classes: "visible readonly"},
  ],
  sqlGrid: null,
  sqlEditor: null,
  execSql: function() {
  	var sqlText = isql.sqlEditor.getValue();
  	console.info("Excuting SQL [%s]", sqlText);
    sendRemoteRequest(
      'ws://localhost:4243/ws',
      {
        "t":"req",
        "svc": "sqlcatalog",
        "op": "execsql", 
        "args": {
          "includemeta":"true", 
          "sql" : sqlText          
        }
      }).then(
      function(result) {
        console.info("[isql]: exec sql result:%o", result);
      },
      function(err) {
        console.error("[isql]: exec sql failed:%o", err);
      }
    );
  }
};

$(document).ready(function () {
	$('#go-btn').button({}).css({ width: '60px'})
		.click(function(e){
	    	isql.execSql();
	 	});
  console.info("CodeMirror Loaded");
	isql.sqlEditor = CodeMirror.fromTextArea(sqleditor, {
    	mode: "text/x-sql",
    	extraKeys: {
    		"Ctrl-Enter" : function() {
    			isql.execSql();
    		}
    	}
  	});
  isql.sqlEditor.setValue("select * from tsd_tsmeta");
  sqlGrid = $('#isqlGrid').dataTable({
    "bJQueryUI": true,
    "aaSorting" : []
  });
	console.info("SQLGrid Loaded");
});