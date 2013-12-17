var cstatus_icons = [
  "/img/red-light-16X16.png",
  "/img/amber-light-16X16.png",
  "/img/green-light-16X16.png"
];

function loadApp(appName) {
  
  var uri = '/app/' + appName + '/' + appName + '.html';
  console.info('Loading App [%s]', uri);
  $('#desktop_content').empty().append($('<iframe id="' + appName + '-app" src="' + uri + '"></iframe>'));
  
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




