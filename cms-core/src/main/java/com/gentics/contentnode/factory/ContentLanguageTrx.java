package com.gentics.contentnode.factory;

import java.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.render.RenderType;

/**
 * AutoCloseable that sets a language to the current renderType (and resets the language in {@link #close()}).
 */
public class ContentLanguageTrx implements AutoCloseable {
	/**
	 * Render type
	 */
	private RenderType renderType;

	/**
	 * Previously set language
	 */
	private ContentLanguage oldLanguage;

	/**
	 * Flag whether language was changed (and needs to be reset)
	 */
	private boolean languageWasSet = false;

	/**
	 * Create an instance and set the language with given code
	 * @param languageCode language code
	 * @throws NodeException
	 */
	public ContentLanguageTrx(String languageCode) throws NodeException {
		this(LanguageFactory.get(languageCode));
	}

	/**
	 * Create an instance and set the given language into the rendertype
	 * @param language language (may be null)
	 * @throws NodeException
	 */
	public ContentLanguageTrx (ContentLanguage language) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		renderType = t.getRenderType();
		if (renderType != null) {
			oldLanguage = renderType.getLanguage();
			if (!Objects.equals(oldLanguage, language)) {
				renderType.setLanguage(language);
				languageWasSet = true;
			}
		}
	}

	@Override
	public void close() {
		if (languageWasSet) {
			renderType.setLanguage(oldLanguage);
		}
	}
}
