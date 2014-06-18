/**
 * Console OpenTSDB Custom Dashboard Editor
 * Whitehead, 2014
 */ 

document.domain = chrome.runtime.id;
$(document).ready(function() { 
	$('#tabs').append($('#dashboardtoolbar'));
  	$('#nativechart-btn').button({icons: {primary: 'ui-icon-tsd'}})
  		.click(function(e){
    		console.info("Adding Native Chart");
    		pickSnapshot();
    	});


    $( "#dashboardtoolbar" ).dialog({}).draggable().resizable();
  	

	console.info("Dashboard Editor Loaded");
});



function pickSnapshot() {
  var dirs = [];
  var iterationPromise  = $.indexedDB("OpenTSDB").objectStore("directories").each(function(item){
    dirs.push(item.value.name);
  });
  iterationPromise.done(function(result, event){
    if(result==null) {
      console.info("Retrieved Directories: [%O]", dirs);
    }
  });	
  var dlg = $( "#dialog_pickSnapshot" ).toggle().dialog({ 
    width: 500, 
    height: 200,
    modal: true,    
    closeOnEscape: true, 
    buttons: {
      Go : function() {
        $('#dialog_saveSnapshotErr').remove();
        var self = this;
      },
      Cancel: function() {
          $('#dialog_saveSnapshotErr').remove();
          $( this ).dialog( "close" );
      }
    }
  });	
  $('#title').combobox([]);
  $('#category').combobox(dirs).change(function() {
  	var cat = $('#category').val();
  	var titles = [];
	var titlesPromise  = $.indexedDB("OpenTSDB").objectStore("snapshots").each(function(item){
		if(cat==item.value.category) {
			titles.push(item.value.title);
		}
  	});
  	titlesPromise.done(function(result, event){
    	if(result==null) {
      		console.info("Retrieved Titles: [%O]", titles);
      		$.each(titles, function(index, item){
      			$('#title').append(new Option(item, item));
      		}) ;      		
    	}
  	});	  	
  });
}
