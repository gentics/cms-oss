package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.devtools.Package;

/**
 * Assert for packages
 */
public class PackageAssert extends AbstractAssert<PackageAssert, Package> {
	/**
	 * Create an instance
	 * @param actual actual package
	 */
	protected PackageAssert(Package actual) {
		super(actual, PackageAssert.class);
	}

	/**
	 * Assert that the package has the given name
	 * @param name expected name
	 * @return fluent API
	 */
	public PackageAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return myself;
	}

	/**
	 * Assert the description
	 * @param description expected description
	 * @return fluent API
	 */
	public PackageAssert hasDescription(String description) {
		assertThat(actual.getDescription()).as(descriptionText() + " description").isEqualTo(description);
		return myself;
	}

	/**
	 * Assert constructs number
	 * @param constructs expected number of constructs
	 * @return fluent API
	 */
	public PackageAssert hasConstructs(int constructs) {
		assertThat(actual.getConstructs()).as(descriptionText() + " constructs").isEqualTo(constructs);
		return myself;
	}

	/**
	 * Assert datasources number
	 * @param datasources expected number of datasources
	 * @return fluent API
	 */
	public PackageAssert hasDatasources(int datasources) {
		assertThat(actual.getDatasources()).as(descriptionText() + " datasources").isEqualTo(datasources);
		return myself;
	}

	/**
	 * Assert object properties number
	 * @param objectProperties expected number of object properties
	 * @return fluent API
	 */
	public PackageAssert hasObjectProperties(int objectProperties) {
		assertThat(actual.getObjectProperties()).as(descriptionText() + " object properties").isEqualTo(objectProperties);
		return myself;
	}

	/**
	 * Assert templates number
	 * @param templates expected number of templates
	 * @return fluent API
	 */
	public PackageAssert hasTemplates(int templates) {
		assertThat(actual.getTemplates()).as(descriptionText() + " templates").isEqualTo(templates);
		return myself;
	}
}
