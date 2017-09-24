CREATE SCHEMA IF NOT EXISTS AJWEBSERVER;

CREATE TABLE IF NOT EXISTS AJWEBSERVER.PARAM (
		DBVERSION				INTEGER NOT NULL,
		APPREVISION				INTEGER NOT NULL,
		DEFAULT_LANG			VARCHAR(5) NOT NULL DEFAULT 'fr',
		DBUUID UUID				NOT NULL DEFAULT RANDOM_UUID()
	);
DELETE FROM AJWEBSERVER.PARAM;
INSERT INTO AJWEBSERVER.PARAM (DBVERSION, APPREVISION, DEFAULT_LANG) VALUES (7, 1, 'fr');

CREATE TABLE IF NOT EXISTS AJWEBSERVER.Resources (
	Path VARCHAR(1024) NOT NULL,
	Length BIGINT,
	CreatedDate DATETIME,
	LastModifiedDate DATETIME,
	
	PRIMARY KEY (Path)
);

CREATE TABLE IF NOT EXISTS AJWEBSERVER.Request (
	IdRequest BIGINT NOT NULL AUTO_INCREMENT,
	Date DATETIME,
	Method VARCHAR(10),
	Uri VARCHAR(255),
	Host VARCHAR(255),
	RequestLength BIGINT,
	ResponseLength BIGINT,
	ResponseCode INTEGER,
	RemoteAddress VARCHAR(128),
	UserAgent VARCHAR(1024),
	Referer VARCHAR(512),
	Header TEXT,
	ResourcePath varchar(1024),
	Duration BIGINT,
	Exception TEXT,
	
	PRIMARY KEY (IdRequest)
);
CREATE INDEX IF NOT EXISTS IX_Request_ResourcePath ON AJWEBSERVER.Request (ResourcePath ASC);
CREATE INDEX IF NOT EXISTS IX_Request_Date ON AJWEBSERVER.Request (Date ASC);

CREATE TABLE IF NOT EXISTS AJWEBSERVER.SessionData (
	IdSession UUID NOT NULL,
	Data OTHER,
	ExpirationDate DATETIME,
	
	PRIMARY KEY (IdSession)
);