package org.ajdeveloppement.webserver.websocket;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.ajdeveloppement.commons.net.http.websocket.FrameHeader;
import org.ajdeveloppement.commons.net.http.websocket.OpCode;
import org.ajdeveloppement.commons.net.http.websocket.StatusCode;
import org.ajdeveloppement.commons.net.http.websocket.WebSocketInputStream;
import org.ajdeveloppement.commons.net.http.websocket.WebSocketInputStreamListener;
import org.ajdeveloppement.commons.net.http.websocket.WebSocketOutputStream;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode;
import org.ajdeveloppement.webserver.HttpReturnCode.ReturnCode;
import org.ajdeveloppement.webserver.HttpSession;
import org.ajdeveloppement.webserver.data.Request;

public class WebSocketResponse extends HttpResponse implements WebSocketInputStreamListener {
	
	private static final String MAGIC_HANDSHAKE_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; //$NON-NLS-1$
	
	private HttpRequest httpRequest;
	private List<WebSocketListener> listeners = new ArrayList<>();
	
	private boolean close = true;
	private OpCode lastMessageType;
	private WebSocketInputStream wsInputStream;
	private WebSocketOutputStream wsOutputStream;
	
	public WebSocketResponse(HttpRequest httpRequest) throws IOException {
		super(HttpReturnCode.ClientError.BadRequest, null, null, 0);
		
		this.httpRequest = httpRequest;
		httpRequest.getHttpSession().setSocketTimeout(0);
	}
	
	private void waitMessage() throws IOException {
		wsInputStream = new WebSocketInputStream(httpRequest.getHttpSession().getInputStream());
		wsInputStream.addWebSocketInputStreamListener(this);
		while (!close && !httpRequest.getHttpSession().getHttpServer().isCancellationRequested()) {
			try {
				lastMessageType = wsInputStream.getNextMessage();
				fireMessage(lastMessageType);
			} catch(IOException ex) {
				close = true;
				fireConnectionClosed();
			}
		}
	}

	private void sendPong() throws IOException {
		if(wsOutputStream != null && !close) {
			wsOutputStream.sendServiceFrame(OpCode.PONG, null, null);
		}
	}
		
	@SuppressWarnings("nls")
	private String getHandshakeResponse(String handshakeKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String response = handshakeKey + MAGIC_HANDSHAKE_UUID;
		
		byte[] digest = MessageDigest.getInstance("SHA-1").digest(response.getBytes("US-ASCII"));
		response = Base64.getEncoder().encodeToString(digest);
		
		return response;
	}

	private void fireMessage(OpCode messageType) {
		for(WebSocketListener listener : listeners)
			listener.messageReceived(messageType);
	}
	
	private void fireConnectionReady() {
		for(WebSocketListener listener : listeners)
			listener.connectionReady();
	}
	
	private void fireConnectionClosed() {
		for(WebSocketListener listener : listeners)
			listener.connectionClosed();
	}
	
	public void addWebSocketListener(WebSocketListener listener) {
		if(!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void removeWebSocketListener(WebSocketListener listener) {
		if(listeners.contains(listener))
			listeners.remove(listener);
	}
	
	public OpCode getLastMessageType() {
		return lastMessageType;
	}
	
	public WebSocketInputStream getWebSocketInputStream() {
		return wsInputStream;
	}
	
	public WebSocketOutputStream getWebSocketOutputStream() {
		return wsOutputStream;
	}
	
	public void sendMessage(String message) throws IOException {
		if(wsOutputStream != null && !close) {
			synchronized (wsOutputStream) {
				wsOutputStream.startMessage(OpCode.TEXT);
				wsOutputStream.write(message.getBytes("UTF-8")); //$NON-NLS-1$
				wsOutputStream.endMessage();
			}
		}
	}
	
	public void sendMessage(byte[] data) throws IOException {
		if(wsOutputStream != null && !close) {
			synchronized (wsOutputStream) {
				wsOutputStream.startMessage(OpCode.BINARY);
				wsOutputStream.write(data);
				wsOutputStream.endMessage();
			}
		}
	}
	
	public String readTextMessage() throws IOException {
		if(wsInputStream != null && !close) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(wsInputStream, Charset.forName("UTF-8"))); //$NON-NLS-1$
			StringBuilder strBuilder = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				strBuilder.append(line);
			}
			
			return strBuilder.toString();
		}
		
		return null;
	}
	
	public void close() throws IOException {
		if(wsOutputStream != null && !close) {
			close = true;
			wsOutputStream.sendServiceFrame(OpCode.CLOSING, StatusCode.NormalClosure, null);
		}
	}
	
	public boolean isClosed() {
		return close;
	}
	
	@SuppressWarnings("nls")
	@Override
	public void send(OutputStream outputStream, Request request)
			throws IOException {
		try {
			boolean handshakeError = false;
			String handshakeKey = httpRequest.getHeaderValues().get("sec-websocket-key");
			String handshakeResponse = "";
			try {
				handshakeResponse = getHandshakeResponse(handshakeKey);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				handshakeError = true;
			}
			
			if(handshakeError) {
				super.send(outputStream, request);
			} else {
				
				SimpleDateFormat gmtFrmt = new SimpleDateFormat(
						"E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US); //$NON-NLS-1$
				gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
			
				ReturnCode code = HttpReturnCode.Information.SwitchingProtocols;
				
				PrintWriter writer = new PrintWriter(outputStream);
				writer.print(String
						.format("%s %s %s", PROTOCOL, code.getCode(), code.getDescription())); //$NON-NLS-1$
				writer.print(CRLF);
				
				writer.print(createHeaderProperty("Upgrade", "WebSocket"));
				writer.print(createHeaderProperty("Connection", "Upgrade"));
				writer.print(createHeaderProperty("Sec-WebSocket-Accept", handshakeResponse));
				//writer.print(createHeaderProperty("Sec-WebSocket-Protocol", "chat"));
				
				if(httpRequest.getHeaderValues().get("origin") != null)
					writer.print(createHeaderProperty("Access-Control-Allow-Origin", httpRequest.getHeaderValues().get("origin")));
				
				writer.print(createHeaderProperty("Access-Control-Allow-Headers", "content-type"));
				writer.print(createHeaderProperty("Access-Control-Allow-Headers", "authorization"));
				writer.print(createHeaderProperty("Access-Control-Allow-Headers", "x-websocket-extensions"));
				writer.print(createHeaderProperty("Access-Control-Allow-Headers", "x-websocket-version"));
				writer.print(createHeaderProperty("Access-Control-Allow-Headers", "x-websocket-protocol"));
				writer.print(createHeaderProperty("Access-Control-Allow-Credentials", "true"));
				writer.print(createHeaderProperty("Date", gmtFrmt.format(new Date()))); //$NON-NLS-1$
				writer.print(createHeaderProperty("Server", SERVER_NAME)); //$NON-NLS-1$
				
				writer.print(CRLF);
				writer.flush();
				
				try {
					wsOutputStream = new WebSocketOutputStream(httpRequest.getHttpSession().getOutputStream());
					close = false;
					fireConnectionReady();
					waitMessage();
				} catch(EOFException | SocketException e) {
					/* Considere EOF as a normal case */
					e.printStackTrace();
				}
				
				
			}
		} finally {
			close = true;
			
			fireConnectionClosed();
		}
	}

	@Override
	public void frameReceived(FrameHeader frameHeader) {
		if(frameHeader.getOpCode() == OpCode.PING) {
			try {
				sendPong();
			} catch (IOException e) {
			}
		} else if(frameHeader.getOpCode() == OpCode.CLOSING) {
			close = true;
		}
	}
}
