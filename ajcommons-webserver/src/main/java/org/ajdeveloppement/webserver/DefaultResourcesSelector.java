/*
 * Créé le 12 juin 2016 à 11:43:47 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2016 - Aurélien JEOFFRAY
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
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ajdeveloppement.commons.PluginClassLoader;
import org.ajdeveloppement.commons.io.XMLSerializer;

/**
 * Default resources dispatcher for an http request
 * 
 * @author Aurelien JEOFFRAY
 *
 */
@XmlRootElement(name="fileSelector")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefaultResourcesSelector implements ResourcesSelector, DeploymentServiceListener {
	@XmlElement(name="host")
	private List<Host> hosts = new ArrayList<>();
	private transient List<String> webAppsDirectories = new ArrayList<>();
	private transient Map<String, ClassLoader> webAppsClassLoaders = new HashMap<>();

	private File basePath;
	
	private transient Map<String, RewriteUrlRules> rewriteUrlRulesCache = new HashMap<>();
	
	private transient boolean webDirectoriesDiscovered = false;
	
	private transient DeploymentService deploymentService;
	
	/**
	 * Find existing web app in ({@link #getBasePath()}) directory and deploy new webapp archive
	 * @throws IOException 
	 */
	@SuppressWarnings("nls")
	private synchronized void discoverWebDirectories() {
		if(webDirectoriesDiscovered)
			return;
		
		//find all webapp dir
		File[] webDirectories = getBasePath().listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
		
		//search the default host definition
		Optional<Host> defaultHost = hosts.stream().filter(h -> h.isDefaultHost()).findFirst();
		if(!defaultHost.isPresent()) {
			//if default host is not defined, search root webapp and create a default host based on this root
			Optional<File> webRootDirectory = Arrays.asList(webDirectories).stream().filter(wd -> wd.getName().equalsIgnoreCase("root")).findFirst();
			if(webRootDirectory.isPresent()) {
				Host host = new Host();
				host.setDefaultHost(true);
				host.setResourcesPath(webRootDirectory.get().getName());
				
				defaultHost = Optional.of(host);
				hosts.add(host);
			}
		}
		
		//map webapp with host and alias
		for(File webDirectory : webDirectories) {
			//declare webapp
			webAppsDirectories.add(webDirectory.getName());
			
			//root app is declared before
			if(!webDirectory.getName().equalsIgnoreCase("root")) {
				//search associate host
				Optional<Host> currentWebDirectoryHost = hosts.stream()
						.filter(h -> h.getResourcesPath().equalsIgnoreCase(webDirectory.getName())).findFirst();
				if(!currentWebDirectoryHost.isPresent()) {
					//if no associate host, search alias
					Optional<Alias> currentWebDirectoryAlias = hosts.stream().flatMap(h -> h.getListAlias().stream())
							.filter(a -> a.getResourcesPath().equalsIgnoreCase(webDirectory.getName())).findFirst();
					if(!currentWebDirectoryAlias.isPresent()) {
						//if not declared create a new alias on default host and associate app to this
						Host firstHost = null;
						if(defaultHost.isPresent())
							firstHost = defaultHost.get();
						else if(hosts.size() > 0)
							firstHost = hosts.get(0);
						
						if(firstHost != null) {
							Alias alias = new Alias();
							alias.setAliasPrefix("/" + webDirectory.getName().toLowerCase()); //$NON-NLS-1$
							alias.setResourcesPath(webDirectory.getName().toLowerCase());
							
							firstHost.getListAlias().add(alias);
						} else {
							Host host = new Host();
							host.setDefaultHost(true);
							host.setResourcesPath(webDirectory.getName());
							
							hosts.add(host);
						}
					}
				}
			}
		}
		
		webDirectoriesDiscovered = true;
	}
	
	@Override
	public void webAppDeployed(String appName) {
		//Arret des services restartable
		
		webAppsClassLoaders.remove(appName);
		rewriteUrlRulesCache.remove(appName);
		
		synchronized (hosts) {
			webAppsDirectories.clear();
			webDirectoriesDiscovered = false;
			discoverWebDirectories();
		}
		
		//redemarrage des services restartable
	}
	
	/**
	 * Instantiate a new classloader for a webapp
	 * 
	 * @param webAppName the name of the application for which instantiate a class loader
	 * @return the instantiate classloader
	 * @throws IOException
	 */
	private ClassLoader createClassLoader(String webAppName) throws IOException {
		if(deploymentService != null) {
			Path webAppPath = deploymentService.getCurrentInstallDir(webAppName).toPath();
			ClassLoader classLoader = new PluginClassLoader(Thread.currentThread().getContextClassLoader(), webAppPath.toFile());
			
			webAppsClassLoaders.put(webAppName, classLoader);
			
			return classLoader;
		}
		
		return null;
	}
	
	/**
	 * Return the alias associate to an host and uri if exist
	 * 
	 * @param host the base host
	 * @param requestUri the uri to test
	 * @return the associate alias or null if their is no alias
	 */
	private Alias selectAlias(Host host, String requestUri) {
		Optional<Alias> alias = host.getListAlias().stream()
				.filter(a -> requestUri.startsWith(a.getAliasPrefix() + "/") || requestUri.equals(a.getAliasPrefix())).findFirst(); //$NON-NLS-1$
		if(alias.isPresent())
			return alias.get();
		
		return null;
	}
	
	/**
	 * Select the web app associate to an alias or host
	 * 
	 * @param host
	 * @param alias
	 * @return
	 */
	private String selectWebApp(Host host, Alias alias) {
		if(alias != null)
			return alias.getResourcesPath();
		
		if(host != null)
			return host.getResourcesPath();
		
		return null;
	}
	
	/**
	 * Return the deployment service used to deploy web resources on server
	 */
	@Override
	public DeploymentService getDeploymentService() {
		return this.deploymentService;
	}
	
	/**
	 * Change the deployment service used to deploy web resources on server
	 */
	@Override
	public void setDeploymentService(DeploymentService deploymentService) {
		if(this.deploymentService != null)
			this.deploymentService.removeDeploymentServiceListener(this);
		
		if(deploymentService != null)
			deploymentService.addDeploymentServiceListener(this);
		
		this.deploymentService = deploymentService;
	}
	
	/**
	 * Remove instance of dynamic classloader used to load web content resources and force to generate new instance
	 */
	@Override
	public void resetClassLoader(String webAppName) {
		webAppsClassLoaders.remove(webAppName);
	}
	
	@Override
	public ClassLoader getClassLoader(String webAppDir) throws IOException {
		ClassLoader classloader = webAppsClassLoaders.get(webAppDir);
		if(classloader == null)
			classloader = createClassLoader(webAppDir);
		
		return classloader;
	}
	
	/**
	 * @return the webAppsDirectories
	 */
	@Override
	public List<String> getWebAppsName() {
		discoverWebDirectories();
		
		return webAppsDirectories;
	}
	
	@Override
	public List<Host> getHosts() {
		discoverWebDirectories();
		
		return hosts;
	}

	@Override
	public void setHosts(List<Host> hosts) {
		if(this.hosts != null) {
			synchronized (this.hosts) {
				this.hosts = hosts;
			}
		} else
			this.hosts = hosts;
	}
	
	/**
	 * @return the basePath
	 */
	@Override
	public File getBasePath() {
		return basePath;
	}

	/**
	 * @param basePath the basePath to set
	 */
	@Override
	public void setBasePath(File basePath) {
		this.basePath = basePath;
	}
	
	@Override
	public Host selectHost(HttpRequest httpRequest) {
		String sessionHost = httpRequest.getHost();
		if(sessionHost != null) {
			Optional<Host> matchedHost = getHosts().stream()
					.filter(h -> !h.isDefaultHost() && h.match(sessionHost)).findFirst();
			if(!matchedHost.isPresent()) {
				matchedHost = getHosts().stream().filter(h -> h.isDefaultHost()).findFirst();
			}
			
			if(matchedHost.isPresent())
				return matchedHost.get();
		} else {
			Optional<Host> matchedHost = getHosts().stream().filter(h -> h.isDefaultHost()).findFirst();
			
			if(matchedHost.isPresent())
				return matchedHost.get();
		}
		
		return null;
	}
	
	@Override
	public Alias selectAlias(HttpRequest httpRequest) {
		Host matchedHost = selectHost(httpRequest);
		Alias alias = null;
		if(matchedHost != null) {
			String requestUrl = httpRequest.getRequestUrl();
			
			alias = selectAlias(matchedHost, requestUrl);
		}
		
		return alias;
	}
	
	@Override
	public File selectBasePath(String webAppName) throws IOException {
		if(deploymentService != null)
			return deploymentService.getCurrentInstallDir(webAppName);
		
		return null;
	}
	
	@Override
	public String selectWebApp(HttpRequest httpRequest) {
		Host matchedHost = selectHost(httpRequest);
		Alias alias = null;
		if(matchedHost != null) {
			String requestUrl = httpRequest.getRequestUrl();
			
			alias = selectAlias(matchedHost, requestUrl);
		}
		
		return selectWebApp(matchedHost, alias);
	}
	
	@Override
	public File selectWebAppPersistentDirectory(String webAppName) {
		File webAppDir = new File(getBasePath(), webAppName);
		if(webAppDir.exists()) {
			File persistentDir = new File(webAppDir, "persistent"); //$NON-NLS-1$
			if(!persistentDir.exists())
				persistentDir.mkdir();
			
			return persistentDir;
		}
		
		return null;
	}
	
	@SuppressWarnings("nls")
	@Override
	public String selectHostAliasKey(String webAppName) {
		for(Host host : getHosts()) {
			String hostAlias = null;
			
			if(host.isDefaultHost())
				hostAlias =  "*";
			else
				hostAlias =  host.getHostPattern();
			
			if(host.getResourcesPath().equalsIgnoreCase(webAppName)) {
				return hostAlias;
			}
			
			for(Alias alias : host.getListAlias()) {
				if(alias.getResourcesPath().equalsIgnoreCase(webAppName)) {
					return hostAlias + alias.getAliasPrefix();
				}
			}
		};
		
		return null;
	}
	
	/**
	 * return rewriting rules applicable to given request
	 * 
	 * @param httpRequest the request to return url rewriting rules
	 */
	@Override
	public RewriteUrlRules getRewriteUrlRules(HttpRequest httpRequest) throws JAXBException, IOException {
		String webAppName = selectWebApp(httpRequest);
		
		return getRewriteUrlRules(webAppName);
	}
	
	/**
	 * Return url rewriting rules associate to an webapp
	 * 
	 * @param webAppName
	 * @return
	 * @throws JAXBException
	 * @throws IOException
	 */
	public RewriteUrlRules getRewriteUrlRules(String webAppName) throws JAXBException, IOException {
		RewriteUrlRules rewriteUrlRules = rewriteUrlRulesCache.get(webAppName);
		if(rewriteUrlRules == null && deploymentService != null) {
			File rewriteUrlRulesFile = new File(deploymentService.getCurrentInstallDir(webAppName), "urlrewrite.xml"); //$NON-NLS-1$
			if(rewriteUrlRulesFile.exists()) {
				rewriteUrlRules = XMLSerializer.loadMarshallStructure(rewriteUrlRulesFile, RewriteUrlRules.class);
			}
		}
		
		return rewriteUrlRules;
	}
}