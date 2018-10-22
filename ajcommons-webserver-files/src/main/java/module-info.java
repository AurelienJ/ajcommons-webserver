module org.ajdeveloppement.webserver.files {
	requires org.ajdeveloppement.commons.core;
	requires org.ajdeveloppement.commons.persistence;
	requires org.ajdeveloppement.commons.persistence.sql;
	requires org.ajdeveloppement.webserver.core;
//	requires java.xml.bind;
	requires java.sql;
//	requires java.scripting;
//	requires java.compiler;
	
	provides org.ajdeveloppement.webserver.services.RequestProcessor with org.ajdeveloppement.webserver.services.files.FilesService;
	
	exports org.ajdeveloppement.webserver.services.files;
}