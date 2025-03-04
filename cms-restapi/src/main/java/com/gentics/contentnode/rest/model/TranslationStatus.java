/**
 * 
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Class encapsulating the synchronization information of page translations
 * @author norbert
 */
@XmlRootElement
public class TranslationStatus implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8209212374300554292L;

	/**
	 * Id of the page with which the page is in sync
	 */
	protected Integer pageId;
    
	/**
	 * name of the page
	 */
	protected String name;

	/**
	 * Version Timestamp of the page version, with which the page is in sync
	 */
	protected Integer versionTimestamp;

	/**
	 * Language of the page, with which this page is in sync
	 */
	protected String language;

	/**
	 * True when the page still is in sync (the given version timestamp points to the latest version of the other page)
	 */
	protected boolean inSync;

	/**
	 * Version Number of the page version with which this page is in sync
	 */
	protected String version;

	/**
	 * Latest version of the page with which this page is in sync
	 */
	protected Latest latestVersion;

	/**
	 * Constructor used by JAXB
	 */
	public TranslationStatus() {}

	/**
	 * Page id of the page with which the given page is in sync
	 * @return the pageId
	 */
	public Integer getPageId() {
		return pageId;
	}

	/**
	 * Set the page id of the page with which the given page is in sync
	 * @param pageId the pageId to set
	 */
	public void setPageId(Integer pageId) {
		this.pageId = pageId;
	}
    
	/**
	 * Page name of the page with which the given page is in sync 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the page name of the page with which the given page is in sync
	 * @param name the name to be set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Version timestamp of the synchronized version
	 * @return the versionTimestamp
	 */
	public Integer getVersionTimestamp() {
		return versionTimestamp;
	}

	/**
	 * Set the version timestamp of the synchronized version
	 * @param versionTimestamp the versionTimestamp to set
	 */
	public void setVersionTimestamp(Integer versionTimestamp) {
		this.versionTimestamp = versionTimestamp;
	}

	/**
	 * Language of the synchronized version
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the synchronized version
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * True when the page is in sync with the latest version of the other language variant, false if not
	 * @return the inSync status
	 */
	public boolean isInSync() {
		return inSync;
	}

	/**
	 * Set whether the page is in sync with the latest version of the other language variant
	 * @param inSync the inSync status to set
	 */
	public void setInSync(boolean inSync) {
		this.inSync = inSync;
	}

	/**
	 *Version number of the page version, with which this page is in sync
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version number of the page, with which this page is in sync
	 * @param version version number
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Latest version information
	 * @return the latestVersion latest version
	 */
	public Latest getLatestVersion() {
		return latestVersion;
	}

	/**
	 * Set the latest version information
	 * @param latestVersion the latestVersion to set
	 */
	public void setLatestVersion(Latest latestVersion) {
		this.latestVersion = latestVersion;
	}

	/**
	 * Inner class to encapsulate the information about the latest version of the language variant
	 */
	public static class Latest {

		/**
		 * Version Timestamp of the latest page version
		 */
		protected int versionTimestamp;

		/**
		 * Version Number of the latest page version
		 */
		protected String version;

		/**
		 * Constructor for JAXB
		 */
		public Latest() {}

		/**
		 * Version timestamp
		 * @return the versionTimestamp
		 */
		public int getVersionTimestamp() {
			return versionTimestamp;
		}

		/**
		 * Set the version timestamp
		 * @param versionTimestamp the versionTimestamp to set
		 */
		public void setVersionTimestamp(int versionTimestamp) {
			this.versionTimestamp = versionTimestamp;
		}

		/**
		 * Version number
		 * @return the version
		 */
		public String getVersion() {
			return version;
		}

		/**
		 * Set the version number
		 * @param version the version to set
		 */
		public void setVersion(String version) {
			this.version = version;
		}
	}
}
