/*
 * Créé le 11 oct. 2013 à 23:31:27 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2013 - Aurélien JEOFFRAY
 *
 * http://www.ajdeveloppement.org
 *
 * *** CeCILL-C Terms *** 
 *
 * FRANCAIS:
 *
 * Ce logiciel est régi par la licence CeCILL-C soumise au droit français et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL-C telle que diffusée par le CEA, le CNRS et l'INRIA 
 * sur le site "http://www.cecill.info".
 *
 * En contrepartie de l'accessibilité au code source et des droits de copie,
 * de modification et de redistribution accordés par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limitée.  Pour les mêmes raisons,
 * seule une responsabilité restreinte pèse sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les concédants successifs.
 *
 * A cet égard  l'attention de l'utilisateur est attirée sur les risques
 * associés au chargement,  à l'utilisation,  à la modification et/ou au
 * développement et à la reproduction du logiciel par l'utilisateur étant 
 * donné sa spécificité de logiciel libre, qui peut le rendre complexe à 
 * manipuler et qui le réserve donc à des développeurs et des professionnels
 * avertis possédant  des  connaissances  informatiques approfondies.  Les
 * utilisateurs sont donc invités à charger  et  tester  l'adéquation  du
 * logiciel à leurs besoins dans des conditions permettant d'assurer la
 * sécurité de leurs systèmes et ou de leurs données et, plus généralement, 
 * à l'utiliser et l'exploiter dans les mêmes conditions de sécurité. 
 * 
 * Le fait que vous puissiez accéder à cet en-tête signifie que vous avez 
 * pri connaissance de la licence CeCILL-C, et que vous en avez accepté les
 * termes.
 *
 * ENGLISH:
 * 
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package org.ajdeveloppement.webserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocket;
import javax.script.ScriptException;
import javax.xml.bind.JAXBException;

import org.ajdeveloppement.commons.ExceptionUtils;
import org.ajdeveloppement.commons.ObservableList;
import org.ajdeveloppement.commons.net.http.HttpInputStream;
import org.ajdeveloppement.commons.net.http.HttpPostMultipartInputStream;
import org.ajdeveloppement.webserver.HttpReturnCode.ClientError;
import org.ajdeveloppement.webserver.HttpReturnCode.Information;
import org.ajdeveloppement.webserver.HttpReturnCode.Success;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;
import org.ajdeveloppement.webserver.data.Request;

/**
 * Manage an HTTP session (An HTTP request or pool of HTTP request if keep-alive)
 * 
 * @author Aurélien JEOFFRAY
 *
 */
public class HttpSession {
	private static final long MAX_IN_MEMORY_POST = 1024 * 1024; //1 Mo

	private UUID idSession = UUID.randomUUID();
	private Socket sessionSocket;
	private HttpServer httpServer;
	
	private InetAddress remoteAddress;
	private int remotePort;
	
	private HttpRequest currentHttpRequest;
	
	//private Request request;
	private int keepAliveCount = 100;
	
	private static ExecutorService executor = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r);
		t.setName("SessionThread-" + UUID.randomUUID()); //$NON-NLS-1$
		t.setContextClassLoader(Thread.currentThread().getContextClassLoader());
		return t;
	});
	
	private static File tempFilePath = null; 
	private static int maxRequestLength = 8096; //8KB
	private static long maxHeaderLength = 8096; //8KB
	private static long maxPostSize = 1024 * 1024 * 150; //150MB
	
	private static ObservableList<HttpSession> activeSessions = new ObservableList<HttpSession>(Collections.synchronizedList(new ArrayList<HttpSession>()));
	
	/**
	 * Create a new http session for defined socket
	 * 
	 * @param httpServer the parent server instance
	 * @param socket the socket associate to session
	 */
	public HttpSession(HttpServer httpServer, Socket socket) {
		this.setHttpServer(httpServer);
		this.sessionSocket = socket;
	}
	
	/**
	 * Parse HTTP Request header
	 * 
	 * @param inReader the socket http inputstream 
	 * @return <code>true</code> if parse success, <code>false</code> if stream is close
	 * @throws IOException
	 * @throws HttpException
	 * @throws JAXBException 
	 */
	@SuppressWarnings("nls")
	private HttpRequest parseRequest(HttpInputStream inReader) throws IOException, HttpException, JAXBException {
		int headerLength = 0;
		boolean requestLine = true;  
		String line = null;

		String requestUrl = null;
		
		//headerValues.clear();
		
		boolean successParse = false;
		
		HttpRequest httpRequest = new HttpRequest(this);
		
		//For logging
		Request request = new Request();
		request.setDate(new Date());
		request.setRemoteAddress(getRemoteAddress().getHostAddress());
		
		httpRequest.setRequest(request);
		
		StringBuffer header = new StringBuffer();
		Map<String, String> headerValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, String> urlParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, String> cookiesParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		
		while((line = inReader.readLine((int)maxHeaderLength)) != null) {
			if(line.isEmpty()) //End of Header
				break;
			
			header.append(line + "\n"); //$NON-NLS-1$
			
			if(requestLine) {
				int separateurMethodUri = line.indexOf(" "); //$NON-NLS-1$
				if(separateurMethodUri > -1) {
					if(line.length() > maxRequestLength) {
						request.setHeader(header.toString());
						
						throw new HttpException(HttpReturnCode.ClientError.RequestURITooLong, 
								String.format("Max request size is %s byte", maxRequestLength), request);
					}
					
					String method = line.substring(0, separateurMethodUri);
					
					request.setMethod(method);
					
					HttpMethod requestMethod = HttpMethod.get(method);
					if(requestMethod == null) {
						request.setHeader(header.toString());
						
						throw new HttpException(HttpReturnCode.ClientError.BadRequest, 
								String.format("Invalid method \"%s\"", method), request);
					}
					
					httpRequest.setRequestMethod(requestMethod);
					
					String protocol = null;
					int separateurProtocol = line.lastIndexOf(' ');
					if(separateurProtocol < 0 || separateurProtocol < separateurMethodUri+1)
						separateurProtocol = line.length();
					else
						protocol = line.substring(separateurProtocol+1);
					requestUrl = line.substring(separateurMethodUri+1, separateurProtocol);
					
					httpRequest.setProtocol(protocol);
				} else {
					request.setHeader(header.toString());
					
					throw new HttpException(HttpReturnCode.ClientError.BadRequest, 
							"HTTP request is malformed (no method-uri space separator)", request);
				}
				
				requestLine = false;
			} else {
				headerLength += line.length();
				if(headerLength > getMaxHeaderLength()) {
					request.setHeader(header.toString());
					
					throw new HttpException(HttpReturnCode.ClientError.RequestHeaderFieldsTooLarge,
							null, request);
				}
				
				try {
					parseLineHeader(line, headerValues, cookiesParameters);
				} catch(HttpException ex) {
					//on enrichit l'exception avec l'entete de requete connu au momement
					//de la production de celle ci
					request.setHeader(header.toString());
					ex.setRequest(request);
					
					throw ex;
				}
			}
			
			successParse = true;
		}
		
		if(successParse) {
			httpRequest.setRequestUrl(requestUrl);
			httpRequest.setHeaderValues(headerValues);
			httpRequest.setCookiesParameters(cookiesParameters);
			
			RewriteUrlRules rewriteUrlRules  = httpServer.getFileSelector().getRewriteUrlRules(httpRequest);
			if(rewriteUrlRules != null) {
				Alias alias= httpServer.getFileSelector().selectAlias(httpRequest);
				if(alias != null)
					rewriteUrlRules.setBasePath(alias.getAliasPrefix());
				requestUrl = rewriteUrlRules.rewrite(requestUrl);
			
				httpRequest.setRequestUrl(requestUrl);
			}
			
			parseRequestUrl(httpRequest, urlParameters);
		} else {
			httpRequest = null;
		}
		
		if(headerValues.containsKey("X-Forwarded-For"))
			request.setRemoteAddress(headerValues.get("X-Forwarded-For"));
		
		request.setRequestLength(headerLength);
		if(httpRequest != null) {
			request.setHeader(header.toString());
			request.setUri(httpRequest.getRequestUri());
			request.setUrl(httpRequest.getRequestUrl());
			request.setUserAgent(httpRequest.getUserAgent());//
			request.setReferer(httpRequest.getReferer());
			request.setHost(httpRequest.getHost());
		}
		
		currentHttpRequest = httpRequest;
		
		return httpRequest;
	}
	
	/**
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	private void parseRequestUrl(HttpRequest httpRequest, Map<String, String> urlParameters) throws UnsupportedEncodingException {
		int separatorParamsUrl = httpRequest.getRequestUrl().indexOf('?');
		if(separatorParamsUrl > -1) {
			String[] entries = httpRequest.getRequestUrl().substring(separatorParamsUrl+1).split("&"); //$NON-NLS-1$
			for(String entry : entries) {
				int separatorKeyValue = entry.indexOf('=');
				if(separatorKeyValue > -1) {
					String key = URLDecoder.decode(entry.substring(0, separatorKeyValue), "UTF-8"); //$NON-NLS-1$
					String value = URLDecoder.decode(entry.substring(separatorKeyValue+1), "UTF-8"); //$NON-NLS-1$
					
					if(!urlParameters.containsKey(key))
						urlParameters.put(key, value);
				} else {
					if(!urlParameters.containsKey(entry))
						urlParameters.put(entry, ""); //$NON-NLS-1$
				}
			}
		}
		
		if(separatorParamsUrl < 0)
			separatorParamsUrl = httpRequest.getRequestUrl().length();
		String requestUri = URLDecoder.decode(httpRequest.getRequestUrl().substring(0, separatorParamsUrl), "UTF-8"); //$NON-NLS-1$
		
		httpRequest.setRequestUri(requestUri);
		httpRequest.setUrlParameters(urlParameters);
	}
	
	/**
	 * Parse d'une ligne d'entete
	 * 
	 * @param line
	 * @throws HttpException
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("nls")
	private void parseLineHeader(String line, 
			Map<String, String> headerValues, 
			Map<String, String> cookiesParameters) throws HttpException, UnsupportedEncodingException {
		int entryValueSeparator = line.indexOf(':');
		if(entryValueSeparator > -1) {
			String key = line.substring(0, entryValueSeparator);
			String value = line.substring(entryValueSeparator+1).trim();
			
			if(key.toLowerCase().equals("cookie")) { //$NON-NLS-1$
				parseCookies(value, cookiesParameters);
			}
			
			if(!headerValues.containsKey(key))
				headerValues.put(key, value);
		} else {
			throw new HttpException(HttpReturnCode.ClientError.BadRequest, "Malformed header line");
		}
	}
	
	/**
	 * 
	 * @param cookieHeader
	 * @throws UnsupportedEncodingException 
	 */
	private void parseCookies(String cookieHeader, Map<String, String> cookiesParameters) throws UnsupportedEncodingException {
		String[] cookies = cookieHeader.split("; "); //$NON-NLS-1$
		for(String cookie : cookies) {
			int separatorKeyValue = cookie.indexOf('=');
			if(separatorKeyValue > -1) {
				String key = URLDecoder.decode(cookie.substring(0, separatorKeyValue), "UTF-8"); //$NON-NLS-1$
				String value = URLDecoder.decode(cookie.substring(separatorKeyValue+1), "UTF-8"); //$NON-NLS-1$
				
				if(!cookiesParameters.containsKey(key))
					cookiesParameters.put(key, value);
			} else {
				if(!cookiesParameters.containsKey(cookie))
					cookiesParameters.put(cookie, ""); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * 
	 * @param inReader
	 * @throws IOException
	 * @throws HttpException
	 */
	private void parsePostContent(HttpInputStream inReader, HttpRequest httpRequest) throws IOException, HttpException {
		Map<String, String> postParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		long dataLength = 0;
		try {
			dataLength = httpRequest.getContentLength();
		} catch(NumberFormatException e) {
			throw new HttpException(ClientError.BadRequest);
		}
		
		if(dataLength > maxPostSize)
			throw new HttpException(ClientError.RequestEntityTooLarge);
		
		if(httpRequest.getRequest() != null)
			httpRequest.getRequest().setRequestLength(httpRequest.getRequest().getRequestLength() + dataLength);
		
		String contentType = httpRequest.getHeaderValues().get("content-type"); //$NON-NLS-1$
		
		if(contentType != null && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) { //$NON-NLS-1$
			if(dataLength > 0) {
				String postMessage = readContentAsString(Charset.forName("US-ASCII")); //$NON-NLS-1$
				
				String[] entries = postMessage.split("&"); //$NON-NLS-1$
				for(String entry : entries) {
					int separatorKeyValue = entry.indexOf('=');
					if(separatorKeyValue > -1) {
						String key = URLDecoder.decode(entry.substring(0, separatorKeyValue), "UTF-8"); //$NON-NLS-1$
						String value = URLDecoder.decode(entry.substring(separatorKeyValue+1), "UTF-8"); //$NON-NLS-1$
						
						if(!postParameters.containsKey(key))
							postParameters.put(key, value);
					} else {
						if(!postParameters.containsKey(entry))
							postParameters.put(entry, ""); //$NON-NLS-1$
					}
				}
				
				httpRequest.setPostParameters(postParameters);
			}
		}
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private ByteBuffer createPostTempFile() throws IOException,
			FileNotFoundException {
		ByteBuffer contentBuffer;
		File tempPostFile = File.createTempFile(idSession.toString(), "", tempFilePath); //$NON-NLS-1$
		try(RandomAccessFile randomAccessFile = new RandomAccessFile(tempPostFile, "rw")) { //$NON-NLS-1$
			byte[] buf = new byte[4096];
			long dataLength = getContentLength();
			long nbTotalRead = 0;
			int read = 0;
			while ((read = getInputStream().read(buf, 0, 4096)) > -1) {
				randomAccessFile.write(buf, 0, read);
				nbTotalRead += read;
				if(dataLength > -1 && nbTotalRead >= dataLength)
					break;
			}
			
			contentBuffer = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
			randomAccessFile.seek(0);

			return contentBuffer;
		}
	}
	
	/**
	 * Verify if client is authorized to access to ressources
	 * 
	 * @return null if access according or an http error response or auth token request else
	 * @throws HttpException 
	 */
	private HttpResponse checkAccess(HttpRequest httpRequest) throws HttpException {
		HttpResponse response = null;
		try {
			AccessVerifier accessVerifier = getHttpServer().getAccessVerifier();
			if(accessVerifier != null)
				response = accessVerifier.verifyAccess(this,httpRequest);
		} catch (ScriptException e) {
			throw new HttpException(HttpReturnCode.ServerError.InternalServerError, e.toString());
		}
		
		return response;
	}
	
	/**
	 * @param httpInStream
	 * @param httpRequest
	 * @return
	 * @throws HttpException
	 * @throws IOException
	 */
	@SuppressWarnings("nls")
	private HttpResponse processResponse(HttpInputStream httpInStream,
			HttpRequest httpRequest) throws HttpException, IOException {
		HttpResponse response;
		
		if(httpRequest.getHost() == null)
			response  = new HttpResponse(ClientError.BadRequest, "plain/text", "request without hostname (see RFC2616 section 14.23)");
		else
			response = checkAccess(httpRequest); //Contrôle d'acces

		//Si acces autorisé
		if(response == null) {
			//Cas particulier methode post
			if(httpRequest.getRequestMethod() == HttpMethod.POST) {
				if(httpRequest.getHeaderValues().containsKey("expect") && httpRequest.getHeaderValues().get("expect").toLowerCase().contains("100-continue")) {//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					long dataLength = 0;
					try {
						dataLength = Long.parseLong(httpRequest.getHeaderValues().get("content-length"));
					} catch(NumberFormatException e) {
						throw new HttpException(ClientError.BadRequest, "Invalid \"content-length\" value");
					}
					
					if(dataLength > maxPostSize) {
						throw new HttpException(ClientError.ExpectationFailed,  "\"content-length\" > " + maxPostSize);
					}
					
					response = new HttpResponse(Information.Continue, null, (InputStream)null);
					response.addHeader("Connection", "close");
				} else {
					parsePostContent(httpInStream, httpRequest);
				}
			}
			
			if(httpRequest.getRequestMethod() == HttpMethod.OPTIONS) {
				response = new HttpResponse(Success.NoContent, "text/plain; charset=utf-8", (InputStream)null); //$NON-NLS-1$
				response.addHeader("Allow", "GET, POST, HEAD");  //$NON-NLS-1$//$NON-NLS-2$
			} else {
				if(httpRequest.getRequestUri() != null) {
					if(httpServer.getRequestProcessor() != null)
						response = httpServer.getRequestProcessor().serve(httpRequest);
				}
				
				if(response == null) {
					response = new HttpResponse(Success.NoContent, "text/plain; charset=utf-8", (InputStream)null); //$NON-NLS-1$
				}
			}
		}
		return response;
	}
	
	/**
	 * @param out
	 * @param keepAlive
	 * @param response
	 * @param httpRequest
	 */
	private void sendResponse(boolean keepAlive,
			HttpResponse response, HttpRequest httpRequest) {
		if(response != null && httpRequest != null) {
			if(httpRequest.getRequestMethod() == HttpMethod.HEAD)
				response.setInputStream(null);
			response.setKeepAlive(keepAlive);
			if(keepAlive)
				response.setKeepAliveCount(keepAliveCount);
			try {
				if(!sessionSocket.isClosed() && !sessionSocket.isOutputShutdown() && !getHttpServer().isCancellationRequested()) {
					OutputStream out = sessionSocket.getOutputStream();
					
					response.send(out, httpRequest.getRequest());
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Return <code>true</code> if this is a request method that attempt a body part (POST, PUT, PATCH)
	 * 
	 * @param httpRequest the tested request
	 * @return <code>true</code> if this is a PUT, POST or PATCH request
	 */
	private boolean isBodyRequest(HttpRequest httpRequest) {
		boolean bodyRequest = httpRequest.getRequestMethod() == HttpMethod.POST
				|| httpRequest.getRequestMethod() == HttpMethod.PUT
				|| httpRequest.getRequestMethod() == HttpMethod.PATCH
				|| httpRequest.getRequestMethod() == HttpMethod.DELETE;
		
		return bodyRequest;
	}
	
	/**
	 * @return the activeConnection
	 */
	public static ObservableList<HttpSession> getActiveSessions() {
		return activeSessions;
	}

	/**
	 * @return the httpServer
	 */
	public HttpServer getHttpServer() {
		return httpServer;
	}

	/**
	 * @param httpServer the httpServer to set
	 */
	public void setHttpServer(HttpServer httpServer) {
		this.httpServer = httpServer;
	}
	
	/**
	 * Close the current http connection
	 */
	public void closeSession() {
		if(!sessionSocket.isClosed())
			try {
				sessionSocket.close();
			} catch (IOException e) { }
	}

	/**
	 * The network address of remote host
	 * 
	 * @return network address of remote host
	 */
	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * @return the remotePort
	 */
	public int getRemotePort() {
		return remotePort;
	}
	
	public int getLocalPort() {
		return sessionSocket.getLocalPort();
	}

	/**
	 * @return the currentHttpRequest
	 */
	public HttpRequest getCurrentHttpRequest() {
		return currentHttpRequest;
	}

	/**
	 * @param currentHttpRequest the currentHttpRequest to set
	 */
	public void setCurrentHttpRequest(HttpRequest currentHttpRequest) {
		this.currentHttpRequest = currentHttpRequest;
	}

	/**
	 * @return the requestMethodrequest
	 */
	@Deprecated
	public HttpMethod getRequestMethod() {
		return currentHttpRequest.getRequestMethod();
	}

	/**
	 * @return the requestUrl
	 */
	@Deprecated
	public String getRequestUrl() {
		return currentHttpRequest.getRequestUrl();
	}

	/**
	 * @return the requestUri
	 */
	@Deprecated
	public String getRequestUri() {
		return currentHttpRequest.getRequestUri();
	}

	/**
	 * @return the protocol
	 */
	@Deprecated
	public String getProtocol() {
		return currentHttpRequest.getProtocol();
	}

	/**
	 * @return the headerValues
	 */
	@Deprecated
	public Map<String, String> getHeaderValues() {
		return currentHttpRequest.getHeaderValues();
	}
	
	/**
	 * @return the urlParameters
	 */
	@Deprecated
	public Map<String, String> getUrlParameters() {
		return currentHttpRequest.getUrlParameters();
	}
	
	/**
	 * @return the cookiesParameters
	 */
	@Deprecated
	public Map<String, String> getCookiesParameters() {
		return currentHttpRequest.getCookiesParameters();
	}

	/**
	 * @return the postParameters
	 */
	@Deprecated
	public Map<String, String> getPostParameters() {
		return currentHttpRequest.getPostParameters();
	}
	
	/**
	 * indicates if remote session host accept or not gzip encoding response.
	 * 
	 * @return <code>true</code> if client accept gzip encoding body response
	 */
	@Deprecated
	public boolean acceptGzipEncoding() {
		return currentHttpRequest.acceptGzipEncoding();
	}
	
	/**
	 * return if client require a keep-alive socket connection or not
	 * 
	 * @return <code>true</code> if client require a keep-alive socket connection
	 */
	public boolean keepAliveRequire() {
		return currentHttpRequest.getHeaderValues().containsKey("connection") && currentHttpRequest.getHeaderValues().get("connection").toLowerCase().contains("keep-alive"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * return the content length of request or -1 if unknown
	 * 
	 * @return the content length of request
	 */
	@Deprecated
	public long getContentLength() {
		return currentHttpRequest.getContentLength();
	}
	
	/**
	 * Return true if request is a POST request with "multipart/form-data" disposition
	 * as RFC1867.
	 * 
	 * @return true if request is a POST request with "multipart/form-data"
	 */
	@Deprecated
	public boolean isMutipartPostRequest() {
		return currentHttpRequest.isMutipartPostRequest();
	}
	
	/**
	 * Return true if request is a POST request with "application/x-www-form-urlencoded" disposition
	 * 
	 * @return true if request is a POST request with "application/x-www-form-urlencoded"
	 */
	@Deprecated
	public boolean isXWwwFormUrlEncoded() {
		return currentHttpRequest.isXWwwFormUrlEncoded();
	}

	/**
	 * @return the keepAliveCount
	 */
	public int getKeepAliveCount() {
		return keepAliveCount;
	}

	/**
	 * @param keepAliveCount the keepAliveCount to set
	 */
	public void setKeepAliveCount(int keepAliveCount) {
		this.keepAliveCount = keepAliveCount;
	}

	/**
	 * Return the client user agent
	 * 
	 * @return the client user agent
	 */
	@Deprecated
	public String getUserAgent() {
		return currentHttpRequest.getUserAgent();
	}
	
	/**
	 * Return client referer
	 * 
	 * @return the client referer
	 */
	@Deprecated
	public String getReferer() {
		return currentHttpRequest.getReferer();
	}
	
	/**
	 * Return the requested host
	 * 
	 * @return the requested host
	 */
	@Deprecated
	public String getHost() {
		return currentHttpRequest.getHost();
	}
	
	/**
	 * Return if underlying is a clear connection or a tls secure connection
	 * 
	 * @return
	 */
	public boolean isTlsSession() {
		return sessionSocket instanceof SSLSocket;
	}
	
	public int getSocketTimeout() throws SocketException {
		return sessionSocket.getSoTimeout();
	}
	
	public void setSocketTimeout(int timeout) throws SocketException {
		sessionSocket.setSoTimeout(timeout);
	}

	/**
	 * Return the underling socket inputstream from client Host
	 * 
	 * @return the underling socket inputstream from client Host
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		return sessionSocket.getInputStream();
	}
	
	/**
	 * If request is a multipart POST, return an {@link HttpPostMultipartInputStream} to read post content
	 * 
	 * @return the underling http post content inputstream from client Host
	 * @throws IOException
	 */
	public HttpPostMultipartInputStream getHttpPostMultipartInputStream() throws IOException {
		return new HttpPostMultipartInputStream(new HttpInputStream(sessionSocket.getInputStream()));
	}
	
	public ByteBuffer readContentAsByteBuffer() throws FileNotFoundException, IOException {
		ByteBuffer contentBuffer = null;
		long dataLength = currentHttpRequest.getContentLength();
		
		if(dataLength > 0) {
			if(dataLength > MAX_IN_MEMORY_POST) {
				contentBuffer = createPostTempFile();
			} else {
				InputStream in = getInputStream();
				byte[] contentBytes = new byte[(int)dataLength];
				int offset = 0;
				int readBuffer = 4096;
				for(int read = in.read(contentBytes, offset, 4096); read != -1;) {
					offset += read;
					if(dataLength < offset + readBuffer)
						readBuffer = (int)(dataLength - offset);
				}
				
				contentBuffer = ByteBuffer.wrap(contentBytes);
			}
		} else {
			contentBuffer = createPostTempFile();
		}
		
		return contentBuffer;
	}
	
	/**
	 * Read the content part of request and return this as a string
	 * 
	 * @param charset the charset to convert binary input stream to string
	 * @return the body request as string
	 * @throws IOException
	 */
	public String readContentAsString(Charset charset)
			throws IOException {
		byte[] buffer = new byte[4096];
		StringBuilder postMessage = new StringBuilder();
		long dataLength = currentHttpRequest.getContentLength();
		InputStream inputStream = getInputStream();
		int nbRead = 0;
		long nbTotalRead = 0;
		while((nbRead = inputStream.read(buffer)) > -1) {
			postMessage.append(new String(Arrays.copyOfRange(buffer, 0, nbRead), charset));
			nbTotalRead += nbRead;
			if(dataLength > -1 && nbTotalRead >= dataLength)
				break;
		}
		return postMessage.toString();
	}
	
	/**
	 * Return the underling socket outputstream to client Host
	 * 
	 * @return the underling socket outputstream to client Host
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws IOException {
		return sessionSocket.getOutputStream();
	}
	
	/**
	 * @return the maxRequestLength
	 */
	public static int getMaxRequestLength() {
		return maxRequestLength;
	}

	/**
	 * @param maxRequestLength the maxRequestLength to set
	 */
	public static void setMaxRequestLength(int maxRequestLength) {
		HttpSession.maxRequestLength = maxRequestLength;
	}

	/**
	 * @return the maxHeaderLength
	 */
	public static long getMaxHeaderLength() {
		return maxHeaderLength;
	}

	/**
	 * @param maxHeaderLength the maxHeaderLength to set
	 */
	public static void setMaxHeaderLength(long maxHeaderLength) {
		HttpSession.maxHeaderLength = maxHeaderLength;
	}

	/**
	 * @return the maxPostSize
	 */
	public static long getMaxPostSize() {
		return maxPostSize;
	}

	/**
	 * @param maxPostSize the maxPostSize to set
	 */
	public static void setMaxPostSize(long maxPostSize) {
		HttpSession.maxPostSize = maxPostSize;
	}

	/**
	 * Start an http session thread for client socket
	 * 
	 * @param socket the socket associate to httpsession
	 */
	public static void openSession(HttpServer httpServer, final Socket socket) {
		
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				HttpSession session = new HttpSession(httpServer, socket);
				activeSessions.add(session);
				try {
					session.process();
				} finally {
					activeSessions.remove(session);
				}
			}
		});
	}
	
	/**
	 * Parse request and send response
	 */
	public void process() {
		if(sessionSocket != null && !sessionSocket.isClosed()) {
			remoteAddress = sessionSocket.getInetAddress();
			remotePort = sessionSocket.getPort();
			

			try {
				InputStream in = sessionSocket.getInputStream();
				
				HttpInputStream httpInStream = new HttpInputStream(in);

				boolean close = false;
				do {
					boolean keepAlive = false;
					HttpResponse response = null;
					HttpRequest httpRequest = null;
					Request request = null;
					Instant start = null;
					try {
						httpRequest = parseRequest(httpInStream);
						if(httpRequest == null)
							break;
						
						if(httpRequest.getRequestUrl() == null)
							continue;
						
						start = Instant.now();
						
						keepAlive = keepAliveRequire();
						
						response = processResponse(httpInStream, httpRequest);
					} catch (HttpException e) {
						String responseMessage = e.getReturnMessage() == null ? e.getError().getDescription() : e.getReturnMessage();

						if(httpRequest != null)
							request = httpRequest.getRequest();
						else if(e.getRequest() != null)
							request = e.getRequest();
						
						if(request != null) {
							request.setResponseCode(e.getError().getCode());
							request.setResponseLength(responseMessage.length());

							request.setException(responseMessage + "\n" + ExceptionUtils.toString(e)); //$NON-NLS-1$
						}
						
						response = new HttpResponse(e.getError(), "text/plain; charset=utf-8", responseMessage); //$NON-NLS-1$
						System.err.println(Instant.now().toString() + " - " + ExceptionUtils.toString(e)); //$NON-NLS-1$
					} catch (Exception e) {
						if(e instanceof SocketTimeoutException)
							throw (SocketTimeoutException)e;
						
						String responseMessage = ExceptionUtils.toString(e);
						if(httpRequest != null) {
							httpRequest.getRequest().setResponseCode(HttpReturnCode.ServerError.InternalServerError.getCode());
							httpRequest.getRequest().setResponseLength(responseMessage.length());
							
							httpRequest.getRequest().setException(ExceptionUtils.toString(e));
						}
						
						response = new HttpResponse(HttpReturnCode.ServerError.InternalServerError, "text/plain; charset=utf-8", responseMessage); //$NON-NLS-1$
						System.err.println(Instant.now().toString() + " - " + ExceptionUtils.toString(e)); //$NON-NLS-1$
					}
					
					//if a body request fail, the body content 
					//is not read a stream can't received any new request because this implied
					//read body content before => we close tcp session stream
					if(keepAlive && isBodyRequest(httpRequest) && response.getReturnCode().getCode()>=400) {
						keepAlive = false;
						response.setKeepAlive(false);
					}
					
					sendResponse(keepAlive, response, httpRequest);
					
					if((httpRequest != null && httpRequest.getRequest() != null) || request != null) {
						
						if(httpRequest != null)
							request = httpRequest.getRequest();
						if(start != null) {
							long duration = ChronoUnit.MILLIS.between(start,Instant.now());
							request.setDuration(duration);
						}
						getHttpServer().saveLogEntry(request);
					}
					
					if(keepAlive)
						keepAliveCount--;
					
					if(getHttpServer().isCancellationRequested() || !keepAlive 
							|| keepAliveCount == 0 || httpRequest.getRequestUrl() == null)
						close = true;
				} while(!close && !sessionSocket.isClosed());
				
				
			} catch (SocketTimeoutException e) {
				//On ignore car c'est une interruption normal
			} catch (IOException e) {
				System.err.println(Instant.now().toString() + " - " + e.getLocalizedMessage()); //$NON-NLS-1$
			} finally {
				closeSession();
			}
		}
	}
}
