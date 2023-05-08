package com.gentics.contentnode.devtools.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateTagModel extends TagModel {
	private boolean editableInPage;

	private boolean mandatory;

	public boolean isEditableInPage() {
		return editableInPage;
	}

	public void setEditableInPage(boolean editableInPage) {
		this.editableInPage = editableInPage;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
}
