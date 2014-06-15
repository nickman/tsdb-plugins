document.domain = chrome.runtime.id;
$(document).ready(function() { 
	console.info("Started WebView Window");
	$('body').append($('<webview id="foo" src="http://localhost:4242/#start=5m-ago&amp;m=sum:sys.cpu%7Bcpu=*,type=combined%7D&amp;o=&amp;yrange=%5B0:%5D&amp;key=out%20right%20top%20box&amp;wxh=1500x300&amp;autoreload=15" style="width:100%; height:100%" name="OpenTSDBWindow"></webview>'))
  	var webview = document.getElementById("foo");
  	console.info("WebView: %O", webview);
  	var indicator = document.querySelector(".indicator");
      
    var loadstart = function() {
      console.info("Loading WebView Window");
    }
    var loadstop = function() {
      console.info("Loaded WebView Window");
    }
    webview.addEventListener("loadstart", loadstart);
    webview.addEventListener("loadstop", loadstop);  
});