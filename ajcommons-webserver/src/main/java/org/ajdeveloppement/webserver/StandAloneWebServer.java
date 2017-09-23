/*
 * Créé le 6 oct. 2013 à 13:13:36 pour AjCommons (Bibliothèque de composant communs)
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;

import javax.script.ScriptException;
import javax.xml.bind.JAXBException;

import org.ajdeveloppement.commons.AjResourcesReader;
import org.ajdeveloppement.commons.ClassPathHacker;
import org.ajdeveloppement.commons.PluginClassLoader;
import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.commons.io.XMLSerializer;
import org.ajdeveloppement.commons.persistence.sql.ContextDomain;
import org.ajdeveloppement.commons.persistence.sql.SqlContext;
import org.ajdeveloppement.webserver.services.ExtensibleHttpRequestProcessor;
import org.ajdeveloppement.webserver.services.files.FilesService;

/**
 * Serveur HTTP de contenu JS
 * 
 * @author Aurelien JEOFFRAY
 * 
 */
public class StandAloneWebServer {
	private static final String WEBSERVER_CONFIG = "webserver"; //$NON-NLS-1$
	private static final String DB_CONFIG = "db"; //$NON-NLS-1$

	private static final String WEBSERVER_LISTEN_PORT = "webserver.listen.port"; //$NON-NLS-1$
	private static final String WEBSERVER_SSLLISTEN_PORT = "webserver.ssllisten.port"; //$NON-NLS-1$
	private static final String WEBSERVER_FILESELECTOR_FILE = "webserver.fileselector.file"; //$NON-NLS-1$
	private static final String WEBSERVER_STATIC_ALLOWEDGZIPEXT = "webserver.static.allowedgzipext"; //$NON-NLS-1$
	private static final String WEBSERVER_SERVICE_ORDER = "webserver.service.order"; //$NON-NLS-1$
	private static final String WEBSERVER_EXT_LIBS = "webserver.ext.libs"; //$NON-NLS-1$
	
	private static final String WEBSERVER_CERTIFICATE_ALIAS = "webserver.certificateAlias"; //$NON-NLS-1$
	private static final String WEBSERVER_PKCS12PASSWORD = "webserver.pkcs12password"; //$NON-NLS-1$
	private static final String WEBSERVER_PKCS12_KEY_STORE_FILE = "webserver.pkcs12KeyStore.file"; //$NON-NLS-1$

	private static AjResourcesReader staticParameters = new AjResourcesReader(WEBSERVER_CONFIG);
	private static AjResourcesReader dbConfig = new AjResourcesReader(DB_CONFIG);
	
	private static HttpServer httpServer;
	
	@SuppressWarnings("nls")
	private static void defineDatabaseContext() throws SQLException {
		String dbConnectionUrl = dbConfig.getResourceString("connection.url");
		String user = dbConfig.getResourceString("connection.user");
		String password = dbConfig.getResourceString("connection.password");
		
		ContextDomain contextDomain = new ContextDomain();
		contextDomain.setDatabaseUrl(dbConnectionUrl);
		contextDomain.setUser(user);
		contextDomain.setPassword(password);
		contextDomain.setPersistenceDialect("h2");
		
		SqlContext.getContextDomains().put(HttpServer.WEBSERVER_DB_DOMAIN, contextDomain);
	}
	
	/**
	 * Return the file selector
	 * 
	 * @return the file selector
	 */
	private static ResourcesSelector getFileSelector() {
		ResourcesSelector fileSelector = null;
		String fileSelectorFilePath = staticParameters.getResourceString(WEBSERVER_FILESELECTOR_FILE);
		if(fileSelectorFilePath != null && !fileSelectorFilePath.isEmpty()) {
			URL fileSelectorURL = staticParameters.getClass().getResource(fileSelectorFilePath);
			if(fileSelectorURL != null) {
				File fileSelectorFile = new File(fileSelectorURL.getPath());
				if(fileSelectorFile.exists()) {
					try {
						fileSelector = XMLSerializer.loadMarshallStructure(fileSelectorFile, DefaultResourcesSelector.class);
					} catch (JAXBException | IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(fileSelector == null)
			fileSelector = new DefaultResourcesSelector();
		return fileSelector;
	}
	
	/**
	 * @param fileSelector
	 */
	private static void initilizeClasspath(ResourcesSelector fileSelector) {
		String extendedLibrairiesPath = staticParameters.getResourceString(WEBSERVER_EXT_LIBS);
		try {
			PluginClassLoader dynamicClassLoader = new PluginClassLoader(Thread.currentThread().getContextClassLoader(), null);
			Thread.currentThread().setContextClassLoader(dynamicClassLoader);
			
			//Ext dir classpath
			if(extendedLibrairiesPath != null && !extendedLibrairiesPath.isEmpty()) {
				File extDir = new File(extendedLibrairiesPath);
				
				dynamicClassLoader.addDirectoryJars(extDir);
				if(extDir != null && extDir.exists()) {
					for(File jar : extDir.listFiles(f -> f.getName().endsWith(".jar"))) //$NON-NLS-1$
						ClassPathHacker.addFile(jar);
				}
			}
			
			//ResourcesSelector Dirs classpath
			//for(String dirPath : fileSelector.getWebAppsDirectories()) {
			//	dynamicClassLoader.addDirectoryJars(new File(fileSelector.getBasePath(), dirPath));
			//}
			
		} catch (MalformedURLException | SecurityException |  IllegalArgumentException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	private static void start() {
		System.setProperty("nashorn.option.scripting", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		
		FilesService.setAllowedGzipExt(Arrays.asList(staticParameters.getResourceString(WEBSERVER_STATIC_ALLOWEDGZIPEXT).split(","))); //$NON-NLS-1$
		
		String[] servicesOrder = staticParameters.getResourceString(WEBSERVER_SERVICE_ORDER).split(","); //$NON-NLS-1$
		
		ResourcesSelector fileSelector = getFileSelector();
		DeploymentService deployment = new DefaultDeploymentService();
		deployment.setBasePath(fileSelector.getBasePath());
		try {
			deployment.deployAll();
		} catch (IOException e1) {
			throw new UncheckedException(e1);
		}
		fileSelector.setDeploymentService(deployment);
		
		initilizeClasspath(fileSelector);
		
		try {
			defineDatabaseContext();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		httpServer = new HttpServer(staticParameters.getResourceInteger(WEBSERVER_LISTEN_PORT));
		httpServer.setPkcs12KeyStorePath(staticParameters.getResourceString(WEBSERVER_PKCS12_KEY_STORE_FILE));
		httpServer.setPkcs12KeyStorePassword(staticParameters.getResourceString(WEBSERVER_PKCS12PASSWORD));
		httpServer.setCertificateAlias(staticParameters.getResourceString(WEBSERVER_CERTIFICATE_ALIAS));
		httpServer.setListenSslPort(staticParameters.getResourceInteger(WEBSERVER_SSLLISTEN_PORT));
		httpServer.setFileSelector(fileSelector);
		httpServer.setRequestProcessor(new ExtensibleHttpRequestProcessor(servicesOrder));
		
		httpServer.start();
	}

	/**
	 * @param args
	 * @throws ScriptException 
	 */
	public static void main(String... args) throws ScriptException {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				Thread.setDefaultUncaughtExceptionHandler(null);

				if(httpServer != null)
					httpServer.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		start();
	}
	
	
	/**
	 * Stop WebServer instance
	 * 
	 * @param args
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void stop(String... args) throws IOException, SQLException {
		if(httpServer != null)
			httpServer.stop();
		
		System.exit(0);
	}
}