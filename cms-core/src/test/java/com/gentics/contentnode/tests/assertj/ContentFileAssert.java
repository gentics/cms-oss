package com.gentics.contentnode.tests.assertj;

import com.gentics.contentnode.object.File;

/**
 * Assert for files
 */
public class ContentFileAssert extends AbstractLocalizableNodeObjectAssert<ContentFileAssert, File> {

	protected ContentFileAssert(File actual) {
		super(actual, ContentFileAssert.class);
	}

}
