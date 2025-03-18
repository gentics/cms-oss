package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericJobStatusResponse extends GenericResponse {

	private static final long serialVersionUID = -1087971433702824544L;

	@XmlAttribute(name = "import")
	private JobStatus import_;

	public GenericJobStatusResponse() {
		super();
	}

	public GenericJobStatusResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Status of the latest import
	 * 
	 * @return
	 */
	public JobStatus getImport() {
		return import_;
	}

	/**
	 * Set import job status.
	 * 
	 * @param import_
	 */
	public void setImport(JobStatus import_) {
		this.import_ = import_;
	}
}
