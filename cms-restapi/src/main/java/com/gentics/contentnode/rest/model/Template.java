package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Template object representing a template in GCN
 * @author johannes2
 */
@XmlRootElement
public class Template implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2765341587856553956L;

	/**
	 * Id of the template
	 */
	private Integer id;

	/**
	 * Global Id of the template
	 */
	private String globalId;

	/**
	 * Name of the template
	 */
	private String name;

	/**
	 * Description of the template
	 */
	private String description;

	/**
	 * Creator of the file
	 */
	protected User creator;

	/**
	 * Date when the file was created
	 */
	protected int cdate;

	/**
	 * Latest contributor to the file
	 */
	protected User editor;

	/**
	 * Date when the file was modified last
	 */
	protected int edate;

	/**
	 * true when the template is locked, false if not
	 */
	protected boolean locked;

	/**
	 * true when the template is inherited from a master channel, false if not
	 */
	protected boolean inherited;

	/**
	 * true if the template is a master template, false if not
	 */
	protected boolean master;

	/**
	 * id of the master template, if this template is a localized copy of another
	 */
	protected Integer masterId;

	/**
	 * Markup language
	 */
	protected MarkupLanguage markupLanguage;

	/**
	 * Folder id of the folder for which the template was fetched
	 */
	protected Integer folderId;

	/**
	 * Path to the template, separated by '/', starting and ending with '/'
	 */
	private String path;

	/**
	 * name of the node the object was inherited from
	 */
	private String inheritedFrom;

	/**
	 * name of the node, the master object belongs to
	 */
	private String masterNode;
    
	/**
	 * Object tags in the template
	 */
	private Map<String, Tag> objectTags;

	/**
	 * Template tags in the template
	 */
	private Map<String, TemplateTag> templateTags;
    
	/**
	 * Template source
	 */
	private String source;

	/**
	 * Channel ID
	 */
	protected Integer channelId;

	/**
	 * Channelset ID
	 */
	protected Integer channelSetId;

	/**
	 * Constructor for JAXB
	 */
	public Template() {}

	/**
	 * Name of the node this template is inherited from
	 * @return
	 */
	public String getInheritedFrom() {
		return inheritedFrom;
	}
    
	/**
	 * sets inherited from
	 * @param inheritedFrom
	 */
	public void setInheritedFrom(String inheritedFrom) {
		this.inheritedFrom = inheritedFrom;
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
	 * ID
	 * @return
	 */
	public Integer getId() {
		return this.id;
	}

	/**
	 * Sets the template id
	 * @param id
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Global Id of the template
	 * @return the global Id
	 */
	public String getGlobalId() {
		return globalId;
	}

	/**
	 * Set the global Id
	 * @param globalId global Id
	 */
	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	/**
	 * Name of the template
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the template
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 *Template description
	 * @return
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the template description
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Creator
	 * @return the creator
	 */
	public User getCreator() {
		return creator;
	}

	/**
	 * Creation date
	 * @return the cdate
	 */
	public int getCdate() {
		return cdate;
	}

	/**
	 * Last Editor
	 * @return the editor
	 */
	public User getEditor() {
		return editor;
	}

	/**
	 * Last Edit date
	 * @return the edate
	 */
	public int getEdate() {
		return edate;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(User creator) {
		this.creator = creator;
	}

	/**
	 * @param cdate the cdate to set
	 */
	public void setCdate(int cdate) {
		this.cdate = cdate;
	}

	/**
	 * @param editor the editor to set
	 */
	public void setEditor(User editor) {
		this.editor = editor;
	}

	/**
	 * @param edate the edate to set
	 */
	public void setEdate(int edate) {
		this.edate = edate;
	}

	/**
	 * True if the template is locked
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
	 * Markup language
	 * @return the markupLanguage
	 */
	public MarkupLanguage getMarkupLanguage() {
		return markupLanguage;
	}

	/**
	 * @param markupLanguage the markupLanguage to set
	 */
	public void setMarkupLanguage(MarkupLanguage markupLanguage) {
		this.markupLanguage = markupLanguage;
	}

	/**
	 * True if the template is inherited
	 * @return the inherited
	 */
	public boolean isInherited() {
		return inherited;
	}

	/**
	 * @param inherited the inherited to set
	 */
	public void setInherited(boolean inherited) {
		this.inherited = inherited;
	}

	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}

	/**
	 * Folder ID
	 * @return
	 */
	public Integer getFolderId() {
		return folderId;
	}

	/**
	 * Master ID
	 * @return
	 */
	public Integer getMasterId() {
		return masterId;
	}

	public void setMasterId(Integer masterId) {
		this.masterId = masterId;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Template) {
			if (id == null) {
				return false;
			}
			return id.equals(((Template) o).id);
		} else {
			return false;
		}
	}

	/**
	 * Folder path
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
    
	/**
	 * Set the tags
	 * 
	 * @param templateTags
	 *            tags
	 */
	public void setTemplateTags(Map<String, TemplateTag> templateTags) {
		this.templateTags = templateTags;
	}

	/**
	 * Tags of the template
	 * 
	 * @return tags
	 */
	public Map<String, TemplateTag> getTemplateTags() {
		return this.templateTags;
	}

	/**
	 * Returns the list of object tags for this template
	 * 
	 * @return
	 */
	public Map<String, Tag> getObjectTags() {
		return this.objectTags;
	}

	/**
	 * Sets the object tags for this template
	 * 
	 * @param objectTags
	 */
	public void setObjectTags(Map<String, Tag> objectTags) {
		this.objectTags = objectTags;
	}

	/**
	 * Returns the template source
	 * 
	 * @return source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Sets the source of the template
	 * 
	 * @param source
	 */
	public void setSource(String source) {
		this.source = source;
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
	 * Get whether this template is a master template
	 * @return true for master templates, false for localized copies
	 */
	public boolean isMaster() {
		return master;
	}

	/**
	 * Set true for master templates, false for localized copies
	 * @param master true for master template
	 */
	public void setMaster(boolean master) {
		this.master = master;
	}
}
