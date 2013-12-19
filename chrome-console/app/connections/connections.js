/**
 * Console OpenTSDB Server Connection Manager
 * Whitehead, 2014
 */ 

document.domain = chrome.runtime.id;
var _db = null;
var _connectionStore = null;
var _connections = {};
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
      console.info("Connection Data:[%o]", data);
      initGrid(data);
    },
    function(evt) {
      console.error("Failed to load connections-->[%o]", evt);
    }
  );
}

function handleCellEdit(value, settings) {
    console.info("Handling Edit:  value:%o,  settings:%o, this:%o", value, settings, this);
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
  cTable = $('#connectionsGrid').dataTable({
    "bJQueryUI": true,
		"fnRowCallback": function( nRow, aData, iDisplayIndex, iDisplayIndexFull ) {
			console.info("fnRowCallback: nRow:%o, aData:%o, iDisplayIndex:%o, iDisplayIndexFull:%o", nRow, aData, iDisplayIndex, iDisplayIndexFull);
			nRow.id = 'crow_' + aData[0];
			
				//.attribute('id', 'crow_' + aData[0]);
    }    
  });
  console.info("Editable Configured");
  $.each(data, function(index, item) {
    // <!-- {name: 'Default', url: 'ws://localhost:4243/ws', type: 'websocket', permission: false}, -->
    $('#connectionsGrid').dataTable().fnAddData( [
      item.name, item.auto, item.url, item.type, item.permission, item.permission_pattern
    ] ); 
  });
  cTable.$('td').editable( handleCellEdit , {
    "callback": function( sValue, y ) {
	    var aPos = cTable.fnGetPosition( this );
	    cTable.fnUpdate( sValue, aPos[0], aPos[1] );
    },
    "height": "14px",
    "width": "100%"
  } );  
  
}

$(document).ready(function() { 
 
    loadConnections();
});




