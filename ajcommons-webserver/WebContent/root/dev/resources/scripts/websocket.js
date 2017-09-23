/**
 * Exemple WebSocket Text Usage
 */
register(["/websocket"], (function() {
	var Thread = Java.type("java.lang.Thread");
	var WebSocketResponse = Java.type("org.ajdeveloppement.webserver.websocket.WebSocketResponse");

	var websocketClients = [];

	function init(basePath) {
		var t = new Thread(function() {
			while(true) {
				Thread.sleep(10000);
				
				for(var i in websocketClients) {
					print("envoyer HW 2");
					websocketClients[i].sendMessage("Hello World!");
				}
			}
		});
		t.start();
	}

	function getPage(httpRequest) {
		var isWebsocket = httpRequest.getHeaderValues().get("upgrade") == "websocket";
		var webSocketResponse = null;
		if(isWebsocket) {
			print("ok");
			var webSocketResponse = new WebSocketResponse(httpRequest);
			webSocketResponse.addWebSocketListener(
					new org.ajdeveloppement.webserver.websocket.WebSocketListener() {
						connectionReady: function() {
							print("ouverture connection");
						},
					
						messageReceived: function(message) {
							print(webSocketResponse.readTextMessage());
							webSocketResponse.sendMessage("reçue");
						},
						connectionClosed: function() {
							print("fermeture connection");
						}
					});
			
			websocketClients.push(webSocketResponse);
		} else {
			webSocketResponse = ResponseFormatter.getGzipedResponseForOutputTemplate(httpRequest, "Ceci n'est pas une connexion websocket");
		}
		
		
		return webSocketResponse;
	}
	
	return {
		init: init,
		getPage: getPage
	};
})());