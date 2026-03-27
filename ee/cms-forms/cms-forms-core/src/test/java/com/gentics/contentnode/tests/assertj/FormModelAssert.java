package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.form.FormModel;

public class FormModelAssert extends AbstractAssert<FormModelAssert, FormModel> {
	/**
	 * Create an instance
	 * @param actual actual form model
	 */
	protected FormModelAssert(FormModel actual) {
		super(actual, FormModelAssert.class);
	}

	public FormModelAssert isOnline() {
		assertThat(actual.isOnline()).as(descriptionText() + " online").isTrue();
		return this;
	}

	public FormModelAssert isOffline() {
		assertThat(actual.isOnline()).as(descriptionText() + " online").isFalse();
		return this;
	}

	public FormModelAssert isModified() {
		assertThat(actual.isModified()).as(descriptionText() + " modified").isTrue();
		return this;
	}

	public FormModelAssert isNotModified() {
		assertThat(actual.isModified()).as(descriptionText() + " modified").isFalse();
		return this;
	}

	public FormModelAssert isPlannedOnline(int at, String version) {
		assertThat(actual.getTimeManagement()).as(descriptionText() + " time management").isNotNull()
				.hasFieldOrPropertyWithValue("at", at);
		assertThat(actual.getTimeManagement().getVersion()).as(descriptionText() + " time management version")
				.isNotNull().hasFieldOrPropertyWithValue("number", version);
		return this;
	}

	public FormModelAssert isNotPlannedOnline() {
		assertThat(actual.getTimeManagement()).as(descriptionText() + " time management").isNotNull()
				.hasFieldOrPropertyWithValue("at", 0);
		assertThat(actual.getTimeManagement().getVersion()).as(descriptionText() + " time management version").isNull();
		return this;
	}

	public FormModelAssert isPlannedOffline(int at) {
		assertThat(actual.getTimeManagement()).as(descriptionText() + " time management").isNotNull()
				.hasFieldOrPropertyWithValue("offlineAt", at);
		return this;
	}

	public FormModelAssert isNotPlannedOffline() {
		assertThat(actual.getTimeManagement()).as(descriptionText() + " time management").isNotNull()
				.hasFieldOrPropertyWithValue("offlineAt", 0);
		return this;
	}

	public FormModelAssert isNotPlanned() {
		assertThat(actual.isPlanned()).as(descriptionText() + " planned").isFalse();
		return this;
	}

	public FormModelAssert isDeleted() {
		assertThat(actual.getDeleted()).as(descriptionText() + " deleted").isNotNull();
		assertThat(actual.getDeleted().getAt()).as(descriptionText() + " deleted at").isGreaterThan(0);
		assertThat(actual.getDeleted().getBy()).as(descriptionText() + " deleted by").isNotNull();
		return this;
	}

	public FormModelAssert isNotDeleted() {
		assertThat(actual.getDeleted()).as(descriptionText() + " deleted").isNotNull();
		assertThat(actual.getDeleted().getAt()).as(descriptionText() + " deleted at").isEqualTo(0);
		assertThat(actual.getDeleted().getBy()).as(descriptionText() + " deleted by").isNull();
		return this;
	}

	public FormModelAssert isLocked() {
		assertThat(actual.getLockedSince()).as(descriptionText() + " locked since").isGreaterThan(0);
		assertThat(actual.getLockedBy()).as(descriptionText() + " locked by").isNotNull();
		return this;
	}

	public FormModelAssert isNotLocked() {
		assertThat(actual.getLockedSince()).as(descriptionText() + " locked since").isEqualTo(-1);
		assertThat(actual.getLockedBy()).as(descriptionText() + " locked by").isNull();
		return this;
	}

	public FormModelAssert hasVersion(String version) {
		assertThat(actual.getVersion()).as(descriptionText() + " version").isNotNull().hasFieldOrPropertyWithValue("number", version);
		return this;
	}

	public FormModelAssert hasId(int id) {
		assertThat(actual.getId()).as(descriptionText() + " ID").isEqualTo(id);
		return this;
	}

	public FormModelAssert hasFormType(String formType) {
		assertThat(actual.getFormType()).as(descriptionText() + " form type").isEqualTo(formType);
		return this;
	}

	public FormModelAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	public FormModelAssert hasDescription(String description) {
		assertThat(actual.getDescription()).as(descriptionText() + " description").isEqualTo(description);
		return this;
	}

	public FormModelAssert hasFolder(int folderId) {
		assertThat(actual.getFolderId()).as(descriptionText() + " folder ID").isEqualTo(folderId);
		return this;
	}

	public FormModelAssert hasExternalId(String externalId) {
		assertThat(actual.getExternalId()).as(descriptionText() + " external ID").isEqualTo(externalId);
		return this;
	}
}
