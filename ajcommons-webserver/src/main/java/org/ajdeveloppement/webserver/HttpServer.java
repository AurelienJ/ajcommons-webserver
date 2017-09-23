/*
 * Créé le 11 oct. 2013 à 23:20:16 pour AjCommons (Bibliothèque de composant communs)
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.ajdeveloppement.commons.UncheckedException;
import org.ajdeveloppement.commons.persistence.ObjectPersistenceException;
import org.ajdeveloppement.commons.persistence.sql.QResults;
import org.ajdeveloppement.commons.persistence.sql.SqlContext;
import org.ajdeveloppement.commons.security.SSLUtils;
import org.ajdeveloppement.commons.sql.SqlManager;
import org.ajdeveloppement.webserver.data.DatabaseParameters;
import org.h2.tools.RunScript;

/**
 * @author aurelien
 *
 */
public class HttpServer {

	public static final String WEBSERVER_DB_DOMAIN = "AjCommonsWebServer"; //$NON-NLS-1$

	private int listenPort = 0;
	private int listenSslPort = 0;
	private InetAddress bindAddress;
	private int maximumIncomingQueue = 0;
	private Logger logger;
	private AccessVerifier accessVerifier;
	private ResourcesSelector fileSelector;
	private HttpRequestProcessor requestProcessor;

	private String pkcs12KeyStorePath;
	private String pkcs12KeyStorePassword;
	private String certificateAlias = "1"; //$NON-NLS-1$

	private ServerSocket plainServerSocket;
	private ServerSocket sslServerSocket;
	private boolean cancellationRequested = false;

	public HttpServer() {
		this(0, null);
	}

	public HttpServer(int listenPort) {
		this(listenPort, null);
	}

	public HttpServer(int listenPort, InetAddress bindAddress) {
		this.listenPort = listenPort;
		this.bindAddress = bindAddress;

		//TODO use java inject service
		loadControlAccessScript();

		//TODO use java inject service
		try {
			createTables();
			setLogger(new DbLogger());
		} catch (SQLException | IOException | ObjectPersistenceException e) {
			throw new UncheckedException(e);
		}
	}

	private static void createTables()
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

	private void loadControlAccessScript() {
		try {
			URL acccessScriptFile = getClass().getResource("/access.js"); //$NON-NLS-1$
			if (acccessScriptFile != null) {
				String script = ""; //$NON-NLS-1$
				for (String line : Files.readAllLines(Paths.get(acccessScriptFile.toURI()), Charset.forName("UTF-8"))) { //$NON-NLS-1$
					script += line + "\n"; //$NON-NLS-1$
				}

				accessVerifier = new AccessVerifier();
				accessVerifier.setAccessScript(script);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void socketListen(ServerSocketFactory factory, int port) {
		try {
			try (ServerSocket serverSocket = factory.createServerSocket(port, maximumIncomingQueue, bindAddress)) {
				if (serverSocket instanceof SSLServerSocket && factory instanceof SSLServerSocketFactory) {
					System.out.println(Arrays.toString(((SSLServerSocketFactory) factory).getSupportedCipherSuites()));

					@SuppressWarnings("resource")
					SSLServerSocket sslServerSocket = ((SSLServerSocket) serverSocket);

					sslServerSocket
							.setEnabledCipherSuites(((SSLServerSocketFactory) factory).getSupportedCipherSuites());

					// SNIMatcher matcher =
					// SNIHostName.createSNIMatcher("www\\.example\\.(com|org)");
					// Collection<SNIMatcher> matchers =
					// Collections.singletonList(matcher);

					SSLParameters params = sslServerSocket.getSSLParameters();
					params.setUseCipherSuitesOrder(true);
					// params.setSNIMatchers(matchers);
					sslServerSocket.setSSLParameters(params);

					if (port == 0)
						listenSslPort = serverSocket.getLocalPort();

					this.sslServerSocket = serverSocket;
				} else {
					if (port == 0)
						listenPort = serverSocket.getLocalPort();
					this.plainServerSocket = serverSocket;
				}

				System.out.println("Listen on port tcp : " + serverSocket.getLocalPort()); //$NON-NLS-1$

				while (!serverSocket.isClosed() && !cancellationRequested) {
					try {
						Socket socket = serverSocket.accept();
						socket.setSoTimeout(15000);
						if (socket instanceof SSLSocket) {

						}

						if (socket != null)
							HttpSession.openSession(this, socket);
					} catch (Exception e) {
						if (e instanceof SocketException)
							System.out.println("End server listen"); //$NON-NLS-1$
						else
							e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void plainSocketListen() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		socketListen(factory, listenPort);
	}

	private void tlsSocketListen() {
		try {
			if (pkcs12KeyStorePassword == null || pkcs12KeyStorePath == null || pkcs12KeyStorePath.isEmpty())
				return;

			char[] password = pkcs12KeyStorePassword.toCharArray();

			KeyStore keystore = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
			keystore.load(this.getClass().getResourceAsStream(pkcs12KeyStorePath), password);

			SSLServerSocketFactory socketFactory = SSLUtils.getSSLServerSocketFactory(keystore, certificateAlias,
					password);

			socketListen(socketFactory, listenSslPort);
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param maximumIncomingQueue
	 */
	public void setMaximumIncomingQueue(int maximumIncomingQueue) {
		this.maximumIncomingQueue = maximumIncomingQueue;
	}

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @param logger
	 *            the logger to set
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * @return the accessVerifier
	 */
	public AccessVerifier getAccessVerifier() {
		return accessVerifier;
	}

	/**
	 * @param accessVerifier
	 *            the accessVerifier to set
	 */
	public void setAccessVerifier(AccessVerifier accessVerifier) {
		this.accessVerifier = accessVerifier;
	}

	/**
	 * @return the fileSelector
	 */
	public ResourcesSelector getFileSelector() {
		return fileSelector;
	}

	/**
	 * @param fileSelector
	 *            the fileSelector to set
	 */
	public void setFileSelector(ResourcesSelector fileSelector) {
		this.fileSelector = fileSelector;
	}

	/**
	 * The Request processor use to threat HTTP requests
	 * 
	 * @return Request processor use to threat HTTP requests
	 */
	public HttpRequestProcessor getRequestProcessor() {
		return requestProcessor;
	}

	/**
	 * @param requestProcessor
	 *            the requestProcessor to set
	 */
	public void setRequestProcessor(HttpRequestProcessor requestProcessor) {
		this.requestProcessor = requestProcessor;
		if (requestProcessor != null)
			this.requestProcessor.setServer(this);
	}

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	/**
	 * @return the listenSslPort
	 */
	public int getListenSslPort() {
		return listenSslPort;
	}

	/**
	 * @param listenSslPort
	 *            the listenSslPort to set
	 */
	public void setListenSslPort(int listenSslPort) {
		this.listenSslPort = listenSslPort;
	}

	/**
	 * @return the pkcs12KeyStorePath
	 */
	public String getPkcs12KeyStorePath() {
		return pkcs12KeyStorePath;
	}

	/**
	 * @param pkcs12KeyStorePath
	 *            the pkcs12KeyStorePath to set
	 */
	public void setPkcs12KeyStorePath(String pkcs12KeyStorePath) {
		this.pkcs12KeyStorePath = pkcs12KeyStorePath;
	}

	/**
	 * @return the pkcs12KeyStorePassword
	 */
	public String getPkcs12KeyStorePassword() {
		return pkcs12KeyStorePassword;
	}

	/**
	 * @param pkcs12KeyStorePassword
	 *            the pkcs12KeyStorePassword to set
	 */
	public void setPkcs12KeyStorePassword(String pkcs12KeyStorePassword) {
		this.pkcs12KeyStorePassword = pkcs12KeyStorePassword;
	}

	/**
	 * @return the certificateAlias
	 */
	public String getCertificateAlias() {
		return certificateAlias;
	}

	/**
	 * @param certificateAlias
	 *            the certificateAlias to set
	 */
	public void setCertificateAlias(String certificateAlias) {
		this.certificateAlias = certificateAlias;
	}

	public InetAddress getBindAddress() {
		return bindAddress;
	}

	public void setBindAddress(InetAddress bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * @return the cancellationRequested
	 */
	public boolean isCancellationRequested() {
		return cancellationRequested;
	}

	public void start() {
		start(false);
	}

	/**
	 * 
	 */
	@SuppressWarnings("nls")
	public void start(boolean asDaemon) {
		Thread t = new Thread(() -> plainSocketListen());
		t.setName("Plain-socket-listener-thread");
		t.setDaemon(asDaemon);
		t.start();

		Thread ttls = new Thread(() -> tlsSocketListen());
		ttls.setName("Tls-socket-listener-thread");
		ttls.setDaemon(asDaemon);
		ttls.start();
	}

	public void stop() throws IOException {
		cancellationRequested = true;
		if (plainServerSocket != null && !plainServerSocket.isClosed())
			plainServerSocket.close();
		if (sslServerSocket != null && !sslServerSocket.isClosed())
			sslServerSocket.close();
	}
}
