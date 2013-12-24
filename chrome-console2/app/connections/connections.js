/**
 * Console OpenTSDB Server Connection Manager
 * Whitehead, 2014
 */ 

//document.domain = chrome.runtime.id;
var _db = null;
var _connectionStore = null;
var _connections = {};
var _dirty = {};
var cTable = null;

var columnAttributes = [      
    {id:'id', classes: "visible readonly"},
    {id:'name', classes: "visible editable", attr:{itype:'textarea'}},
    {id:'auto', classes: "visible editable", attr:{itype:'checkbox'}},
    {id:'url', classes: "visible editable", attr:{itype:'textarea'}},
    {id:'type', classes: "visible editable", attr:{itype:'select'}, select:{'tcp':'TCP', 'websocket':'WebSocket', 'http':'HTTP'}},  // add: 'selected':<value>
    {id:'permission', classes: "visible readonly"},
    {id:'permission_pattern', classes: "visible readonly"},
    {id:'actions', classes: "visible readonly"}
];

var oaAttrs = [];


function loadConnections() {
  sendRequest({port:'db', name:'allData', 'args': ['connections']}).then(
    function(data) {
      var rows = [];
      $.each(data, function(index, item) {
        rows.push([item.id, item.name, item.auto, item.url, item.type, item.permission, item.permission_pattern, '']);    
        item.dirty = false;
      });
      _connections = data;
      console.info("Connection Data:[%o]", data);
      initGrid(rows);
    },
    function(evt) {
      console.error("Failed to load connections-->[%o]", evt);
    }    
  );
  
}

function handleCellEdit(value, settings) {
    //console.info("Handling Edit:  value:%o,  settings:%o, this:%o", value, settings, this);
    console.info("Handling Edit:  this:%o, id:%s, newvalue:%s", this, this.id, value);
    return validateEdit(this, value);
}

function validateEdit(src, value) {
    var val = null;
    switch($(src).attr('name')) {
      case 'name':
	val = value.trim();
	if(val.length<1) throw "Invalid Name. Zero length";
	return val;
      case 'auto':
	if(value==true || value==false || value=='true' || value=='false') {
	  if(value=='true') return true;
	  else if(value=='false') return false;
	  else return value;
	} else {
	    throw "Invalid Auto. Not a boolean";
	}
      default :
	return value;

    }
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
  
  
  
  //  $('td:eq(4)', nRow).html( '<b>A</b>' );
  
  cTable = $('#connectionsGrid').dataTable({"bJQueryUI": true, "aoColumns": oaAttrs,    
    "fnRowCallback": function( nRow, aData, iDisplayIndex, iDisplayIndexFull) {
	//console.info("fnRowCallback: nRow:%o, aData:%o, iDisplayIndex:%o, iDisplayIndexFull:%o", nRow, aData, iDisplayIndex, iDisplayIndexFull);
	var id = aData[0];
	var actionIndex = aData.length-1;
	var actionSelector = 'td:eq(' + actionIndex + ')';
	var rowId = 'crow_' + aData[0];
	nRow.id = rowId;
	//console.group("Cells for row %s", iDisplayIndex);
	$.each(nRow.childNodes, function(index, item) {
	  cAttrs = columnAttributes[index];
	  var cellSelector = 'td:eq(' + index + ')';				
	  var rowColId = rowId + "_" + (index);
	  $(cellSelector, nRow).css({layout: 'inline', 'vertical-allignment':'middle'});
	  //console.info("Cell Decorator:  rowColId:[%s], arr:[%s], name:[%s], classes:[%s]", rowColId, index, cAttrs.id, cAttrs.classes);
	  $(cellSelector, nRow).attr('id', rowColId).attr('arr', index).attr('name', cAttrs.id).addClass(cAttrs.classes);
	  $(actionSelector, nRow).empty().append($('<span><span>')
	    .append($('<span>Delete</span>').attr('id', rowColId + '_del_btn').button({icons: {primary:'ui-icon-close',text: true}}).css({ 'font-size': '0.9em', float : 'left', padding : '.4em .4em .4em 2em;'}))
	    .append($('<span>Connect</span>').attr('id', rowColId + '_con_btn').button({icons: {primary:'ui-icon-folder-open',text: true}}).css({ 'font-size': '0.9em', float : 'left', padding : '.4em .4em .4em 2em;'}))
    )
	});

    }
  });
  $('#connectionsGrid').dataTable().fnAddData(data);
  $('#connectionsGrid td').css({padding: '0px 0px'});
  
  
  cTable.$('td.editable').editable( handleCellEdit , {    
    "callback": function( sValue, y ) {
	    var aPos = cTable.fnGetPosition( this );
	    cTable.fnUpdate( sValue, aPos[0], aPos[1] );
    },
    "onedit" : function(a, b) {
      console.info("edit-onedit: revert value: [%s]", b.revert);	
      return true;
    },
    "onsubmit" : function(a,b) {
	     console.info("edit-submit: Validating edit from [%s] to [%s]", b.revert, b.firstChild[0].value);	
    },
    "onerror": function (settings, original, xhr) {
        original.reset();
    },
    "height": "10px",
    "width": "100%"
  } );
    
}

$(document).ready(function() { 
  $.each(columnAttributes, function(index, item) {
    var attrs = {bAutoWidth: true};
    attrs.bVisible = (item.classes.indexOf('invisible')==-1);
    oaAttrs.push(attrs);
  });

  loadConnections();
});




