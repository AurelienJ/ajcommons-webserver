module org.ajdeveloppement.webserver.logger {
	requires org.ajdeveloppement.commons.core;
	requires org.ajdeveloppement.commons.persistence;
	requires org.ajdeveloppement.commons.persistence.sql;
	requires org.ajdeveloppement.webserver.core;
	requires java.sql;
	requires com.h2database;

	provides org.ajdeveloppement.webserver.Logger with org.ajdeveloppement.webserver.logger.DbLogger;
}