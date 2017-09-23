/*
 * Créé le 13 nov. 2013 à 22:36:49 pour AjCommons (Bibliothèque de composant communs)
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
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Dedicated access resources right verifier.<br>
 * This class invoke a javascript nashorn script define by {#link {@link #setAccessScript(String)} to control access.<br>
 * This script must implement {@link AccessInterface#verifyAccess(HttpSession, HttpRequest)} 
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public class AccessVerifier {
	
	/**
	 * Interface to implement js script
	 */
	public interface AccessInterface {
		/**
		 * The access verifier function
		 * 
		 * @param session the current http network session
		 * @param httpRequest the http request metadata
		 * @return <code>null</code> if access is verified or an http response indicate invalid or custom response
		 */
		HttpResponse verifyAccess(HttpSession session, HttpRequest httpRequest);
	}

	private String accessScript = "function verifyAccess(session) { return null; }"; //$NON-NLS-1$
	
	private AccessInterface accessVerifier;
	private boolean isVerifierBuild = false;
	
	/**
	 * Return the js verifier script content
	 * 
	 * @return the js verifier script content
	 */
	public String getAccessScript() {
		return accessScript;
	}

	/**
	 * Define the js verifier script. this script must implement {@link AccessInterface#verifyAccess(HttpSession, HttpRequest)} 
	 * 
	 * @param accessScript the js verifier script.
	 */
	public void setAccessScript(String accessScript) {
		this.accessScript = accessScript;
		
		isVerifierBuild = false;
	}

	private void buildScript() throws ScriptException {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript"); //$NON-NLS-1$
		
		CompiledScript script = ((Compilable)scriptEngine).compile(accessScript);
		script.eval();
		
		accessVerifier = ((Invocable)script.getEngine()).getInterface(AccessInterface.class);
		
		isVerifierBuild = true;
	}
	
	/**
	 * Verify access right for an httprequest
	 * 
	 * @param session the current http network session
	 * @param httpRequest the http request metadata
	 * @return <code>null</code> if access is verified or an http response indicate invalid or custom response
	 * @throws ScriptException
	 */
	public HttpResponse verifyAccess(HttpSession session, HttpRequest httpRequest) throws ScriptException {
		if(!isVerifierBuild)
			buildScript();
		
		if(accessVerifier != null)
			return accessVerifier.verifyAccess(session, httpRequest);
		return null;
	}
}
