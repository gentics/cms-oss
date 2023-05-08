package com.gentics.contentnode.rest.model.request;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Template;

/**
 * Request for saving a template
 *
 */
@XmlRootElement
public class TemplateSaveRequest {
	/**
	 * The template to save
	 */
	private Template template;

	/**
	 * Flag whether the template shall be unlocked after saving
	 */
	private boolean unlock = false;

	/**
	 * Flag whether pages shall be synchronized while saving the template
	 */
	private boolean syncPages = true;

	/**
	 * List of tag (names) which need to be synchronized
	 */
	private List<String> sync;

	/**
	 * Flag to enforce sync of incompatible tags in pages
	 */
	private boolean forceSync = false;

	/**
	 * List of tag (names) which need to be deleted.
	 */
	private List<String> delete;

	/**
	 * Create an empty instance
	 */
	public TemplateSaveRequest() {
	}

	/**
	 * Create an instance with the given template
	 * @param template template
	 */
	public TemplateSaveRequest(Template template) {
		this.template = template;
	}

	/**
	 * Template to be saved
	 * @return the template
	 */
	public Template getTemplate() {
		return template;
	}

	/**
	 * Set the template to be saved
	 * @param template the template to set
	 */
	public void setTemplate(Template template) {
		this.template = template;
	}

	/**
	 * True, if the template shall be unlocked after saving, false if not
	 * @return true to unlock
	 */
	public boolean isUnlock() {
		return unlock;
	}

	/**
	 * Set true to unlock the template after saving
	 * @param true to unlock
	 */
	public void setUnlock(boolean unlock) {
		this.unlock = unlock;
	}

	/**
	 * True to synchronize the pages while saving the template, false if not
	 * @return true to synchronize pages
	 */
	public boolean isSyncPages() {
		return syncPages;
	}

	/**
	 * Set true to synchronize the pages while saving the template, false to not synchronize
	 * @param syncPages true to synchronize pages
	 */
	public void setSyncPages(boolean syncPages) {
		this.syncPages = syncPages;
	}

	/**
	 * Optional list of tags in pages to synchronize (if syncPages is true). If left empty, all tags will be synchronized.
	 * @return list of tag names
	 */
	public List<String> getSync() {
		return sync;
	}

	/**
	 * Set tag names to synchronize
	 * @param sync list of tag names
	 */
	public void setSync(List<String> sync) {
		this.sync = sync;
	}

	/**
	 * True to force synchronization of incompatible tags in pages (if syncPages is true)
	 * @return true to force sync
	 */
	public boolean isForceSync() {
		return forceSync;
	}

	/**
	 * Set force sync flag
	 * @param forceSync flag
	 */
	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	/**
	 * List of tag names to be deleted from the template
	 * @return list of tag names to be deleted
	 */
	public List<String> getDelete() {
		return delete;
	}

	/**
	 * Get the list of tagnames to be deleted from the template
	 * @param list of tag names to be deleted
	 */
	public void setDelete(List<String> delete) {
		this.delete = delete;
	}
}
