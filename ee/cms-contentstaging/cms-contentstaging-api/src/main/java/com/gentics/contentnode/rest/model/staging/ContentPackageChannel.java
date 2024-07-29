package com.gentics.contentnode.rest.model.staging;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagedChannel;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentPackageChannel extends StagedChannel implements ContentStagingPackageObject {

	private static final long serialVersionUID = -2731465132724688019L;

	private List<StagedError> errors;

	public void setErrors(List<StagedError> errors) {
		this.errors = errors;
	}

	@Override
	public List<StagedError> getErrors() {
		return errors;
	}
}
