package com.gentics.contentnode.rest.model.staging;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagedFolder;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageFolder extends StagedFolder implements ContentStagingPackageObject {

	private static final long serialVersionUID = -4399685837666227783L;

	private List<StagedError> errors;

	public void setErrors(List<StagedError> errors) {
		this.errors = errors;
	}

	@Override
	public List<StagedError> getErrors() {
		return errors;
	}

	@Override
	public void clone(StagedFolder origin) {
		super.clone(origin);
		this.setId(origin.getId());
		this.setName(origin.getName());
	}
}
