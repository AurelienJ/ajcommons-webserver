/*
 * Créé le 01 nov. 2013 à 21:36:49 pour AjCommons (Bibliothèque de composant communs)
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
package org.ajdeveloppement.webserver.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.ajdeveloppement.commons.CollectionsUtils;
import org.ajdeveloppement.commons.ExceptionUtils;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpRequestProcessor;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode.ClientError;
import org.ajdeveloppement.webserver.HttpReturnCode.ReturnCode;
import org.ajdeveloppement.webserver.HttpReturnCode.ServerError;
import org.ajdeveloppement.webserver.HttpServer;

/**
 * @author aurelien
 *
 */
public class ExtensibleHttpRequestProcessor implements HttpRequestProcessor {
	
	private List<RequestProcessor> services = new ArrayList<>();
	private String[] servicesOrder;
	private HttpServer server;

	/**
	 * 
	 */
	public ExtensibleHttpRequestProcessor() {
		this(null);
	}
	
	public ExtensibleHttpRequestProcessor(String[] servicesOrder) {
		this.servicesOrder = servicesOrder;
	}
	
	private void loadService() {
		services.clear();
		
		Map<String, RequestProcessor> requestProcessors = CollectionsUtils.asMap(
				ServiceLoader.load(RequestProcessor.class),
				element -> element.getClass().getName());
		
		List<RequestProcessor> servicesOrdered = new ArrayList<RequestProcessor>();
		if(servicesOrder != null && servicesOrder.length > 0) {
			for(String serviceName : servicesOrder) {
				RequestProcessor service = requestProcessors.get(serviceName);
				if(service != null) {
					servicesOrdered.add(service);
					requestProcessors.remove(serviceName);
				}
			}
		}
		
		for(RequestProcessor requestProcessor : requestProcessors.values())
			servicesOrdered.add(0,requestProcessor);
		
		for(RequestProcessor requestProcessor : servicesOrdered) {
			try {
				services.add(requestProcessor);
				
				requestProcessor.init(server);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("nls")
	private HttpResponse getErrorResponse(HttpRequest httpRequest, ReturnCode returnCode, String errorContent) {
		for(RequestProcessor requestProcessor : services) {
			HttpResponse response = requestProcessor.serveErrorResponse(httpRequest, returnCode, errorContent);
			if(response != null)
				return response;
		}
		
		return new HttpResponse(returnCode, "text/plain; charset=utf-8", returnCode.getDescription() + ": " + errorContent); 
	}
	
	/**
	 * @return the services
	 */
	public List<RequestProcessor> getServices() {
		return services;
	}
	
	public RequestProcessor getService(String name) {
		Optional<RequestProcessor> service = services.stream().filter(s -> s.getClass().getName().equals(name)).findFirst();
		if(service.isPresent())
			return service.get();
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> serviceClass) {
		Optional<RequestProcessor> service = services.stream().filter(s -> s.getClass() == serviceClass).findFirst();
		if(service.isPresent())
			return (T)service.get();
		return null;
	}
	
	/**
	 * @return the server
	 */
	@Override
	public HttpServer getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	@Override
	public void setServer(HttpServer server) {
		this.server = server;
		loadService();
	}

	/* (non-Javadoc)
	 * @see org.ajdeveloppement.webserver.HttpRequestProcessor#serve(org.ajdeveloppement.webserver.HttpSession)
	 */
	@Override
	public HttpResponse serve(HttpRequest httpRequest) {
		try {
			for(RequestProcessor requestProcessor : services) {
				if(requestProcessor.canServe(httpRequest)) {
					HttpResponse response = requestProcessor.serve(httpRequest);
					if(response != null)
						return response;
				}
			}
			
			return getErrorResponse(httpRequest, ClientError.NotFound, httpRequest.getRequestUrl());
		} catch(Exception e) {
			e.printStackTrace();
			
			String responseMessage = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage();
			if(httpRequest != null && httpRequest.getRequest() != null) {
				if(responseMessage == null)
					responseMessage = ""; //$NON-NLS-1$
				httpRequest.getRequest().setResponseCode(ServerError.InternalServerError.getCode());
				httpRequest.getRequest().setResponseLength(responseMessage.length());

				httpRequest.getRequest().setException(responseMessage + "\n" + ExceptionUtils.toString(e)); //$NON-NLS-1$
			}
			
			return getErrorResponse(httpRequest, ServerError.InternalServerError, responseMessage);
		}
	}
}
