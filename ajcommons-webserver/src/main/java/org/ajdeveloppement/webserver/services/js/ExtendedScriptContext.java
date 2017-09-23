/*
 * Créé le 12 nov. 2013 à 13:59:57 pour AjCommons (Bibliothèque de composant communs)
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
package org.ajdeveloppement.webserver.services.js;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

/**
 * @author a.jeoffray
 *
 */
public class ExtendedScriptContext implements ScriptContext {

	private ScriptContext parentScriptContext;
	private ScriptContext internalScriptContext = new SimpleScriptContext(); 
	
	public ExtendedScriptContext(ScriptContext parentScriptContext) {
		this.parentScriptContext = parentScriptContext;
		
		if(parentScriptContext != null) {
			this.internalScriptContext.setReader(parentScriptContext.getReader());
			this.internalScriptContext.setWriter(parentScriptContext.getWriter());
			this.internalScriptContext.setErrorWriter(parentScriptContext.getErrorWriter());
			this.internalScriptContext.setBindings(
					new ExtendedBindings(this.internalScriptContext.getBindings(ScriptContext.ENGINE_SCOPE), parentScriptContext.getBindings(ScriptContext.ENGINE_SCOPE)), ScriptContext.ENGINE_SCOPE);
		}
	}
	
	
	public ScriptContext getParentScriptContext() {
		return parentScriptContext;
	}

	public void setParentScriptContext(ScriptContext parentScriptContext) {
		this.parentScriptContext = parentScriptContext;
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(String name) {
		Object attribute = internalScriptContext.getAttribute(name);
		if(attribute == null && parentScriptContext != null)
			attribute = parentScriptContext.getAttribute(name);
		return attribute;
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getAttribute(java.lang.String, int)
	 */
	@Override
	public Object getAttribute(String name, int scope) {
		Object attribute = internalScriptContext.getAttribute(name, scope);
		if(attribute == null && parentScriptContext != null)
			attribute = parentScriptContext.getAttribute(name, scope);
		return attribute;
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getAttributesScope(java.lang.String)
	 */
	@Override
	public int getAttributesScope(String name) {
		int scope = internalScriptContext.getAttributesScope(name);
		if(scope == 0 && parentScriptContext != null)
			scope = parentScriptContext.getAttributesScope(name);
		return scope;
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getBindings(int)
	 */
	@Override
	public Bindings getBindings(int scope) {
		return internalScriptContext.getBindings(scope);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getErrorWriter()
	 */
	@Override
	public Writer getErrorWriter() {
		return internalScriptContext.getErrorWriter();
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getReader()
	 */
	@Override
	public Reader getReader() {
		return internalScriptContext.getReader();
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getScopes()
	 */
	@Override
	public List<Integer> getScopes() {
		return internalScriptContext.getScopes();
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#getWriter()
	 */
	@Override
	public Writer getWriter() {
		return internalScriptContext.getWriter();
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#removeAttribute(java.lang.String, int)
	 */
	@Override
	public Object removeAttribute(String name, int scope) {
		return internalScriptContext.removeAttribute(name, scope);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#setAttribute(java.lang.String, java.lang.Object, int)
	 */
	@Override
	public void setAttribute(String name, Object value, int scope) {
		internalScriptContext.setAttribute(name, value, scope);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#setBindings(javax.script.Bindings, int)
	 */
	@Override
	public void setBindings(Bindings bindings, int scope) {
		internalScriptContext.setBindings(bindings, scope);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#setErrorWriter(java.io.Writer)
	 */
	@Override
	public void setErrorWriter(Writer writer) {
		internalScriptContext.setErrorWriter(writer);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#setReader(java.io.Reader)
	 */
	@Override
	public void setReader(Reader reader) {
		internalScriptContext.setReader(reader);
	}

	/* (non-Javadoc)
	 * @see javax.script.ScriptContext#setWriter(java.io.Writer)
	 */
	@Override
	public void setWriter(Writer writer) {
		internalScriptContext.setWriter(writer);
	}

}
