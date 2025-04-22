package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Base for responses containing staging status of elements
 *
 * @param <K> response key type
 */
@XmlRootElement
public abstract class AbstractStagingResponse<K> extends GenericResponse implements StagingResponse<K> {

	private static final long serialVersionUID = 6521291483369880099L;

	/**
	 * Staging status: File ID / status.
	 */
	private Map<K, StagingStatus> stagingStatus;

	public AbstractStagingResponse() {
		super();
	}

	public AbstractStagingResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	@Override
	public Map<K, StagingStatus> getStagingStatus() {
		return stagingStatus;
	}

	@Override
	public void setStagingStatus(Map<K, StagingStatus> stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
