var cstatus_icons = [
  "/img/red-light-16X16.png",
  "/img/amber-light-16X16.png",
  "/img/green-light-16X16.png"
];


function loadApp(appName) {
  var appId = appName + "_app";
  var uri = '/app/' + appName + '/' + appName + '.html';
  console.info('Loading App [%s]', uri);
  
  $('#desktop_content').empty().append($('<iframe id="' + appId + '" src="' + uri + '"></iframe>').css('display', 'inline').css('width', '80%').css('height', '80%').css('overflow', 'scroll')); //.width($('#desktop_content').width()));
  
}


function initialize() {
  // ====================================
  //	Open DB
  // ====================================
  //window.opentsdb.db.opendb();
  // ====================================
  //	Buttons
  // ====================================
  $('#tabs li a').each(function(){    
    var btn = $(this);
    console.info("Enabling Button: id [%s], app [%s]", btn.attr('id'), btn.attr('app')); 
    btn.button({}).css({ width: '100%'}).click(function(e){
      loadApp(btn.attr('app'));
    });
    if(btn.hasClass('btnDisabled')) {
      btn.toggle();
    }
  });
  /*
  $('#connections-btn').button({}).css({ width: '100%'})
  .click(function(e){
    loadApp('connections');
  });
  $('#isql-btn').button({ disabled: false }).css({ width: '100%'})
  .click(function(e){
    loadApp('isql');
  });

  $('#status-btn').button({ disabled: true }).css({ width: '100%'})
  .click(function(e){
    console.info("Status:%o", e);
  });
  */
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




