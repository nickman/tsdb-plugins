	function toggleLiveResizing () {
		$.each( $.layout.config.borderPanes, function (i, pane) {
			var o = myLayout.options[ pane ];
			o.livePaneResizing = !o.livePaneResizing;
		});
	};
	
	function toggleStateManagement ( skipAlert, mode ) {
		if (!$.layout.plugins.stateManagement) return;

		var options	= myLayout.options.stateManagement
		,	enabled	= options.enabled // current setting
		;
		if ($.type( mode ) === "boolean") {
			if (enabled === mode) return; // already correct
			enabled	= options.enabled = mode
		}
		else
			enabled	= options.enabled = !enabled; // toggle option

		if (!enabled) { // if disabling state management...
			myLayout.deleteCookie(); // ...clear cookie so will NOT be found on next refresh
			if (!skipAlert)
				alert( 'This layout will reload as the options specify \nwhen the page is refreshed.' );
		}
		else if (!skipAlert)
			alert( 'This layout will save & restore its last state \nwhen the page is refreshed.' );

		// update text on button
		try {
			var $Btn = $('#btnToggleState');
			var text = $Btn.html()|"";
			if (enabled)
				$Btn.html( text.replace(/Enable/i, "Disable") );
			else
				$Btn.html( text.replace(/Disable/i, "Enable") );
		} catch (e) {}
	};

	// set EVERY 'state' here so will undo ALL layout changes
	// used by the 'Reset State' button: myLayout.loadState( stateResetSettings )
	var stateResetSettings = {
		north__size:		"auto"
	,	north__initClosed:	false
	,	north__initHidden:	false
	,	south__size:		"auto"
	,	south__initClosed:	false
	,	south__initHidden:	false
	,	west__size:			100
	,	west__initClosed:	false
	,	west__initHidden:	false
	,	east__size:			300
	,	east__initClosed:	true
	,	east__initHidden:	false
	};

	var myLayout;

	$(document).ready(function () {

		// this layout could be created with NO OPTIONS - but showing some here just as a sample...
		// myLayout = $('body').layout(); -- syntax with No Options

		myLayout = $('body').layout({

		//	reference only - these options are NOT required because 'true' is the default
			closable:					true	// pane can open & close
		,	resizable:					true	// when open, pane can be resized 
		,	slidable:					true	// when closed, pane can 'slide' open over other panes - closes on mouse-out
		,	livePaneResizing:			true

		//	some resizing/toggling settings
		,	north__slidable:			false	// OVERRIDE the pane-default of 'slidable=true'
		,	north__togglerLength_closed: '100%'	// toggle-button is full-width of resizer-bar
		,	north__spacing_closed:		20		// big resizer-bar when open (zero height)
		,	south__resizable:			false	// OVERRIDE the pane-default of 'resizable=true'
		,	south__spacing_open:		0		// no resizer-bar when open (zero height)
		,	south__spacing_closed:		20		// big resizer-bar when open (zero height)
		,	east__initClosed:	true
		,	north__size: 		"auto"
		//	some pane-size settings
		,	west__minSize:				100
		,	west__size:					'12%'
		,	east__size:					'15%'
		,	east__minSize:				200
		,	east__maxSize:				.5 // 50% of layout width
		,	center__minWidth:			100

		//	some pane animation settings
		,	west__animatePaneSizing:	false
		,	west__fxSpeed_size:			"fast"	// 'fast' animation when resizing west-pane
		,	west__fxSpeed_open:			1000	// 1-second animation when opening west-pane
		,	west__fxSettings_open:		{ easing: "easeOutBounce" } // 'bounce' effect when opening
		,	west__fxName_close:			"none"	// NO animation when closing west-pane

		//	enable showOverflow on west-pane so CSS popups will overlap north pane
		,	west__showOverflowOnHover:	true

		//	enable state management
		,	stateManagement__enabled:	true // automatic cookie load & save enabled by default

		,	showDebugMessages:			true // log and/or display messages from debugging & testing code
		});

		// if there is no state-cookie, then DISABLE state management initially
		var cookieExists = !$.isEmptyObject( myLayout.readCookie() );
		if (!cookieExists) toggleStateManagement( true, false );

		myLayout
			// add event to the 'Close' button in the East pane dynamically...
			//.bindButton('#btnCloseEast', 'close', 'east')
	
			// add event to the 'Toggle South' buttons in Center AND South panes dynamically...
			//.bindButton('.south-toggler', 'toggle', 'south')
			
			// add MULTIPLE events to the 'Open All Panes' button in the Center pane dynamically...
			//.bindButton('#openAllPanes', 'open', 'north')
			//.bindButton('#openAllPanes', 'open', 'south')
			//.bindButton('#openAllPanes', 'open', 'west')
			//.bindButton('#openAllPanes', 'open', 'east')

			// add MULTIPLE events to the 'Close All Panes' button in the Center pane dynamically...
			//.bindButton('#closeAllPanes', 'close', 'north')
			//.bindButton('#closeAllPanes', 'close', 'south')
			//.bindButton('#closeAllPanes', 'close', 'west')
			//.bindButton('#closeAllPanes', 'close', 'east')

			// add MULTIPLE events to the 'Toggle All Panes' button in the Center pane dynamically...
			//.bindButton('#toggleAllPanes', 'toggle', 'north')
			//.bindButton('#toggleAllPanes', 'toggle', 'south')
			//.bindButton('#toggleAllPanes', 'toggle', 'west')
			//.bindButton('#toggleAllPanes', 'toggle', 'east')
		;

		// 'Reset State' button requires updated functionality in rc29.15+
		//if ($.layout.revision && $.layout.revision >= 0.032915)
			//$('#btnReset').show();

 	});
