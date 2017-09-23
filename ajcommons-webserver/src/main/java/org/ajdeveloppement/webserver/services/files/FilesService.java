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
package org.ajdeveloppement.webserver.services.files;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import org.ajdeveloppement.commons.AjResourcesReader;
import org.ajdeveloppement.commons.io.FileUtils;
import org.ajdeveloppement.commons.persistence.ObjectPersistenceException;
import org.ajdeveloppement.commons.persistence.sql.QField;
import org.ajdeveloppement.commons.persistence.sql.QResults;
import org.ajdeveloppement.commons.persistence.sql.ResultRow;
import org.ajdeveloppement.webserver.HttpException;
import org.ajdeveloppement.webserver.HttpRequest;
import org.ajdeveloppement.webserver.HttpResponse;
import org.ajdeveloppement.webserver.HttpReturnCode;
import org.ajdeveloppement.webserver.HttpReturnCode.ClientError;
import org.ajdeveloppement.webserver.HttpReturnCode.Redirection;
import org.ajdeveloppement.webserver.HttpReturnCode.Success;
import org.ajdeveloppement.webserver.HttpServer;
import org.ajdeveloppement.webserver.ResourcesSelector;
import org.ajdeveloppement.webserver.ResourcesSelector.Alias;
import org.ajdeveloppement.webserver.data.Request;
import org.ajdeveloppement.webserver.data.Resources;
import org.ajdeveloppement.webserver.data.T_Request;
import org.ajdeveloppement.webserver.data.T_Resources;
import org.ajdeveloppement.webserver.services.RequestProcessor;
import org.ajdeveloppement.webserver.utils.StackTraceUtil;

/**
 * Service for ajcommons webserver use to serve static files
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public class FilesService implements RequestProcessor {
	private static final String MIMES_TYPE_CONFIG = "mimes"; //$NON-NLS-1$
	private static final String DEFAULT_PAGE_NAME = "index.html"; //$NON-NLS-1$
	
	//private static File defaultBasePath;
	private static List<String> allowedGzipExt = new ArrayList<String>();
	private static String defaultPageName = DEFAULT_PAGE_NAME;
	
	
	private static AjResourcesReader mimesTypes = new AjResourcesReader(MIMES_TYPE_CONFIG);
	
	
	private ReentrantLock lock = new ReentrantLock();
	
	private Map<String, byte[]> filesBuffer = new HashMap<String, byte[]>();
	private ResourcesSelector fileSelector;
	
	/**
	 * Add the numbers of hours specfied by "hours" to given date.
	 * 
	 * @param date the date to add hours
	 * @param hours the number added hours. Can be negatif
	 * @return the new date with the numbers hours added (or remove if minus)
	 */
	private static Date addHours(Date date, int hours)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, hours); //minus number would decrement the hours
        return cal.getTime();
    }

	/**
	 * For performance stats, insert requested resources properties to database.
	 * 
	 * @param returnFile
	 * @throws ObjectPersistenceException 
	 * @throws IOException 
	 */
	private synchronized void insertRequestInDb(HttpRequest httpRequest, File returnFile) throws ObjectPersistenceException, IOException {
		Resources fileResource = QResults.from(Resources.class)
				.where(T_Resources.PATH.equalTo(returnFile.getAbsolutePath()))
				//.useContext(new SqlContext()) //now SqlContext is thread safe
				.first();
		BasicFileAttributes fileAttributes = Files.readAttributes(Paths.get(returnFile.toURI()), BasicFileAttributes.class);
		if(fileResource == null) {
			fileResource = new Resources();
			fileResource.setPath(returnFile.getAbsolutePath());
		}
	
		fileResource.setLength(returnFile.length());
		fileResource.setCreateDate(new Date(fileAttributes.creationTime().toMillis()));
		fileResource.setLastModifiedDate(new Date(returnFile.lastModified()));
		
		fileResource.save();

		if(httpRequest.getRequest() != null) {
			httpRequest.getRequest().setResourcePath(returnFile.getAbsolutePath());
		}
	}

	/**
	 * Return the name of default page (file that respond to "/" uri - by default index.html)
	 * 
	 * @return the defaultPageName
	 */
	public static String getDefaultPageName() {
		return defaultPageName;
	}

	/**
	 * Define the name of default page (file that respond to "/" uri)
	 * @param defaultPageName the defaultPageName to set
	 */
	public static void setDefaultPageName(String defaultPageName) {
		FilesService.defaultPageName = defaultPageName;
	}

	/**
	 * get list of file extension that can be served in gzip stream
	 * 
	 * @return List of file extension that can be served in gzip stream
	 */
	public static List<String> getAllowedGzipExt() {
		return allowedGzipExt;
	}

	/**
	 * define list of file extension that can be served in gzip stream
	 * 
	 * @param allowedGzipExt List of file extension that can be served in gzip stream
	 */
	public static void setAllowedGzipExt(List<String> allowedGzipExt) {
		FilesService.allowedGzipExt = allowedGzipExt;
	}
	
	/**
	 * Return memory cached files path by service
	 * 
	 * @return memory cached files path by service
	 */
	public Set<String> getCachedFiles() {
		return filesBuffer.keySet();
	}

	/**
	 * Start init service
	 */
	@SuppressWarnings("nls")
	@Override
	public void init(HttpServer server) {
		fileSelector = server.getFileSelector();
		
		//Prend les 10 resources static les plus utilisées sur la dernière heure
		//et de moins de 1Mo et les met en cache en mémoire vive pour plus de performance
		Thread t = new Thread(() -> {
			while(true) {
				try {
					Thread.sleep(1000 * 60 * 10); //10min
					
					//SELECT a.Path,count(*) as NbRequest FROM Request a
					//INNER JOIN Resources b ON a.Path=b.Path
					//WHERE Length < 1024 * 1024 and RequestDate > DATEADD('HOUR', -1, NOW())
					//GROUP BY a.Path ORDER BY count(*) DESC LIMIT 10;
					
					QField<Integer> nbRequest = QField.<Integer>custom(null, "*", "NbRequest", "count"); 
					
					Iterable<ResultRow> topTenResources = QResults.from(Request.class)
							.innerJoin(Resources.class, T_Request.RESOURCEPATH.equalTo(T_Resources.PATH))
							.where(T_Resources.LENGTH.lowerThan(1024l * 1024l)
									.and(T_Request.DATE.upperThan(addHours(new Date(), -1))))
							.limit(10)
							.orderBy(nbRequest.toOrderByDesc())
							.groupBy(T_Request.RESOURCEPATH)
							.select(T_Request.RESOURCEPATH, nbRequest);
					
					List<String> topTenUri = new ArrayList<String>();
					topTenResources.forEach(resource -> topTenUri.add(resource.getValue(T_Request.RESOURCEPATH)));

					String[] cachedFiles = filesBuffer.keySet().toArray(new String[filesBuffer.size()]);
					for(String cachedFile : cachedFiles) {
						if(!topTenUri.contains(cachedFile))
							filesBuffer.remove(cachedFile);
					}
					
					for(String uri : topTenUri) {
						if(uri != null && !filesBuffer.containsKey(uri)) {
							try {
								File filePath = new File(uri);
								if(filePath.exists()) {
									byte[] fileContent = new byte[(int)filePath.length()];
									
									try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath))) {
										in.read(fileContent, 0, fileContent.length);
									}
	
									filesBuffer.put(uri, fileContent);
								}
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				} catch (InterruptedException | SQLException e) {
				}
			}
		});
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	/**
	 * Return if service can serve the requested resources. The FilesService return always true to serve.
	 * 
	 * @return always <code>true</code> for this service
	 */
	@Override
	public boolean canServe(HttpRequest httpRequest) {
		return fileSelector != null;
	}

	/**
	 * Serve file content if  exists
	 */
	@Override
	public HttpResponse serve(HttpRequest httpRequest) throws HttpException {
		SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US); //$NON-NLS-1$
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		
		boolean isGzipedRessources = false;
		boolean isCachedRessources = false;
		boolean useDefaultPageName = false;
		
		String requestUri = httpRequest.getRequestUri();
		
		File path = null;

		//determine la base correspondant à l'alias de la requete
		try {
			File basePath = fileSelector.selectBasePath(fileSelector.selectWebApp(httpRequest));
	
			String pathString = basePath + File.separator + "www"; //$NON-NLS-1$
			if(pathString != null && !pathString.isEmpty()) {
				Alias alias = fileSelector.selectAlias(httpRequest);
				if(alias != null)
					requestUri = alias.supressAlias(requestUri);
				path = new File(pathString, requestUri);
			}
			
			if(!path.getAbsolutePath().startsWith(new File(pathString).getAbsolutePath()))
				return new HttpResponse(ClientError.Forbidden, "text/plain; charset=utf-8", "Requested Resources out of BaseDir");  //$NON-NLS-1$//$NON-NLS-2$
	
			
			if (path.isDirectory() && !requestUri.endsWith("/")) { //$NON-NLS-1$
				HttpResponse response = new HttpResponse(Redirection.MovedPermanently, "text/plain; charset=utf-8", (InputStream)null);  //$NON-NLS-1$
				response.addHeader("Location", httpRequest.getRequestUri() + "/");  //$NON-NLS-1$//$NON-NLS-2$
				return response; 
			}
			
			if (path.isDirectory()) {
				path = new File(path, defaultPageName); 
				useDefaultPageName = true;
			}

			String extension = path.getName().substring(path.getName().lastIndexOf('.')+1).toLowerCase();
			
			//Filtre minifier -> retourne le path du fichier minifié
			
			File gzipedFile = null;
			if(httpRequest.acceptGzipEncoding() && allowedGzipExt.contains(extension)) {
				File gzipCachePath = new File(basePath, "cache"); //$NON-NLS-1$
				gzipedFile = new File(gzipCachePath, requestUri + ".gz"); //$NON-NLS-1$
				if(useDefaultPageName)
					gzipedFile = new File(gzipedFile, defaultPageName + ".gz"); //$NON-NLS-1$
				
				if(!gzipedFile.exists() || gzipedFile.lastModified() < path.lastModified()) {
					lock.lock();
					try {
						if(path.exists() && path.isFile()) {
							gzipedFile.getParentFile().mkdirs();
							
							try {
								FileUtils.compressFile(path, gzipedFile);
								
								if(filesBuffer.containsKey(gzipedFile.getAbsolutePath()))
									filesBuffer.remove(gzipedFile.getAbsolutePath());
								
								isGzipedRessources = true;
							} catch (IOException e) {
							}
						}
					} finally {
						lock.unlock();
					}
				} else {
					isGzipedRessources = true;
					if(filesBuffer.containsKey(gzipedFile.getAbsolutePath()))
						isCachedRessources = true;
				}
			} else {
				if(filesBuffer.containsKey(path.getAbsolutePath()))
					isCachedRessources = true;
			}
			
			InputStream stream = null;
			String mimeType = "application/octet-stream"; //$NON-NLS-1$
			
			if (path.exists() && path.isFile()) {
				File returnFile = isGzipedRessources ? gzipedFile : path;
				
				if(httpRequest.getHeaderValues().containsKey("if-modified-since")) { //$NON-NLS-1$
					try {
						Date dateLastModified = gmtFrmt.parse(httpRequest.getHeaderValues().get("if-modified-since")); //$NON-NLS-1$
						if(path.lastModified() / 1000 <= dateLastModified.getTime() / 1000) {
							HttpResponse response = new HttpResponse(Redirection.NotModified, mimeType, (InputStream)null);
							return response;
						}
					} catch (ParseException e) {
					}
				}
				
				try {
					insertRequestInDb(httpRequest, returnFile);
				} catch (ObjectPersistenceException | IOException e1) {
					e1.printStackTrace();
				}
				
				try {
					
					if(!isCachedRessources)
						stream = new FileInputStream(isGzipedRessources ? gzipedFile : path);
					else
						stream =  new ByteArrayInputStream(filesBuffer.get(returnFile.getAbsolutePath()));
					
					if(extension != null && !extension.isEmpty() && mimesTypes.containsKey(extension)) {
						mimeType = mimesTypes.getResourceString(extension);
					}
				} catch(IOException e) {
					throw new HttpException(HttpReturnCode.ServerError.InternalServerError, StackTraceUtil.getStackTrace(e));
				}
			}
	
			if(stream != null) {
				
				HttpResponse response = new HttpResponse(Success.OK, mimeType, stream, (isGzipedRessources ? gzipedFile : path).length());
				response.addHeader("Last-Modified", gmtFrmt.format(path.lastModified())); //$NON-NLS-1$
				//response.addHeader("ETag","\"" + Converters.byteArrayToHexString(FileUtils.getSHA256Hash(isGzipedRessources ? gzipedFile : path)) + "\""); //$NON-NLS-1$
				if(isGzipedRessources)
					response.setGzip(true);
				
				return response;
			}
		} catch(IOException e) {
			throw new HttpException(HttpReturnCode.ServerError.InternalServerError, StackTraceUtil.getStackTrace(e));
		}
		
		return null;
	}
}
