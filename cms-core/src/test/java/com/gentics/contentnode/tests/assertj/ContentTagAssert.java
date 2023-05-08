package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.contentnode.object.ContentTag;

/**
 * Assert for content tags
 */
public class ContentTagAssert extends AbstractTagAssert<ContentTagAssert, ContentTag> {
	/**
	 * Create an instance
	 * @param actual actual tag
	 */
	protected ContentTagAssert(ContentTag actual) {
		super(actual, ContentTagAssert.class);
	}

	/**
	 * Assert comes from template
	 * @param flag boolean
	 * @return fluent API
	 */
	public ContentTagAssert comesFromTemplate(boolean flag) {
		assertThat(actual.comesFromTemplate()).as(descriptionText() + " comesFromTemplate").isEqualTo(flag);
		return myself;
	}
}
