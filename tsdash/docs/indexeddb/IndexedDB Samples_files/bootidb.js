// Include this script to bootstrap Microsoft HTLM5 IndexedDB - Experimental Release
function AddTestOnlyWarning() {
    var message = '<div style="height: 39px; background-color:Red; color:White; "><p style="font-size:18px; top: 20px;">'+
                  '<b>This page is using MS IndexedDB prototype code. The code is for testing-purposes only!</b></p>'+
                  '</div>';

    var bodyEl = document.getElementsByTagName("body");
    if (bodyEl.length == 0) {
        var newEl = document.createElement("body");
    } else {
        var newEl = document.createElement("div");

    }
    newEl.innerHTML = message;
    if (bodyEl.length == 0)
	    document.getElementsByTagName("html")[0].appendChild(newEl);
	else
		bodyEl[0].insertBefore(newEl,bodyEl[0].childNodes.item(0));

}

if (!window.indexedDB) {
    window.indexedDB = new ActiveXObject("SQLCE.Factory.4.0");
    window.indexedDBSync = new ActiveXObject("SQLCE.FactorySync.4.0");

    if (window.JSON) {
        window.indexedDB.json = window.JSON;
        window.indexedDBSync.json = window.JSON;
    } else {
            var jsonObject = {
            parse: function(txt) {
                if (txt === "[]") return [];
                if (txt === "{}") return {};
                throw { message: "Unrecognized JSON to parse: " + txt };
            }
        };
        window.indexedDB.json = jsonObject;
        window.indexedDBSync.json = jsonObject;
    
    }
    
    // Add some interface-level constants and methods.
    window.IDBDatabaseException = {
        UNKNOWN_ERR : 0,
        NON_TRANSIENT_ERR : 1,
        NOT_FOUND_ERR : 2,
        CONSTRAINT_ERR : 3,
        DATA_ERR : 4,
        NOT_ALLOWED_ERR : 5,
        SERIAL_ERR : 11,
        RECOVERABLE_ERR : 21,
        TRANSIENT_ERR : 31,
        TIMEOUT_ERR : 32,
        DEADLOCK_ERR : 33
    };

    window.IDBKeyRange = {
        SINGLE: 0,
        LEFT_OPEN : 1,
        RIGHT_OPEN : 2,
        LEFT_BOUND : 4,
        RIGHT_BOUND : 8
    };

    window.IDBRequest = {
        INITIAL: 0,
        LOADING: 1,
        DONE: 2
    }

    window.IDBKeyRange.only = function (value) {
        return window.indexedDB.range.only(value);
    };

    window.IDBKeyRange.leftBound = function (bound, open) {
        return window.indexedDB.range.leftBound(bound, open);
    };

    window.IDBKeyRange.rightBound = function (bound, open) {
        return window.indexedDB.range.rightBound(bound, open);
    };

    window.IDBKeyRange.bound = function (left, right, openLeft, openRight) {
        return window.indexedDB.range.bound(left, right, openLeft, openRight);
    };
}
