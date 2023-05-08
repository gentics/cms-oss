package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.factory.Trx.operate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;

/**
 * Assert for pages
 */
public class PageAssert extends PublishableNodeObjectAssert<PageAssert, Page> {

	protected PageAssert(Page actual) {
		super(actual, PageAssert.class);
	}

	public PageAssert isQueued() throws NodeException {
		operate(() -> assertThat(actual.isQueued()).as(descriptionText() + " queued").isTrue());
		return this;
	}

	public PageAssert isNotQueued() throws NodeException {
		operate(() -> assertThat(actual.isQueued()).as(descriptionText() + " queued").isFalse());
		return this;
	}

	public PageAssert hasQueuedPublish(SystemUser user) throws NodeException {
		assertEquals(descriptionText() + " queued pub user", user, actual.getPubQueueUser());
		hasNoQueuedPublishAt();
		return this;
	}

	public PageAssert hasNoQueuedPublish() throws NodeException {
		assertNull(descriptionText() + " queued pub user", actual.getPubQueueUser());
		hasNoQueuedPublishAt();
		return this;
	}

	public PageAssert hasQueuedPublishAt(SystemUser user, int timestamp, String version) throws NodeException {
		assertEquals(descriptionText() + " queued time_pub user", user, actual.getPubQueueUser());
		assertEquals(descriptionText() + " queued time_pub", new ContentNodeDate(timestamp), actual.getTimePubQueue());
		assertNotNull(descriptionText() + " queued time_pub version", actual.getTimePubVersionQueue());
		assertEquals(descriptionText() + " queued time_pub version", version, actual.getTimePubVersionQueue().getNumber());
		return this;
	}

	public PageAssert hasNoQueuedPublishAt() throws NodeException {
		assertEquals(descriptionText() + " queued time_pub", new ContentNodeDate(0), actual.getTimePubQueue());
		assertNull(descriptionText() + " queued time_pub version", actual.getTimePubVersionQueue());
		return this;
	}

	public PageAssert hasQueuedOffline(SystemUser user) throws NodeException {
		assertEquals(descriptionText() + " queued off user", user, actual.getOffQueueUser());
		hasNoQueuedOfflineAt();
		return this;
	}

	public PageAssert hasNoQueuedOffline() throws NodeException {
		assertNull(descriptionText() + " queued off user", actual.getOffQueueUser());
		hasNoQueuedOfflineAt();
		return this;
	}

	public PageAssert hasQueuedOfflineAt(SystemUser user, int timestamp) throws NodeException {
		assertEquals(descriptionText() + " queued time_off user", user, actual.getOffQueueUser());
		assertEquals(descriptionText() + " queued time_off", new ContentNodeDate(timestamp), actual.getTimeOffQueue());
		return this;
	}

	public PageAssert hasNoQueuedOfflineAt() throws NodeException {
		assertEquals(descriptionText() + " queued time_off", new ContentNodeDate(0), actual.getTimeOffQueue());
		return this;
	}

	/**
	 * Assert that the page has the given language
	 * @param code expected language code
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert hasLanguage(String code) throws NodeException {
		assertThat(actual.getLanguage()).as(descriptionText() + " language").isNotNull().hasFieldOrPropertyWithValue("code", code);
		return myself;
	}

	/**
	 * Assert that the page has the given language variant
	 * @param code expected language code of the variant
	 * @param variant expected variant
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert hasLanguageVariant(String code, Page variant) throws NodeException {
		assertThat(actual.getLanguageVariant(code)).as(descriptionText() + " Language variant in " + code).isEqualTo(variant);
		return myself;
	}

	/**
	 * Assert that the page has the given page as page variant
	 * @param variant expected variant
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert hasPageVariant(Page variant) throws NodeException {
		assertThat(actual.getPageVariants()).as(descriptionText() + " page variants").contains(variant);
		return myself;
	}

	public PageAssert hasChannel(Node channel) throws NodeException {
		assertThat(actual.getChannel()).as(descriptionText() + " channel").isEqualTo(channel);
		return myself;
	}

	public PageAssert isMaster() throws NodeException {
		assertThat(actual.isMaster()).as(descriptionText() + " master flag").isTrue();
		return myself;
	}

	public PageAssert isNotMaster() throws NodeException {
		assertThat(actual.isMaster()).as(descriptionText() + " master flag").isFalse();
		return myself;
	}

	public PageAssert hasMaster(Page master) throws NodeException {
		assertThat(actual.getMaster()).as(descriptionText() + " master").isNotNull().isEqualTo(master);
		return myself;
	}

	public PageAssert isInheritedIn(Node channel) throws NodeException {
		NodeObject variant = MultichannellingFactory.getChannelVariant(actual, channel);
		assertThat(variant).as(descriptionText() + " variant in " + channel).isEqualTo(actual);
		return myself;
	}

	public PageAssert isLocalizedAs(Node channel, Page localizedVariant) throws NodeException {
		NodeObject variant = MultichannellingFactory.getChannelVariant(actual, channel);
		assertThat(variant).as(descriptionText() + " variant in " + channel).isEqualTo(localizedVariant);
		return myself;
	}

	/**
	 * Assert that the object is excluded
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert isExcluded() throws NodeException {
		assertThat(actual.isExcluded()).as(descriptionText() + " excluded").isTrue();
		return myself;
	}

	/**
	 * Assert that the object is not excluded
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert isNotExcluded() throws NodeException {
		assertThat(actual.isExcluded()).as(descriptionText() + " excluded").isFalse();
		return myself;
	}

	/**
	 * Assert that the object is disinherited in exactly the given channels
	 * @param channels channels
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert isDisinheritedIn(Node... channels) throws NodeException {
		assertThat(actual.getDisinheritedChannels()).as(descriptionText() + " disinherited channels").containsOnly(channels);
		return myself;
	}

	/**
	 * Assert that the object is disinherited by default
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert isDisinheritedByDefault() throws NodeException {
		assertThat(actual.isDisinheritDefault()).as(descriptionText() + " disinherited by default").isTrue();
		return myself;
	}

	/**
	 * Assert that the object is not disinherited by default
	 * @return fluent API
	 * @throws NodeException
	 */
	public PageAssert isNotDisinheritedByDefault() throws NodeException {
		assertThat(actual.isDisinheritDefault()).as(descriptionText() + " disinherited by default").isFalse();
		return myself;
	}
}
