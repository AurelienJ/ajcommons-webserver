/**
 * 
 */
package org.ajdeveloppement.webserver.viewbinder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aurelien
 *
 */
public class Description {
	private String key;
	private List<Reference> references = new ArrayList<>();
	private List<Collection> collections = new ArrayList<>(); 
	
	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the references
	 */
	public List<Reference> getReferences() {
		return references;
	}

	/**
	 * @param references the references to set
	 */
	public void setReferences(List<Reference> references) {
		this.references = references;
	}

	/**
	 * @return the collections
	 */
	public List<Collection> getCollections() {
		return collections;
	}

	/**
	 * @param collections the collections to set
	 */
	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	public static class Reference {
		private String name;
		
		private String key;
		
		public Reference(String name, String key) {
			this.name = name;
			this.key = key;
		}
		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @param key the key to set
		 */
		public void setKey(String key) {
			this.key = key;
		}
	}
	
	public static class Collection {
		private String name;
		
		private String key;

		/**
		 * @param name the name of collection
		 * @param key 
		 */
		public Collection(String name, String key) {
			super();
			this.name = name;
			this.key = key;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @param key the key to set
		 */
		public void setKey(String key) {
			this.key = key;
		}
	}
}
