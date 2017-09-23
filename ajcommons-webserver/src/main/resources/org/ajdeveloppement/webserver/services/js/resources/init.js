load("intern:ScriptLoader.js");

ResponseFormatter = Java.type("org.ajdeveloppement.webserver.services.js.ResponseFormatter");

(function(global) {
	var JsService = Java.type("org.ajdeveloppement.webserver.services.js.JsService");
	
	var _server;
	var webPages = [];
	var uris = {};
	
	function extend( a, b ) {
	    for( var key in b )
	        if( b.hasOwnProperty(key) )
	            a[key] = b[key];
	    return a;
	}
	
	/**
	 * 
	 */
	function register(serviceUris, webPage) {
		var internalWebPage = {
				init: function(server) { },
				getPage: function(session) { return null; }
			};
		extend(internalWebPage, webPage);
		
		webPages.push(internalWebPage);
		for(var i in serviceUris) {
			uris[serviceUris[i]] = internalWebPage;
			print("register uri:" + serviceUris[i]);
		}
		
		internalWebPage.init(_server);
	};
	
	function unregister(webPage) {
		extend(internalWebPage, webPage);
	}
	
	function getUriWithoutAliasPrefix(httpRequest) {
		var uri = httpRequest.getRequestUri();
		
		var fileSelector = httpRequest.getHttpSession().getHttpServer().getFileSelector();
		var host = fileSelector.selectHost(httpRequest);
		var alias = fileSelector.selectAlias(httpRequest);
		if(alias != null)
			uri = alias.supressAlias(uri);
		
		return uri;
	}
	
	function init(server) {
		_server = server;
	}
	
	function canServe(httpRequest) {
		var uri = getUriWithoutAliasPrefix(httpRequest);

		if(uris[uri] != undefined && uris[uri] != null)
			return true;

		return false;
	};
	
	function serve(httpRequest) {
		var uri = getUriWithoutAliasPrefix(httpRequest);
		
		if(uris[uri] != undefined && uris[uri] != null) {
			if(typeof uris[uri].getPage === "function")
				return uris[uri].getPage(httpRequest);
		}
			
		return null;
	};
	
	function serveErrorResponse(httpRequest, errorCode, errorContent) {
		return null;
	}
	
	global.extend = extend;
	
	global.register = register;
	global.init = init;
	global.canServe = canServe;
	global.serve = serve;
	global.serveErrorResponse = serveErrorResponse;
	global.unload = function() {};
})(this);
