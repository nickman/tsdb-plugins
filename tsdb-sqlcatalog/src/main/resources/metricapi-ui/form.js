$(document).ready(function() {

	// To turn on debugging, uncomment this line
	// Alpaca.logLevel = Alpaca.DEBUG;
	
    var data = {
    };

    var schema = {
        "type": "object",
        "properties": {
            "name": {
                "type": "string"
            },
            "age": {
                "type": "number",
                "minimum": 0,
                "maximum": 50
            },
            "phone": {
                "type": "string"
            },
            "country": {
                "type": "string",
                "required": true
            }
        }
    };

    var options = {
        "fields": {
            "name": {
                "type": "text",
                "label": "Name"
            },
            "age": {
                "type": "number",
                "label": "Age"
            },
            "phone": {
                "type": "phone",
                "label": "Phone"
            },
            "country": {
                "type": "country",
                "label": "Country"
            }
        }
    };

    var postRenderCallback = function(control) {

    };

    $("#form").alpaca({
        "data": data,
        "schema": schema,
        "options": options,
        "postRender": postRenderCallback,
        "view": "VIEW_WEB_EDIT"
    });
});