package com.gentics.contentnode.staging;

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
@StoredType("error")
public class StagingError implements Serializable {

	private static final long serialVersionUID = 6003292888194582180L;

	private String path;
	private String error;
	private String globalId;

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
	public StagingError(String globalId, String path, String error) {
		this.path = path;
		this.error = error;
		this.globalId = globalId;
	}

	/**
	 * Adapter method.
	 * 
	 * @param path
	 * @param t
	 * @return
	 */
	@XmlTransient
	@JsonIgnore
    public static final StagingError fromThrowable(String globalId, Path path, Throwable t) {
		return new StagingError(globalId, path.toString(), t.getMessage());
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
}
