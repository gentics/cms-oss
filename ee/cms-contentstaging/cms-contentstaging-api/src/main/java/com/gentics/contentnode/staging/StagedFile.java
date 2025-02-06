package com.gentics.contentnode.staging;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST entity for the file, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedFile extends StagedLocalizableObject {

	public static final String TYPE_NAME = "file";

	private static final long serialVersionUID = -6697807378224359696L;

	/**
	 * Mimetype of the File
	 */
	protected String fileType;

	/**
	 * Description of the file
	 */
	protected String description;

	/**
	 * Filesize of the file
	 */
	protected Integer fileSize;

	/**
	 * Objecttags in the file (objecttags)
	 */
	protected Map<String, StagedObjectTag> objectTags;

	/**
	 * Nice URL of the file
	 */
	private String niceUrl;

	/**
	 * Alternate URLs
	 */
	private SortedSet<String> alternateUrls;

	/**
	 * True if the file shall be force to go online, even if nothing depends on it
	 * (may be null if status is undetermined).
	 */
	private Boolean forceOnline;

	/**
	 * Custom cdate
	 */
	private Integer customCdate;

	/**
	 * Custom edate
	 */
	private Integer customEdate;

	/**
	 * File type
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * File size
	 * @return the fileSize
	 */
	public Integer getFileSize() {
		return fileSize;
	}

	/**
	 * Map of object tags of this file
	 * @return
	 */
	public Map<String, StagedObjectTag> getObjectTags() {
		return objectTags;
	}

	/**
	 * @param fileType the fileType to set
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(Integer fileSize) {
		this.fileSize = fileSize;
	}

	public void setObjectTags(Map<String, StagedObjectTag> objectTags) {
		// the tags shall be stored in a treemap, so that they will be ordered in natural ordering of the keys (names)
		this.objectTags = objectTags != null ? new TreeMap<>(objectTags) : null;
	}

	/**
	 * Nice URL
	 * @return nice URL
	 */
	public String getNiceUrl() {
		return niceUrl;
	}

	/**
	 * Set the nice URL
	 * @param niceUrl nice URL
	 */
	public void setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
	}

	/**
	 * Alternate URLs (in alphabetical order)
	 * @return sorted alternate URLs
	 */
	public SortedSet<String> getAlternateUrls() {
		return alternateUrls;
	}

	/**
	 * Set the alternate URLs
	 * @param alternateUrls alternate URLs
	 */
	public void setAlternateUrls(SortedSet<String> alternateUrls) {
		this.alternateUrls = alternateUrls;
	}

	/**
	 * Flag for force online
	 * @return flag value
	 */
	public Boolean getForceOnline() {
		return forceOnline;
	}

	/**
	 * Set the force online flag
	 * @param forceOnline flag
	 */
	public void setForceOnline(Boolean forceOnline) {
		this.forceOnline = forceOnline;
	}

	/**
	 * Custom creation date of the page (set to 0 for clearing custom creation date and falling back to the real creation date)
	 * @return custom creation date
	 */
	public Integer getCustomCdate() {
		return customCdate;
	}

	/**
	 * Set custom creation date
	 * @param customCdate custom creation date
	 */
	public void setCustomCdate(Integer customCdate) {
		this.customCdate = customCdate;
	}

	/**
	 * Custom edit date of the page (set to 0 for clearing custom edit date and falling back to the real edit date)
	 * @return custom edit date
	 */
	public Integer getCustomEdate() {
		return customEdate;
	}

	/**
	 * Set custom edit date
	 * @param customEdate custom edit date
	 */
	public void setCustomEdate(Integer customEdate) {
		this.customEdate = customEdate;
	}

	/**
	 * Clone the given origin into this entity.
	 * 
	 * @param origin
	 */
	@XmlTransient
	@JsonIgnore
	public void clone(StagedFile origin) {
		super.clone(origin);
		this.fileType = origin.fileType;
		this.description = origin.description;
		this.fileSize = origin.fileSize;
		this.objectTags = new TreeMap<>(origin.objectTags);
		this.niceUrl = origin.niceUrl;
		this.alternateUrls = origin.alternateUrls;
		this.forceOnline = origin.forceOnline;
		this.customCdate = origin.customCdate;
		this.customEdate = origin.customEdate;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return TYPE_NAME;
	}

	@XmlTransient
	@JsonIgnore
	@Override
	public boolean hasBinary() {
		return true;
	}
}
