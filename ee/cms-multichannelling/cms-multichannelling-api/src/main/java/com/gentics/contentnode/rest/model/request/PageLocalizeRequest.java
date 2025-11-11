package com.gentics.contentnode.rest.model.request;

import java.io.Serial;

public class PageLocalizeRequest extends LocalizeRequest {

	@Serial
	private static final long serialVersionUID = -2225065775671067134L;

	/**
	 * Whether the localization is for tags or the whole page.
	 */
	private Boolean partial;

	/**
	 * Whether the localization is for tags or the whole page.
	 * @return Whether the localization is for tags or the whole page.
	 */
	public Boolean getPartial() {
		return partial;
	}

	/**
	 * Set whether the localization is for tags or the whole page.
	 * @param partial Whether the localization is for tags or the whole page.
	 * @return Fluent API
	 */
	public PageLocalizeRequest setPartial(Boolean partial) {
		this.partial = partial;

		return this;
	}
}
