/*
 * Créé le 7 avr. 2015 à 10:01:47 pour ArcCompetition
 *
 * Copyright 2002-2015 - Aurélien JEOFFRAY
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
package org.ajdeveloppement.webserver.services.webapi.helpers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.stream.Collectors;

import org.ajdeveloppement.commons.ClassUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Aurélien JEOFFRAY
 *
 */
public class JsonHelper {
	
	private static ObjectMapper jsonMapper = new ObjectMapper();
	
	static {
		jsonMapper.findAndRegisterModules();
		jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		jsonMapper.setDateFormat( new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")); //$NON-NLS-1$
	}
	
	private static ObjectMapper getMapper() {
		return jsonMapper;
	}
	/**
	 * Replace invalide char by the escape value
	 * 
	 * @param string the string to escape
	 * @return the escaped string 
	 */
	@SuppressWarnings("nls")
	public static String escapeInvalidJsonChar(String string) {
		return string.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}
	
	/**
	 * Serialize an abject to json string
	 * @param object the json serializable object
	 * @return the json serialized object value
	 * @throws JsonProcessingException 
	 */
	public static <T> String toJson(T object) throws JsonProcessingException {
		return getMapper().writeValueAsString(object);
	}
	
	public static <T> T fromJson(String json, Class<T> valueType) throws IOException {
		return getMapper().readValue(json, valueType);
	}
	
	/**
	 * Return a { "success": false, "error": "errorMessage" } json string
	 * 
	 * @param errorMessage
	 * @return the fail response
	 */
	@SuppressWarnings("nls")
	public static String getFailSuccessResponse(String errorMessage) {
		return String.format("{ \"success\": false, \"error\": \"%s\" }",
				escapeInvalidJsonChar(errorMessage));
	}
	
	/**
	 * Return a json success response with the first property is
	 * <code>"success":true</code> follow with parameters map
	 * 
	 * @param parameters keyvaluepairs added to success response json
	 * @return the json success response
	 */
	@SuppressWarnings("nls")
	public static String getSuccessResponse(Map<String, Object> parameters) {
		String params = "";
		if(parameters != null && parameters.size() > 0) {
			params = "," + parameters.entrySet().stream().map(e -> {
					String value = e.getValue().toString();
					if(!ClassUtils.isWrapperType(e.getValue().getClass()) || e.getValue().getClass() == Character.class)
						value = "\"" + escapeInvalidJsonChar(value) + "\"";
					return "\"" + e.getKey() + "\":" + value;
				}).collect(Collectors.joining(","));
		}
		
		return String.format("{ \"success\":true%s }",
				params);
	}

}
