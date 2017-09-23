/*
 * Créé le 12 oct. 2013 à 11:43:14 pour AjCommons (Bibliothèque de composant communs)
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

/**
 * Enum of HTTP return code
 * 
 * @author "Aurélien JEOFFRAY"
 *
 */
public class HttpReturnCode {
	
	/**
	 * Return Code base interface
	 */
	public interface ReturnCode {
		public int getCode();
		public String getDescription();
	}
	
	/**
	 * HTTP informations code (1xx)
	 */
	@SuppressWarnings("nls")
	public enum Information implements ReturnCode {
		Continue(100, "Continue"),
		SwitchingProtocols(101, "Switching Protocols"),
		Processing(102, "Processing"),
	 	ConnectionTimedOut(118, "Connection timed out");
		
		private int code;
		private String description;
		
		private Information(int code, String description) {
			this.code = code;
			this.description = description;
		}
		
		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}
	
	/**
	 * HTTP succces code (2xx)
	 */
	@SuppressWarnings("nls")
	public enum Success implements ReturnCode {
		OK(200, "OK"), 
		CREATED(201, "Created"),
		NoContent(204, "No Content");
		
		private int code;
		private String description;
		
		private Success(int code, String description) {
			this.code = code;
			this.description = description;
		}

		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getDescription() {
			return description;
		}
		
	}
	
	/**
	 * HTTP redirect code (3xx)
	 */
	@SuppressWarnings("nls")
	public enum Redirection implements ReturnCode {
		
		MultipleChoices(300, "Multiple Choices"),
	 	MovedPermanently(301, "Moved Permanently"),
	 	MovedTemporarily(302, "Moved Temporarily"),
	 	SeeOther(303, "See Other"),
		NotModified(304, "Not Modified"); 
		
		private int code;
		private String description;
		
		private Redirection(int code, String description) {
			this.code = code;
			this.description = description;
		}

		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}
	
	/**
	 * HTTP client origine error code (4xx)
	 */
	public enum ClientError implements ReturnCode {
	 	BadRequest(400, "Bad Request"), //$NON-NLS-1$
	 	Unauthorized(401, "Unauthorized"), //$NON-NLS-1$
	 	PaymentRequired(402, "Payment Required"), //$NON-NLS-1$
	 	Forbidden(403, "Forbidden"), //$NON-NLS-1$
	 	NotFound(404, "Not Found"), //$NON-NLS-1$
	 	RequestEntityTooLarge(413, "Request Entity Too Large"), //$NON-NLS-1$
	 	RequestURITooLong(414, "Request-URI Too Long"), //$NON-NLS-1$
	 	ExpectationFailed(417, "Expectation Failed"),//$NON-NLS-1$
	 	RequestHeaderFieldsTooLarge(431, "Request Header Fields Too Large"); //$NON-NLS-1$
	 	
	 	private int code;
		private String description;
	 	
	 	private ClientError(int code, String description) {
	 		this.code = code;
	 		this.description = description;
	 	}
	 	
	 	/**
		 * @return the code
		 */
	 	@Override
		public int getCode() {
			return code;
		}

		/**
		 * @return the description
		 */
		@Override
		public String getDescription() {
			return description;
		}
	}
	
	/**
	 * HTTP server origine error code (5xx)
	 */
	public enum ServerError implements ReturnCode {
		InternalServerError(500, "Internal StandAloneWebServer Error"); //$NON-NLS-1$
		
		private int code;
		private String description;
		
		/**
		 * @param code
		 * @param description
		 */
		private ServerError(int code, String description) {
			this.code = code;
			this.description = description;
		}
		
		/**
		 * @return the code
		 */
		@Override
		public int getCode() {
			return code;
		}
		
		/**
		 * @return the description
		 */
		@Override
		public String getDescription() {
			return description;
		}
		
		
	}
}
