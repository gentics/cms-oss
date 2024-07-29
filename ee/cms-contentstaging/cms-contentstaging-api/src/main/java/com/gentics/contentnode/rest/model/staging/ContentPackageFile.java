package com.gentics.contentnode.rest.model.staging;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagedFile;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageFile extends StagedFile implements ContentStagingPackageObject {

	private static final long serialVersionUID = 1550505508250136873L;

	private List<StagedError> errors;

	public void setErrors(List<StagedError> errors) {
		this.errors = errors;
	}

	@Override
	public List<StagedError> getErrors() {
		return errors;
	}
}
