module org.ajdeveloppement.webserver.logger {
	requires java.sql;
	requires java.desktop;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.google.common;

	provides com.fasterxml.jackson.databind.Module with org.ajdeveloppement.webserver.viewbinder.jackson.ViewModule;
}