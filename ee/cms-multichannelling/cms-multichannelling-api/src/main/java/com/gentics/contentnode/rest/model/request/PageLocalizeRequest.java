package com.gentics.contentnode.rest.model.request;

import java.io.Serial;

public class PageLocalizeRequest extends LocalizeRequest {

	@Serial
	private static final long serialVersionUID = -2225065775671067134L;

	private Boolean partial;

	public Boolean getPartial() {
		return partial;
	}

	public PageLocalizeRequest setPartial(Boolean partial) {
		this.partial = partial;

		return this;
	}
}
