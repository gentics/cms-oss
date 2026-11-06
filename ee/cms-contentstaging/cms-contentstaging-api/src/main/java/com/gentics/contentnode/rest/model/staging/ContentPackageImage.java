package com.gentics.contentnode.rest.model.staging;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagedImage;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageImage extends StagedImage implements ContentStagingPackageObject {

	private static final long serialVersionUID = 6955386209104399379L;

	private List<StagedError> errors;

	public void setErrors(List<StagedError> errors) {
		this.errors = errors;
	}

	@Override
	public List<StagedError> getErrors() {
		return errors;
	}

	@Override
	public void clone(StagedImage origin) {
		super.clone(origin);
		this.setId(origin.getId());
		this.setName(origin.getName());
	}
}
