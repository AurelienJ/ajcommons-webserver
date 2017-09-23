/*
 * Créé le 14 juil. 2016 à 14:27:00 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2016 - Aurélien JEOFFRAY
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ajdeveloppement.commons.Converters;
import org.ajdeveloppement.commons.io.FileUtils;

/**
 * @author aurelien
 *
 */
public class DefaultDeploymentService implements DeploymentService {

	private File basePath;
	
	private transient Map<String, File> webAppsCurrentInstall = new HashMap<>();
	
	private List<DeploymentServiceListener> listeners = new ArrayList<>();
	
	@Override
	public void setBasePath(File basePath) {
		this.basePath = basePath;
	}
	
	@Override
	public void addDeploymentServiceListener(DeploymentServiceListener listener) {
		if(!listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public void removeDeploymentServiceListener(DeploymentServiceListener listener) {
		listeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.ajdeveloppement.webserver.DeploymentService#deployAll()
	 */
	@SuppressWarnings("nls")
	@Override
	public void deployAll() throws IOException {
		cleanOldDeployments();

		//search all existing war to unpack
		if(basePath != null && basePath.exists() && basePath.isDirectory()) {
			File[] webPackedDirectories = basePath.listFiles(f -> f.isFile() && !f.getName().startsWith(".") && f.getName().toLowerCase().endsWith(".war"));
			for(File webPackedArchive : webPackedDirectories) {
				deploy(webPackedArchive);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.ajdeveloppement.webserver.DeploymentService#deploy(java.io.File)
	 */
	@SuppressWarnings("nls")
	@Override
	public void deploy(File webPackedArchive) throws IOException {
		if(basePath == null)
			return;
		
		//use name of war file as webapp directory an webapp name
		String appName = webPackedArchive.getName().substring(0, webPackedArchive.getName().length()-4);
		File appBaseDirectory = new File(basePath, appName);
		File currentInstallDirectory = getCurrentInstallDir(appName);
		
		boolean unpack = true;
		
		//calculate hash of war package
		String hash = Converters.byteArrayToHexString(FileUtils.getSHA256Hash(webPackedArchive));
		
		//search hash of already unpacked webapp if exist and compare results
		//if equal do not unpack
		File hashFile = null;
		if(currentInstallDirectory != null) {
			hashFile = new File(currentInstallDirectory, "hash.txt");
			if(hashFile.exists()) {
				String dirHash = Files.lines(hashFile.toPath()).collect(Collectors.joining());
				if(dirHash.equals(hash))
					unpack = false;
			}
		}
		
		if(unpack) {
			//create a subdirectory for new version with timestamp
			String newInstallDir = String.valueOf(Instant.now().toEpochMilli());
			File installDir = new File(appBaseDirectory, newInstallDir);
			if(!installDir.exists())
				installDir.mkdirs();
			
			//unzip webapp archive in the new installdir
			FileUtils.unzipToDirectory(webPackedArchive, installDir);

			//generate new hash file
			hashFile = new File(installDir, "hash.txt");
			try(PrintStream hashFileStream = new PrintStream(hashFile)) {
				hashFileStream.print(hash);
			}
			
			//change current to new installDir
			File currentInstallDirFile = new File(appBaseDirectory, "current");
			if(currentInstallDirFile.exists())
				currentInstallDirFile.delete();
			
			try(PrintStream currentInstallDirStream = new PrintStream(currentInstallDirFile)) {
				currentInstallDirStream.print(newInstallDir);
			}
			
			webAppsCurrentInstall.put(appName, installDir);
//			webAppsClassLoaders.remove(appName);
//			rewriteUrlRulesCache.remove(appName);
//			if(!webAppsDirectories.contains(appName))
//				webAppsDirectories.add(appName);
			fireWebAppDeployed(appName);
		}
	}
	
	/**
	 * Return the current webapp installation directory
	 * 
	 * @param webApp
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("nls")
	@Override
	public File getCurrentInstallDir(String webApp) throws IOException {
		if(webApp == null || basePath == null)
			return null;
		
		if(webAppsCurrentInstall.containsKey(webApp))
			return webAppsCurrentInstall.get(webApp);
		
		File webAppDir = new File(basePath, webApp);
		
		File currentFile = new File(webAppDir, "current");
		if(!currentFile.exists())
			return null;
		
		String dirInstall = Files.lines(currentFile.toPath()).collect(Collectors.joining());
		
		File webAppCurrentInstallDir = new File(webAppDir, dirInstall);
		webAppsCurrentInstall.put(webApp, webAppCurrentInstallDir);
		
		return webAppCurrentInstallDir;
	}
	
	@SuppressWarnings("nls")
	private void cleanOldDeployments() throws IOException {
		if(basePath != null && basePath.exists() && basePath.isDirectory()) {
			//find all webapp dir
			File[] webDirectories = basePath.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
			for(File webDirectory : webDirectories) {
				String webAppName = webDirectory.getName();
				
				File currentInstallDir = getCurrentInstallDir(webAppName);
				for(File oldDeployment : webDirectory.listFiles(f -> f.isDirectory() 
						&& !f.equals(currentInstallDir) 
						&& !f.getName().startsWith(".")
						&& !f.getName().equals("persistent"))) {
					FileUtils.deleteFilesPath(oldDeployment);
				}
			}
		}
	}
	
	private void fireWebAppDeployed(String appName) {
		for(DeploymentServiceListener listener: listeners)
			listener.webAppDeployed(appName);
	}
}
