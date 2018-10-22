module org.ajdeveloppement.webserver.core {
	requires org.ajdeveloppement.commons.core;
	requires org.ajdeveloppement.commons.persistence;
	requires org.ajdeveloppement.commons.persistence.sql;
	requires java.xml.bind;
	requires java.sql;
	requires java.scripting;
	requires java.compiler;

	exports org.ajdeveloppement.webserver;
	exports org.ajdeveloppement.webserver.data;
	exports org.ajdeveloppement.webserver.utils;
	exports org.ajdeveloppement.webserver.services;
	exports org.ajdeveloppement.webserver.websocket;
}