/*
 * Créé le 8 avr. 2015 à 18:30:28 pour ArcCompetition
 *
 * Copyright 2002-2015 - Aurélien JEOFFRAY
 *
 * http://arccompetition.ajdeveloppement.org
 *
 * *** CeCILL Terms *** 
 *
 * FRANCAIS:
 *
 * Ce logiciel est un programme informatique servant à gérer les compétions
 * de tir à l'Arc. 
 *
 * Ce logiciel est régi par la licence CeCILL soumise au droit français et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL telle que diffusée par le CEA, le CNRS et l'INRIA 
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
 * pris connaissance de la licence CeCILL, et que vous en avez accepté les
 * termes.
 *
 * ENGLISH:
 * 
 * This software is a computer program whose purpose is to manage the young special archery
 * tournament.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
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
 * knowledge of the CeCILL license and that you accept its terms.
 *
 *  *** GNU GPL Terms *** 
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.ajdeveloppement.webserver.services.webapi;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode;
import org.ajdeveloppement.webserver.ResourcesSelector;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;

/**
 * Represent the context of an http request
 * 
 * @author Aurélien JEOFFRAY
 *
 */
public class HttpContext {

	private static ThreadLocal<HttpContext> currentThreadContext = new ThreadLocal<>();
	/**
	 * The associate http request
	 */
	private HttpRequest httpRequest;
	
	/**
	 * Annotations filter associate to Http request
	 */
	private Annotation filterAnnotation;
	
	/**
	 * Context MetaData
	 */
	private Map<Object, Object> metadatas = new HashMap<>();
	
	/**
	 * The http return code
	 */
	private HttpReturnCode.ReturnCode returnCode = HttpReturnCode.Success.OK;
	
	/**
	 * The http response mime type
	 */
	private String mimeType = "application/json"; //$NON-NLS-1$
	
	/**
	 * The http response extra header
	 */
	private Map<String, String> headers = new HashMap<>();
	
	/**
	 * A custom httpresponse instance
	 */
	private HttpResponse customResponse;
	
	
	/**
	 * 
	 */
	public HttpContext(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
		currentThreadContext.set(this);
	}
	
	/**
	 * @return the httpRequest
	 */
	public HttpRequest getHttpRequest() {
		return httpRequest;
	}
	
	/**
	 * @param httpRequest the httpRequest to set
	 */
	public void setHttpRequest(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
	}
	
	public ResourcesSelector getResourcesSelector() {
		return httpRequest.getHttpSession().getHttpServer().getFileSelector();
	}

	/**
	 * @return the filtersAnnotations
	 */
	public Annotation getFilterAnnotation() {
		return filterAnnotation;
	}

	/**
	 * @param filtersAnnotations the filtersAnnotations to set
	 */
	public void setFilterAnnotation(Annotation filterAnnotation) {
		this.filterAnnotation = filterAnnotation;
	}

	/**
	 * @return the metadatas
	 */
	public Map<Object, Object> getMetadatas() {
		return metadatas;
	}
	
	@SuppressWarnings("unchecked")
	public <K,V> V getMetadata(K key) {
		return (V)metadatas.get(key);
	}
	
	public <K> void removeMetadata(K key) {
		metadatas.remove(key);
	}

	/**
	 * @param metadatas the metadatas to set
	 */
	public void setMetadatas(Map<Object, Object> metadatas) {
		this.metadatas = metadatas;
	}
	
	public <K, V> void addMetadata(K key, V value) {
		metadatas.put(key, value);
	}
	
	@SuppressWarnings("nls")
	public String getCurrentAppUrl() throws UnsupportedEncodingException {
		boolean isTlsSession = getHttpRequest().getHttpSession().isTlsSession();
		String host = "http" + (isTlsSession ? "s" : "") + "://" 
				+ getHttpRequest().getHost();
		
//		int localPort = getHttpRequest().getHttpSession().getLocalPort();
//		if((!isTlsSession && localPort != 80) || (isTlsSession && localPort != 443))
//			host += ":" + localPort;
		
		Alias alias = getHttpRequest().getHttpSession().getHttpServer().getFileSelector().selectAlias(getHttpRequest());
		if(alias != null)
			host = host + alias.getAliasPrefix();
		
		return host;
	}

	/**
	 * @return returnCode
	 */
	public HttpReturnCode.ReturnCode getReturnCode() {
		return returnCode;
	}
	/**
	 * @param returnCode returnCode à définir
	 */
	public void setReturnCode(HttpReturnCode.ReturnCode returnCode) {
		this.returnCode = returnCode;
	}
	/**
	 * @return mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}
	/**
	 * @param mimeType mimeType à définir
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * @param property
	 * @param value
	 */
	public void addHeader(String property, String value) {
		headers.put(property, value);
	}

	/**
	 * @return the customResponse
	 */
	public HttpResponse getCustomResponse() {
		return customResponse;
	}

	/**
	 * @param customResponse the customResponse to set
	 */
	public void setCustomResponse(HttpResponse customResponse) {
		this.customResponse = customResponse;
	}

	/**
	 * @return the currentThreadContext
	 */
	public static HttpContext getCurrentThreadContext() {
		return currentThreadContext.get();
	}
}
