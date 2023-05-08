package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.Construct;

public class ConstructModelAssert extends AbstractAssert<ConstructModelAssert, Construct> {

	protected ConstructModelAssert(Construct actual) {
		super(actual, ConstructModelAssert.class);
	}

	public ConstructModelAssert matches(com.gentics.contentnode.object.Construct construct) {
		assertThat(actual.getId()).as(descriptionText() + " ID").isEqualTo(construct.getId());
		assertThat(actual.getGlobalId()).as(descriptionText() + " global ID").isEqualTo(construct.getGlobalId().toString());
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(construct.getName().toString());
		assertThat(actual.getDescription()).as(descriptionText() + " description").isEqualTo(construct.getDescription().toString());
		assertThat(actual.getKeyword()).as(descriptionText() + " keyword").isEqualTo(construct.getKeyword());
		return myself;
	}
}
