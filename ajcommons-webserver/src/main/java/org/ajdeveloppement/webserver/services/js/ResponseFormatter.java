/*
 * Créé le 31 juil. 2014 à 14:06:28 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2014 - Aurélien JEOFFRAY
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpSession;

/**
 * Formattage de la réponse Http
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public class ResponseFormatter {
	private HttpRequest httpRequest;
	private byte[] binaryOutput;
	
	/**
	 * 
	 * @param binaryOutput le contenue binaire de de la réponse à transmettre
	 * @param session la session http lié
	 */
	public ResponseFormatter(byte[] binaryOutput, HttpRequest httpRequest) {
		this.binaryOutput = binaryOutput;
		this.httpRequest = httpRequest;
	}
	
	/**
	 * Indique si le client accept l'encodage gzip
	 * 
	 * @return <code>true</code> si le client accept la compression gzip
	 */
	@SuppressWarnings("nls")
	private boolean acceptGzipEncoding() {
		return this.httpRequest.getHeaderValues().containsKey("accept-encoding") 
				&& this.httpRequest.getHeaderValues().get("accept-encoding").contains("gzip");
	}
	
	/**
	 * Compresse le contenu binaire fournit au constructeur en gzip
	 * 
	 * @return le flux binaire compressé
	 * @throws IOException
	 */
	private byte[] encodeToGzip() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream out = new GZIPOutputStream(baos);
		out.write(this.binaryOutput);
		out.finish();
		
		this.binaryOutput = baos.toByteArray();
		
		return this.binaryOutput;
	}
	
	/**
	 * Compile une réponse http pour le contenu fournit en parametre
	 * 
	 * @param session
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public static HttpResponse getGzipedResponseForOutputTemplate(HttpRequest httpRequest, String content) throws IOException {
		return getGzipedResponseForOutputTemplate(httpRequest, content.getBytes("UTF-8"), "text/html; charset=utf-8"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static HttpResponse getGzipedResponseForOutputTemplate(HttpRequest httpRequest, String content, String mimeType) throws IOException {
		return getGzipedResponseForOutputTemplate(httpRequest, content.getBytes("UTF-8"), mimeType); //$NON-NLS-1$
	}
	
	/**
	 * Compile une réponse http pour le contenu fournit en parametre
	 * 
	 * @param session the http session to produce response
	 * @param binaryOutput the binary response body content
	 * @param mimeType the returned content mime-type
	 * @return the http response
	 * @throws IOException
	 */
	public static HttpResponse getGzipedResponseForOutputTemplate(HttpRequest httpRequest, byte[] binaryOutput, String mimeType) throws IOException {
		boolean gzip = false;

		ResponseFormatter gz = new ResponseFormatter(binaryOutput, httpRequest);
		if(gz.acceptGzipEncoding()) {
			binaryOutput = gz.encodeToGzip();
			gzip = true;
		}
		ByteArrayInputStream stream = new ByteArrayInputStream(binaryOutput);
		
		HttpResponse response = new HttpResponse(org.ajdeveloppement.webserver.HttpReturnCode.Success.OK, mimeType, stream); 
		response.setGzip(gzip);
		
		return response;
	}
}
