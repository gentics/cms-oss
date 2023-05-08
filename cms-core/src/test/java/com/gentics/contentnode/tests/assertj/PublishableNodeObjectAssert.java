package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.factory.Trx.operate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.PublishableNodeObject;

public class PublishableNodeObjectAssert<S extends PublishableNodeObjectAssert<S, A>, A extends PublishableNodeObject> extends AbstractNodeObjectAssert<S, A> {
	protected String[] comparedVersionFields = {"number", "current", "published", "date"};

	protected PublishableNodeObjectAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public S isOnline() throws NodeException {
		operate(() -> assertThat(actual.isOnline()).as(descriptionText() + " online").isTrue());
		return myself;
	}

	public S isOffline() throws NodeException {
		operate(() -> assertThat(actual.isOnline()).as(descriptionText() + " online").isFalse());
		return myself;
	}

	public S isModified() throws NodeException {
		assertThat(actual.isModified()).as(descriptionText() + " modified").isTrue();
		return myself;
	}

	public S isNotModified() throws NodeException {
		assertThat(actual.isModified()).as(descriptionText() + " modified").isFalse();
		return myself;
	}

	public S isPlanned() throws NodeException {
		assertThat(actual.isPlanned()).as(descriptionText() + " planned").isTrue();
		return myself;
	}

	public S isNotPlanned() throws NodeException {
		assertThat(actual.isPlanned()).as(descriptionText() + " planned").isFalse();
		return myself;
	}

	/**
	 * Use the given fields for comparing node versions
	 * @param fields fields
	 * @return fluent API
	 */
	public S comparingVersionFields(String ...fields) {
		this.comparedVersionFields = fields;
		return myself;
	}

	/**
	 * Assert that the object has exactly the expected object versions (in the order from newest to oldest)
	 * @param versions
	 * @return
	 * @throws NodeException
	 */
	public S hasVersions(NodeObjectVersion... versions) throws NodeException {
		assertThat(actual.getVersions()).as(descriptionText() + " versions")
				.usingElementComparatorOnFields(comparedVersionFields).containsExactly(versions);
		return myself;
	}

	public S hasPublishAt(int timestamp, String version) throws NodeException {
		assertEquals(descriptionText() + " time_pub", new ContentNodeDate(timestamp), actual.getTimePub());
		assertNotNull(descriptionText() + " time_pub version", actual.getTimePubVersion());
		assertEquals(descriptionText() + " time_pub version", version, actual.getTimePubVersion().getNumber());
		return myself;
	}

	public S hasNoPublishAt() throws NodeException {
		assertEquals(descriptionText() + " time_pub", new ContentNodeDate(0), actual.getTimePub());
		assertNull(descriptionText() + " time_pub version", actual.getTimePubVersion());
		return myself;
	}

	public S hasOfflineAt(int timestamp) throws NodeException {
		assertEquals(descriptionText() + " time_off", new ContentNodeDate(timestamp), actual.getTimeOff());
		return myself;
	}

	public S hasNoOfflineAt() throws NodeException {
		assertEquals(descriptionText() + " time_off", new ContentNodeDate(0), actual.getTimeOff());
		return myself;
	}
}
