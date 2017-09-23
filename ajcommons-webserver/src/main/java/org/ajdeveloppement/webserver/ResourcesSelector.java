/*
 * Créé le 16 mars 2014 à 14:17:46 pour AjCommons (Bibliothèque de composant communs)
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
package org.ajdeveloppement.webserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Resources dispatcher for an http request
 * 
 * @author Aurelien JEOFFRAY
 *
 */
public interface ResourcesSelector {
	
	DeploymentService getDeploymentService();
	void setDeploymentService(DeploymentService deploymentService);
	
	public void resetClassLoader(String webAppDir);
	
	public ClassLoader getClassLoader(String webAppDir) throws IOException;
	
	/**
	 * @return the webAppsDirectories
	 */
	public List<String> getWebAppsName();
	
	public List<Host> getHosts();

	public void setHosts(List<Host> hosts);
	
	/**
	 * @return the basePath
	 */
	public File getBasePath();

	/**
	 * @param basePath the basePath to set
	 */
	public void setBasePath(File basePath);
	
	public Host selectHost(HttpRequest httpRequest);
	
	public Alias selectAlias(HttpRequest httpRequest);
	
	public File selectBasePath(String webAppName) throws IOException;
	
	public File selectWebAppPersistentDirectory(String webAppName);
	
	public String selectWebApp(HttpRequest httpRequest);
	
	public String selectHostAliasKey(String webAppName);
	
	public RewriteUrlRules getRewriteUrlRules(HttpRequest httpRequest) throws JAXBException, IOException;
	
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Host {
		@XmlAttribute(name="default")
		private boolean defaultHost = false;
		
		@XmlAttribute(name="pattern")
		private String hostPattern;
		
		private String resourcesPath;
		
		@XmlElement(name="alias")
		private List<Alias> listAlias = new ArrayList<>();

		public String getHostPattern() {
			return hostPattern;
		}

		public void setHostPattern(String hostPattern) {
			this.hostPattern = hostPattern;
		}

		public boolean isDefaultHost() {
			return defaultHost;
		}

		public void setDefaultHost(boolean defaultHost) {
			this.defaultHost = defaultHost;
		}

		public String getResourcesPath() {
			return resourcesPath;
		}

		public void setResourcesPath(String resourcesPath) {
			this.resourcesPath = resourcesPath;
		}

		public List<Alias> getListAlias() {
			return listAlias;
		}

		public void setListAlias(List<Alias> listAlias) {
			this.listAlias = listAlias;
		}
		
		public boolean match(String host) {
			if(host != null)
				return host.matches(hostPattern);
			
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (defaultHost ? 1231 : 1237);
			result = prime * result
					+ ((hostPattern == null) ? 0 : hostPattern.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Host other = (Host) obj;
			if (defaultHost != other.defaultHost)
				return false;
			if (hostPattern == null) {
				if (other.hostPattern != null)
					return false;
			} else if (!hostPattern.equals(other.hostPattern))
				return false;
			return true;
		}
	}
	
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Alias {
		@XmlAttribute(name="prefix")
		private String aliasPrefix;
		
		private String resourcesPath;
		
		private static Alias defaultAlias = new Alias();

		public String getAliasPrefix() {
			return aliasPrefix;
		}

		public void setAliasPrefix(String aliasPrefix) {
			this.aliasPrefix = aliasPrefix;
		}

		public String getResourcesPath() {
			return resourcesPath;
		}

		public void setResourcesPath(String resourcesPath) {
			this.resourcesPath = resourcesPath;
		}
		
		public String supressAlias(String uri) {
			return uri.substring(aliasPrefix.length());
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((aliasPrefix == null) ? 0 : aliasPrefix.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Alias other = (Alias) obj;
			if (aliasPrefix == null) {
				if (other.aliasPrefix != null)
					return false;
			} else if (!aliasPrefix.equals(other.aliasPrefix))
				return false;
			return true;
		}

		public static Alias getDefaultAlias() {
			return defaultAlias;
		}
	}
}
