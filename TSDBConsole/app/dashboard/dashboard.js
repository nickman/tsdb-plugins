/**
 * Console OpenTSDB Custom Dashboard Editor
 * Whitehead, 2014
 */ 

var dlg = null;

var dlgTitle = 'OpenTSDB Console: Select Chart Snapshot';
var comboCategories = null;
var comboTitles = null;

var dlgContent = "<div id='dialog_pickSnapshot' style='display: none;'>  \
    <form> \
      <fieldset id='snapshotFieldset'> \
        <label for='category'>Category:</label> \
        <input type='text' name='category' id='category' class='text ui-widget-content ui-corner-all' value='' style='width: 85%'> \
        <br> \
        <label for='title'>Title:</label> \
        <input type='text' name='title' id='title' class='text ui-widget-content ui-corner-all' value='' style='width: 85%'>  \
      </fieldset> \
    </form></div>";


document.domain = chrome.runtime.id;
$(document).ready(function() { 
  $('#dashboardcontainer').empty().append($('<iframe id="sandboxDashboard" src="' + chrome.runtime.getURL("/app/dashboard/sandbox-dashboard.html") + '"></iframe>')
    .css('display', 'block')
    .css('width', '100%')
    .css('height', '100%')); //.width($('#desktop_content').width()));
  registerImageHandler();
  makeDialog();
	$('#tabs').append($('#dashboardtoolbar'));
  	$('#nativechart-btn').button({icons: {primary: 'ui-icon-tsd'}})
  		.click(function(e){
    		console.info("Adding Native Chart");
    		pickSnapshot();
    	});


    $( "#dashboardtoolbar" ).dialog({

    }).draggable().resizable();
});

function makeDialog() {
  if(dlg==null) {
    dlg = $(dlgContent).dialog({
      title: dlgTitle,
      autoOpen: false,
      closeOnEscape: true,
      modal: true,
      width: 600,
      height: 150,
      position: ['center', 'center'],
      create: function(event, ui) {
        var cIndex = jQuery.combobox.instances==null ? 0 : jQuery.combobox.instances.length;
        $('#title').width(400).combobox([]);
        $('#category').width(400).combobox([]);
        $('#category').on("change", function(evt) {          
          var cat = $('#category').val();
          console.info("CAT SELECT: [%s] evt: [%O]", cat, evt);
          if(cat!=null && cat.length > 0) {
            popTitles(cat);
          }
        });
        comboTitles = jQuery.combobox.instances[cIndex];
        comboCategories = jQuery.combobox.instances[cIndex+1];
        $('#dialog_pickSnapshot a.combobox_button').css('height', '20').css('width', '20');
      },
      open:  function(event, ui) {
        clearBoth();
        popDirectories();
      },
      buttons: {
        Go : function() {
          $('#dialog_saveSnapshotErr').remove();
          addNativeChart($('#category').val(), $('#title').val());
          $( this ).dialog( "close" );
        },
        Cancel: function() {
            $('#dialog_saveSnapshotErr').remove();
            $( this ).dialog( "close" );
        }
      }      
    });
  }
}

function addNativeChart(directory, title) {
  console.info("Adding Native Chart [%s / %s]", directory, title);
}


function clearBoth() {
  clearDirs();
  clearTitles();  
}

function clearDirs() {
  $('#category').val("");
  comboCategories.setSelectOptions([]);
}

function clearTitles() {
  $('#title').val("");
  comboTitles.setSelectOptions([]);
}

function pickSnapshot() {
  dlg.dialog("open");

}

function popDirectories() {
  var dirs = [];
  var iterationPromise  = $.indexedDB("OpenTSDB").objectStore("directories").each(function(item){
    dirs.push(item.value.name);
  });
  iterationPromise.done(function(result, event){
    if(result==null) {
      console.info("Retrieved Directories: [%O]", dirs);
      comboCategories.setSelectOptions(dirs);
      if(dirs.length>0) {
        $('#category').val(dirs[0]);
        popTitles(dirs[0]);
      }
    }
  }); 
}

function popTitles(directory) {
  var titles = [];
  $('#title').val('');
  var iterationPromise  = $.indexedDB("OpenTSDB").objectStore("snapshots").each(function(item){
    if(item.value.category==directory) {
      titles.push(item.value.title);
    }
  });
  iterationPromise.done(function(result, event){
    if(result==null) {
      console.info("Retrieved Titles: [%O]", titles);
      comboTitles.setSelectOptions(titles);
      if(titles.length>0) {
        $('#title').val(titles[0]);
      }
    }
  }); 
}

function registerImageHandler() {
  $('img').livequery(function(){
    $(this).load(function(){
      console.info("Replacing img source: [%O]", this);
    })    
  });
}


/*
    var x = { 
        widgetTitle : "System CPU Summary", //Title of the widget
        widgetId: "id008", //unique id for the widget
        imgUrl: "http://localhost:8080/q?start=5m-ago&ignore=2550&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&wxh=500x300&png" //content for the widget

    }
        var handle = null;
        function go() {
          $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<div id='id008img'>"});
          var remoteImage = new RAL.RemoteImage({ src: "http://opentsdb:8080/q?start=5m-ago&ignore=3380&m=sum:sys.cpu%7Bcpu=*,type=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&nokey&wxh=500x300&png", width: 377, height: 190});        
          var container = document.querySelector('#id008img');        
          container.appendChild(remoteImage.element);
          //RAL.Queue.add(remoteImage, false);
          handle = RAL.Queue.start();
        }

        function go() {
          $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<img id='id008img' src='" + x.imgUrl+ "'>"});
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
