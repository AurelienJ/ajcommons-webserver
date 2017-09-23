/*
 * Créé le 13 oct. 2013 à 11:33:32 pour AjCommons (Bibliothèque de composant communs)
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.ajdeveloppement.webserver.HttpReturnCode.ReturnCode;
import org.ajdeveloppement.webserver.data.Request;

/**
 * @author "Aurélien JEOFFRAY"
 * 
 */
public class HttpResponse {
	protected static final String SERVER_NAME = "AjDeveloppement Light HttpServer/0.3"; //$NON-NLS-1$
	protected static final String PROTOCOL = "HTTP/1.1"; //$NON-NLS-1$
	protected static final String CRLF = "\r\n"; //$NON-NLS-1$

	private ReturnCode returnCode;
	private String mimeType;
	private InputStream data;
	private long datalength;

	private Map<String, String> headers = new HashMap<String, String>();
	private boolean chunkedTransfer;
	private boolean gzip;
	private boolean keepAlive;
	private int keepAliveCount = 0;

	public HttpResponse(ReturnCode returnCode, String mimeType, InputStream data) {
		this(returnCode, mimeType, data, 0);
	}

	public HttpResponse(ReturnCode returnCode, String mimeType,
			String responseText) {
		this(returnCode, mimeType, new ByteArrayInputStream(
				responseText.getBytes(Charset.forName("UTF-8"))), responseText.getBytes(Charset.forName("UTF-8")).length); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param returnCode
	 * @param mimeType
	 * @param data
	 * @param datalength
	 */
	public HttpResponse(ReturnCode returnCode, String mimeType,
			InputStream data, long datalength) {
		this.returnCode = returnCode;
		this.mimeType = mimeType;
		this.data = data;
		this.datalength = datalength;
	}

	private long sendContent(OutputStream outputStream) throws IOException {
		long totalRead = 0;
		
		try {
			if (data != null) {
				byte[] inBuffer = new byte[4096];
				int nbRead = 0;
				
				try {
					OutputStream outStream = outputStream;
	
					try {
						while ((nbRead = data.read(inBuffer)) > -1) {
							if (chunkedTransfer)
								outStream.write((String.format("%x", nbRead) + CRLF).getBytes()); //$NON-NLS-1$
							outStream.write(inBuffer, 0, nbRead);
							if (chunkedTransfer)
								outputStream.write(CRLF.getBytes());
	
							totalRead += nbRead;
							if (datalength > 0 && totalRead >= datalength) {
								break;
							}
						}
					} finally {
						try {
							data.close();
						} catch (IOException e) {
						}
					}
					if (chunkedTransfer)
						outStream.write((0 + CRLF + CRLF).getBytes());
					outStream.flush();
					
					
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		} finally {
			outputStream.flush();
		}
		
		return totalRead;
	}

	protected String createHeaderProperty(String property, String value) {
		return property + ": " + value + CRLF; //$NON-NLS-1$
	}
	
	public ReturnCode getReturnCode() {
		return returnCode;
	}
	
	public void setReturnCode(ReturnCode returnCode) {
		this.returnCode = returnCode;
	}

	public void addHeader(String property, String value) {
		headers.put(property, value);
	}

	public void setChunkedTransfer(boolean chunkedTransfer) {
		this.chunkedTransfer = chunkedTransfer;
	}

	/**
	 * @return the gzip
	 */
	public boolean isGzip() {
		return gzip;
	}

	/**
	 * @param gzip
	 *            the gzip to set
	 */
	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}

	/**
	 * @return the keepAlive
	 */
	public boolean isKeepAlive() {
		return keepAlive;
	}

	/**
	 * @param keepAlive
	 *            the keepAlive to set
	 */
	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * @return the keepAliveCount
	 */
	public int getKeepAliveCount() {
		return keepAliveCount;
	}

	/**
	 * @param keepAliveCount the keepAliveCount to set
	 */
	public void setKeepAliveCount(int keepAliveCount) {
		this.keepAliveCount = keepAliveCount;
	}

	/**
	 * The response data stream
	 * @param data
	 */
	public void setInputStream(InputStream data) {
		this.data = data;
	}

	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Send the request response on given outputStream
	 * 
	 * @param outputStream the output stream to send response
	 * @throws IOException
	 */
	public void send(OutputStream outputStream, Request request) throws IOException {
		SimpleDateFormat gmtFrmt = new SimpleDateFormat(
				"E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US); //$NON-NLS-1$
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$

		//System.out.println(returnCode.getCode() + " "+ returnCode.getDescription()); //$NON-NLS-1$

		PrintWriter writer = new PrintWriter(outputStream);
		writer.print(String
				.format("%s %s %s", PROTOCOL, returnCode.getCode(), returnCode.getDescription())); //$NON-NLS-1$
		writer.print(CRLF);
		
		if (mimeType != null)
			writer.print(createHeaderProperty("Content-Type", mimeType)); //$NON-NLS-1$

		for (Entry<String, String> kv : headers.entrySet()) {
			writer.print(createHeaderProperty(kv.getKey(), kv.getValue()));
		}

		writer.print(createHeaderProperty("Date", gmtFrmt.format(new Date()))); //$NON-NLS-1$
		writer.print(createHeaderProperty("Server", SERVER_NAME)); //$NON-NLS-1$
		//writer.print(createHeaderProperty("Accept-Ranges", "bytes")); //$NON-NLS-1$ //$NON-NLS-2$

		if (keepAlive) {
			writer.print(createHeaderProperty("Connection", "Keep-Alive")); //$NON-NLS-1$//$NON-NLS-2$
			writer.print(createHeaderProperty("Keep-Alive", "timeout=10, max=" + keepAliveCount)); //$NON-NLS-1$//$NON-NLS-2$
		}

		if (!chunkedTransfer && datalength == 0) {
			datalength = data != null ? data.available() : 0;
		}

		if (gzip) {
			writer.print(createHeaderProperty("Content-Encoding", "gzip")); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(createHeaderProperty("Vary", "Accept-Encoding")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!chunkedTransfer && datalength > 0)
			writer.print(createHeaderProperty(
					"Content-Length", Long.toString(datalength))); //$NON-NLS-1$

		if (chunkedTransfer)
			writer.print(createHeaderProperty("Transfer-Encoding", "chunked")); //$NON-NLS-1$ //$NON-NLS-2$

		writer.print(CRLF);
		writer.flush();
		
		if(request != null)
			request.setResponseCode(returnCode.getCode());

		long responseLength = sendContent(outputStream);
		
		if(request != null)
			request.setResponseLength(responseLength);
	}
}
