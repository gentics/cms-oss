package com.gentics.contentnode.rest.model.request;

import java.io.Serial;

import com.gentics.contentnode.rest.model.LocalizationType;

public class PageLocalizeRequest extends LocalizeRequest {

	@Serial
	private static final long serialVersionUID = -2225065775671067134L;

	/**
	 * Whether the localization is for tags or the whole page.
	 */
	private LocalizationType localizationType;

	/**
	 * The localization type.
	 * @return The localization type.
	 */
	public LocalizationType getLocalizationType() {
		return localizationType;
	}

	/**
	 * Set the localization type.
	 * @param localizationType The localization type.
	 * @return Fluent API
	 */
	public PageLocalizeRequest setLocalizationType(LocalizationType localizationType) {
		this.localizationType = localizationType;

		return this;
	}
}
