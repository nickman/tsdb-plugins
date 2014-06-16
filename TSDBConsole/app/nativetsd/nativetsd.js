/**
 * Console OpenTSDB Native Console WebView Loader
 * Whitehead, 2014
 */ 

document.domain = chrome.runtime.id;
var webView = null;

$(document).ready(function() { 
  
  $('#snap-btn').button({icons: {primary: 'ui-snap-tsd'}}).css({ width: '30%'})
  .click(function(e){
    console.info('Taking Snap...');    
    getSnapshotUrl().then(
      function(snap) {
        console.info("Snapshot [%s]", snap); 
        saveSnapshot(snap);
      }
    );
  });
  $('#snap-btn').toggle();

  console.info("Popping Dialog")
  $( "#dialog_openNativeConsole" ).dialog({ 
    width: 600, 
    modal: true,    
    closeOnEscape: true, 
    buttons: {
      Load : function() {
        var url = $('#tsdurl').val();
        $( this ).dialog( "close" );        
        loadWebView(url);

      },
      Cancel: function() {
          $( this ).dialog( "close" );
      }
    }
  });
});

function getSnapshotUrl() {
  var d = $.Deferred();
  webView.executeScript({code: "$('img.gwt-Image')[0].src"}, function(r) {     
    d.resolve(r[0]);
  });
  return d.promise();
}


function loadWebView(url) {
  console.info("Started WebView Window");
  window.addEventListener('message', function(e) {  
    console.info("MESSAGE: %O", e);
  });
  //$('body').append($('<webview id="foo" src="http://localhost:4242/#start=5m-ago&amp;m=sum:sys.cpu%7Bcpu=*,type=combined%7D&amp;o=&amp;yrange=%5B0:%5D&amp;key=out%20right%20top%20box&amp;wxh=1500x300&amp;autoreload=15" style="width:100%; height:100%" name="OpenTSDBWindow"></webview>'))
  $('body').append($('<webview id="foo" src="' + url + '" style="width:100%; height:100%" name="OpenTSDBWindow"></webview>'))
    webView = document.getElementById("foo");
    console.info("WebView: %O", webView);
    var indicator = document.querySelector(".indicator");
      
    var loadstart = function() {
      console.info("Loading WebView Window");
    }
    var loadstop = function() {
      console.info("Loaded WebView Window");
      
      webView.executeScript({file: "/js/jquery/jquery-2.0.3.js"}, function(r) {                   
          $('#snap-btn').toggle();        
      });
      //webView.executeScript({ file: "injects.js" }, function(results) { 
      webView.executeScript({ code: "window.addEventListener('message', function(e) { e.source.postMessage('message', '*'); } ); " }, function(results) { 
        console.info("Inject Results: %O", results); 
        webView.contentWindow.postMessage({msg:'Hello'}, '*');
        //webView.executeScript({ code: "GETIMGSRC()" }, function(results) { console.info("Invoke Results: %O", results); });
      });
    }
    var contentload = function() {
      console.info("ContentLoadeded in WebView Window");
    }

    var loadcommit = function() {
      console.info("LoadCommit in WebView Window");

    }




    var onMessage = function(msg) {
      console.info("MESSAGE: %O", msg);
    }

    webView.addEventListener("loadstart", loadstart);
    webView.addEventListener("loadstop", loadstop);    
    webView.addEventListener("contentload", contentload);    
    webView.addEventListener("loadcommit", loadcommit);    
    webView.addEventListener("message", onMessage);    
    webView.addEventListener("consolemessage", onMessage);    


}


function saveSnapshot(tsdurl) {
  var dlg = $( "#dialog_saveSnapshot" ).dialog({ 
    width: 900, 
    modal: true,    
    closeOnEscape: true, 
    buttons: {
      Load : function() {
        
      },
      Cancel: function() {
          $( this ).dialog( "close" );
      }
    }
  });
  console.info("Save Dialog: %O", dlg);
  $('#snapshot').val(tsdurl);

}

/*



  <div id="dialog_saveSnapshot" title="OpenTSDB Console: Save Chart Snapshot" >  
    <form>
      <fieldset>
        <label for="category">Category:</label>
        <input type="text" name="category" id="category" class="text ui-widget-content ui-corner-all" value="" style="width: 65%">
        <label for="snapshot">Snapshot:</label>
        <input type="text" name="snapshot" id="snapshot" class="text ui-widget-content ui-corner-all" value="" style="width: 95%">
      </fieldset>
    </form>  
      <div id="tabs" >
        <ul>
            <li><a id="savesnapshot-btn">Save</a></li>
            <li><a id="cancelsnapshot-btn">Cancel</a></li>
        </ul>
        </div>    
  </div>




function onWebViewLoaded() {
  console.dir(arguments);
}

function loadWebView(url) {
  var fqUrl = "/app/nativetsd/tsdwindow.html";
  console.info("Loading OpenTSDB Native Console at [%s] in window [%s]", url, fqUrl);
  chrome.app.window.create(fqUrl, {
  'id' : 'tsdWindow',
  'state' : 'normal',
    'bounds' : {
      'width': Math.round(window.screen.availWidth*0.8),
      'height': Math.round(window.screen.availHeight*0.8),
      'left' : Math.round((window.screen.availWidth-(window.screen.availWidth*0.8))/2),
      'top' : Math.round((window.screen.availHeight-(window.screen.availHeight*0.8))/2)
    }
  });
}
*/



