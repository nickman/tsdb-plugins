/*
* Copyright 2009-2012 Webtrends Inc. All Rights Reserved.
* WEBTRENDS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
*
*/

/*
* Uncomment the line below to force SSL communication on all calls to Webtrends 
* optimization and tracking servers.
*/
//z27a3=true;

/* Uncomment the line below to enable the global config file support. 
*  (wtopt.config.js)
*/
//zb410=true;

/* Uncomment the line below to modify the global config file search path. */
//z7bcb=["","/"];

function z0691(){this.zfff3=false;this.z9ea2=false;this.ze3c2=false;this.z2b20=[];this.zf310=[];this.z23eb={};this.zc901=false;this.z907d=null;this.z93cb=null;this.z4e9a=null;this.zefb8=false;this.Debug=null;var z1a23=this;this.z95e8=function(){this.z23eb["s_keyToken"]="7a08ed056240453dfff2658fc5833e520e045f10f5";this.z23eb["s_domainKey"]="628645";this.z23eb["s_capiMajorVersion"]="3";this.z23eb["s_capiMinorVersion"]="2";this.z23eb["s_capiIncVersion"]="2";this.z23eb["s_capiDebug"]=("false"=="true");this.z23eb["s_allowCapiConfig"]=(typeof(zb410)=="undefined"?false:zb410);this.z23eb["s_capiConfigFile"]="wtopt.config.js";this.z23eb["s_capiConfigSearchPath"]=(typeof(z7bcb)=="undefined"?["","/"]:z7bcb);this.z23eb["s_pageProtocol"]=(typeof(z27a3)=="undefined"||!z27a3?(document.location.protocol=="https:"?"https":"http"):"https");this.z23eb["s_otsServer"]="ots.optimize.webtrends.com";this.z23eb["s_otsCDNServer"]="";this.z23eb["s_wasServer"]="was.optimize.webtrends.com";this.z23eb["s_otsWebApp"]="/ots";this.z23eb["s_otsWebAppServlet"]="/ots";this.z23eb["s_pageMode"]="dom";this.z23eb["s_pageDisplayMode"]="visibility";this.z23eb["s_eventHandlers"]={};this.z23eb["s_scriptServer"]=this.z23eb["s_otsCDNServer"]==""?this.z23eb["s_otsServer"]+this.z23eb["s_otsWebApp"]+"/lib/":this.z23eb["s_otsCDNServer"];this.z23eb["s_otsDebuggerLibUrl"]=this.z23eb["s_pageProtocol"]+"://"+this.z23eb["s_scriptServer"]+this.z23eb["s_capiMajorVersion"]+"."+this.z23eb["s_capiMinorVersion"]+"/"+"wt_debugger"+(this.z23eb["s_capiDebug"]?"_debug":"")+".js";var onload=function(){if(z1a23.zefb8){return;}z1a23.zefb8=true;z1a23.z88d3(new WTEvent(WTEvent.z6fd7,WTEvent.STATUS_SUCCESS));};if(window.addEventListener){window.addEventListener('load',function(){window.removeEventListener("load",arguments.callee,false);z1a23.Debug.z5d59("capi: onload fired");onload();},false);}else if(window.attachEvent){window.attachEvent('onload',function(){window.detachEvent("onload",arguments.callee,false);z1a23.Debug.z5d59("capi: onload fired");onload();});}this.Debug=new this.z22f7();this.zd0b7(WTEvent.ABORT,this.z8cdc,this.z8cdc);this.zd0b7(WTEvent.z4519,this.z99db,this.z99db);if(this.z23eb["s_allowCapiConfig"]){zc7ef();}else{this.zc901=true;}};var zc7ef=function(){try{var zbf8e=function(event){var z3883=event.target;if(z3883){for(var z2186 in z3883){z1a23.z23eb[z2186]=z3883[z2186];};}z1a23.z88d3(new WTEvent(WTEvent.z0cf5,WTEvent.STATUS_SUCCESS),true);z1a23.zc901=true;};z1a23.zd0b7(WTEvent.CAPI_CONFIG,zbf8e);for(z2ca0=0;z2ca0<z1a23.z23eb["s_capiConfigSearchPath"].length;z2ca0++){var z7e19={};z7e19["type"]="text/javascript";z7e19["async"]="true";z7e19["defer"]="true";z7e19["src"]=z1a23.z23eb["s_capiConfigSearchPath"][z2ca0]+z1a23.z23eb["s_capiConfigFile"];z1a23.ze82b("script",z7e19,null,document.getElementsByTagName('head')[0]);}}catch(z392c){z1a23.Debug.z1dbb("Unable to load wtop.config file, check error for details.",0,z392c);}};var zbb92=function(){if(navigator.cookieEnabled){return true;}document.cookie="_wt.areCookiesEnabled=true;";var z5273=document.cookie.indexOf("_wt.areCookiesEnabled=true")>=0;document.cookie="_wt.areCookiesEnabled=true; expires="+new Date().toGMTString();return z5273;};this.zcceb=function(zf64e){var zccdd={};if(!(typeof zf64e=="undefined"||zf64e===null)){zccdd=zf64e;}function z0309(z0735,z0a96){if(typeof zccdd[z0735]=="undefined"){zccdd[z0735]=z0a96;}};for(var z2186 in this.z23eb){z0309(z2186,this.z23eb[z2186]);}z0309("s_version","Optimize_v7.11.0.14245-20120807-1521, Protocol: "+zccdd["s_capiMajorVersion"]+"."+zccdd["s_capiMinorVersion"]+"."+zccdd["s_capiIncVersion"]);z0309("s_otsLibUrl",zccdd["s_pageProtocol"]+"://"+this.z23eb["s_scriptServer"]+zccdd["s_capiMajorVersion"]+"."+zccdd["s_capiMinorVersion"]+"/"+"wt_lib"+(zccdd["s_capiDebug"]?"_debug":"")+".js");z0309("s_pageTimeout",4000);z0309("s_conversionTimeout",2000);this.Debug.z5060("start: Webtrends Optimize CAPI Version: "+zccdd["s_version"]);if(!zbb92()){throw "Cookies are not enabled, Webtrends Optimize is disabled.";}var z41a0=zccdd["contentWrapper"];var z9180=null;if(z41a0){z9180=document.getElementById(z41a0);this.Debug.zfda7("capiParams: using wrapper ["+z41a0+"] for test alias ["+zccdd["testAlias"]+"]");}else if(zccdd["testAlias"]){this.Debug.zfda7("capiParams: ["+z41a0+"] using moniker ["+zccdd["testAlias"]+"]");z9180=document.getElementById(zccdd["testAlias"]);}if(document.cookie.indexOf("_wt.debugwindow=")>=0){var zaa91=document.cookie.indexOf("_wt.debugwindow=");var zcd83=document.cookie.split(';');for(var z205e=0;z205e<zcd83.length;z205e++){var z8403=zcd83[z205e];while(z8403.charAt(0)==' ')z8403=z8403.substring(1,z8403.length);if(z8403.indexOf(zaa91)==0){zccdd["debug"].length=z8403.substring(zaa91.length,z8403.length);zccdd["debug"]=true;break;};};}if(zccdd["debug"]&&zccdd["debug"].length>0){this.Debug.z5dbe=false;this.Debug.z6c38=zccdd["debug"].length;this.Debug.refresh(true);}if(!z9180){this.Debug.zfda7("capiParams: using body to prepare run.");z9180=document.body;}z0309("r_cWrapEl",z9180);if(zccdd["s_requestType"]=="conversion"){if(zccdd["r_redirectLink"]){var z9de9=zccdd["r_redirectLink"];setTimeout(function(){if(z9de9.zd96e&&z9de9.zd96e=="true"){z1a23.Debug.z5060("conversion: timed out - redirecting to url ["+z9de9.za3b2+"]");z9de9.href=z9de9.za3b2;z9de9.target=z9de9.z6165;z9de9.zd96e="";if(z9de9.z6165&&z9de9.z6165!=""&&z9de9.z6165!==null){window.open(z9de9.za3b2,z9de9.z6165);}else{window.location.href=z9de9.href;};}},zccdd["s_conversionTimeout"]);};}else{if(!this.z907d){this.z907d=setTimeout(function(){WTOptimize.fireEvent(new WTEvent(WTEvent.z4519,WTEvent.STATUS_FAULT,zccdd));},zccdd["s_pageTimeout"]);this.z93cb=new Date();this.Debug.z5060("timer: started ["+this.z93cb+"].");}else{this.Debug.z5060("timer: already started, using current timer.");}this.zfa9c(z9180,zccdd["s_pageDisplayMode"],false,zccdd["overlayColor"]);}this.z88d3(new WTEvent(WTEvent.PREPARE,WTEvent.STATUS_SUCCESS,zccdd));return zccdd;};this.z3e29=function(zf64e){try{var zccdd=this.zcceb(zf64e);this.zf310.push(zccdd);if(typeof WTOptimize.z32ad=="undefined"){var z51e6=function(event){var zff3a=WTOptimize.zf310.pop();WTOptimize.z88d3(new WTEvent(WTEvent.INITIALIZE,event.z937d,zff3a));WTOptimize.z32ad.z311d(zff3a["testAlias"],zff3a);};var z5f24=function(event){var zff3a=WTOptimize.zf310.pop();WTOptimize.Debug.z1dbb("library: failed to load.");WTOptimize.z88d3(new WTEvent(WTEvent.ABORT,event.z937d,zff3a));};this.zd0b7(WTEvent.zd938,z51e6,z5f24);if(!this.z9ea2){this.z9ea2=true;var z8ce5=function(z392c){try{WTOptimize.Debug.zfda7("library: checking library to see if it has been loaded.");if(z392c){if(z392c.srcElement&&(z392c.srcElement.readyState=="complete"||z392c.srcElement.readyState=="loading")){return;};}if(typeof WTOptimize.z32ad=="undefined"){WTOptimize.z88d3(new WTEvent(WTEvent.zd938,WTEvent.STATUS_FAULT));}}catch(z676d){WTOptimize.Debug.z1dbb("library: failed to verify library",0,z676d);WTOptimize.z88d3(new WTEvent(WTEvent.ABORT,WTEvent.STATUS_FAULT));}};var zbade={};var zfce9={};zbade["type"]="text/javascript";zbade["src"]=zccdd["s_otsLibUrl"];zbade["async"]="true";zbade["defer"]="true";zfce9["onload"]=z8ce5;zfce9["error"]=z8ce5;zfce9["onreadystatechange"]=z8ce5;this.Debug.zfda7("initialize: Loading Server Library ["+zbade["src"]+"]");setTimeout(function(){z1a23.ze82b("script",zbade,zfce9,document.getElementsByTagName('head')[0]);},0);};}else{this.Debug.zfda7("initialize: Server Library already loaded, will now prepare optimize.");WTOptimize.z88d3(new WTEvent(WTEvent.INITIALIZE,WTEvent.STATUS_SUCCESS,zccdd));WTOptimize.z32ad.z311d(zccdd["testAlias"],zccdd);}}catch(z392c){this.Debug.z1dbb("initialize: error during framework setup.",0,z392c);WTOptimize.z88d3(new WTEvent(WTEvent.INITIALIZE,WTEvent.STATUS_FAULT,zccdd));WTOptimize.z88d3(new WTEvent(WTEvent.ABORT,WTEvent.STATUS_FAULT,zccdd));}};this.zfa9c=function(z9de9,type,z508a,color){if(type=="display"){z9de9.style.display=(z508a?'':'none');if(z9de9!=document.body&&!z508a){document.body.style.display='';};}else if(type=="visibility"){z9de9.style.visibility=(z508a?'visible':'hidden');z9de9.style.hidden=!z508a;if(z9de9!=document.body&&!z508a){document.body.style.visibility='visible';document.body.style.hidden=false;};}else if(type=="shift"){z9de9.style.position=(z508a?'relative':'absolute');z9de9.style.left=(z508a?'auto':'-1000%');if(z9de9!=document.body&&!z508a){document.body.style.position='relative';document.body.style.left='auto';};}else if(type=="overlay"){var z5958=document.getElementById('wt_overlay');var zeb47=document.getElementById('wt_overlayStyle');var bgColor=(typeof(color)=="undefined")?"#ffffff":color;if(z508a&&z5958){z5958.parentNode.removeChild(z5958);if(zeb47){zeb47.parentNode.removeChild(zeb47);};}else if(!z508a&&!z5958){if(!zeb47){var zb861=document.createElement('style');zb861.setAttribute('type','text/css');zb861.setAttribute('id','wt_overlayStyle');var z87ef='#wt_overlay{position:absolute;width:100%;height:100%;top:0px;right:0px;bottom:0px;left:0px;background-color:'+bgColor+';z-index:2147483646}';if(zb861.styleSheet){zb861.styleSheet.cssText=z87ef;}else{zb861.appendChild(document.createTextNode(z87ef));}document.getElementsByTagName('head')[0].appendChild(zb861);}if(typeof(color)=="undefined"){z5958=document.createElement('iframe');z5958.frameBorder=0;}else{z5958=document.createElement('div');}z5958.id='wt_overlay';document.getElementsByTagName('body')[0].appendChild(z5958);};}else if(type=="none"){}var zf64e={};zf64e['displayType']=type;zf64e['display']=z508a;WTOptimize.z88d3(new WTEvent(WTEvent.HIDESHOW,WTEvent.STATUS_SUCCESS,z9de9,zf64e));};this.z9d5a=function(z9de9,event,z4d76){if(z9de9.addEventListener){z9de9.addEventListener(event,z4d76,false);}else if(z9de9.attachEvent){z9de9.attachEvent(event,z4d76);}else{eval('elm.'+event+'=func;');}};this.z99db=function(event){WTOptimize.z4e9a=new Date();if(WTOptimize.z907d){clearTimeout(WTOptimize.z907d);WTOptimize.z907d=null;}WTOptimize.Debug.z5060(event.name+": expired, it took longer than expected. ["+WTOptimize.z4e9a+"]");WTOptimize.z8cdc(event);};this.z8cdc=function(event){if(WTOptimize.z907d){WTOptimize.z4e9a=new Date();clearTimeout(WTOptimize.z907d);WTOptimize.Debug.z5060(event.name+": clearing active timer ["+WTOptimize.z4e9a+"]");}if((event.target&&event.target["r_cWrapEl"])&&!event.target["defaultUrl"]){var z9180=event.target["r_cWrapEl"];z1a23.zfa9c(z9180,event.target["s_pageDisplayMode"],true,event.target["overlayColor"]);}else if(event.target&&event.target["defaultUrl"]){WTOptimize.z88d3(new WTEvent(WTEvent.DONE,event["state"]));WTOptimize.ze3c2=true;WTOptimize.Debug.z5060(event.name+": redirecting to error url ["+event.target["defaultUrl"]+"]");document.location.href=event.target["defaultUrl"];return;}for(var z205e=0;z205e<WTOptimize.z2b20.length;z205e++){var z2945=WTOptimize.z2b20[z205e];var z9180=z2945.zccdd["r_cWrapEl"];if(z2945&&z9180&&!z2945.zccdd["defaultUrl"]){try{z1a23.zfa9c(z9180,z2945.zccdd["s_pageDisplayMode"],true,z2945.zccdd["overlayColor"]);WTOptimize.Debug.z1dbb(event.name+": render page due to error, check log for more information. [alias: "+z2945.zccdd["testAlias"]+", dom id: "+z9180.id+"]",0);}catch(z392c){WTOptimize.Debug.z1dbb(event.name+": during abort on a render of a page an error occurred, check log for more information. [alias: "+z2945.zccdd["testAlias"]+", dom id: "+z9180.id+"]",0,z392c);};}else if(z2945&&z2945.zccdd["defaultUrl"]){WTOptimize.z88d3(new WTEvent(WTEvent.DONE,event["state"]));WTOptimize.ze3c2=true;WTOptimize.Debug.z5060(event.name+": redirecting to error url ["+z2945.zccdd["defaultUrl"]+"] for alias ["+z2945.zccdd["testAlias"]+"]");document.location.href=z2945.zccdd["defaultUrl"];return;};}if(!WTOptimize.zc901){z1a23.zfa9c(document.body,this.z23eb["s_pageDisplayMode"],true,this.z23eb["overlayColor"]);}WTOptimize.z88d3(new WTEvent(WTEvent.DONE,event["state"]));WTOptimize.ze3c2=true;};WTEvent=function(name,z937d,target,zccdd){if(name){name=name.toLowerCase();}this["name"]=name;this["handler"]=null;this["state"]=WTEvent.STATUS_UNKNOWN;if(z937d){this["state"]=z937d;}this["target"]=target;this["params"]={};if(zccdd){this["params"]=zccdd;}};WTEvent.z6fd7="dom_onload";WTEvent.HIDESHOW="hide_show";WTEvent.CAPI_CONFIG="capi_config";WTEvent.z0cf5="capi_config_processed";WTEvent.z4519="timer";WTEvent.INITIALIZE="initialize";WTEvent.PREPARE="prepare";WTEvent.CONFIGURE="configure";WTEvent.PREPROCESS="preprocess";WTEvent.PROCESS="process";WTEvent.zd938="library_load";WTEvent.INVALID="invalid";WTEvent.ABORT="abort";WTEvent.PRERENDER="prerender";WTEvent.RENDER="render";WTEvent.DONE="done";WTEvent.PAGEVIEW="pageview";WTEvent.CONVERSION="conversion";WTEvent.STATUS_SUCCESS="success";WTEvent.STATUS_FAULT="fault";WTEvent.STATUS_UNKNOWN="unknown";this.zd0b7=function(name,z9204,z5430){if(!name||(!z9204&&!z5430)){this.Debug.z5d59("events: Can not add event handler, missing name or listenrs. ");return;}name=name.toLowerCase();if(!this.z23eb["s_eventHandlers"][name]){this.z23eb["s_eventHandlers"][name]={};this.z23eb["s_eventHandlers"][name]["success"]=[];this.z23eb["s_eventHandlers"][name]["fault"]=[];}if(z9204){var zdd00=false;for(var z205e=0;z205e<this.z23eb["s_eventHandlers"][name]["success"].length;z205e++){if(this.z23eb["s_eventHandlers"][name]["success"][z205e]==z9204){zdd00=true;break;};}if(!zdd00){this.z23eb["s_eventHandlers"][name]["success"].push(z9204);};}if(z5430){var zdd00=false;for(var z11b6=0;z11b6<this.z23eb["s_eventHandlers"][name]["fault"].length;z11b6++){if(this.z23eb["s_eventHandlers"][name]["fault"][z11b6]==z9204){zdd00=true;break;};}if(!zdd00){this.z23eb["s_eventHandlers"][name]["fault"].push(z5430);};}};this.za099=function(name,z9204,z5430){if(!name){this.Debug.z5d59("events: Can not remove event handler, missing name.");return;}name=name.toLowerCase();if(!this.z23eb["s_eventHandlers"][name]){this.z23eb["s_eventHandlers"][name]={};this.z23eb["s_eventHandlers"][name]["success"]=[];this.z23eb["s_eventHandlers"][name]["fault"]=[];}if(!z9204&&!z5430){delete this.z23eb["s_eventHandlers"][name];return;}if(z9204){for(var z205e=0;z205e<this.z23eb["s_eventHandlers"][name]["success"].length;z205e++){if(this.z23eb["s_eventHandlers"][name]["success"][z205e]==z9204){delete this.z23eb["s_eventHandlers"][name]["success"][z205e];break;};};}if(z5430){for(var z11b6=0;z11b6<this.z23eb["s_eventHandlers"][name]["fault"].length;z11b6++){if(this.z23eb["s_eventHandlers"][name]["fault"][z11b6]==z9204){delete this.z23eb["s_eventHandlers"][name]["fault"][z11b6];break;};};}};this.z88d3=function(event,async,z8dce){if(WTOptimize.ze3c2&&!z8dce){WTOptimize.Debug.z1dbb("fireEvent: Aborted due to prior error, check error message for details.",0);return;}if(!this.z23eb["s_eventHandlers"][event["name"]]){this.Debug.z5d59("fireEvent: no registered event was found for event name: "+event["name"]);return;}var z6907=this.z23eb["s_eventHandlers"][event["name"]][event["state"]];if(!z6907){this.Debug.z5d59("fireEvent: no event handler was registered for event: "+event["name"]+" state: "+event["state"]);return;}for(var z205e=0;z205e<z6907.length;z205e++){if(!z6907[z205e]){continue;}try{event["handler"]=z6907[z205e];event["params"]["eventID"]=(new Date()).getTime();this.Debug.z5d59("fireEvent: [name:"+event["name"]+", state:"+event["state"]+", function:"+event["handler"].toString()+"]");if(async){setTimeout(function(){event["handler"](event);},0);}else{event["handler"](event);}}catch(z392c){this.Debug.z1dbb("Unhandled Event Exception, [name: "+event["name"]+", state: "+event["state"]+", function: "+event["handler"].toString()+"]",0,z392c);};}};this.z22f7=function(){this.z6c38=-1;this.z4706=false;this.z5dbe=false;var zd40e=[];this.z2dc5=null;var zed9b=false;var ERROR=0;var z91ec=1;var zdb41=2;var z9118=3;this.logInfo=function(z086a){this.z5060(z086a);};this.logDebug=function(z086a){this.zfda7(z086a);};this.logTrace=function(z086a){this.z5d59(z086a);};this.logError=function(z086a,code,z392c){this.z1dbb(z086a,code,z392c);};this.z5d59=function(z086a){this.z94ca(z9118,z086a);};this.zfda7=function(z086a){this.z94ca(zdb41,z086a);};this.z5060=function(z086a){this.z94ca(z91ec,z086a);};this.z1dbb=function(z086a,code,z392c){if(!code){code="";}else{code="("+code+") ";}var z96cf=code+z086a+(z392c?"\n"+z392c["message"]+(z392c["name"]?" ["+z392c["name"]+"]":"")+(z392c["fileName"]?"\n ("+z392c["fileName"]+":"+z392c["lineNumber"]+")\n"+z392c["stack"]:""):"");this.z94ca(ERROR,z96cf);};this.z94ca=function(zf7c5,z086a){if((!this.z2dc5||!this.z4706)&&!this.z5dbe){zd40e.push([zf7c5,z086a]);}if(this.z5dbe){zd40e=[];return;}if(this.z6c38<zf7c5){return;}if(this.z2dc5){this.z2dc5.z2e47(zf7c5,z086a);}};this.refresh=function(z937d){this.z4706=z937d;if(this.z2dc5&&z937d){this.z2dc5.z8c7c(zd40e);zd40e=[];}else{if(this.z6c38>0&&!zed9b){zed9b=true;var zbade={};zbade["type"]="text/javascript";zbade["async"]="true";zbade["defer"]="true";zbade["src"]=z1a23.z23eb["s_otsDebuggerLibUrl"];setTimeout(function(){z1a23.ze82b("script",zbade,null,document.getElementsByTagName('head')[0]);},0);};}};};this.ze82b=function(type,zbade,zfce9,parent,z9520){if(!z9520){if(parent){z9520=parent.ownerDocument;}if(!z9520){z9520=document;};}var n=z9520.createElement(type);if(zbade){for(var z205e in zbade){if(zbade[z205e]&&zbade.hasOwnProperty(z205e)){n.setAttribute(z205e,zbade[z205e]);};};}if(zfce9){for(var z392c in zfce9){if(zfce9[z392c]&&zfce9.hasOwnProperty(z392c)){z1a23.z9d5a(n,z392c,zfce9[z392c]);};};}if(parent){parent.appendChild(n);}return n;};this.z95e8();};if(typeof(WTOptimize)=="undefined"){WTOptimize=new z0691();}WTOptimize.lookup=function(value,type){for(var z205e=0;z205e<this.z2b20.length;z205e++){var zae74=this.z2b20[z205e];if(zae74.zccdd["testAlias"]==value&&(!type||type==zae74.zccdd["s_requestType"])){return zae74;};}};WTOptimize.setup=function(za69d,z606a){try{if(WTOptimize.ze3c2){WTOptimize.Debug.z1dbb("setup: Aborted due to prior error, check error message for details.",0);return;}var zccdd={};if(typeof(za69d)=="object"){zccdd=za69d;}else if(typeof(za69d)=="string"){if(typeof(z606a)!="undefined"){zccdd=z606a;}zccdd["testAlias"]=za69d;}if(typeof(za69d)!="string"){if(WTOptimize.zfff3){WTOptimize.Debug.z1dbb("setup: Only one empty setup call is supported.",0);return;}WTOptimize.zfff3=true;}if(typeof(za69d)=="string"){for(var z205e in WTOptimize.zf310){if(za69d==WTOptimize.zf310[z205e]["testAlias"]){WTOptimize.Debug.z1dbb("setup: Duplicate setup calls with the same alias are not supported.",0);return;};};}zccdd["s_requestType"]="control";if(!WTOptimize.zc901){var z0fee=this;WTOptimize.zd0b7(WTEvent.z0cf5,(function(){z0fee.z3e29(zccdd);}));WTOptimize.zfa9c(document.body,this.z23eb["s_pageDisplayMode"],false,this.z23eb["overlayColor"]);}else{this.z3e29(zccdd);}}catch(z392c){WTOptimize.Debug.z1dbb("setup: Fatal error, check error message for details.",0,z392c);var event=new WTEvent((z392c.zbee9?z392c.zbee9:WTEvent.ABORT),WTEvent.STATUS_FAULT,this);event.zccdd["error"]=z392c;WTOptimize.z88d3(event);}};WTOptimize.conversion=function(za69d,z606a,z51e5){var zccdd={};try{if(WTOptimize.ze3c2){WTOptimize.Debug.z1dbb("conversion: Aborted due to prior error, check error message for details.",0);return true;}if(typeof(za69d)=="object"){zccdd=za69d;}else if(typeof(za69d)=="string"){if(typeof(z606a)!="undefined"){if(typeof(z606a)=="object"){if(typeof(z51e5)!="undefined"){zccdd=z51e5;}var ze121;var z9de9;if(z606a.type){ze121=z606a;z9de9=(z606a.currentTarget)?z606a.currentTarget:z606a.srcElement;}else if(z606a.tagName){z9de9=z606a;}if(ze121){if(ze121.preventDefault){ze121.preventDefault();}else{ze121.returnValue=false;};}if(z9de9){if(z9de9.href){if(!z9de9.zd96e){z9de9.zd96e="true";z9de9.za3b2=z9de9.href;z9de9.z6165=z9de9.target;z9de9.href="javascript:void(0);";z9de9.target="";}zccdd["r_redirectLink"]=z9de9;};}else{zccdd=z606a;};};}zccdd["testAlias"]=za69d;}zccdd["s_requestType"]="conversion";WTOptimize.za099(WTEvent.CONTROL_RESPONSE);if(!WTOptimize.zc901){var z0fee=this;WTOptimize.zd0b7(WTEvent.CAPI_CONFIG,(function(){z0fee.z3e29(zccdd);}));}else{this.z3e29(zccdd);}return false;}catch(z392c){if(typeof(z606a)=="object"&&z606a.href&&zccdd["r_redirectLink"]===z606a){z606a.href=z606a.za3b2;z606a.target=z606a.z6165;}WTOptimize.Debug.z1dbb("conversion: Fatal error, check error message for details.",0,z392c);var event=new WTEvent((z392c.zbee9?z392c.zbee9:WTEvent.ABORT),WTEvent.STATUS_FAULT,this);event.zccdd["error"]=z392c;WTOptimize.z88d3(event);return true;}};WTOptimize.addEventHandler=function(name,z9204,z5430){this.zd0b7(name,z9204,z5430);};WTOptimize.removeEventHandler=function(name,z9204,z5430){this.za099(name,z9204,z5430);};WTOptimize.fireEvent=function(event,async){this.z88d3(event,async);};if(!this.JSON){JSON={};}(function(){function zd081(n){return n<10?'0'+n:n;};if(typeof Date.prototype.zb0f6!=='function'){Date.prototype.zb0f6=function(z2186){return this.getUTCFullYear()+'-'+zd081(this.getUTCMonth()+1)+'-'+zd081(this.getUTCDate())+'T'+zd081(this.getUTCHours())+':'+zd081(this.getUTCMinutes())+':'+zd081(this.getUTCSeconds())+'Z';};String.prototype.zb0f6=Number.prototype.zb0f6=Boolean.prototype.zb0f6=function(z2186){return this.valueOf();};}var cx=/[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,z8027=/[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,z6f38,indent,zd578={'\b':'\\b','\t':'\\t','\n':'\\n','\f':'\\f','\r':'\\r','"':'\\"','\\':'\\\\'},zf0b5;function quote(zd03c){z8027.lastIndex=0;return z8027.test(zd03c)?'"'+zd03c.replace(z8027,function(za18f){var z8403=zd578[za18f];return typeof z8403==='string'?z8403:'\\u'+('0000'+za18f.charCodeAt(0).toString(16)).slice(-4);})+'"':'"'+zd03c+'"';};function z086a(z2186,z4e98){var z205e,z8836,z2455,length,zbfaf=z6f38,z126b,value=z4e98[z2186];if(value&&typeof value==='object'&&typeof value.zb0f6==='function'){value=value.zb0f6(z2186);}if(typeof zf0b5==='function'){value=zf0b5.call(z4e98,z2186,value);}switch(typeof value){case 'string':return quote(value);case 'number':return isFinite(value)?String(value):'null';case 'boolean':case 'null':return String(value);case 'object':if(!value){return 'null';}z6f38+=indent;z126b=[];if(Object.prototype.toString.apply(value)==='[object Array]'){length=value.length;for(z205e=0;z205e<length;z205e+=1){z126b[z205e]=z086a(z205e,value)||'null';}z2455=z126b.length===0?'[]':z6f38?'[\n'+z6f38+z126b.join(',\n'+z6f38)+'\n'+zbfaf+']':'['+z126b.join(',')+']';z6f38=zbfaf;return z2455;}if(zf0b5&&typeof zf0b5==='object'){length=zf0b5.length;for(z205e=0;z205e<length;z205e+=1){z8836=zf0b5[z205e];if(typeof z8836==='string'){z2455=z086a(z8836,value);if(z2455){z126b.push(quote(z8836)+(z6f38?': ':':')+z2455);};};};}else{for(z8836 in value){if(Object.hasOwnProperty.call(value,z8836)){z2455=z086a(z8836,value);if(z2455){z126b.push(quote(z8836)+(z6f38?': ':':')+z2455);};};};}z2455=z126b.length===0?'{}':z6f38?'{\n'+z6f38+z126b.join(',\n'+z6f38)+'\n'+zbfaf+'}':'{'+z126b.join(',')+'}';z6f38=zbfaf;return z2455;};};if(typeof JSON.stringify!=='function'){JSON.stringify=function(value,z226f,z2d70){var z205e;z6f38='';indent='';if(typeof z2d70==='number'){for(z205e=0;z205e<z2d70;z205e+=1){indent+=' ';}}else if(typeof z2d70==='string'){indent=z2d70;}zf0b5=z226f;if(z226f&&typeof z226f!=='function'&&(typeof z226f!=='object'||typeof z226f.length!=='number')){throw new Error('JSON.stringify');}return z086a('',{'':value});};}if(typeof JSON.parse!=='function'){JSON.parse=function(text,zffea){var zd4f0;function zf540(z4e98,z2186){var z8836,z2455,value=z4e98[z2186];if(value&&typeof value==='object'){for(z8836 in value){if(Object.hasOwnProperty.call(value,z8836)){z2455=zf540(value,z8836);if(z2455!==undefined){value[z8836]=z2455;}else{delete value[z8836];};};};}return zffea.call(z4e98,z2186,value);};cx.lastIndex=0;if(cx.test(text)){text=text.replace(cx,function(za18f){return '\\u'+('0000'+za18f.charCodeAt(0).toString(16)).slice(-4);});}if(/^[\],:{}\s]*$/.test(text.replace(/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g,'@').replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,']').replace(/(?:^|:|,)(?:\s*\[)+/g,''))){zd4f0=eval('('+text+')');return typeof zffea==='function'?zf540({'':zd4f0},''):zd4f0;}throw new SyntaxError('JSON.parse');};}})();(function(z9c9a){function z8403(){};for(var z500d="assert,clear,count,debug,dir,dirxml,error,exception,firebug,group,groupCollapsed,groupEnd,info,log,memoryProfile,memoryProfileEnd,profile,profileEnd,table,time,timeEnd,timeStamp,trace,warn".split(","),za18f;za18f=z500d.pop();){z9c9a[za18f]=z9c9a[za18f]||z8403;}})((function(){try{console.log();return window.console;}catch(z0d75){return window.console={};}})());

/*/////////////////////////////////////////////////////////////
 DESC..: CSS utility object - for easy adding of stylesheets
 PARAMS: sCSS - STRING - initial CSS rules to add
 -------------------------------------------------------------*/
var css = function(sCSS) {
    var rules = sCSS || '';
    return {
        'add':  function(rule) { if(rule.length) rules += rule+'\n'; },
        'get':      function() { return rules.replace(/[\s]{2,}/mg,' '); },
        'output':   function(sID)
        {
            var nStyle;
            if(rules.length) {
                if(sID) nStyle = document.getElementById(sID);
                if(nStyle) {
                    // if(bDebug) console.warn('WTO css.output(): style with ID '+sID+' already exists.');
                    return false;
                }

                // Insert stylesheet into DOM
                nStyle = document.createElement("style");
                nStyle.setAttribute("type", "text/css");

                // Set id if provided
                if(sID) nStyle.id = sID;

                if (nStyle.styleSheet) { nStyle.styleSheet.cssText = rules; }
                else { nStyle.appendChild(document.createTextNode(rules)); }
                document.getElementsByTagName("head")[0].appendChild(nStyle);

                // Zero out rules
                rules = '';
            }
        },
        'remove': function(sID, oDoc) {
            if(!sID)
            {
                //if(bDebug) console.warn('WTO css.remove(): missing parameter "sID"');
                return false;
            }

            if(!oDoc) oDoc = window.document;

            // Remove CSS
            var eCSS = oDoc.getElementById(sID);
            if(eCSS && eCSS.nodeName.toLowerCase() == 'style') eCSS.parentNode.removeChild(eCSS);
        }
    }
};

var wtObj = {
    s_pageMode: 'head',
    s_pageDisplayMode: 'none',
    s_pageTimeout: 10000
}

var urlWhitelist = [

    // TEST 5
    /videohive.net\/deposit\/?(\?|#|$)/i
	
    // TEST 6
    ,/https:\/\/tutsplus.com\/take-the-tour\/?(\?|#|$)/i
	
    // TEST 7
    ,/signup.php/i
    ,/course/i
	
    // TEST 11
    ,/videohive.net\/?(\?|#|$)/i
    ,/account\.envato\.com\/sign_(in|up)/i
	
    // TEST 14
    ,/https:\/\/tutsplus.com\/amember\/signup.php($|#|\?)/i
];


for(var i=0;i<urlWhitelist.length;i++)
{
    if(window.location.href.match(urlWhitelist[i]))
	{
        wtObj.s_pageTimeout=4000;
		wtObj.s_pageDisplayMode='none'; // Use CSS to hide specific page areas

        var css_hide;
        if( window.location.href.match(/account\.envato\.com\/sign_(in|up)/i) )
            css_hide = new css('body { visibility:hidden; }');
        else
            css_hide = new css('#content-wrap, #content { visibility:hidden; }');

        css_hide.output('wto-css-capi');

        // Redisplay handler
        WTOptimize.addEventHandler(WTEvent.HIDESHOW, function (event) {
            if (event.params && event.params.display == true) {
                css_hide.remove('wto-css-capi');
            }
        });

        // Safeguard to redisplay page if unexpected happens
        setTimeout(function ()
        {
            css_hide.remove('wto-css-capi');
        }, 7000);
			
        break;
    }
}

try {
	var WTO_CTrack=function(g){var j=g.s_domainKey||WTOptimize.z23eb.s_domainKey;var p=g.s_keyToken||WTOptimize.z23eb.s_keyToken;var m="control";var a=j+(g.projectAlias?"-"+g.projectAlias:"");if(g.conversionPoint){m="track"}var e="https://ots.optimize.webtrends.com/ots/api/rest-1.2/"+m+"/"+a+"?keyToken="+p+"&preprocessed=true&_wt.encrypted=true&testGroup=shared";if(g.projectAlias&&document.cookie.match("_wt.control-"+j+"-"+g.projectAlias)===null){return}var r=[];var k=document.cookie.match(/_wt\.(control|user|mode)-[^=]+=[^;\s$]+/ig);if(k){for(var h=0,l=k.length;h<l;h++){var d=k[h].split("=");var b=d[0];var f=d[1];r.push('"'+b+'":{"value":"'+encodeURIComponent(f)+'"}')}var n=(g.URL?"&url="+encodeURIComponent(g.URL):"");var q=(g.conversionPoint&&("&conversionPoint="+g.conversionPoint)||"");var o=(g.data?"&data="+g.data:"");var c=document.createElement("img");c.src=e+n+"&cookies={"+r.join(",")+"}"+q+o}};

	var meta = document.getElementsByTagName('meta');
	for(var i=0; i<meta.length; i++){
		if(meta[i].name == 'WT.tx_e'){
			WTO_CTrack({
				projectAlias: 'ta_11VideoHiveDepositsPageMVT',
				conversionPoint: 'Page_Purchase',
			});
		} else if(meta[i].name == 'WT.tx_s' && meta[i].content){
			WTO_CTrack({
				projectAlias: 'ta_11VideoHiveDepositsPageMVT',
				conversionPoint: 'Purchase_Revenue',
				data: '{"Revenue":"'+meta[i].content+'"}'
			});
		}
	}
} catch(err){}