/**
 * 
 */
package org.ajdeveloppement.webserver.services.js;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.script.ScriptException;

import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.webserver.HttpException;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode.ReturnCode;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.ResourcesSelector;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;
import org.ajdeveloppement.webserver.ResourcesSelector.Host;
import org.ajdeveloppement.webserver.services.RequestProcessor;
import org.ajdeveloppement.webserver.services.RestartableRequestProcessor;
/**
 * @author Aurélien JEOFFRAY
 *
 */
public class JsService implements RestartableRequestProcessor {
	
	private static final String jsResourcesPath = "resources"; //$NON-NLS-1$
	private static final String jsMainScript = "scripts/main.js"; //$NON-NLS-1$
	
	private Map<String, WebApp> webApps = new HashMap<>();
	private ResourcesSelector fileSelector;
	private HttpServer server;

	/**
	 * 
	 */
	public JsService() {
		URL.setURLStreamHandlerFactory(new InternStreamHandlerFactory());
	}
	
	private void startWebApps() throws ScriptException {
		webApps.clear();
		
		if(fileSelector != null) {
			
			List<Thread> webAppsThreads = new ArrayList<>();
			
			fileSelector.getWebAppsName().forEach(webAppDir -> webAppsThreads.add(startDeclaredWebAppAsync(webAppDir)));

			webAppsThreads.forEach(t -> {
				try {
					if(t != null)
						t.join();
				} catch (InterruptedException e) {
					throw new UncheckedException(e);
				}
			});
		}
	}
	
	private void execute(File script, Path basePath, String webAppDir) throws ScriptException {
		WebApp webApp = webApps.get(webAppDir);
		if(webApp != null) {
			webApp.unload();
		} else {
			webApp = new WebApp(server, script, basePath.toFile());
			webApps.put(webAppDir, webApp);
		}
		
		webApp.execute();
	}
	
	private RequestProcessor getWebApp(Host host, Alias alias) {
		RequestProcessor webApp = null;
		if(alias == null)
			return webApps.get(host.getResourcesPath());
		return webApps.get(alias.getResourcesPath());
	}
	
	public Map<String, WebApp> getWebApps() {
		return webApps;
	}
	
	@Override
	public boolean isRunning(String baseContainer) {
		WebApp webApp = webApps.get(baseContainer);
		if(webApp != null)
			return webApp.isRunning();
		return false;
	}
	
	@Override
	public Thread startDeclaredWebAppAsync(String baseContainer) {
		stopDeclaredWebApp(baseContainer);
		
		Thread webAppStartThread = null;
		
		try {
			Path webAppPath = fileSelector.selectBasePath(baseContainer).toPath();
			Path basePath = Paths.get(fileSelector.selectBasePath(baseContainer).getAbsolutePath(), jsResourcesPath);

			File mainScript = new File(basePath.toFile(), jsMainScript);
			if(mainScript.exists()) {
				try {
					
					ClassLoader classLoader = fileSelector.getClassLoader(baseContainer);
					
					webAppStartThread = Executors.defaultThreadFactory().newThread(() ->{
						try {
							execute(mainScript, basePath, baseContainer);
						} catch (Exception e) {
							throw new UncheckedException(e);
						}
					});
					webAppStartThread.setContextClassLoader(classLoader);
					webAppStartThread.start();
				} catch (Exception e) {
					throw new UncheckedException(e);
				}
			}
			
			
		} catch (IOException e) {
			throw new UncheckedException(e);
		}
		
		return webAppStartThread;
	}
	
	@Override
	public void stopDeclaredWebApp(String baseContainer) {
		WebApp webApp = webApps.get(baseContainer);
		if(webApp != null && webApp.isRunning()) {
			
			try {
				Thread t = Executors.defaultThreadFactory().newThread(() ->{
					webApp.unload();
				});
				t.setContextClassLoader(fileSelector.getClassLoader(baseContainer));
				t.start();
				t.join();
				
				webApps.remove(webApp);
			} catch (InterruptedException | IOException e) {
				throw new UncheckedException(e);
			}
		}
	}

	@Override
	public void init(HttpServer server) {
		try {
			this.server = server;
			fileSelector = server.getFileSelector();
			startWebApps();
		} catch (ScriptException e) {
			throw new UncheckedException(e);
		}
	}
	
	@Override
	public boolean canServe(HttpRequest httpRequest) {
		Host host = fileSelector.selectHost(httpRequest);
		if(host != null) {
			Alias alias = fileSelector.selectAlias(httpRequest);
			
			RequestProcessor webApp = getWebApp(host, alias);
			if(webApp != null) {
				return webApp.canServe(httpRequest);
			}

		}
		return false;
	}

	@Override
	public HttpResponse serve(HttpRequest httpRequest) throws HttpException {
		Host host = fileSelector.selectHost(httpRequest);
		if(host != null) {
			Alias alias = fileSelector.selectAlias(httpRequest);

			RequestProcessor webApp = getWebApp(host, alias);
			if(webApp != null)
				return webApp.serve(httpRequest);
		}
		
		return null;
	}
	
	@Override
	public HttpResponse serveErrorResponse(HttpRequest httpRequest,
			ReturnCode errorCode, String errorContent) {
		Host host = fileSelector.selectHost(httpRequest);
		if(host != null) {
			Alias alias = fileSelector.selectAlias(httpRequest);

			RequestProcessor webApp = getWebApp(host, alias);
			if(webApp != null)
				return webApp.serveErrorResponse(httpRequest, errorCode, errorContent);
		}
		
		return null;
	}
}
