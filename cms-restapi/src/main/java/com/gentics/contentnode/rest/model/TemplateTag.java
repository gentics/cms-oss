package com.gentics.contentnode.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag object representing a templaetag in the GCN
 * 
 * @author johannes
 */
@XmlRootElement
public class TemplateTag extends Tag {

	/**
	 * Default uid
	 */
	private static final long serialVersionUID = 4482787195062242170L;

	/**
	 * Flag that is used to determine whether the template tag is editable in page or not
	 */
	Boolean editableInPage;

	/**
	 * Flag that is used to determine whether the template tag is mandatory or not
	 */
	Boolean mandatory;

	/**
	 * Get the flag that defines whether the tag is mandatory or not
	 * 
	 * @return
	 */
	public Boolean getMandatory() {
		return mandatory;
	}

	/**
	 * Set the mandatory flag for this template tag
	 * 
	 * @param Flag
	 *            that defines whether the tag is mandatory
	 */
	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	/**
	 * Get the flag that defines whether the tag is editable in page or not
	 * 
	 * @return
	 */
	public Boolean getEditableInPage() {
		return editableInPage;
	}

	/**
	 * Set the editable in page flag for this template tag
	 * 
	 * @param Flag
	 *            that defines whether the tag is editable in page
	 */
	public void setEditableInPage(Boolean editableInPage) {
		this.editableInPage = editableInPage;
	}
}
