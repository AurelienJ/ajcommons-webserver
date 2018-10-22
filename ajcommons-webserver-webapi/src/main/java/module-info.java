module org.ajdeveloppement.webserver.webapi {
	requires org.ajdeveloppement.commons.core;
	requires org.ajdeveloppement.webserver.core;
	
	requires guice;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.xml;
	requires governator;
	requires java.sql;
	requires java.compiler;

	provides javax.annotation.processing.Processor with org.ajdeveloppement.webserver.services.webapi.processor.WebApiControllerProcessor;
	provides org.ajdeveloppement.webserver.services.RequestProcessor with org.ajdeveloppement.webserver.services.webapi.ApiService;
	
	exports org.ajdeveloppement.webserver.services.webapi;
	exports org.ajdeveloppement.webserver.services.webapi.annotations;
	exports org.ajdeveloppement.webserver.services.webapi.helpers;
}