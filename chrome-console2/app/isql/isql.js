/**
 * Console OpenTSDB Server, Metric Catalog SQL
 * Whitehead, 2014
 */ 

var isql = {
  me: isql,
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
        
        $('#sqlresults').children().remove();
        $('#sqlresults').append($('<table id="isqlGrid" cellpadding="0" cellspacing="0" border="0" class="display" width="90%" style="font-size: smaller; width: 90%; height: 100%;"><thead><tr id="headerrow"></tr></thead><tbody></tbody></table>'));
        var columnNames = [];
        $.each(result.msg.meta, function(index, value){
          columnNames.push({sTitle: value.name, mData: value.name.toLowerCase()});
          $('#headerrow').append("<th>" + value.name + "</th>");
        }); 
        var rows = [];
        $.each(result.msg.data, function(index, value){
          if(value.custom!=null) {
            value.custom = "";
          }
          rows.push(value);
        });
        try {
          console.info("Rendering table: columns:[%o], rows:[%o]", columnNames, rows);
          $('#isqlGrid').dataTable({
              "bJQueryUI": true,
              "aaSorting" : [],
              "sScrollY": "95%",
              "sScrollX": "95%",
//              "sScrollXInner": "100%",
              "bScrollCollapse": true,                            
              "aoColumns": columnNames,
              "aaData" : rows
            });               
        } catch (e) {
          console.error("Failed to init sqlgrid [%o]", e);
        }
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
  /*
  sqlGrid = $('#isqlGrid').dataTable({
    "bJQueryUI": true,
    "aaSorting" : []
  });
  $('#isqlGrid>thead').children().first().attr("id", "headerrow")
	console.info("SQLGrid Loaded");
  */
});