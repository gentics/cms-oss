package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.object.Tag;

/**
 * Abstract Assert for Tags
 *
 * @param <S> self type
 * @param <A> type of actual Tag instance
 */
public abstract class AbstractTagAssert<S extends AbstractObjectAssert<S, A>, A extends Tag> extends AbstractNodeObjectAssert<S, A> {
	/**
	 * Create an instance
	 * @param actual actual tag instance
	 * @param selfType self type
	 */
	protected AbstractTagAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	/**
	 * Assert name
	 * @param name name
	 * @return fluent API
	 */
	public S hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return myself;
	}

	/**
	 * Assert enabled
	 * @param flag flag
	 * @return fluent API
	 */
	public S isEnabled(boolean flag) {
		assertThat(actual.isEnabled()).as(descriptionText() + " enabled").isEqualTo(flag);
		return myself;
	}

	/**
	 * Assert editable
	 * @param flag flag
	 * @return fluent API
	 * @throws NodeException
	 */
	public S isEditable(boolean flag) throws NodeException {
		assertThat(actual.isEditable()).as(descriptionText() + " editable").isEqualTo(flag);
		return myself;
	}

	/**
	 * Assert construct
	 * @param globalId globalId
	 * @return fluent API
	 * @throws NodeException
	 */
	public S hasConstruct(GlobalId globalId) throws NodeException {
		assertThat(actual.getConstruct()).as(descriptionText() + " construct").isNotNull().has(globalId);
		return myself;
	}

	/**
	 * Assert that the tag has a part with given name, type and text
	 * @param clazz parttype class
	 * @param partName part name
	 * @param text expected text
	 * @return fluent API
	 * @throws NodeException
	 */
	public S hasPartWithText(Class<? extends TextPartType> clazz, String partName, String text) throws NodeException {
		TextPartType partType = getPartType(clazz, actual, partName);
		assertThat(partType).as(descriptionText() + " part " + partName).isNotNull();
		assertThat(partType.getValueObject().getValueText()).as(descriptionText() + " part " + partName).isEqualTo(text);
		return myself;
	}
}
