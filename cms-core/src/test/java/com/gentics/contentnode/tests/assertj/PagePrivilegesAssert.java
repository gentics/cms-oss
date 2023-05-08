package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.model.PagePrivileges;
import com.gentics.contentnode.rest.model.Privilege;

/**
 * Assert for PagePrivileges
 */
public class PagePrivilegesAssert extends AbstractAssert<PagePrivilegesAssert, PagePrivileges> {
	/**
	 * Create an instance
	 * 
	 * @param actual
	 *            actual item
	 */
	protected PagePrivilegesAssert(PagePrivileges actual) {
		super(actual, PagePrivilegesAssert.class);
	}

	/**
	 * Assert that the instance is consistent (does not contain null value)
	 * @return fluent API
	 */
	public PagePrivilegesAssert isConsistent() {
		for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
			assertThat(actual.get(priv)).as(priv.name()).isNotNull();
		}
		return this;
	}

	/**
	 * Assert that the instance grants only the given privileges
	 * @param grant expected
	 * @return fluent API
	 */
	public PagePrivilegesAssert grantsOnly(Privilege... grant) {
		List<Privilege> grantList = Arrays.asList(grant);
		for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
			if (grantList.contains(priv)) {
				assertThat(actual.get(priv)).as(priv.name()).isTrue();
			} else {
				assertThat(actual.get(priv)).as(priv.name()).isFalse();
			}
		}
		return this;
	}

	/**
	 * Assert that the instance grants all privileges
	 * @return fluent API
	 */
	public PagePrivilegesAssert grantsAll() {
		for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
			assertThat(actual.get(priv)).as(priv.name()).isTrue();
		}
		return this;
	}

	/**
	 * Assert that the instance grants all but the given privileges
	 * @param nogrant privileges
	 * @return fluent API
	 */
	public PagePrivilegesAssert grantsAllBut(Privilege... nogrant) {
		List<Privilege> nograntList = Arrays.asList(nogrant);
		for (Privilege priv : Privilege.forRoleCheckType(Page.TYPE_PAGE)) {
			if (nograntList.contains(priv)) {
				assertThat(actual.get(priv)).as(priv.name()).isFalse();
			} else {
				assertThat(actual.get(priv)).as(priv.name()).isTrue();
			}
		}
		return this;
	}
}
