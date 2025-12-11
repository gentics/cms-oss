package com.gentics.contentnode.staging;

import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * REST entity for the page, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedPage extends StagedLocalizableObject {

	@XmlTransient
	@JsonIgnore
	public static final String TYPE_NAME = "page";

	@XmlTransient
	@JsonIgnore
    public void clone(StagedPage origin) {
		super.clone(origin);
		this.niceUrl = origin.niceUrl;
		this.alternateUrls = origin.alternateUrls;
		this.fileName = origin.fileName;
		this.description = origin.description;
		this.template = origin.template;
		this.priority = origin.priority;
		this.language = origin.language;
		this.contentsetId = origin.contentsetId;
		this.contentId = origin.contentId;
		this.customCdate = origin.customCdate;
		this.customEdate = origin.customEdate;
		this.partiallyLocalized = origin.partiallyLocalized;
		if (origin.tags != null) {
			this.tags = new TreeMap<>(origin.tags);
		}
		if (origin.version != null) {
			this.version.clone(origin.version);
		}
	}

	private StagedObjectVersion version = new StagedObjectVersion();

	public StagedObjectVersion getVersion() {
		return version;
	}

	public void setVersion(StagedObjectVersion version) {
		this.version = version;
	}

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3067197290960415842L;

	/**
	 * Nice URL of the page
	 */
	private String niceUrl;

	/**
	 * Alternate URLs
	 */
	private SortedSet<String> alternateUrls;

	/**
	 * Filename of the page
	 */
	private String fileName;

	/**
	 * Description of the page
	 */
	private String description;

	/**
	 * Referenced template
	 */
	private StagedReference template;

	/**
	 * Priority of the page
	 */
	private Integer priority;

	/**
	 * Language of the page (code of the contentgroup)
	 */
	private String language;

	/**
	 * Id of the contentset
	 */
	private String contentsetId;

	/**
	 * Id of the content
	 */
	private String contentId;

	/**
	 * Tags in the page (contenttags)
	 */
	private Map<String, StagedTag> tags;

	/**
	 * Objecttags of the page
	 */
	private Map<String, StagedObjectTag> objectTags;

	/**
	 * Custom cdate
	 */
	private Integer customCdate;

	/**
	 * Custom edate
	 */
	private Integer customEdate;

	/**
	 * Whether this page is partially localized
	 */
	private boolean partiallyLocalized;

	/**
	 * Nice URL
	 * @return nice URL
	 */
	public String getNiceUrl() {
		return niceUrl;
	}

	/**
	 * Alternate URLs (in alphabetical order)
	 * @return sorted alternate URLs
	 */
	public SortedSet<String> getAlternateUrls() {
		return alternateUrls;
	}

	/**
	 * Filename
	 * @return the fileName
	 */
	public String getFileName() {
		return this.fileName;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Template
	 * @return the template
	 */
	public StagedReference getTemplate() {
		return this.template;
	}

	/**
	 * Priority
	 * @return the priority
	 */
	public Integer getPriority() {
		return this.priority;
	}

	/**
	 * Language Code (if page has a language)
	 * @return the language
	 */
	public String getLanguage() {
		return this.language;
	}

	/**
	 * Contentset ID
	 * @return contentset ID
	 */
	public String getContentsetId() {
		return contentsetId;
	}

	/**
	 * Content ID
	 * @return content ID
	 */
	public String getContentId() {
		return contentId;
	}

	/**
	 * Tags of the page
	 * @return tags
	 */
	public Map<String, StagedTag> getTags() {
		return this.tags;
	}

	/**
	 * Objecttags of the page
	 * @return objecttags
	 */
	public Map<String, StagedObjectTag> getObjectTags() {
		return objectTags;
	}

	/**
	 * Set the object tags
	 * @param objectTags object tags
	 */
	public void setObjectTags(Map<String, StagedObjectTag> objectTags) {
		// the tags shall be stored in a treemap, so that they will be ordered in natural ordering of the keys (names)
		this.objectTags = objectTags != null ? new TreeMap<>(objectTags) : null;
	}

	/**
	 * Set the nice URL
	 * @param niceUrl nice URL
	 */
	public void setNiceUrl(String niceUrl) {
		this.niceUrl = niceUrl;
	}

	/**
	 * Set the alternate URLs
	 * @param alternateUrls alternate URLs
	 */
	public void setAlternateUrls(SortedSet<String> alternateUrls) {
		this.alternateUrls = alternateUrls;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(StagedReference template) {
		this.template = template;
	}

	/**
	 * @param priority the priority to set
	 */
	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Set the contentset ID
	 * @param contentsetId contentset ID
	 */
	public void setContentsetId(String contentsetId) {
		this.contentsetId = contentsetId;
	}

	/**
	 * Set the content ID
	 * @param contentId content ID
	 */
	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	/**
	 * Set the tags
	 * @param tags tags
	 */
	public void setTags(Map<String, StagedTag> tags) {
		// the tags shall be stored in a treemap, so that they will be ordered in natural ordering of the keys (names)
		this.tags = tags != null ? new TreeMap<>(tags) : null;
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
	 * Whether this page is partially localized
	 * @return Whether this page is partially localized
	 */
	public boolean isPartiallyLocalized() {
		return partiallyLocalized;
	}

	/**
	 * Set whether this page is partially localized
	 * @param partiallyLocalized Whether this page is partially localized
	 */
	public void setPartiallyLocalized(boolean partiallyLocalized) {
		this.partiallyLocalized = partiallyLocalized;
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
    public Optional<StagedObjectVersion> maybeVersioned() {
		return Optional.of(this.version);
	}
}
