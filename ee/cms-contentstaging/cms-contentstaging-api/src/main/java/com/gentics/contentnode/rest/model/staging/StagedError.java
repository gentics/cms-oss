package com.gentics.contentnode.rest.model.staging;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.staging.StagingErrorKind;

/**
 * Content staging error DTO.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedError implements Serializable {

	private static final long serialVersionUID = 8502237505057916884L;

	private StagingErrorKind kind;
	private String error;

	public StagedError() {
	}

	public StagedError(String error, StagingErrorKind kind) {
		this.error = error;
		this.kind = kind;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public StagingErrorKind getKind() {
		return kind;
	}

	public void setKind(StagingErrorKind kind) {
		this.kind = kind;
	}
}
