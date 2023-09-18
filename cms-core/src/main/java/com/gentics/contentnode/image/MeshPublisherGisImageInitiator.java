package com.gentics.contentnode.image;

import com.gentics.contentnode.publish.mesh.MeshPublisher;

/**
 * Initiator for {@link MeshPublisher}.
 * 
 * @author plyhun
 *
 */
public class MeshPublisherGisImageInitiator implements GisImageInitiator<Object[]> {

	private final int nodeId;
	private final int entityId;
	private final int entityType;
	private final String fieldKey;
	private String webrootpath;

	public MeshPublisherGisImageInitiator(int nodeId, int entityId, int entityType, String fieldKey) {
		this.nodeId = nodeId;
		this.entityId = entityId;
		this.entityType = entityType;
		this.fieldKey = fieldKey;
	}

	@Override
	public void setImageData(String webrootPath) {
		this.webrootpath = webrootPath;
	}

	@Override
	public Object[] getInitiatorForeignKey() {
		return new Object[] {nodeId, entityId, entityType, fieldKey, webrootpath};
	}
}
