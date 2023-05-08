package com.gentics.contentnode.tests.assertj;

import com.gentics.contentnode.object.ContentRepository;

/**
 * Assert for datasources
 */
public class ContentRepositoryAssert extends AbstractNodeObjectAssert<ContentRepositoryAssert, ContentRepository> {
	/**
	 * Create an instance
	 * @param actual actual instance
	 */
	protected ContentRepositoryAssert(ContentRepository actual) {
		super(actual, ContentRepositoryAssert.class);
	}
}
