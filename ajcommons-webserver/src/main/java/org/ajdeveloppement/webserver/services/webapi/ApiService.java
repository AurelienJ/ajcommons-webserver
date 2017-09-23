/*
 * Créé le 31 juil. 2014 à 13:05:50 pour ArcCompetition
 *
 * Copyright 2002-2014 - Aurélien JEOFFRAY
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ajdeveloppement.commons.ExceptionUtils;
import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.webserver.HttpException;
import org.ajdeveloppement.webserver.HttpMethod;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.ResourcesSelector;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;
import org.ajdeveloppement.webserver.ResourcesSelector.Host;
import org.ajdeveloppement.webserver.services.RestartableRequestProcessor;
import org.ajdeveloppement.webserver.services.webapi.Container.RouteMatcher;
import org.ajdeveloppement.webserver.services.webapi.annotations.Body;
import org.ajdeveloppement.webserver.services.webapi.annotations.HttpServiceId;
import org.ajdeveloppement.webserver.services.webapi.annotations.UrlParameter;
import org.ajdeveloppement.webserver.services.webapi.helpers.Converter;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * @author Aurélien JEOFFRAY
 *
 */
public class ApiService implements RestartableRequestProcessor {

	private HttpServer server;
	private ResourcesSelector fileSelector;
	
	private Map<String, List<ApiApplication>> applications = new HashMap<>();
	private Map<String, Container> containers = new HashMap<>();
	private Map<Class<?>, RequestFilter> filters = new HashMap<>();
	private Map<Class<?>, ResponseHandler> responseHandlers = new HashMap<>();
	private ResponseHandler defaultResponseHandler = new DefaultResponseHandler();
	private Map<Class<?>, BodyParamHandler> typeBodyParamHandlers = new HashMap<>();
	private Map<String, BodyParamHandler> mimeBodyParamHandlers = new HashMap<>();
	private BodyParamHandler defaultBodyHandler = new DefaultBodyParamHandler();
	private Map<Container, Injector> injectors = new WeakHashMap<>();
	
	public ApiService() {
	}

	private static Pattern charsetPattern = Pattern.compile("charset=([^;]+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	private String getUriWithoutAliasPrefix(HttpRequest httpRequest) {
		String uri = httpRequest.getRequestUri();

		ResourcesSelector fileSelector = httpRequest.getHttpSession().getHttpServer().getFileSelector();
		Host host = fileSelector.selectHost(httpRequest);
		if (host != null) {
			Alias alias = fileSelector.selectAlias(httpRequest);
			if (alias != null)
				uri = alias.supressAlias(uri);
		}

		return uri;
	}

	@SuppressWarnings("nls")
	private String getContainerKey(HttpRequest httpRequest) {
		Host host = fileSelector.selectHost(httpRequest);

		if (host != null) {
			Alias alias = fileSelector.selectAlias(httpRequest);

			return (host.getHostPattern() != null ? host.getHostPattern() : "*")
					+ (alias != null ? alias.getAliasPrefix() : "");
		}

		return null;
	}

	private Container getContainer(HttpRequest httpRequest) {
		String key = getContainerKey(httpRequest);
		if (key != null)
			return getContainer(key);

		return null;
	}
	
	/**
	 * @param context
	 * @param ids
	 * @param urlParameters
	 * @param methodService
	 * @return
	 * @throws Exception
	 */
	private Object[] getRequestParameters(HttpContext context, String[] ids, Map<String, String> urlParameters,
			Method methodService) throws Exception {
		Object[] params = null;
		
		// Lecture des paramètres de la requête
		if (methodService.getParameterCount() != 0) {
			params = new Object[methodService.getParameterCount()];

			Arrays.fill(params, null);

			int idParameterIndex = 0;
			for (int i = 0; i < methodService.getParameterCount(); i++) {
				Parameter parameter = methodService.getParameters()[i];
				Annotation[] parametersAnnotation = parameter.getAnnotations();

				Class<?> parameterType = methodService.getParameterTypes()[i];
				if (parametersAnnotation != null && parametersAnnotation.length > 0) {
					if (parameter.isAnnotationPresent(Body.class)) {
						String bodyRequestEncoding = "UTF-8"; //$NON-NLS-1$
						String bodyContentType = "application/json"; //$NON-NLS-1$

						if (context.getHttpRequest().getHeaderValues().containsKey("content-type")) { //$NON-NLS-1$
							String contentType = context.getHttpRequest().getHeaderValues()
									.get("content-type"); //$NON-NLS-1$
							int sepCharset = contentType.indexOf(";"); //$NON-NLS-1$
							if (sepCharset > -1)
								bodyContentType = contentType.substring(0, sepCharset).trim();
							else
								bodyContentType = contentType.trim();

							Matcher m = charsetPattern.matcher(bodyContentType);
							if (m.matches()) {
								bodyRequestEncoding = m.group(1).toUpperCase();
							}
						}

						BodyParamHandler bodyHandler = typeBodyParamHandlers
								.get(methodService.getParameterTypes()[i]);

						if (bodyHandler == null)
							bodyHandler = mimeBodyParamHandlers.get(bodyContentType);

						if (bodyHandler == null)
							bodyHandler = defaultBodyHandler;

						if (bodyHandler != null) {
							params[i] = bodyHandler.getBodyParameter(context,
									methodService.getParameterTypes()[i], bodyContentType,
									bodyRequestEncoding);
						}
					} else if (parameter.isAnnotationPresent(UrlParameter.class)) {
						String key = parameter.getAnnotation(UrlParameter.class).value();
						if (key.isEmpty())
							key = parameter.getName();

						if (urlParameters.containsKey(key)) {
							String strValue = urlParameters.get(key);

							params[i] = Converter.parse(parameterType, strValue);
						} else if (parameterType.isPrimitive()) {
							if (parameterType != Boolean.TYPE)
								params[i] = 0;
							else
								params[i] = false;
						}
					} else if (parameter.isAnnotationPresent(HttpServiceId.class)) {
						int index = parameter.getAnnotation(HttpServiceId.class).value();
						if (index == -1)
							index = idParameterIndex;

						if (ids != null && ids.length > index && ids[index] != null)
							params[i] = Converter.parse(parameterType, ids[index]);
						else if (parameterType.isPrimitive()) {
							if (parameterType != Boolean.TYPE)
								params[i] = 0;
							else
								params[i] = false;
						}
						idParameterIndex++;
					}
				} else if (HttpContext.class.isAssignableFrom(parameterType)) {
					params[i] = context;
				}
			}
		}
		return params;
	}
	
	/**
	 * @param container
	 * @return
	 * @throws Exception 
	 */
	private Injector getInjector(Container container) throws Exception {
		synchronized (container) {
			Injector injector = injectors.get(container);
			if(injector == null) {
				injector = LifecycleInjector.builder().withModules(container.getInjectorModules()).build().createInjector();
				LifecycleManager manager = injector.getInstance(LifecycleManager.class);

			    manager.start();
			    
				injectors.put(container, injector);
			}
			return injector;
		}
	}
	
	/**
	 * @param context
	 * @param methodService
	 * @return
	 * @throws Exception 
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Object getControllerInstance(HttpContext context, Container container, Method methodService) throws Exception {
		Object controller = null;
		if(!Modifier.isStatic(methodService.getModifiers())) {
			Class<?> controllerClass = methodService.getDeclaringClass();
			
			Injector injector = getInjector(container);
						
			controller = injector.createChildInjector(new HttpContextModule(context)).getInstance(controllerClass);
//			
//			Constructor<?> constructor = controllerClass.getConstructors()[0];
//			Object[] constructorParams = new Object[constructor.getParameterCount()];
//			if(constructor.getParameterCount() > 0) {
//				Arrays.fill(constructorParams, null);
//				
//				Parameter[] parameters = constructor.getParameters();
//				for(int i = 0; i < constructor.getParameterCount(); i++) {
//					if(HttpContext.class.isAssignableFrom(parameters[i].getType())) {
//						constructorParams[i] = context; 
//					} else {
//						constructorParams[i] = Injectors.build(parameters[i].getType()).create();
//					}
//				}
//			}
//			
//			controller = constructor.newInstance(constructorParams);
//			
//			for(Field field : controllerClass.getDeclaredFields()) {
//				if(field.isAnnotationPresent(Inject.class)) {
//					field.setAccessible(true);
//					if(HttpContext.class.isAssignableFrom(field.getType())) {
//						field.set(controller, context);
//					} else {
//						field.set(controller, ;
//					}
//				}
//			}
		}
		return controller;
	}

	/**
	 * Invoke method controller associate with endPointKey
	 * 
	 * @param context
	 *            the http request context
	 * @param endPointKey
	 *            the endpoint service key
	 * @param ids
	 *            key of requested element
	 * @return response content
	 * 
	 * @throws Exception
	 */
	private HttpResponse invokeService(HttpContext context, String endPointKey, String[] ids) throws Exception {
		Object responseObject = null;

		Map<String, String> urlParameters = context.getHttpRequest().getUrlParameters();

		Container container = getContainer(context.getHttpRequest());
		if (container == null)
			return null;

		HttpResponse response = null;

		ClassLoader initialThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// Utilisation du classloader propre au container
			Thread.currentThread().setContextClassLoader(container.getClassLoader());

			// Récupération de la clé rest
			long nbKeySeparator = endPointKey.chars().filter(c -> c == '/').count();
			long nbIdParams = ids.length;
			if (nbIdParams > 0 && ids[(int) nbIdParams - 1] == null)
				nbIdParams -= 1;
			if (nbIdParams > nbKeySeparator) {
				endPointKey += "/id"; //$NON-NLS-1$
			}

			// récupération des méthodes de traitements disponible pour l'url
			Map<HttpMethod, Method> serviceMethod = container.getEndpointsServices().get(endPointKey);
			if (serviceMethod != null) {
				// récupération de la méthode de traitement associé au verbe
				Method methodService = serviceMethod.get(context.getHttpRequest().getRequestMethod());
				if (methodService != null) {
					for(Annotation annotation : methodService.getAnnotations()) {
						RequestFilter requestFilter = filters.get(annotation.annotationType());
						if(requestFilter != null) {
							context.setFilterAnnotation(annotation);
							Injector injector = getInjector(container);
							
							injector.injectMembers(requestFilter);
							
							response = requestFilter.filter(context);
							
							if(response != null)
								break;
						}
					}
					
					if(response == null) {
						//récupération des paramètres
						Object[] params = getRequestParameters(context, ids, urlParameters, methodService);
						
						//instanciation du controller si nécessaire et injection de dépendance
						Object controller = getControllerInstance(context, container, methodService);
	
						// appel de la methode de traitement et production du
						// résultat
						responseObject = methodService.invoke(controller, params);
					}
				}
			}

			if (responseObject != null) {
				ResponseHandler handler = responseHandlers.get(responseObject.getClass());
				if (handler == null)
					handler = defaultResponseHandler;

				HttpResponse tmpResponse = handler.getHttpResponse(context, responseObject);
				if (tmpResponse != null) {
					tmpResponse.setReturnCode(context.getReturnCode());
					if (context.getMimeType() != null && !context.getMimeType().isEmpty())
						tmpResponse.setMimeType(context.getMimeType());
					context.getHeaders().entrySet().forEach(e -> tmpResponse.addHeader(e.getKey(), e.getValue()));

					response = tmpResponse;
				}
			} else {
				if (context.getCustomResponse() != null) {
					response = context.getCustomResponse();
				}
			}

		} finally {
			Thread.currentThread().setContextClassLoader(initialThreadContextClassLoader);
		}

		return response;
	}

	private void startWebApps() {
		fileSelector.getWebAppsName().forEach(webAppDir -> startDeclaredWebAppAsync(webAppDir));
	}

	/**
	 * Add a container on api service.
	 * 
	 * @param key
	 *            the container domains space in form [domain
	 *            pattern]([/alias]). if there is no "domain pattern", use "*"
	 *            wildcard.
	 * 
	 * @param container
	 *            the controllers container
	 */
	public void addContainer(String key, Container container) {
		containers.put(key, container);
	}

	public void removeContainer(String key) {
		containers.remove(key);
	}

	public Container getContainer(String key) {
		return containers.get(key);
	}
	
	public void addRequestFilter(Class<?> annotation, RequestFilter filter) {
		filters.put(annotation, filter);
	}
	
	public void removeRequestFilter(Class<?> annotation) {
		filters.remove(annotation);
	}
	
	public void getRequestFilter(Class<?> annotation) {
		filters.get(annotation);
	}

	public void addResponseHandler(Class<?> type, ResponseHandler handler) {
		responseHandlers.put(type, handler);
	}

	public void removeResponseHandler(Class<?> type) {
		responseHandlers.remove(type);
	}

	public void addBodyParamHandler(Class<?> type, BodyParamHandler handler) {
		typeBodyParamHandlers.put(type, handler);
	}

	public void removeBodyParamHandler(Class<?> type) {
		typeBodyParamHandlers.remove(type);
	}

	public void addBodyParamHandler(String mimeType, BodyParamHandler handler) {
		mimeBodyParamHandlers.put(mimeType, handler);
	}

	public void removeBodyParamHandler(String mimeType) {
		mimeBodyParamHandlers.remove(mimeType);
	}

	/**
	 * @return the defaultResponseHandler
	 */
	public ResponseHandler getDefaultResponseHandler() {
		return defaultResponseHandler;
	}

	/**
	 * @param defaultResponseHandler
	 *            the defaultResponseHandler to set
	 */
	public void setDefaultResponseHandler(ResponseHandler defaultResponseHandler) {
		this.defaultResponseHandler = defaultResponseHandler;
	}

	/**
	 * @return the defaultBodyHandler
	 */
	public BodyParamHandler getDefaultBodyHandler() {
		return defaultBodyHandler;
	}

	/**
	 * @param defaultBodyHandler
	 *            the defaultBodyHandler to set
	 */
	public void setDefaultBodyHandler(BodyParamHandler defaultBodyHandler) {
		this.defaultBodyHandler = defaultBodyHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ajdeveloppement.webserver.services.RequestProcessor#init()
	 */
	@Override
	public void init(HttpServer server) {
		this.server = server;
		fileSelector = server.getFileSelector();

		startWebApps();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ajdeveloppement.webserver.services.RequestProcessor#canServe(org.
	 * ajdeveloppement.webserver.HttpSession)
	 */
	@Override
	public boolean canServe(HttpRequest httpRequest) {
		Container container = getContainer(httpRequest);

		String uri = getUriWithoutAliasPrefix(httpRequest);

		if (container != null && container.matchEntryPointPattern(uri).getMatcher().matches())
			return true;

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ajdeveloppement.webserver.services.RequestProcessor#serve(org.
	 * ajdeveloppement.webserver.HttpSession)
	 */
	@SuppressWarnings("nls")
	@Override
	public HttpResponse serve(HttpRequest httpRequest) throws HttpException {
		Container container = getContainer(httpRequest);
		if (container == null)
			return null;

		String uri = getUriWithoutAliasPrefix(httpRequest);
		// String uri = httpRequest.getRequestUri();
		RouteMatcher routeMatcher = container.matchEntryPointPattern(uri);
		if (routeMatcher.getMatcher().matches()) {
			Map<String, String> urlParameters = httpRequest.getUrlParameters();

			KeyIdPair keyIdPair = KeyIdPair.getKeyIdPair(routeMatcher.getMatcher(), routeMatcher.getSubPattern());

			String key = null;
			String[] ids = null;
			if (keyIdPair != null) {
				key = keyIdPair.getFullKey();
				ids = keyIdPair.getIds();
			}

			if ((key == null || key.isEmpty()) && urlParameters.containsKey("key"))
				key = urlParameters.get("key");

			if (key != null) {
				try {
					HttpContext context = new HttpContext(httpRequest);

					return invokeService(context, key, ids);
				} catch (Exception e) {
					System.err.println(ExceptionUtils.toString(e));

					return new HttpResponse(HttpReturnCode.ServerError.InternalServerError, "text/plain; charset=utf-8", //$NON-NLS-1$
							ExceptionUtils.toString(e));
				}
			}
		}

		return new HttpResponse(HttpReturnCode.ClientError.NotFound, "text/plain; charset=utf-8", "Unknown request"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public boolean isRunning(String baseContainer) {
		List<ApiApplication> apiApplications = applications.get(baseContainer);
		if (apiApplications != null && apiApplications.size() > 0) {
			return true;
		}
		return false;
	}

	@Override
	public Thread startDeclaredWebAppAsync(String baseContainer) {
		Thread t = Executors.defaultThreadFactory().newThread(() -> {
			try {
				stopDeclaredWebApp(baseContainer);
				
				applications.put(baseContainer, new ArrayList<>());
				
				ClassLoader wepAppClassLoader = fileSelector.getClassLoader(baseContainer);
				Thread.currentThread().setContextClassLoader(wepAppClassLoader);

				ServiceLoader.load(ApiApplication.class, wepAppClassLoader).forEach(apiApplication -> {
					apiApplication.setBaseContainerName(baseContainer);
					apiApplication.setServer(server);
					apiApplication.start();

					applications.get(baseContainer).add(apiApplication);
				});
			} catch (Exception e) {
				throw new UncheckedException(e);
			}
		});
		t.start();
		return t;
	}

	@Override
	public void stopDeclaredWebApp(String baseContainer) {
		List<ApiApplication> apiApplications = applications.get(baseContainer);
		if (apiApplications != null) {
			try {
				ClassLoader wepAppClassLoader = fileSelector.getClassLoader(baseContainer);
				
				Thread t = Executors.defaultThreadFactory().newThread(() -> {
					String containerKey = server.getFileSelector()
							.selectHostAliasKey(baseContainer);
					Container container = getContainer(containerKey);
					
					apiApplications.forEach(app -> {
						app.stop();
					});
					
					try {
						Injector injector = getInjector(container);
						if(injector != null) {
							LifecycleManager manager = injector.getInstance(LifecycleManager.class);
							if(manager != null)
								manager.close();
						}
					} catch (Exception e) {
						throw new UncheckedException(e);
					}
				});
				t.setContextClassLoader(wepAppClassLoader);
				t.start();
				t.join();
				
				applications.remove(baseContainer);
			} catch (IOException | InterruptedException e) {
				throw new UncheckedException(e);
			}
		}
	}

	private static class KeyIdPair {
		private String key;
		private String id;

		private KeyIdPair nextPair;

		/**
		 * @param key
		 * @param id
		 */
		public KeyIdPair(String key, String id) {
			this.key = key;
			this.id = id;
		}

		/**
		 * @return id
		 */
		@SuppressWarnings("unused")
		public String getId() {
			return id;
		}

		/**
		 * @param nextPair
		 *            nextPair à définir
		 */
		public void setNextPair(KeyIdPair nextPair) {
			this.nextPair = nextPair;
		}

		@SuppressWarnings("nls")
		public String getFullKey() {
			String key = this.key;
			if (nextPair != null) {
				key += "/" + nextPair.getFullKey();
			}

			return key;
		}

		public String[] getIds() {
			String[] ids = new String[] { this.id };
			if (nextPair != null) {
				ids = new String[nextPair.getIds().length + 1];
				ids[0] = this.id;
				System.arraycopy(nextPair.getIds(), 0, ids, 1, nextPair.getIds().length);
			}

			return ids;
		}

		@SuppressWarnings("nls")
		public static KeyIdPair getKeyIdPair(Matcher matcher, Pattern subEntryPointPattern) {
			KeyIdPair keyIdPair = null;
			if (matcher.matches()) {
				String key = matcher.group("key");
				String id = matcher.group("id");
				String params = matcher.group("next");
				KeyIdPair nextPair = null;
				if (params != null && !params.isEmpty()) {
					Matcher subRoute = subEntryPointPattern.matcher(params);
					nextPair = getKeyIdPair(subRoute, subEntryPointPattern);
				}

				keyIdPair = new KeyIdPair(key, id);
				keyIdPair.setNextPair(nextPair);
			}

			return keyIdPair;
		}
	}
}
