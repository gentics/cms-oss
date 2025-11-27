/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: Page.java,v 1.7.2.1.2.2.2.4 2011-03-24 09:25:07 johannes2 Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Page object representing a page in GCN
 * @author floriangutmann
 */
@XmlRootElement
public class Page extends PublishableContentItem implements Serializable {

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
	 * Path to the page, separated by '/', starting and ending with '/'
	 */
	private String path;

	/**
	 * Id of the template of this page
	 */
	private Integer templateId;

	/**
	 * Id of the folder of this page
	 */
	private Integer folderId;

	/**
	 * Id of the contentset
	 */
	private Integer contentSetId;

	/**
	 * Id of the contentgroup
	 */
	private Integer contentGroupId;

	/**
	 * Priority of the page
	 */
	private Integer priority;

	/**
	 * Flag if the page is read-only or available for writing
	 */
	private boolean readOnly;

	/**
	 * Language of the page (code of the contentgroup)
	 */
	private String language;

	/**
	 * Language name of the page
	 */
	private String languageName;

	/**
	 * Tags in the page (contenttags and objecttags)
	 */
	private Map<String, Tag> tags;

	/**
	 * time management of the page
	 */
	private TimeManagement timeManagement;

	/**
	 * Publish workflow
	 */
	private Workflow workflow;

	/**
	 * Page variants of the current page
	 */
	private List<Page> pageVariants;

	/**
	 * Map of language variants of the page. Keys are the language ids, values are the pages
	 */
	private Map<Object, Page> languageVariants;

	/**
	 * URL to the page
	 * This url is the relative path to the page preview
	 * in the GCN backend.
	 */
	private String url;

	/**
	 * The page's live URL.
	 * This is the assumed URL the page will
	 * have, when being published on the webserver.
	 */
	private String liveUrl;

	/**
	 * The page's publish path
	 */
	private String publishPath;

	/**
	 * The online status of the page.
	 */
	private boolean online;

	/**
	 * Flag for modified pages
	 */
	private boolean modified;

	/**
	 * Flag for pages in queue
	 */
	private boolean queued;

	/**
	 * Flag for pages that have time management set
	 */
	private boolean planned;

	/**
	 * Template of the page
	 */
	private Template template;

	/**
	 * Folder of the page
	 */
	private Folder folder;

	/**
	 * Name of the node the object was inherited from
	 */
	private String inheritedFrom;

	/**
	 * Id of the node the object was inherited from.
	 */
	private Integer inheritedFromId;

	/**
	 * Name of the node, the master object belongs to
	 */
	private String masterNode;

	/**
	 * Id of the node the master object belongs to.
	 */
	private Integer masterNodeId;

	/**
	 * true when the page is inherited from a master channel, false if not
	 */
	protected boolean inherited;

	/**
	 * true if the page is a master page, false if not
	 */
	protected boolean master;

	/**
	 * true when the page is locked, false if not
	 */
	protected boolean locked;

	/**
	 * Date when the page was locked, or -1 if the page is not locked
	 */
	protected int lockedSince = -1;

	/**
	 * User, who locked the page (if {@link #locked} is true)
	 */
	protected User lockedBy;

	/**
	 * Translation status
	 */
	protected TranslationStatus translationStatus;

	/**
	 * Page versions
	 */
	protected List<PageVersion> versions;

	/**
	 * Current page version
	 */
	protected PageVersion currentVersion;

	/**
	 * Published page version
	 */
	protected PageVersion publishedVersion;

	/**
	 * Content ID
	 */
	protected Integer contentId;

	/**
	 * Channel ID
	 */
	protected Integer channelId;

	/**
	 * Channelset ID
	 */
	protected Integer channelSetId;

	/**
	 * Disinherited channels
	 */
	private Set<Node> disinheritedChannels;

	/**
	 * Whether this page is excluded from multichannelling.
	 */
	private Boolean excluded;

	/**
	 * Whether this folder is disinherited by default in new channels.
	 */
	private Boolean disinheritDefault;

	/**
	 * Whether this page is disinherited in some channels
	 */
	private Boolean disinherited;

	/**
	 * Custom cdate
	 */
	private Integer customCdate;

	/**
	 * Custom edate
	 */
	private Integer customEdate;

	/**
	 * Whether this page has been partially localized.
	 */
	private LocalizationType localizationType;

	/**
	 * Constructor used by JAXB
	 */
	public Page() {
		super(ItemType.page);
	}

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
	 * Template ID
	 * @return the templateId
	 */
	public Integer getTemplateId() {
		return this.templateId;
	}

	/**
	 * Folder ID
	 * @return the folderId
	 */
	public Integer getFolderId() {
		return this.folderId;
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
	 * Language Name (if page has a language)
	 * @return name of the page language
	 */
	public String getLanguageName() {
		return languageName;
	}

	/**
	 * Tags of the page
	 * @return tags
	 */
	public Map<String, Tag> getTags() {
		return this.tags;
	}

	/**
	 * Name of the node, this page is inherited from.
	 * @return
	 */
	public String getInheritedFrom() {
		return inheritedFrom;
	}

	/**
	 * Sets the name of the node this page is inherited from.
	 * @param inheritedFrom
	 */
	public void setInheritedFrom(String inheritedFrom) {
		this.inheritedFrom = inheritedFrom;
	}

	/**
	 * Return the node id of the node this page is inherited from.
	 * @return
	 */
	public Integer getInheritedFromId() {
		return inheritedFromId;
	}

	/**
	 *  Set the id of the node this page is inherited from.
	 * @param inheritedFromId
	 */
	public void setInheritedFromId(Integer inheritedFromId) {
		this.inheritedFromId = inheritedFromId;
	}

	/**
	 * Name of the node, the master object belongs to
	 * @return node name
	 */
	public String getMasterNode() {
		return masterNode;
	}

	/**
	 * Set the name of the node, the master object belongs to
	 * @param masterNode node name
	 */
	public void setMasterNode(String masterNode) {
		this.masterNode = masterNode;
	}

	/**
	 * Return the id of the node, the master object belongs to.
	 * @return
	 */
	public Integer getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the id of the node the master object belongs to. 
	 * @param masterNodeId
	 */
	public void setMasterNodeId(Integer masterNodeId) {
		this.masterNodeId = masterNodeId;
	}

	/**
	 *Page variants of the page
	 * @return pageVariants
	 */
	public List<Page> getPageVariants() {
		return this.pageVariants;
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
	 * @param templateId the templateId to set
	 */
	public void setTemplateId(Integer templateId) {
		this.templateId = templateId;
	}

	/**
	 * @param folderId the folderId to set
	 */
	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
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
	 * Set the language name
	 * @param languageName language name
	 */
	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	/**
	 * Set the tags
	 * @param tags tags
	 */
	public void setTags(Map<String, Tag> tags) {
		this.tags = tags;
	}

	/**
	 * True if the page was fetched readonly, false if fetched in edit mode
	 * @return
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * Time Management
	 * @return the timeManagement
	 */
	public TimeManagement getTimeManagement() {
		return timeManagement;
	}

	/**
	 * @param timeManagement the timeManagement to set
	 */
	public void setTimeManagement(TimeManagement timeManagement) {
		this.timeManagement = timeManagement;
	}

	public void setPageVariants(List<Page> variantIds) {
		this.pageVariants = variantIds;
	}

	/**
	 * Folder path to the page
	 * @return path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path
	 * @param path the path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * URL to the page
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the live URL of the page
	 * @return
	 */
	public void setLiveUrl(String liveUrl) {
		this.liveUrl = liveUrl;
	}

	/**
	 * Live URL to the page
	 * @return
	 */
	public String getLiveUrl() {
		return liveUrl;
	}

	/**
	 * Set the publish path
	 * @param publishPath publish path
	 */
	public void setPublishPath(String publishPath) {
		this.publishPath = publishPath;
	}

	/**
	 * Publish path
	 * @return publish path
	 */
	public String getPublishPath() {
		return publishPath;
	}

	/**
	 * Whether the page is currently online.
	 *
	 * @return <code>true</code> if the page is currently online, <code>false</code> otherwise.
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Set the online status of the page.
	 * @param online The online status.
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * Whether the page is modified (the last version of the page is not the currently published one)
	 * @return true for modified page
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Set modified flag
	 * @param modified flag
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * Whether the page is in queue for being published or taken offline
	 * @return true for queued page
	 */
	public boolean isQueued() {
		return queued;
	}

	/**
	 * Set queued flag
	 * @param queued flag
	 */
	public void setQueued(boolean queued) {
		this.queued = queued;
	}

	/**
	 * Whether the page has time management set or not
	 * @return true for time management
	 */
	public boolean isPlanned() {
		return planned;
	}

	/**
	 * Set planned flag
	 * @param planned flag
	 */
	public void setPlanned(boolean planned) {
		this.planned = planned;
	}

	/**
	 * Template
	 * @return the template
	 */
	public Template getTemplate() {
		return template;
	}

	/**
	 * Folder
	 * @return the folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(Template template) {
		this.template = template;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
		if (folder != null) {
			this.folderId = folder.getId();
		}
	}

	/**
	 * Contentset ID
	 * @return the contentSetId
	 */
	public Integer getContentSetId() {
		return contentSetId;
	}

	/**
	 * Contentgroup ID
	 * @return the contentGroupId
	 */
	public Integer getContentGroupId() {
		return contentGroupId;
	}

	/**
	 * @param contentSetId the contentSetId to set
	 */
	public void setContentSetId(Integer contentSetId) {
		this.contentSetId = contentSetId;
	}

	/**
	 * @param contentGroupId the contentGroupId to set
	 */
	public void setContentGroupId(Integer contentGroupId) {
		this.contentGroupId = contentGroupId;
	}

	/**
	 * Language variants
	 * @return the languageVariants
	 */
	public Map<Object, Page> getLanguageVariants() {
		return languageVariants;
	}

	/**
	 * @param languageVariants the languageVariants to set
	 */
	public void setLanguageVariants(Map<Object, Page> languageVariants) {
		this.languageVariants = languageVariants;
	}

	/**
	 * True if the page was inherited
	 * @return
	 */
	public boolean isInherited() {
		return inherited;
	}

	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	/**
	 * True if the page is locked
	 * @return the locked
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * @param locked the locked to set
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Timestamp, since when the page is locked, or -1 if it is not locked
	 * @return timestamp of page locking or -1
	 */
	public int getLockedSince() {
		return lockedSince;
	}

	/**
	 * Set the timestamp, since when the page is locked
	 * @param lockedSince timestamp of page locking or -1
	 */
	public void setLockedSince(int lockedSince) {
		this.lockedSince = lockedSince;
	}

	/**
	 *User, who locked the page
	 * @return user
	 */
	public User getLockedBy() {
		return lockedBy;
	}

	/**
	 * Set locked by
	 * @param lockedBy user, who locked the page
	 */
	public void setLockedBy(User lockedBy) {
		this.lockedBy = lockedBy;
	}

	/**
	 * Workflow attached to the page
	 * @return the workflow
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	/**
	 * @param workflow the workflow to set
	 */
	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	/**
	 * Translation status information
	 * @return translation status information
	 */
	public TranslationStatus getTranslationStatus() {
		return translationStatus;
	}

	/**
	 * Set the translation status information
	 * @param translationStatus translation status information
	 */
	public void setTranslationStatus(TranslationStatus translationStatus) {
		this.translationStatus = translationStatus;
	}

	/**
	 * Current version of the page
	 * @return current version
	 */
	public PageVersion getCurrentVersion() {
		return currentVersion;
	}

	/**
	 * Published version of the page
	 * @return published version
	 */
	public PageVersion getPublishedVersion() {
		return publishedVersion;
	}

	/**
	 * Page versions
	 * @return page versions
	 */
	public List<PageVersion> getVersions() {
		return versions;
	}

	/**
	 * Set the current version
	 * @param currentVersion current version
	 */
	public void setCurrentVersion(PageVersion currentVersion) {
		this.currentVersion = currentVersion;
	}

	/**
	 * Set the published version
	 * @param publishedVersion published version
	 */
	public void setPublishedVersion(PageVersion publishedVersion) {
		this.publishedVersion = publishedVersion;
	}

	/**
	 * Set the page versions
	 * @param versions page versions
	 */
	public void setVersions(List<PageVersion> versions) {
		this.versions = versions;
	}

	/**
	 * Add the given rest tags to the map of all other rest tags for this page
	 *
	 * @param restTags
	 */
	public void addTags(Map<String, com.gentics.contentnode.rest.model.Tag> restTags) {
		if (this.tags == null) {
			this.tags = new HashMap<String, com.gentics.contentnode.rest.model.Tag>();
		}
		this.tags.putAll(restTags);
	}

	/**
	 * Get the content id
	 * @return content id
	 */
	public Integer getContentId() {
		return contentId;
	}

	/**
	 * Set the content id
	 * @param contentId content id
	 */
	public void setContentId(Integer contentId) {
		this.contentId = contentId;
	}

	/**
	 * Get the channelset id
	 * @return channelset id
	 */
	public Integer getChannelSetId() {
		return channelSetId;
	}

	/**
	 * Set the channelset id
	 * @param channelSetId channelset id
	 */
	public void setChannelSetId(Integer channelSetId) {
		this.channelSetId = channelSetId;
	}

	/**
	 * Get the channel id
	 * @return channel id
	 */
	public Integer getChannelId() {
		return channelId;
	}

	/**
	 * Set the channel id
	 * @param channelId channel id
	 */
	public void setChannelId(Integer channelId) {
		this.channelId = channelId;
	}

	/**
	 * Get whether this page is a master page
	 * @return true for master pages, false for localized copies
	 */
	public boolean isMaster() {
		return master;
	}

	/**
	 * Set true for master pages, false for localized copies
	 * @param master true for master page
	 */
	public void setMaster(boolean master) {
		this.master = master;
	}

	/**
	 * the set of disinherited channels for this object
	 *
	 * @return the set of disinherited channels
	 */
	public Set<Node> getDisinheritedChannels() {
		return disinheritedChannels;
	}

	/**
	 * Set the disinherited channels for this object
	 *
	 * @param disinheritedChannels
	 *            the set of disinherited channnels
	 */
	public void setDisinheritedChannels(Set<Node> disinheritedChannels) {
		this.disinheritedChannels = disinheritedChannels;
	}

	/**
	 * Whether this page is excluded from multichannelling.
	 *
	 * @return true iff the page should be excluded from multichannelling
	 */
	public Boolean isExcluded() {
		return excluded;
	}

	/**
	 * Set wether the page is excluded from multichannelling
	 *
	 * @param excluded
	 *            if true, the page will be excluded from multichannelling
	 */
	public void setExcluded(Boolean excluded) {
		this.excluded = excluded;
	}

	/**
	 * Whether this folder is disinherited by default in new channels.
	 *
	 * @return <code>true</code> if the folder is disinherited in new channels,
	 *		<code>false</code> otherwise.
	 */
	public Boolean isDisinheritDefault() {
		return disinheritDefault;
	}

	/**
	 * Set whether this folder should be disinherited by default in new channels.
	 *
	 * @param disinheritDefault If set to <code>true</code> this folder will be
	 *		disinherited by default in new channels.
	 */
	public void setDisinheritDefault(Boolean disinheritDefault) {
		this.disinheritDefault = disinheritDefault;
	}

	/**
	 * True if the page is disinherited in some channels
	 * @return true iff the page is disinherited
	 */
	public Boolean isDisinherited() {
		return disinherited;
	}

	/**
	 * Set whether the page is disinherited
	 * @param disinherited true if disinherited
	 */
	public void setDisinherited(Boolean disinherited) {
		this.disinherited = disinherited;
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

	public LocalizationType getLocalizationType() {
		return localizationType;
	}

	public Page setLocalizationType(LocalizationType localizationType) {
		this.localizationType = localizationType;

		return this;
	}
}
