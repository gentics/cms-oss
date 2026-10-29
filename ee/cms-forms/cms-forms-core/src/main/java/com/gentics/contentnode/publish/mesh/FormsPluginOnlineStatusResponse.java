package com.gentics.contentnode.publish.mesh;

import com.gentics.mesh.core.rest.common.RestModel;

public class FormsPluginOnlineStatusResponse implements RestModel {
	private Boolean online;

	public Boolean getOnline() {
		return online;
	}

	public FormsPluginOnlineStatusResponse setOnline(Boolean online) {
		this.online = online;
		return this;
	}
}
