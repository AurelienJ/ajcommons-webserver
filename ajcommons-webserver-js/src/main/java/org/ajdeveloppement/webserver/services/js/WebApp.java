package org.ajdeveloppement.webserver.services.js;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.ajdeveloppement.commons.PluginClassLoader;
import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.webserver.ResourcesSelector;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;
import org.ajdeveloppement.webserver.ResourcesSelector.Host;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpReturnCode.ReturnCode;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.HttpSession;
import org.ajdeveloppement.webserver.services.RequestProcessor;

public class WebApp implements RequestProcessor {
	private File mainScript;
	private File basePath;
	private HttpServer server;
	
	private WebAppInterface webApp;
	private boolean running = false;
	
	public WebApp(HttpServer server, File script, File basePath) {
		this.server = server;
		this.mainScript = script;
		this.basePath = basePath;
	}

	/**
	 * @return the mainScript
	 */
	public File getMainScript() {
		return mainScript;
	}

	/**
	 * @param mainScript the mainScript to set
	 */
	public void setMainScript(File mainScript) {
		this.mainScript = mainScript;
	}

	/**
	 * @return the basePath
	 */
	public File getBasePath() {
		return basePath;
	}

	/**
	 * @param basePath the basePath to set
	 */
	public void setBasePath(File basePath) {
		this.basePath = basePath;
	}
	
	public boolean isRunning() {
		return running;
	}

	@SuppressWarnings("nls")
	public void execute() throws ScriptException {
		System.out.println("Start \"" + basePath.getPath() + "\" App");
		running = true;
		
		try {
			ScriptEngineManager se = new ScriptEngineManager();
			ScriptEngine scriptEngine = se.getEngineByName("JavaScript"); //$NON-NLS-1$
			scriptEngine.put("basePath", basePath); //$NON-NLS-1$
			//scriptEngine.eval("print('class loader courant: ' + java.lang.Thread.currentThread().getContextClassLoader().toString());");
			scriptEngine.eval("load('" + mainScript.getPath().replace("\\", "\\\\") + "');");
			
			webApp = ((Invocable)scriptEngine).getInterface(WebAppInterface.class);
			if(webApp != null)
				webApp.init(server);
		} catch(ScriptException e) {
			running = false;
			throw e;
		}
	}
	
	@SuppressWarnings("nls")
	public void unload() {
		System.out.println("Stop \"" + basePath.getPath() + "\" App");
		if(webApp != null)
			webApp.unload();
		
		running = false;
	}

	@Override
	public void init(HttpServer server) {
		if(webApp != null && running)
			webApp.init(server);
	}

	@Override
	public boolean canServe(HttpRequest httpRequest) {
		if(webApp != null && running)
			return webApp.canServe(httpRequest);
		return false;
	}

	@Override
	public HttpResponse serve(HttpRequest httpRequest) {
		if(webApp != null && running)
			return webApp.serve(httpRequest);
		return null;
	}
	
	@Override
	public HttpResponse serveErrorResponse(HttpRequest httpRequest,
			ReturnCode errorCode, String errorContent) {
		if(webApp != null && running)
			return webApp.serveErrorResponse(httpRequest, errorCode, errorContent);
		return null;
	}
}
