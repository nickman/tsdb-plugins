/**
 * Console OpenTSDB Custom Dashboard Editor Sandbox
 * Whitehead, 2014
 */ 

var widgetDefinitions = [];

$(document).ready(function() {
	console.info("[Dashboard-Sandbox] Loading Sanboxed Dashboard.....");

	window.addEventListener("message", route);
	console.info("[Dashboard-Sandbox] Window Name: [" + window.name + "], Document Domain: [" + document.domain + "], Width: [%s], Height: [%s]", $(window).width(), $(window).height());
	$('#dashplate').height($(window).height()).width($(window).width()).sDashboard({
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
	var def = e.data;
	delete def.type;
	def.enableRefresh = true;
	def.ts = 0;
	def.refreshCallBack = function(widgetId) {
		console.info("Refreshing Widget [%s], ts[%s]", widgetId, def.ts);
	}
	$(def.widgetContent).find("img")
	$("#dashplate").sDashboard("addWidget", def); 
	
	$('#id' + def.id)
	var dContent = '#id' + def.data.id + " .sDashboardWidgetContent";
	console.info("Widget Content Selector [%s]", dContent);
	var h = $(dContent).height();
	var w = $(dContent).width();
	var newSize = ("" + w + "x" + h);
	console.info("Replacing [%s] with [%s]", def.data.urlparts.params.wxh, newSize);
	var adjUrl = def.imgUrl.replace(def.data.urlparts.params.wxh, newSize);
	console.info("Adjusted URL: [%s]", adjUrl);
	$('#' + def.imgId).attr('src', adjUrl);	
          //imgUrl: snapshot.snapshot,
          //imgId: ("img" + ("id" + snapshot.id)),


}

function go(x, y) {
	var imgUrl = "http://opentsdb:8080/q?start=5m-ago&ignore=26&m=sum:sys.cpu%7Btype=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&wxh=385x185&png";
	var id = 'id008img';
	try { 
		var def = {
			widgetTitle: "System CPU Summary", 
			widgetId: "id008", 
			enableRefresh: true,
			widgetContent: "<img id='" + id + "' src='" + imgUrl + "'>",
			ts: 0,
			refreshCallBack: function(widgetId) {
				console.info("Refreshing Widget [%s], ts[%s]", widgetId, self.ts);
				this.ts++;
			}
		}
		$("#dashplate").sDashboard("addWidget", def); 
	} catch (e) {
		console.error("%O", e);
	}

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
        imgUrl: "http://opentsdb:8080/#start=5m-ago&m=sum:sys.cpu%7Btype=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&wxh=500x300&autoreload=15" //content for the widget

    }
*/
// var imgUrl = "http://opentsdb:8080/q?start=5m-ago&ignore=26&m=sum:sys.cpu%7Btype=combined,host=PP-WK-NWHI-01%7D&o=&yrange=%5B0:%5D&wxh=500x300&png";
// 385 X 185
// try { $("#dashplate").sDashboard("addWidget", { widgetTitle: "System CPU Summary", widgetId: "id008", widgetContent: "<img id='id008img' src='" + imgUrl + "'>"}); } catch (e) {console.error("%O", e);}

