chrome.app.runtime.onLaunched.addListener(function() { 
  console.info("App Runtime Started");
  window.opentsdb = {};
  chrome.app.window.create('appstart.html', {
    'bounds': {
      'width': Math.round(window.screen.availWidth*0.8),
      'height': Math.round(window.screen.availHeight*0.8)
    }    
  });  
});