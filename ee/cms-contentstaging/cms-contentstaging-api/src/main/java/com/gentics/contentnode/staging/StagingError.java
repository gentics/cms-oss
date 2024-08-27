package com.gentics.contentnode.staging;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import java.io.Serializable;
import java.nio.file.Path;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A distinct error, happened during the staging process.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagingError implements Serializable {

	private static final long serialVersionUID = 6003292888194582180L;

	private String path;
	private String error;
	private String globalId;
	private StagingErrorKind kind;
	private String recommendation;

	/**
	 * Reflection ctor.
	 */
	public StagingError() {
	}

	/**
	 * Inner ctor.
	 * 
	 * @param path
	 * @param error
	 */
	public StagingError(String globalId, String path, String error, StagingErrorKind kind) {
		this.path = path;
		this.error = error;
		this.globalId = globalId;
		this.kind = kind;
		this.setRecommendation(StagingErrorRecommendation.OTHER.toString());
	}

	/**
	 * Adapter method.
	 *
	 * @param path
	 * @param throwable
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
	public static final StagingError fromThrowable(String globalId, Path path, Throwable throwable) {
		var errorMessage = throwable.getMessage();

		if (throwable instanceof NodeException) {
				errorMessage = ((NodeException) throwable).getLocalizedMessage();
		}
		var stagingError = new StagingError(globalId, path.toString(), errorMessage, StagingErrorKind.EXCEPTION);
		stagingError.setRecommendation(StagingErrorRecommendation.fromThrowable(throwable).toString());

		return stagingError;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	public StagingErrorKind getKind() {
		return kind;
	}

	public void setKind(StagingErrorKind kind) {
		this.kind = kind;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}
}
