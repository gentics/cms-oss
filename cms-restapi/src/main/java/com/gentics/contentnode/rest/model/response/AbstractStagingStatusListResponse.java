package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Abstract list response with staging status
 *
 * @param <T> type of the objects contained in the list
 */
@XmlRootElement
public abstract class AbstractStagingStatusListResponse <T extends Object> extends AbstractListResponse<T> implements StagingResponse<String> {
	/**
	 * Serial Version UUID
	 */
	private static final long serialVersionUID = -1785092242334848284L;

	/**
	 * Staging status: File ID / status.
	 */
	private Map<String, StagingStatus> stagingStatus;

	/**
	 * Empty constructor needed by JAXB
	 */
	public AbstractStagingStatusListResponse() {}

	/**
	 * Create an instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public AbstractStagingStatusListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	@Override
	public Map<String, StagingStatus> getStagingStatus() {
		return stagingStatus;
	}

	@Override
	public void setStagingStatus(Map<String, StagingStatus> stagingStatus) {
		this.stagingStatus = stagingStatus;
	}
}
