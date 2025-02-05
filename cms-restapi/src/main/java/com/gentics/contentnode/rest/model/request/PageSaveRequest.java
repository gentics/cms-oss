/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: PageSaveRequest.java,v 1.3.6.1 2011-03-09 15:58:10 norbert Exp $
 */
package com.gentics.contentnode.rest.model.request;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Request used for saving a page. If not set otherwise, a new version will be created (if necessary) and the page will remain locked for the user.
 * 
 * @author floriangutmann
 */
@XmlRootElement
public class PageSaveRequest {
    
	/**
	 * Page in the page save request
	 */
	private Page page;

	/**
	 * Flag whether the page shall be unlocked after saving
	 */
	private boolean unlock = false;

	/**
	 * Flag whether a new page version shall be created, or not
	 */
	private boolean createVersion = true;

	/**
	 * List of tag (names) which need to be deleted.
	 */
	private List<String> delete;

	/**
	 * When true, saving the page with duplicate name will fail, when false, the name will be made unique before saving
	 */
	private Boolean failOnDuplicate;

	/**
	 * When true and the filename is empty, it will be derived from
	 * the page name.
	 */
	private Boolean deriveFileName = false;

	/**
	 * Whether "publish At" shall be cleared
	 */
	private boolean clearPublishAt = false;

	/**
	 * Whether "offline At" shall be cleared
	 */
	private boolean clearOfflineAt = false;

	/**
	 * Constructor used by JAXB
	 */
	public PageSaveRequest() {}
    
	/**
	 * Creates a new PageSaveRequest with a specified page
	 * @param page The page to save
	 */
	public PageSaveRequest(Page page) {
		this.page = page;
	}
    
	/**
	 * Page to be saved
	 * @return page to be saved
	 */
	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	/**
	 * True if the page shall be unlocked after saving, false if not. The default is false.
	 * @return the unlock
	 */
	public boolean isUnlock() {
		return unlock;
	}

	/**
	 * @param unlock the unlock to set
	 */
	public void setUnlock(boolean unlock) {
		this.unlock = unlock;
	}

	/**
	 * True if a page version shall be created, false if not. The default is true.
	 * @return true for creating a page version
	 */
	public boolean isCreateVersion() {
		return createVersion;
	}

	/**
	 * Set whether to create a page version
	 * @param createVersion true to create a page version
	 */
	public void setCreateVersion(boolean createVersion) {
		this.createVersion = createVersion;
	}

	/**
	 * List of tag names of tags, that shall be deleted.
	 * @return the delete
	 */
	public List<String> getDelete() {
		return delete;
	}

	/**
	 * Set the list of tags that need to be deleted
	 * @param delete list of tags that need to be deleted
	 */
	public void setDelete(List<String> delete) {
		this.delete = delete;
	}

	/**
	 * True if saving the page with a duplicate name will fail. If false (default) the name will be made unique before saving
	 * @return true or false
	 */
	public Boolean getFailOnDuplicate() {
		return failOnDuplicate;
	}

	/**
	 * Set whether saving shall fail on duplicate names
	 * @param failOnDuplicate true to fail on duplicate names
	 */
	public void setFailOnDuplicate(Boolean failOnDuplicate) {
		this.failOnDuplicate = failOnDuplicate;
	}

	/**
	 * Indicates whether the filename should be derived from
	 * the page name, when no filename is given in the request.
	 *
	 * By default, the filename will <em>not</em> be derived from
	 * the page name.
	 *
	 * When the filename is provided in the request, this flag is ignored.
	 *
	 * @return <code>true</code> if the filename will be derived from
	 * 		the page name, when the request filename is empty.
	 * 		<code>false</code> otherwise.
	 */
	public Boolean getDeriveFileName() {
		return deriveFileName;
	}

	/**
	 * Set whether the filename should be derived from the page name,
	 * when no filename is given in the request.
	 *
	 * @param deriveFileName When set to <code>true</code> and the request
	 * 		filename is empty, the filename will be derived from the page name.
	 * 		When set to <code>false</code> <em>or</em> the filename is not
	 * 		empty, the behavior while saving the page is not changed.
	 */
	public void setDeriveFileName(Boolean deriveFileName) {
		this.deriveFileName = deriveFileName;
	}

	/**
	 * Flag to clear the "publish At" data (time and version)
	 * @return true to clear
	 */
	public boolean isClearPublishAt() {
		return clearPublishAt;
	}

	/**
	 * Set true to clear "publish At" data
	 * @param clearPublishAt flag
	 */
	public void setClearPublishAt(boolean clearPublishAt) {
		this.clearPublishAt = clearPublishAt;
	}

	/**
	 * Flag to clear the "offline At" data (time)
	 * @return true to clear
	 */
	public boolean isClearOfflineAt() {
		return clearOfflineAt;
	}

	/**
	 * Set true to clear "offline At" data
	 * @param clearOfflineAt flag
	 */
	public void setClearOfflineAt(boolean clearOfflineAt) {
		this.clearOfflineAt = clearOfflineAt;
	}
}
