package com.gentics.contentnode.rest.model.response;

import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Interface for responses containing staging status
 *
 * @param <K>
 */
public interface StagingResponse<K> {
	/**
	 * Staging status of contained objects
	 * @return staging status map
	 */
	Map<K, StagingStatus> getStagingStatus();

	/**
	 * Set the staging status
	 * @param stagingStatus
	 */
	void setStagingStatus(Map<K, StagingStatus> stagingStatus);
	}
