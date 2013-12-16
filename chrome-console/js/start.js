var cstatus_icons = [
  "/img/red-light-16X16.png",
  "/img/amber-light-16X16.png",
  "/img/green-light-16X16.png"
];

function initialize() {
  // Connect button
  $('#serverUrlBtn').button({
    icons: {
      primary: "ui-icon-gear",
      secondary: "ui-icon-triangle-1-s"
    },text: false});
try {
var g_db = null;
var dbRequest = window.indexedDB.open(“Example”);
dbRequest.onerror = function (evt) {
alert(“Database error: “ + evt.target.error.name);
}
dbRequest.onsuccess = function (evt) {
g_db = evt.target.result; // IDBDatabase object
}
} catch (e) {
  console.error("%s", e);
}  

}

$(document).ready(function() { 
    console.info("App Loaded");
    initialize();
});




