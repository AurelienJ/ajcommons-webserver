/*
 * Créé le 14 mai 2015 à 11:32:07 pour AjCommons (Bibliothèque de composant communs)
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
package org.ajdeveloppement.webserver.services.webapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ajdeveloppement.webserver.HttpMethod;
import org.ajdeveloppement.webserver.services.webapi.annotations.HttpService;
import org.ajdeveloppement.webserver.services.webapi.annotations.HttpServiceId;

import com.google.inject.Module;

/**
 * @author aurelien
 *
 */
public class Container {
	
	public static class RouteMatcher {
		private Matcher matcher;
		private Pattern subPattern;

		/**
		 * @param matcher
		 * @param subPattern
		 */
		public RouteMatcher(Matcher matcher, Pattern subPattern) {
			super();
			this.matcher = matcher;
			this.subPattern = subPattern;
		}
		
		/**
		 * @return the matcher
		 */
		public Matcher getMatcher() {
			return matcher;
		}
		/**
		 * @param matcher the matcher to set
		 */
		public void setMatcher(Matcher matcher) {
			this.matcher = matcher;
		}
		/**
		 * @return the subPattern
		 */
		public Pattern getSubPattern() {
			return subPattern;
		}
		/**
		 * @param subPattern the subPattern to set
		 */
		public void setSubPattern(Pattern subPattern) {
			this.subPattern = subPattern;
		}
	}
	
	public static class Route {
		private String key;
		private Pattern subPattern;
		
		/**
		 * 
		 */
		public Route() {
		}
		
		/**
		 * @param key
		 * @param subPattern
		 */
		public Route(String key, Pattern subPattern) {
			this.key = key;
			this.subPattern = subPattern;
		}
		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}
		/**
		 * @param key the key to set
		 */
		public void setKey(String key) {
			this.key = key;
		}
		/**
		 * @return the subPattern
		 */
		public Pattern getSubPattern() {
			return subPattern;
		}
		/**
		 * @param subPattern the subPattern to set
		 */
		public void setSubPattern(Pattern subPattern) {
			this.subPattern = subPattern;
		}
	}
	
	private Map<String, Map<HttpMethod, Method>> endpointsServices = new HashMap<>();
	private Map<Pattern, Route> endpointsServicesCustomRoutes = new HashMap<>();
	
	private String entryPointPrefix = "/api";  //$NON-NLS-1$
	private String routePathRegex = "/(?<key>[^/]+)(?:/(?<id>[^/]+))?(?<next>/.+)?/?"; //$NON-NLS-1$
	
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	
	private Map<Class<?>, ApiApplication> servicesDiscovered = new HashMap<>();
	
	private List<Module> modules = new ArrayList<>();
		
	/**
	 * The container's classLoader
	 * 
	 * @return the classLoader of container
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Define the class loader associate to container. If not defined, get Thread.currentThread().getContextClassLoader()
	 * 
	 * @param classLoader the class loader associate to container
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Discover endpoint api service in a service container class.
	 * An endpoint service method is a static method with first parameters {@link HttpContext}
	 * and an annotation {@link HttpService}
	 * 
	 * @param servicesContainer the container that contains service endpoint method
	 */
	@SuppressWarnings("nls")
	public void discoverServices(ApiApplication hostApp, Class<?> servicesContainer) {
		if(servicesDiscovered.containsKey(servicesContainer))
			return;
		
		servicesDiscovered.put(servicesContainer, hostApp);
		
		for(Method m : servicesContainer.getMethods()) {
			HttpService endPoint = m.getAnnotation(HttpService.class);
			if(endPoint != null) {
				
				String customRoute = "";
				String[] customNextPatterns = null;
				
				if(!endPoint.routePrefix().isEmpty())
					customRoute = endPoint.routePrefix();
				
				if(!endPoint.routePattern().isEmpty())
					customRoute += endPoint.routePattern();
				
				String endPointKey = endPoint.key();
				
				long nbIdParams = Arrays.asList(m.getParameters()).stream().filter(p -> p.getAnnotation(HttpServiceId.class) != null).count();
				long nbKeySeparator = endPointKey.chars().filter(c -> c == '/').count();
				if(nbIdParams > nbKeySeparator) {
					endPointKey += "/id";
				}
				
				if(!customRoute.isEmpty())
					endpointsServicesCustomRoutes.put(Pattern.compile(customRoute, Pattern.CASE_INSENSITIVE), 
							new Route(endPointKey, 
									Pattern.compile(!endPoint.routeSubPattern().isEmpty() ? endPoint.routeSubPattern() : endPoint.routePattern(), Pattern.CASE_INSENSITIVE)));
				
				if(!endpointsServices.containsKey(endPointKey))
					endpointsServices.put(endPointKey, new HashMap<HttpMethod, Method>());
				
				for(HttpMethod method : endPoint.methods())
					endpointsServices.get(endPointKey).put(method, m);
			}
		}
	}
	
	@SuppressWarnings("nls")
	public void removeServices(Class<?> servicesContainer) {
		if(servicesDiscovered.containsKey(servicesContainer)) {
			servicesDiscovered.remove(servicesContainer);
			
			for(Method m : servicesContainer.getMethods()) {
				HttpService endPoint = m.getAnnotation(HttpService.class);
				if(endPoint != null) {
					String customRoute = "";
					
					if(!endPoint.routePrefix().isEmpty())
						customRoute = endPoint.routePrefix();
					
					if(!endPoint.routePattern().isEmpty())
						customRoute += endPoint.routePattern();
					
					String endPointKey = endPoint.key();
					
					long nbIdParams = Arrays.asList(m.getParameters()).stream().filter(p -> p.getAnnotation(HttpServiceId.class) != null).count();
					long nbKeySeparator = endPointKey.chars().filter(c -> c == '/').count();
					if(nbIdParams > nbKeySeparator) {
						endPointKey += "/id";
					}
					
					if(!customRoute.isEmpty())
						endpointsServicesCustomRoutes.remove(Pattern.compile(customRoute, Pattern.CASE_INSENSITIVE));
					
					if(endpointsServices.containsKey(endPointKey))
						endpointsServices.remove(endPointKey);
				}
			}
		}
	}
	
	public Pattern getEntryPointPattern() {
		return Pattern.compile(entryPointPrefix + routePathRegex, Pattern.CASE_INSENSITIVE);
	}
	
	public Pattern getSubEntryPointPattern() {
		return Pattern.compile(routePathRegex, Pattern.CASE_INSENSITIVE);
	}
	
	public RouteMatcher matchEntryPointPattern(String uri) {
		Matcher m = null;
		for(Entry<Pattern, Route> entry : endpointsServicesCustomRoutes.entrySet()) {
			m = entry.getKey().matcher(uri);
			if(m.matches())
				return new RouteMatcher(m, entry.getValue().getSubPattern());
		}
		
		return new RouteMatcher(getEntryPointPattern().matcher(uri), getSubEntryPointPattern());
	}
	
	/**
	 * @return the entryPointPrefix
	 */
	public String getEntryPointPrefix() {
		return entryPointPrefix;
	}

	/**
	 * @param entryPointPrefix the entryPointPrefix to set
	 */
	public void setEntryPointPrefix(String entryPointPrefix) {
		this.entryPointPrefix = entryPointPrefix;
	}

	/**
	 * @return the routePathRegex
	 */
	public String getRoutePathRegex() {
		return routePathRegex;
	}

	/**
	 * @param routePathRegex the routePathRegex to set
	 */
	public void setRoutePathRegex(String routePathRegex) {
		this.routePathRegex = routePathRegex;
	}

	/**
	 * @return the endpointsServices
	 */
	public Map<String, Map<HttpMethod, Method>> getEndpointsServices() {
		return endpointsServices;
	}
	
	public ApiApplication getHostApplication(Class<?> servicesContainer) {
		return servicesDiscovered.get(servicesContainer);
	}
	
	public List<Module> getInjectorModules() {
		return modules;
	}
}
