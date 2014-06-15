var cstatus_icons = [
  "/img/red-light-16X16.png",
  "/img/amber-light-16X16.png",
  "/img/green-light-16X16.png"
];

document.domain = chrome.runtime.id;

function loadApp(appName) {
  var appId = appName + "_app";
  var uri = '/app/' + appName + '/' + appName + '.html';
  console.info('Loading App [%s]', uri);
  
  $('#desktop_content').empty().append($('<iframe id="' + appId + '" src="' + uri + '"></iframe>').css('display', 'block').css('width', '80%').css('height', '80%')); //.width($('#desktop_content').width()));
  
}

function loadWebView(appName) {    
  console.info('Loading WebView [%s]', appName);

}


function initialize() {
  // ====================================
  //	Open DB
  // ====================================
  opendb();
  // ====================================
  //	Buttons
  // ====================================
  $('#nativetsd-btn').button({icons: {primary: 'ui-icon-tsd'}}).css({ width: '100%'})
  .click(function(e){
    console.info('Loading WebView [%s]', 'nativetsd');
    loadApp('nativetsd');
  });
  $('#status-btn').button({ disabled: true }).css({ width: '100%'})
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
  
$('#heliosimg').click(function() { chrome.permissions.request({origins: ["*://localhost/ws"]}, function(a) { console.info("Granted:%o", a); }); });  
}

$(document).ready(function() { 
    console.info("App Loaded");
    initialize();
});




