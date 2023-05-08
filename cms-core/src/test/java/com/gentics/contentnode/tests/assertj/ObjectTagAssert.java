package com.gentics.contentnode.tests.assertj;

import com.gentics.contentnode.object.ObjectTag;

/**
 * Assert for object tags
 */
public class ObjectTagAssert extends AbstractTagAssert<ObjectTagAssert, ObjectTag> {
	/**
	 * Create instance
	 * @param actual actual tag
	 */
	protected ObjectTagAssert(ObjectTag actual) {
		super(actual, ObjectTagAssert.class);
	}
}
