package com.gentics.contentnode.tests.assertj;

import com.gentics.contentnode.object.Content;

/**
 * Assert for contents
 */
public class ContentAssert extends AbstractNodeObjectAssert<ContentAssert, Content> {
	/**
	 * Create instance
	 * @param actual content to assert on
	 */
	protected ContentAssert(Content actual) {
		super(actual, ContentAssert.class);
	}
}
