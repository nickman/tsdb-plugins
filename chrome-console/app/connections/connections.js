/**
 * Console OpenTSDB Server Connection Manager
 * Whitehead, 2014
 */ 

document.domain = chrome.runtime.id;
var _db = null;
var _connectionStore = null;
var _connections = {};
var _dirty = {};
var cTable = null;


var READ_ONLY = 0;
var READ_WRITE = 1;

/*
        "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
            // Append the grade to the default row class name
            if ( aData[4] == "A" )
            {
                $('td:eq(4)', nRow).html( '<b>A</b>' );
            }
        },
        "aoColumnDefs": [ {
                "sClass": "center",
                "aTargets": [ -1, -2 ]
        } ]
*/        

function loadConnections() {
  parent.allData("connections").then(
    function(data) {
      _connections = data;
      console.info("Connection Data:[%o]", data);
      initGrid(data);
    },
    function(evt) {
      console.error("Failed to load connections-->[%o]", evt);
    }
  );
}

function handleCellEdit(value, settings) {
    //console.info("Handling Edit:  value:%o,  settings:%o, this:%o", value, settings, this);
    console.info("Handling Edit:  arr:%s, id:%s, newvalue:%s", this.arr, this.id, value);
    return value;
}

function initGrid(data) {
  console.debug("Initing Grid");

  /*
  $('#connectionsGrid').dataTable( {
	"bPaginate": false,
	"bLengthChange": false,
	"bFilter": false,
	"bSort": false,
	"bInfo": false,
	"bAutoWidth": true
    } ); 	
   */
  
  var columnAttributes = [      
      {id:'id', classes: "invisible readonly"},
      {id:'name', classes: "visible editable", type:'textarea'},
      {id:'auto', classes: "visible editable", type:'checkbox'},
      {id:'url', classes: "visible editable", type:'textarea'},
      {id:'type', classes: "visible editable", type:'select', select:{'tcp':'TCP', 'websocket':'WebSocket', 'http':'HTTP'}},  // add: 'selected':<value>
      {id:'permission', classes: "visible readonly"},
      {id:'permission_pattern', classes: "visible readonly"}
  ];
  
  var oaAttrs = [];
  
  $.each(columnAttributes, function(index, item) {
    var attrs = {bAutoWidth: true};
    attrs.bVisible = (item.classes.indexOf('invisible')==-1);
    oaAttrs.push(attrs);
  });
  
  cTable = $('#connectionsGrid').dataTable({
    "bJQueryUI": true, "aoColumns": oaAttrs,
		"fnRowCallback": function( nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
			//console.info("fnRowCallback: nRow:%o, aData:%o, iDisplayIndex:%o, iDisplayIndexFull:%o", nRow, aData, iDisplayIndex, iDisplayIndexFull);
			var id = aData[0];
			var rowId = 'crow_' + aData[0];
			nRow.id = rowId;
			var cAttrs = null;
			$.each(nRow.childNodes, function(index, item) {
			  cAttrs = columnAttributes[index];
			  item.id = rowId + "_" + (index);
			  $('#' + item.id).attr('arr', (index));
			  $('#' + item.id).addClass(cAttrs.classes);
			});
    }    
  });
  console.info("Editable Configured");
  $.each(data, function(index, item) {
    // <!-- {name: 'Default', url: 'ws://localhost:4243/ws', type: 'websocket', permission: false}, -->
    $('#connectionsGrid').dataTable().fnAddData( [
      item.id, item.name, item.auto, item.url, item.type, item.permission, item.permission_pattern
    ] ); 
  });
  cTable.$('td.editable').editable( handleCellEdit , {
    "callback": function( sValue, y ) {
	    var aPos = cTable.fnGetPosition( this );
	    cTable.fnUpdate( sValue, aPos[0], aPos[1] );
    },
    "onsubmit" : function(a,b) {
	console.info("edit-onsubmit: a:%o, b:%o, this:%o", a,b,this);
	return "foobar";
    },
    "height": "14px",
    "width": "100%"
  } );  
  
}

$(document).ready(function() { 
 
    loadConnections();
});




