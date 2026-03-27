package com.gentics.contentnode.rest.model.form;

import com.gentics.contentnode.rest.model.PublishableContentItem;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.contentnode.rest.model.FormTimeManagement;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.model.User;

/**
 * REST Model of a form
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class FormModel extends PublishableContentItem {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3991275403246334702L;

	/**
	 * Optional external ID
	 */
	private String externalId;

	/**
	 * Form type
	 */
	private String formType;

	/**
	 * Description of the form
	 */
	private String description;

	/**
	 * Id of the folder of this form
	 */
	private Integer folderId;

	/**
	 * Languages
	 */
	private List<String> languages;

	/**
	 * Form data
	 */
	private JsonNode data;


	/**
	 * The online status of the form.
	 */
	private boolean online;

	/**
	 * Flag for modified forms
	 */
	private boolean modified;

	/**
	 * Flag for forms that have time management set
	 */
	private boolean planned;

	/**
	 * true when the form is locked, false if not
	 */
	private boolean locked;

	/**
	 * Date when the form was locked, or -1 if the form is not locked
	 */
	private int lockedSince = -1;

	/**
	 * User, who locked the form (if {@link #locked} is true)
	 */
	private User lockedBy;

	/**
	 * Current form version
	 */
	private ItemVersion version;

	/**
	 * Published form version
	 */
	private ItemVersion publishedVersion;

	/**
	 * Time management of the form
	 */
	private FormTimeManagement timeManagement;

	/**
	 * Optional ID of the success page
	 */
	private Integer successPageId;

	/**
	 * Optional Node ID of the success page
	 */
	private Integer successNodeId;

	/**
	 * Constructor used by JAXB
	 */
	public FormModel() {
		super(ItemType.form);
	}

	/**
	 * Optional external ID
	 * @return external ID
	 */
	public String getExternalId() {
		return externalId;
	}

	/**
	 * Set the external ID
	 * @param externalId external ID
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	/**
	 * Form type
	 * @return form type
	 */
	public String getFormType() {
		return formType;
	}

	/**
	 * Set the form type
	 * @param formType form type
	 */
	public void setFormType(String formType) {
		this.formType = formType;
	}

	/**
	 * Form description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the form description
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Folder ID of the form
	 * @return the folderId
	 */
	public Integer getFolderId() {
		return folderId;
	}

	/**
	 * Set the folder ID
	 * @param folderId the folderId to set
	 */
	public void setFolderId(Integer folderId) {
		this.folderId = folderId;
	}

	/**
	 * Form languages
	 * @return form languages
	 */
	public List<String> getLanguages() {
		return languages;
	}

	/**
	 * Set the form languages
	 * @param languages form languages
	 */
	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	/**
	 * Form data in JSON format
	 * @return form data
	 */
	public JsonNode getData() {
		return data;
	}

	/**
	 * Set the form data
	 * @param data form data
	 */
	public void setData(JsonNode data) {
		this.data = data;
	}

	/**
	 * Whether the form is currently online
	 * @return online flag
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Set the online flag
	 * @param online flag
	 */
	public void setOnline(boolean online) {
		this.online = online;
	}

	/**
	 * Whether the form was modified after being published
	 * @return modified flag
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Set the modified flag
	 * @param modified flag
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	/**
	 * Whether the form has time management set or not
	 * @return true for time management
	 */
	public boolean isPlanned() {
		return planned;
	}

	/**
	 * Set the planned flag
	 * @param planned flag
	 */
	public void setPlanned(boolean planned) {
		this.planned = planned;
	}

	/**
	 * Whether the form is currently locked
	 * @return locked flag
	 */
	public boolean isLocked() {
		return locked;
	}

	/**
	 * Set the locked flag
	 * @param locked flag
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Timestamp, since when the form is locked
	 * @return locked timestamp
	 */
	public int getLockedSince() {
		return lockedSince;
	}

	/**
	 * Set lock timestamp
	 * @param lockedSince timestamp
	 */
	public void setLockedSince(int lockedSince) {
		this.lockedSince = lockedSince;
	}

	/**
	 * User, who locked the form
	 * @return locking user
	 */
	public User getLockedBy() {
		return lockedBy;
	}

	/**
	 * Set the locking user
	 * @param lockedBy user
	 */
	public void setLockedBy(User lockedBy) {
		this.lockedBy = lockedBy;
	}

	/**
	 * Current form version
	 * @return the currentVersion
	 */
	public ItemVersion getVersion() {
		return version;
	}

	/**
	 * Set the current form version
	 * @param version the version to set
	 */
	public void setVersion(ItemVersion version) {
		this.version = version;
	}

	/**
	 * Published form version
	 * @return the publishedVersion
	 */
	public ItemVersion getPublishedVersion() {
		return publishedVersion;
	}

	/**
	 * Set the published form version
	 * @param publishedVersion the publishedVersion to set
	 */
	public void setPublishedVersion(ItemVersion publishedVersion) {
		this.publishedVersion = publishedVersion;
	}

	/**
	 * Time Management
	 * @return the timeManagement
	 */
	public FormTimeManagement getTimeManagement() {
		return timeManagement;
	}

	/**
	 * Set time management
	 * @param timeManagement time management
	 */
	public void setTimeManagement(FormTimeManagement timeManagement) {
		this.timeManagement = timeManagement;
	}

	/**
	 * ID of the optional success page
	 * @return page ID or null
	 */
	public Integer getSuccessPageId() {
		return successPageId;
	}

	/**
	 * Set the success page ID
	 * @param successPageId ID
	 */
	public void setSuccessPageId(Integer successPageId) {
		this.successPageId = successPageId;
	}

	/**
	 * Node ID of the optional success page
	 * @return node ID or null
	 */
	public Integer getSuccessNodeId() {
		return successNodeId;
	}

	/**
	 * Set the success page node ID
	 * @param successNodeId node ID
	 */
	public void setSuccessNodeId(Integer successNodeId) {
		this.successNodeId = successNodeId;
	}
}
