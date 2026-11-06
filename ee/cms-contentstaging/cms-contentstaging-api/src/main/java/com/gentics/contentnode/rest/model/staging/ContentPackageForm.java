package com.gentics.contentnode.rest.model.staging;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagedForm;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageForm extends StagedForm implements ContentStagingPackageObject {

	private static final long serialVersionUID = 7765475835922071181L;

	private List<StagedError> errors;

	public void setErrors(List<StagedError> errors) {
		this.errors = errors;
	}

	@Override
	public List<StagedError> getErrors() {
		return errors;
	}

	@Override
	public void clone(StagedForm origin) {
		super.clone(origin);
		this.setId(origin.getId());
		this.setName(origin.getName());
	}
}
