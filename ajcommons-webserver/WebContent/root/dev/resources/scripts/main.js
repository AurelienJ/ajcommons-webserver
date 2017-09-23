
load("intern:init.js");
load("intern:Sessions.js");

load(basePath + "/scripts/websocket.js");

(function(global) {
	var HttpReturnCode = Java.type("org.ajdeveloppement.webserver.HttpReturnCode");
	var AJTemplate = Java.type("org.ajdeveloppement.commons.AJTemplate");
	var ExceptionUtils = Java.type("org.ajdeveloppement.commons.ExceptionUtils");

	global.serveErrorResponse = function(httpRequest, errorCode, errorContent) {
		try {
			//Customize Error 404 Response
			if(errorCode == HttpReturnCode.ClientError.NotFound) {
				var error404Template = new AJTemplate();
				error404Template.loadTemplate(basePath + "/templates/404.thtml");
				
				error404Template.parse("URL",httpRequest.getRequestUrl());
				
				var notFoundResponse = ResponseFormatter.getGzipedResponseForOutputTemplate(httpRequest.getHttpSession(), 
						error404Template.output());
				notFoundResponse.setReturnCode(errorCode);
				
				return notFoundResponse;
			}
		} catch (e) {
			var errorResponse = ResponseFormatter.getGzipedResponseForOutputTemplate(httpRequest.getHttpSession(), 
					e.stack);
			errorResponse.setReturnCode(HttpReturnCode.ServerError.InternalServerError);
			
			return errorResponse;
		}
		
		return null;
	};
})(this);
