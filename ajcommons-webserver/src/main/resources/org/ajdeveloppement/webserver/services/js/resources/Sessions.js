

function Sessions(httpRequest) {
	this.sessionId = null;
	this.creation = false;
	
	if(httpRequest.getCookiesParameters().containsKey("sessionid")) {
		var sessionId = java.util.UUID.fromString(httpRequest.getCookiesParameters().get("sessionid"));
		if(sessionId != null) {
			this.sessionId = sessionId;
		}
	} else if(httpRequest.getUrlParameters().containsKey("sessionid")) {
		var sessionId = java.util.UUID.fromString(httpRequest.getUrlParameters().get("sessionid"));
		if(sessionId != null) {
			this.sessionId = sessionId;
		}
	}
	
	if(this.sessionId == null)
		this.createSessionId();
}

Sessions.prototype.getSessionId = function() {
	return this.sessionId;
};

Sessions.prototype.createSessionId = function() {
	this.sessionId = java.util.UUID.randomUUID();
	this.creation = true;
};

Sessions.prototype.getSessionData =function(){
	return org.ajdeveloppement.webserver.SessionManager.getSession(this.sessionId);
};

Sessions.prototype.putSessionData = function(data, persistent) {
	return org.ajdeveloppement.webserver.SessionManager.putSession(this.sessionId, persistent === true, data);
};

Sessions.prototype.addCookieHeader = function(response, maxAge) {
	if(this.creation) {
		var expiration = "";
		if(maxAge)
			expiration = ";Max-Age=" + (60 * 60 * 24);
		response.addHeader("Set-Cookie", "SESSIONID="+ this.sessionId.toString() + expiration);
	}
};