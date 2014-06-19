/**
 * Console OpenTSDB Custom Dashboard Editor
 * Whitehead, 2014
 */ 

var dirs = [];
var titles = [];
var dlg = null;

var widgetDefinitions =[]
/*
    { 
        widgetTitle : "System CPU Summary", //Title of the widget
        widgetId: "id008", //unique id for the widget
        widgetContent: "<img src='http://opentsdb:8080/q?start=5m-ago&ignore=2550&m=sum:sys.cpu%7Bcpu=*,type=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&wxh=1900x300&png'>" //content for the widget


        var handle = null;
        function go() {
          $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<div id='id008img'>"});
          var remoteImage = new RAL.RemoteImage({ src: "http://opentsdb:8080/q?start=5m-ago&ignore=3380&m=sum:sys.cpu%7Bcpu=*,type=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&nokey&wxh=500x300&png", width: 377, height: 190});        
          var container = document.querySelector('#id008img');        
          container.appendChild(remoteImage.element);
          //RAL.Queue.add(remoteImage, false);
          handle = RAL.Queue.start();
        }

    
    function blob() {
      $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<img id='id008img' src='/img/loading.gif'>"});
      var url = "http://localhost:8080/q?start=5m-ago&ignore=9&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&nokey&wxh=377x180&png";
      var xhr = new XMLHttpRequest();

      xhr.onerror = function() { console.error(arguments); },
      xhr.onload = function(data) { 
        var blob = data.currentTarget.response;
        console.info("Blob Retrieved: [%O]", blob);         
        var img = $('#id008img')[0];
        window.URL.revokeObjectURL(img.src);
        img.src = window.URL.createObjectURL(blob);
      },
      xhr.open('GET', url, true);
      xhr.responseType = "blob";
      xhr.send();

    }

-webkit-transform: scale3d(1,1,1);

chrome.alarms.create("alarm1", {name: "alarm1", scheduledTime: (Date.now() + 10000)});

function handle(msg, sender, sendFunction) {
  
}

chrome.runtime.onMessage.addListener(

//var remoteImage = new RAL.RemoteImage("http://opentsdb:8080/q?start=5m-ago&ignore=3380&m=sum:sys.cpu%7Bcpu=*,type=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&nokey&wxh=500x300&png");        

    }
]
*/

document.domain = chrome.runtime.id;
$(document).ready(function() { 
  RAL.debug = true;
  RAL.Queue.setMaxConnections(1);
  //RAL.Queue.start();  
  $('#title').combobox(titles);
  $('#category').combobox(dirs);
	$('#tabs').append($('#dashboardtoolbar'));
  	$('#nativechart-btn').button({icons: {primary: 'ui-icon-tsd'}})
  		.click(function(e){
    		console.info("Adding Native Chart");
    		pickSnapshot();
    	});


    $( "#dashboardtoolbar" ).dialog({}).draggable().resizable();
    $("#dashplate").sDashboard({
        dashboardData : widgetDefinitions       
    });  	

	console.info("Dashboard Editor Loaded");
});



function clearDirs() {
  while(dirs.length > 0) {
    dirs.pop();
  }  
}
function clearTitles() {
  while(titles.length > 0) {
    titles.pop();
  }  
}


function pickSnapshot() {
  var iterationPromise  = $.indexedDB("OpenTSDB").objectStore("directories").each(function(item){
    dirs.push(item.value.name);
  });
  iterationPromise.done(function(result, event){
    if(result==null) {
      console.info("Retrieved Directories: [%O]", dirs);
    }
  });	
  dlg = $( "#dialog_pickSnapshot" ).toggle().dialog({ 
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
    clearTitles();
  	var cat = $('#category').val();
  	
	var titlesPromise  = $.indexedDB("OpenTSDB").objectStore("snapshots").each(function(item){
		if(cat==item.value.category) {
			titles.push(item.value.title);
		}
  	});
  	titlesPromise.done(function(result, event){
    	if(result==null) {
      		console.info("Retrieved Titles: [%O]", titles);
      		$.each(titles, function(index, item){
      			titles.push(item);
      		}) ;      		
    	}
  	});	  	
  });
}
