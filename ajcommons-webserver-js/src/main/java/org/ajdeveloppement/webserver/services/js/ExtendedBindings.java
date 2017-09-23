/*
 * Créé le 12 nov. 2013 à 14:17:23 pour AjCommons (Bibliothèque de composant communs)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

/**
 * @author a.jeoffray
 *
 */
public class ExtendedBindings implements Bindings {

	private Bindings bindings;
	private Bindings parentBindings;
	
	public ExtendedBindings(Bindings bindings, Bindings parentBindings) {
		this.bindings = bindings;
		this.parentBindings = parentBindings;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear() {
		this.bindings.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	@Override
	public boolean containsValue(Object value) {
		boolean contains = this.bindings.containsValue(value);
		if(!contains && this.parentBindings != null)
			contains = this.parentBindings.containsValue(value);
		
		return contains;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		Set<java.util.Map.Entry<String, Object>> entries = null;
		if(parentBindings != null) {
			entries = parentBindings.entrySet();
			entries.addAll(bindings.entrySet());
		} else {
			entries = bindings.entrySet();
		}
		return entries;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		boolean empty = this.bindings.isEmpty();
		if(empty && this.parentBindings != null)
			empty = this.parentBindings.isEmpty();
		
		return empty;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<String> keySet() {
		Set<String> keys = null;
		if(parentBindings != null) {
			keys = parentBindings.keySet();
			keys.addAll(bindings.keySet());
		} else {
			keys = bindings.keySet();
		}
		return keys;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	@Override
	public int size() {
		return keySet().size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<Object> values() {
		Collection<Object> values = new ArrayList<>();
		
		for(Entry<String, Object> entry: entrySet()) {
			values.add(entry);
		}
		
		return values;
	}

	/* (non-Javadoc)
	 * @see javax.script.Bindings#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key) {
		boolean contains = this.bindings.containsKey(key);
		if(!contains && this.parentBindings != null)
			contains = this.parentBindings.containsKey(key);
		return contains;
	}

	/* (non-Javadoc)
	 * @see javax.script.Bindings#get(java.lang.Object)
	 */
	@Override
	public Object get(Object key) {
		Object value = this.bindings.get(key);
		if(value == null && this.parentBindings != null)
			value = this.parentBindings.get(key);
		return value;
	}

	/* (non-Javadoc)
	 * @see javax.script.Bindings#put(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object put(String name, Object value) {
		return this.bindings.put(name, value);
	}

	/* (non-Javadoc)
	 * @see javax.script.Bindings#putAll(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> toMerge) {
		this.bindings.putAll(toMerge);
	}

	/* (non-Javadoc)
	 * @see javax.script.Bindings#remove(java.lang.Object)
	 */
	@Override
	public Object remove(Object key) {
		return this.bindings.remove(key);
	}

}
