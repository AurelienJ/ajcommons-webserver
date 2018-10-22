module org.ajdeveloppement.webserver.js {
	requires org.ajdeveloppement.commons.core;
	requires org.ajdeveloppement.webserver.core;
	requires java.scripting;
	
	provides org.ajdeveloppement.webserver.services.RequestProcessor with org.ajdeveloppement.webserver.services.js.JsService;
}