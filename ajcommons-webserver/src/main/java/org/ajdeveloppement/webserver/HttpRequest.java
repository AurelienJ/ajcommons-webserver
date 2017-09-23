/*
 * Créé le 11 mai 2015 à 12:35:44 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2015 - Aurélien JEOFFRAY
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
import java.util.Map;
import java.util.TreeMap;

import org.ajdeveloppement.webserver.data.Request;

/**
 * Represent an http request
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public class HttpRequest {
	private HttpSession httpSession;
	
	private Request request;
	
	private HttpMethod requestMethod;
	private String requestUrl;
	private String requestUri;
	private String protocol;
	
	private Map<String, String> headerValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, String> urlParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, String> cookiesParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, String> postParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	
	/**
	 * @param httpSession
	 */
	public HttpRequest(HttpSession httpSession) {
		this.httpSession = httpSession;
	}
	/**
	 * @return the httpSession
	 */
	public HttpSession getHttpSession() {
		return httpSession;
	}
	/**
	 * @param httpSession the httpSession to set
	 */
	public void setHttpSession(HttpSession httpSession) {
		this.httpSession = httpSession;
	}
	/**
	 * @return the request
	 */
	public Request getRequest() {
		return request;
	}
	/**
	 * @param request the request to set
	 */
	public void setRequest(Request request) {
		this.request = request;
	}
	/**
	 * @return the requestMethod
	 */

	public HttpMethod getRequestMethod() {
		return requestMethod;
	}
	/**
	 * @param requestMethod the requestMethod to set
	 */

	public void setRequestMethod(HttpMethod requestMethod) {
		this.requestMethod = requestMethod;
	}
	/**
	 * @return the requestUrl
	 */

	public String getRequestUrl() {
		return requestUrl;
	}
	/**
	 * @param requestUrl the requestUrl to set
	 */

	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}
	/**
	 * @return the requestUri
	 */

	public String getRequestUri() {
		return requestUri;
	}
	/**
	 * @param requestUri the requestUri to set
	 */

	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}
	/**
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}
	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	/**
	 * @return the headerValues
	 */
	public Map<String, String> getHeaderValues() {
		return headerValues;
	}
	/**
	 * @param headerValues the headerValues to set
	 */
	public void setHeaderValues(Map<String, String> headerValues) {
		this.headerValues = headerValues;
	}
	/**
	 * @return the urlParameters
	 */
	public Map<String, String> getUrlParameters() {
		return urlParameters;
	}
	/**
	 * @param urlParameters the urlParameters to set
	 */
	public void setUrlParameters(Map<String, String> urlParameters) {
		this.urlParameters = urlParameters;
	}
	/**
	 * @return the cookiesParameters
	 */
	public Map<String, String> getCookiesParameters() {
		return cookiesParameters;
	}
	/**
	 * @param cookiesParameters the cookiesParameters to set
	 */
	public void setCookiesParameters(Map<String, String> cookiesParameters) {
		this.cookiesParameters = cookiesParameters;
	}
	/**
	 * @return the postParameters
	 */
	public Map<String, String> getPostParameters() {
		return postParameters;
	}
	/**
	 * @param postParameters the postParameters to set
	 */
	public void setPostParameters(Map<String, String> postParameters) {
		this.postParameters = postParameters;
	}

	
	/**
	 * indicates if remote session host accept or not gzip encoding response.
	 * 
	 * @return <code>true</code> if client accept gzip encoding body response
	 */
	public boolean acceptGzipEncoding() {
		return headerValues.containsKey("accept-encoding") && headerValues.get("accept-encoding").toLowerCase().contains("gzip"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * return the content length of request or -1 if unknown
	 * 
	 * @return the content length of request
	 */
	@SuppressWarnings("nls")
	public long getContentLength() {
		if(headerValues.containsKey("content-length"))
			return Long.parseLong(headerValues.get("content-length"));
		return -1;
	}
	
	/**
	 * Return true if request is a POST request with "multipart/form-data" disposition
	 * as RFC1867.
	 * 
	 * @return true if request is a POST request with "multipart/form-data"
	 */
	@SuppressWarnings("nls")
	public boolean isMutipartPostRequest() {
		if(headerValues.containsKey("content-type")) {
			return headerValues.get("content-type").toLowerCase().startsWith("multipart/form-data");
		}
		return false;
	}
	
	/**
	 * Return true if request is a POST request with "application/x-www-form-urlencoded" disposition
	 * 
	 * @return true if request is a POST request with "application/x-www-form-urlencoded"
	 */
	@SuppressWarnings("nls")
	public boolean isXWwwFormUrlEncoded() {
		if(headerValues.containsKey("content-type")) {
			return headerValues.get("content-type").toLowerCase().startsWith("application/x-www-form-urlencoded");
		}
		return false;
	}
	
	/**
	 * Return the client user agent
	 * 
	 * @return the client user agent
	 */
	public String getUserAgent() {
		if(headerValues.containsKey("user-agent")) //$NON-NLS-1$ 
			return headerValues.get("user-agent"); //$NON-NLS-1$
			
		return null;
	}
	
	/**
	 * Return client referer
	 * 
	 * @return the client referer
	 */
	public String getReferer() {
		if(headerValues.containsKey("referer")) //$NON-NLS-1$ 
			return headerValues.get("referer"); //$NON-NLS-1$
			
		return null;
	}
	
	/**
	 * Return the requested host
	 * 
	 * @return the requested host
	 */
	@SuppressWarnings("nls")
	public String getHost() {
		if(headerValues.containsKey("X-Forwarded-Host"))
			return headerValues.get("X-Forwarded-Host");
		return headerValues.get("host");
	}
}
