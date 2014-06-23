/**
 * Console OpenTSDB Custom Dashboard Editor Sandbox
 * Whitehead, 2014
 */ 

var widgetDefinitions = [];

$(document).ready(function() {
	console.info("[Dashboard-Sandbox] Loading Sanboxed Dashboard.....");
	window.addEventListener("message", route);
	console.info("[Dashboard-Sandbox] Window Name: [" + window.name + "], Document Domain: [" + document.domain + "]");
    $("#dashplate").sDashboard({
        dashboardData : widgetDefinitions       
    });  	
	console.info("[Dashboard-Sandbox] Loaded");


});




function route(e) {
	console.info("[Dashboard-Sandbox] Processing Message from origin: [" + e.origin + "]");
	console.info("[Dashboard-Sandbox] Data:" + JSON.stringify(e.data));
	if(e.data==null || e.data.type==null) {
		unhandled(event, "Null data or type");
		return;
	}
	if(e.data=="ping") {
		ping(e);
		return;
	}
	switch(e.data.type) {
		case "ping":
			ping(e);
			break;
		case "newtile":
			onNewTileRequest(e);
			break;
		default:
			unhandled(e, "Unrecognized Type [" + e.data + "]");
	}
}

function ping(event) {
	event.source.postMessage({name: n, type: "Pong", rt: e.origin}, e.origin)	
}

function unhandled(event, message) {
	console.error("Unhandled Event: " + message)
}

function onNewTileRequest(e) {

}

function go(x, y) {

}

/*
  var x = { 
        widgetTitle : "System CPU Summary", //Title of the widget
        widgetId: "id008", //unique id for the widget
        imgUrl: "http://localhost:8080/q?start=5m-ago&ignore=2550&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&wxh=500x300&png" //content for the widget

    }
*/
// var imgUrl = "http://localhost:8080/q?start=5m-ago&ignore=2550&m=sum:sys.cpu%7Bcpu=*,type=combined%7D&o=&yrange=%5B0:%5D&wxh=380x160&png";
// 385 X 185
// try { $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<img id='id008img' src='" + imgUrl + "'>"}); } catch (e) {console.error("%O", e);}

