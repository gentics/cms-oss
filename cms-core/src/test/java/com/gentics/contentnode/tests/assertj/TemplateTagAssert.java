package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.contentnode.object.TemplateTag;

/**
 * Assert for template tags
 */
public class TemplateTagAssert extends AbstractTagAssert<TemplateTagAssert, TemplateTag> {
	/**
	 * Create an instance
	 * @param actual
	 */
	protected TemplateTagAssert(TemplateTag actual) {
		super(actual, TemplateTagAssert.class);
	}

	/**
	 * Assert editable in page
	 * @param flag flag
	 * @return fluent API
	 */
	public TemplateTagAssert isEditableInPage(boolean flag) {
		assertThat(actual.isPublic()).as(descriptionText() + " editableInPage").isEqualTo(flag);
		return myself;
	}

	/**
	 * Assert mandatory
	 * @param flag flag
	 * @return fluent API
	 */
	public TemplateTagAssert isMandatory(boolean flag) {
		assertThat(actual.getMandatory()).as(descriptionText() + " mandatory").isEqualTo(flag);
		return myself;
	}
}
