package com.gentics.contentnode.image;

public class MeshPublisherGisImageInitiator implements GisImageInitiator<Object[]> {

	private final int nodeId;
	private final int entityId;
	private final int entityType;
	private final String fieldKey;

	public MeshPublisherGisImageInitiator(int nodeId, int entityId, int entityType, String fieldKey) {
		this.nodeId = nodeId;
		this.entityId = entityId;
		this.entityType = entityType;
		this.fieldKey = fieldKey;
	}

	@Override
	public Object[] getInitiatorForeignKey() {
		return new Object[] {nodeId, entityId, entityType, fieldKey};
	}
}
