package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.object.File;
import com.gentics.contentnode.rest.model.FilePrivileges;
import com.gentics.contentnode.rest.model.Privilege;

/**
 * Assert for file privileges
 */
public class FilePrivilegesAssert extends AbstractAssert<FilePrivilegesAssert, FilePrivileges> {
	/**
	 * Create an instance
	 * 
	 * @param actual
	 *            actual item
	 */
	protected FilePrivilegesAssert(FilePrivileges actual) {
		super(actual, FilePrivilegesAssert.class);
	}

	/**
	 * Assert that the instance does not contain null values
	 * @return fluent API
	 */
	public FilePrivilegesAssert isConsistent() {
		for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
			assertThat(actual.get(priv)).as(priv.name()).isNotNull();
		}
		return this;
	}

	/**
	 * Assert that the instance grants only the given privileges
	 * @param grant expected
	 * @return fluent API
	 */
	public FilePrivilegesAssert grantsOnly(Privilege... grant) {
		List<Privilege> grantList = Arrays.asList(grant);
		for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
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
	public FilePrivilegesAssert grantsAll() {
		for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
			assertThat(actual.get(priv)).as(priv.name()).isTrue();
		}
		return this;
	}

	/**
	 * Assert that the instance grants all but the given privileges
	 * @param nogrant privileges
	 * @return fluent API
	 */
	public FilePrivilegesAssert grantsAllBut(Privilege... nogrant) {
		List<Privilege> nograntList = Arrays.asList(nogrant);
		for (Privilege priv : Privilege.forRoleCheckType(File.TYPE_FILE)) {
			if (nograntList.contains(priv)) {
				assertThat(actual.get(priv)).as(priv.name()).isFalse();
			} else {
				assertThat(actual.get(priv)).as(priv.name()).isTrue();
			}
		}
		return this;
	}
}
