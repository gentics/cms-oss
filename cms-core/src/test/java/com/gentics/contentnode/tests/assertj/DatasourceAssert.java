package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.contentnode.object.Datasource;

/**
 * Assert for datasources
 */
public class DatasourceAssert extends AbstractNodeObjectAssert<DatasourceAssert, Datasource> {
	/**
	 * Create an instance
	 * @param actual actual datasource
	 */
	protected DatasourceAssert(Datasource actual) {
		super(actual, DatasourceAssert.class);
	}

	/**
	 * Assert name
	 * @param name name
	 * @return fluent API
	 */
	public DatasourceAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert that datasource is static
	 * @return fluent API
	 */
	public DatasourceAssert isStatic() {
		assertThat(actual.getSourceType()).as(descriptionText() + " type").isEqualTo(Datasource.SourceType.staticDS);
		return this;
	}
}
