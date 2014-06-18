chrome.app.runtime.onLaunched.addListener(function() {
  chrome.app.window.create('main.html', {
	'id' : 'mainWindow',
	'state' : 'normal',
    'bounds' : {
		'width': Math.round(window.screen.availWidth*0.8),
        'height': Math.round(window.screen.availHeight*0.8),
        'left' : Math.round((window.screen.availWidth-(window.screen.availWidth*0.8))/2),
        'top' : Math.round((window.screen.availHeight-(window.screen.availHeight*0.8))/2)
    }
  });
});