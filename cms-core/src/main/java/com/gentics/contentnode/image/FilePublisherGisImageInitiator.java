package com.gentics.contentnode.image;

import com.gentics.contentnode.publish.FilePublisher;

/**
 * Initiator for {@link FilePublisher}.
 * 
 * @author plyhun
 *
 */
public class FilePublisherGisImageInitiator implements GisImageInitiator<Integer> {

	private final int publishId;

	public FilePublisherGisImageInitiator(int publishId) {
		super();
		this.publishId = publishId;
	}

	@Override
	public Integer getInitiatorForeignKey() {
		return publishId;
	}
}
