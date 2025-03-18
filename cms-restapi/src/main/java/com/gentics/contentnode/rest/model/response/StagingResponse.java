package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Base for responses containing staging status of elements
 *
 * @param <K> response key type
 */
@XmlRootElement
public abstract class StagingResponse<K> extends GenericResponse {

	private static final long serialVersionUID = 6521291483369880099L;

	/**
	 * Staging status: File ID / status.
	 */
	private Map<K, StagingStatus> stagingStatus;

	public StagingResponse() {
		super();
	}

	public StagingResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Staging status of contained objects
	 * @return staging status map
	 */
	public Map<K, StagingStatus> getStagingStatus() {
		return stagingStatus;
	}

	/**
	 * Set the staging status
	 * @param stagingStatus
	 */
	public void setStagingStatus(Map<K, StagingStatus> stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
