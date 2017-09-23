/*
 * Créé le 2 mars 2014 à 14:15:49 pour AjCommons (Bibliothèque de composant communs)
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
package org.ajdeveloppement.webserver.logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.ajdeveloppement.commons.persistence.ObjectPersistenceException;
import org.ajdeveloppement.commons.persistence.sql.QResults;
import org.ajdeveloppement.commons.persistence.sql.SqlContext;
import org.ajdeveloppement.commons.sql.SqlManager;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.Logger;
import org.ajdeveloppement.webserver.data.DatabaseParameters;
import org.ajdeveloppement.webserver.data.Request;
import org.h2.tools.RunScript;


/**
 * Helper to save request metadata log ind webserver database
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public class DbLogger implements Logger {
	
	public static final String WEBSERVER_DB_DOMAIN = "AjCommonsWebServer"; //$NON-NLS-1$
	
	private Executor executor = Executors.newSingleThreadExecutor(); 
	
	
	public DbLogger() throws FileNotFoundException, IOException, SQLException, ObjectPersistenceException {
		createTables();
	}
	
	private void createTables()
			throws FileNotFoundException, IOException, SQLException, ObjectPersistenceException {
		Connection dbConnection = SqlContext.getDefaultContext().getConnectionForDomain(WEBSERVER_DB_DOMAIN);
		if (dbConnection != null && dbConnection.isValid(10)) {
			DatabaseMetaData dbMetaData = dbConnection.getMetaData();

			try (ResultSet rs = dbMetaData.getTables(null, "AJWEBSERVER", "PARAM", new String[] { "TABLE" })) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (!rs.next()) {
					URL scriptUrl = HttpServer.class.getResource("/META-INF/persistence/createDb.sql"); //$NON-NLS-1$
					if (scriptUrl != null) {
						URLConnection urlConnection = scriptUrl.openConnection();
						if (urlConnection != null) {
							try (Reader reader = new BufferedReader(
									new InputStreamReader(urlConnection.getInputStream()))) {
								RunScript.execute(dbConnection, reader);
							}
						}
					}
				} else {
					SqlManager sqlManager = new SqlManager(dbConnection, null);
					DatabaseParameters databaseParameters = QResults.from(DatabaseParameters.class).first();
					if (databaseParameters != null) {
						if (databaseParameters.getDbVersion() < 2) {
							sqlManager.executeUpdate("ALTER TABLE AJWEBSERVER.Request ADD Referer VARCHAR(255)"); //$NON-NLS-1$
						}

						if (databaseParameters.getDbVersion() < 3) {
							sqlManager.executeUpdate(
									"CREATE TABLE IF NOT EXISTS AJWEBSERVER.SessionData (IdSession UUID NOT NULL, Data OTHER, PRIMARY KEY (IdSession));"); //$NON-NLS-1$
						}

						if (databaseParameters.getDbVersion() < 4) {
							sqlManager
									.executeUpdate("ALTER TABLE AJWEBSERVER.SessionData ADD ExpirationDate DATETIME;"); //$NON-NLS-1$
						}

						if (databaseParameters.getDbVersion() < 5) {
							sqlManager.executeUpdate("ALTER TABLE AJWEBSERVER.Request ADD Duration BIGINT;"); //$NON-NLS-1$
							sqlManager.executeUpdate("ALTER TABLE AJWEBSERVER.Request ADD Exception TEXT;"); //$NON-NLS-1$
						}

						if (databaseParameters.getDbVersion() < 6) {
							sqlManager.executeUpdate("ALTER TABLE AJWEBSERVER.Request ADD Host VARCHAR(255);"); //$NON-NLS-1$
						}

						if (databaseParameters.getDbVersion() < 7) {
							sqlManager.executeUpdate("ALTER TABLE AJWEBSERVER.Request ALTER Referer VARCHAR(512)"); //$NON-NLS-1$
							sqlManager.executeUpdate("UPDATE AJWEBSERVER.PARAM SET DBVERSION = 7;"); //$NON-NLS-1$
						}
					}
				}
			}
		}
	}
	
	/**
	 * Save a Request log entry in webserver database
	 * 
	 * @param request the request metadata to save in db
	 */
	@Override
	public void saveEntry(Request request) {
		executor.execute(() -> {
			try {
				if(request != null)
					System.out.println(request.toString());
				
				if(request != null)
					request.save();
			} catch (ObjectPersistenceException e) {
				e.printStackTrace();
			}
		});
	}
}
