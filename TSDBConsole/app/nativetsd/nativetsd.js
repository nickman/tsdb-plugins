/**
 * Console OpenTSDB Native Console WebView Loader
 * Whitehead, 2014
 */ 

document.domain = chrome.runtime.id;
var webView = null;

$(document).ready(function() { 
  initDb().then(function(){
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

function initDb() {

  return $.indexedDB("Snapshots", { 
      // The second parameter is optional
      "version" : 2,  // Integer version that the DB should be opened with
      "upgrade" : function(transaction){
        console.info("DB UPGRADE");
      },
      "schema" : {
          "1" : function(transaction){
              console.info("VERSION 1");
              initDirectoriesOS(transaction).then(initSnapshotsOS(transaction))
          },
          "2" : function(transaction){
              console.info("VERSION 2");
          }
      }
  });
}

function initDirectoriesOS(tx) {
  var directoryObjectStore = tx.createObjectStore("directories", {
      "keyPath" : 'name' 
  });    
  var request = directoryObjectStore.add({name: "Default"});
  
  request.onsuccess = function(event){ 
    console.info("----> Inited directories and added Default");
  };
  request.onerror = function(e){
    console.error("Data save failed");
    console.error(e);
    tx.abort();
  };
  return request;
}

// http://james.padolsey.com/javascript/parsing-urls-with-the-dom/

function initSnapshotsOS(tx) {  
  var d = $.Deferred();
  var p = d.promise();
  try {
    tx.createObjectStore("snapshots", {
      "keyPath" : 'fullKey' 
    });    
    console.info("----> Inited snapshots")
    d.resolve();
  } catch (e) {
    d.reject(e);
  }
  return p;
}


  function deleteDb(dbname) {
    var dbRequest = window.indexedDB.deleteDatabase(dbname);
    dbRequest.onsuccess = function(evt){ console.info("Deleted DB:%o", evt); }
    dbRequest.onerror = function(evt){ console.error("Failed to delete db: %o", evt.target.error); }  
  }




/*
    Object Stores
    =============
    directories
      name
 */



function saveSnapshot(tsdurl) {
  var dirs = [];
  var iterationPromise  = $.indexedDB("Snapshots").objectStore("directories").each(function(item){
    dirs.push(item.value.name);
  });
  iterationPromise.done(function(result, event){
    if(result==null) {
      console.info("Retrieved Directories: [%O]", dirs);
    }
  });

  var dlg = $( "#dialog_saveSnapshot" ).dialog({ 
    width: 900, 
    height: 300,
    modal: true,    
    closeOnEscape: true, 
    buttons: {
      Save : function() {
        $('#dialog_saveSnapshotErr').remove();
        try {
          persistSnapshot($('#dialog_saveSnapshot')).then(
            function() {
              // Complete
              $( this ).dialog( "close" );
              
            },
            function(error, event) {
              // Error
            }
          );
        } catch (errors) {

            var msg = "<div id='dialog_saveSnapshotErr'><font color='red'>ERROR:<ul>";
            if($.isArray(errors)) {
              $.each(errors, function(index, m) {
                msg += "<li>" + m + "</li>";
              });
            } else {
              console.error("Save Snapshot Error: %O", errors);
              msg += "<li>" + errors.message + "<ul><li>" + errors.stack + "</li></ul></li>";
            }
            msg += "</ul></font></div>";
            $('#dialog_saveSnapshot').append(msg);   
            msg = null;         
        }
      },
      Cancel: function() {
          $('#dialog_saveSnapshotErr').remove();
          $( this ).dialog( "close" );
      }
    }
  });
  console.info("Save Dialog: %O", dlg);
  $('#snapshot').val(decodeURIComponent(tsdurl));
  $('#category').combobox(dirs);

}



function persistSnapshot() {
  // title, category, snapshot
  var errors = [];
  var title = $('#title').val();  
  if(title==null || title.trim()=="") {
    errors.push("Title was empty");
  }
  var category = $('#category').val();
  if(category==null || category.trim()=="") {
    errors.push("Directory was empty");
  }
  var snapshot = $('#snapshot').val();
  if(snapshot==null || snapshot.trim()=="") {
    errors.push("Snapshot was empty");
  }
  if(errors.length > 0) {
    throw(errors);
  }
  return doPersistCategory(category).then(doPersistSnapshot(category, title, snapshot))
}

function doPersistCategory(category) {
  console.info("Saving Category: [%O]", category);
  var d = $.Deferred();
  var promise = d.promise();
  var objectStore = $.indexedDB("Snapshots").objectStore("directories");
  objectStore.get(category).done(function(x) {
    if(x==null) {
      objectStore.add(category)
        .done(function(){
          d.resolve();
        })
        .fail(function(error, event){
          console.error("Failed to save category: %O", error);
          d.reject(error);
        });      
    } else {
      d.resolve();
    }
  });
  return promise;
}

function doPersistSnapshot(category, title, snapshot) {
  console.info("Saving Snapshot: [%O]", arguments);
  var objectStore = $.indexedDB("Snapshots").objectStore("snapshots");
  objectStore.get(key).done(function(x) {
    if(x==null) {
      var value = {'fullKey': key, 'title': title, 'category': category, 'snapshot': snapshot, 'urlparts' : parseURL(snapshot) };
      objectStore.add(category)
        .done(function(){
          d.resolve();
        })
        .fail(function(error, event){
          console.error("Failed to save snapshot: %O", error);
          d.reject(error);
        });      
    } else {
      d.resolve();
    }
  });
  return promise;
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

// This function creates a new anchor element and uses location
// properties (inherent) to get the desired URL data. Some String
// operations are used (to normalize results across browsers).
// Originally from http://james.padolsey.com/javascript/parsing-urls-with-the-dom/
function parseURL(url) {
    var a =  document.createElement('a');
    a.href = url;
    return {
        source: url,
        protocol: a.protocol.replace(':',''),
        host: a.hostname,
        port: a.port,
        query: a.search,
        params: (function(){
            var ret = {},
                seg = a.search.replace(/^\?/,'').split('&'),
                len = seg.length, i = 0, s;
            for (;i<len;i++) {
                if (!seg[i]) { continue; }
                s = seg[i].split('=');
                ret[s[0]] = s[1];
            }
            return ret;
        })(),
        file: (a.pathname.match(/\/([^\/?#]+)$/i) || [,''])[1],
        hash: a.hash.replace('#',''),
        path: a.pathname.replace(/^([^\/])/,'/$1'),
        relative: (a.href.match(/tps?:\/\/[^\/]+(.+)/) || [,''])[1],
        segments: a.pathname.replace(/^\//,'').split('/')
    };
}

