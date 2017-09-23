package org.ajdeveloppement.webserver.services.js;

import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.HttpSession;

public interface WebAppInterface {
	public void init(HttpServer server);
	public boolean canServe(HttpRequest httpRequest);
	public HttpResponse serve(HttpRequest httpRequest);
	public HttpResponse serveErrorResponse(HttpRequest httpRequest, HttpReturnCode.ReturnCode errorCode, String errorContent);
	public void unload();
}
