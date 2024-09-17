package com.gentics.contentnode.publish.protocol;

public enum PublishState {
	ONLINE(1),
	OFFLINE(0);
	private final int status;

	PublishState(int status) {
		this.status = status;
	}

	public int getValue() {
		return status;
	}
}
