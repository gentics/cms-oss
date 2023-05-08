package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.events.Dependency;

/**
 * Assert for Dependency instances
 */
public class DependencyAssert extends AbstractAssert<DependencyAssert, Dependency> {
	/**
	 * Create instance
	 * @param actual actual object
	 */
	protected DependencyAssert(Dependency actual) {
		super(actual, DependencyAssert.class);
	}

	/**
	 * Assert that the dependency matches
	 * @param dependency
	 * @return fluent API
	 */
	public DependencyAssert matches(Dependency dependency) throws NodeException {
		assertThat(dependency.getSource()).as(descriptionText() + " source object").isEqualTo(actual.getSource());
		assertThat(dependency.getSourceProperty()).as(descriptionText() + " source property").isEqualTo(actual.getSourceProperty());

		assertThat(dependency.getDependent()).as(descriptionText() + " dependent object").isEqualTo(actual.getDependent());
		assertThat(dependency.getDependentProperties()).as(descriptionText() + " dependent properties").containsAllEntriesOf(actual.getDependentProperties());

		assertThat(dependency.getChannelIds()).as(descriptionText() + " channel IDs").containsExactlyElementsOf(actual.getChannelIds());

		return this;
	}
}
