package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LocalizationsItem {
	private Integer objectId;
	private Integer nodeId;
	private boolean online;

	public LocalizationsItem() {
	}

	public Integer getObjectId() {
		return objectId;
	}

	public void setObjectId(Integer objectId) {
		this.objectId = objectId;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}
	
	@Override
	public String toString() {
		return "{LocalizationsItem pageId: " + objectId + ", nodeId: " + nodeId + ", online: " + online + "}";
	}
}
