package com.gentics.contentnode.rest.model.staging;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Content staging error DTO.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedError implements Serializable {

	private static final long serialVersionUID = 8502237505057916884L;

	private String error;

	public StagedError() {
	}

	public StagedError(String error) {
		this.setError(error);
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
