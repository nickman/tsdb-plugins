var cstatus_icons = [
  "/img/red-light-16X16.png",
  "/img/amber-light-16X16.png",
  "/img/green-light-16X16.png"
];

function loadApp(appName) {
  var appId = appName + "_app";
  var uri = '/app/' + appName + '/' + appName + '.html';
  console.info('Loading App [%s]', uri);
  
  $('#desktop_content').empty().append($('<div id="foo"><iframe id="' + appId + '" src="' + uri + '"></iframe><div>'));
  $('#' + appId).toggle();
  $('#desktop_content').append($('#' + appId).html());
  $('#desktop_content').remove('#' + appId);
  
  
  
}

function init() {
    var appName = "connections";
    var appId = appName + "_app";
    var uri = '/app/' + appName + '/' + appName + '.html';
    var curi = chrome.runtime.getURL(uri);
    console.info('Loading App [%s]', uri);
  
    $('#desktop_content').empty().append($('<div id="foo"><iframe id="' + appId + '" src="' + curi + '"></iframe><div>'));
    var ht = $($('#' + appId)[0].ownerDocument).children()[0];
    $('#desktop_content').empty();
    $('#desktop_content').append($(ht).children("body").children());
    console.dir(ht);
}

function initialize() {
  // ====================================
  //	Buttons
  // ====================================
  $('#connections-btn').button({})
  .click(function(e){
    loadApp('connections');
  });
  $('#status-btn').button({ disabled: true })
  .click(function(e){
    console.info("Status:%o", e);
  });
  
  // ====================================
  // Connect button
  $('#serverUrlBtn').button({
    icons: {
      primary: "ui-icon-gear",
      secondary: "ui-icon-triangle-1-s"
    },text: false});
}

$(document).ready(function() { 
    console.info("App Loaded");
    initialize();
});




