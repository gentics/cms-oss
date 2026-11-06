package com.gentics.contentnode.staging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Errors model
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagingErrors implements Serializable {

	private static final long serialVersionUID = 6207215423058843598L;

	private List<StagingError> errors = new ArrayList<>();
	private long timestamp = System.currentTimeMillis();

	public List<StagingError> getErrors() {
		return errors;
	}
	public void setErrors(List<StagingError> errors) {
		this.errors = errors;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
