var ws = null;
var q = null;

$( document ).ready(function() {
	ws = new WebSocketAPIClient();
	q = QueryContext.newContext({timeout: 10000});
	$("#exprfield").focus();
	$("#exprfield").keypress(function(e) {
	    if(e.which == 13) {
		    goAction();		        
	        return false;
	    }
	});
	$("#exprfield").removeAttr("disabled");	
	console.info("Index.js Loaded");
});

function goAction() {	
	if($('#clearqc').is(':checked')) {
		q = QueryContext.newContext({timeout: 10000});
	} 
	var selected = $('input[name=goaction]:checked', '#goAction').val();
	console.info("GO!  (Action is [%s])", selected);
	if("json"==selected) doDisplayJson();
};


function doDisplayJson() {
	var expr = $('#exprfield').val();
	console.info("Executing fetch for expression [%s]", expr);
	// Retrieve data using expr call.
	// write formatted data to jsonOutput
	ws.resolveTSMetas(q, "sys*:dc=dc1,host=WebServer*").then(
		function(result) {
			$('#jsonOutput').empty();
			updateOutputContext(result.q);
			$('#jsonOutput').append("<pre>" + syntaxHighlight(result.data) + "</pre>");
			//$('#jsonOutput').json2html(convert('json',result.data,'open'),transforms.object);
		}	
	);
};

function updateOutputContext(qctx) {	
	var ctx = QueryContext.readContext(qctx);
	if(ctx.isExpired()|ctx.isExhausted()|ctx.getNextIndex()==null) {
		q = QueryContext.newContext({timeout: 10000});
	}	
	$('#qoutCummulative').val(ctx.getCummulative());
	$('#qoutElapsed').val(ctx.getElapsed());
	$('#qoutNextIndex').val(ctx.getNextIndex());
	$('#qoutExhausted').val(ctx.isExhausted());
	$('#qoutExpired').val(ctx.isExpired());	
};