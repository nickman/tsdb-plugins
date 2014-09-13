//========================================================================
//  MetaMetrics Local Relay 
//  Whitehead (nwhitehead AT heliosdev DOT org
//  2014
//========================================================================
// ==============================
// Set up dependencies
// ==============================

requirejs.config({
    baseUrl: 'lib',
    paths: {
        scripts: '../scripts'
    }
});

requirejs(['bacon'],
function () {
  // The mm core namespace
  var $mm = $mm || {};
  var relay = $mm.relay || {};
  relay.local = function(props) {
    props = props || {};
    
  }
  
});