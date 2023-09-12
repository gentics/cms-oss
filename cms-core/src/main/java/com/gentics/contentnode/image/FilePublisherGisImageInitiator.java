package com.gentics.contentnode.image;

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

	@Override
	public boolean initiateIfNotFound() {
		return false;
	}
}
