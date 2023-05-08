package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;

/**
 * Abstract asserter for localizable node objects
 *
 * @param <S>
 * @param <A>
 */
public abstract class AbstractLocalizableNodeObjectAssert<S extends AbstractLocalizableNodeObjectAssert<S, A>, A extends Disinheritable<? extends NodeObject>>
		extends AbstractNodeObjectAssert<S, A> {
	/**
	 * Create instance
	 * @param actual object to assert on
	 * @param selfType self type
	 */
	protected AbstractLocalizableNodeObjectAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	/**
	 * Assert that the object has the channel
	 * @param channel channel
	 * @return fluent API
	 * @throws NodeException
	 */
	public S hasChannel(Node channel) throws NodeException {
		assertThat(actual.getChannel()).as(descriptionText() + " channel").isEqualTo(channel);
		return myself;
	}

	/**
	 * Assert that the object is a master object
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isMaster() throws NodeException {
		assertThat(actual.isMaster()).as(descriptionText() + " master flag").isTrue();
		return myself;
	}

	/**
	 * Assert that the object is not a master object
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isNotMaster() throws NodeException {
		assertThat(actual.isMaster()).as(descriptionText() + " master flag").isFalse();
		return myself;
	}

	/**
	 * Assert that the object has the given master
	 * @param master master
	 * @return fluent API
	 * @throws NodeException
	 */
	public S hasMaster(A master) throws NodeException {
		assertThat(actual.getMaster()).as(descriptionText() + " master").isNotNull().isEqualTo(master);
		return myself;
	}

	/**
	 * Assert that the object is inherited (and not localized) in the channel
	 * @param channel channel
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isInheritedIn(Node channel) throws NodeException {
		NodeObject variant = MultichannellingFactory.getChannelVariant((Disinheritable<? extends NodeObject>) actual,
				channel);
		assertThat(variant).as(descriptionText() + " variant in " + channel).isEqualTo(actual);
		return myself;
	}

	/**
	 * Assert that the object is localized with the given variant in the channel
	 * @param channel channel
	 * @param localizedVariant localized variant
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isLocalizedAs(Node channel, A localizedVariant) throws NodeException {
		NodeObject variant = MultichannellingFactory.getChannelVariant((Disinheritable<? extends NodeObject>) actual,
				channel);
		assertThat(variant).as(descriptionText() + " variant in " + channel).isEqualTo(localizedVariant);
		return myself;
	}

	/**
	 * Assert that the object is excluded
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isExcluded() throws NodeException {
		assertThat(actual.isExcluded()).as(descriptionText() + " excluded").isTrue();
		return myself;
	}

	/**
	 * Assert that the object is not excluded
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isNotExcluded() throws NodeException {
		assertThat(actual.isExcluded()).as(descriptionText() + " excluded").isFalse();
		return myself;
	}

	/**
	 * Assert that the object is disinherited in exactly the given channels
	 * @param channels channels
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isDisinheritedIn(Node... channels) throws NodeException {
		assertThat(actual.getDisinheritedChannels()).as(descriptionText() + " disinherited channels").containsOnly(channels);
		return myself;
	}

	/**
	 * Assert that the object is disinherited by default
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isDisinheritedByDefault() throws NodeException {
		assertThat(actual.isDisinheritDefault()).as(descriptionText() + " disinherited by default").isTrue();
		return myself;
	}

	/**
	 * Assert that the object is not disinherited by default
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isNotDisinheritedByDefault() throws NodeException {
		assertThat(actual.isDisinheritDefault()).as(descriptionText() + " disinherited by default").isFalse();
		return myself;
	}
}
