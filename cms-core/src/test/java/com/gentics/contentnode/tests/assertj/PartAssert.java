package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.object.Part;

/**
 * Assert for parts
 */
public class PartAssert extends AbstractNodeObjectAssert<PartAssert, Part> {
	/**
	 * Create an instance
	 * @param actual actual part
	 */
	protected PartAssert(Part actual) {
		super(actual, PartAssert.class);
	}

	/**
	 * Assert keyword
	 * @param keyword keyword
	 * @return fluent API
	 */
	public PartAssert hasKeyword(String keyword) {
		assertThat(actual.getKeyname()).as(descriptionText() + " keyword").isEqualTo(keyword);
		return this;
	}

	/**
	 * Assert name
	 * @param code language code
	 * @param name name
	 * @return fluent API
	 * @throws NodeException
	 */
	public PartAssert hasName(String code, String name) throws NodeException {
		try (LangTrx lang = new LangTrx(code)) {
			assertThat(actual.getName().toString()).as(descriptionText() + " name").isEqualTo(name);
		}
		return this;
	}

	/**
	 * Assert editable
	 * @param flag flag
	 * @return fluent API
	 */
	public PartAssert isEditable(boolean flag) {
		assertThat(actual.isEditable()).as(descriptionText() + " editable").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert type ID
	 * @param typeId type ID
	 * @return fluent API
	 */
	public PartAssert hasTypeId(int typeId) {
		assertThat(actual.getPartTypeId()).as(descriptionText() + " typeId").isEqualTo(typeId);
		return this;
	}

	/**
	 * Assert order
	 * @param order order
	 * @return fluent API
	 */
	public PartAssert hasOrder(int order) {
		assertThat(actual.getPartOrder()).as(descriptionText() + " order").isEqualTo(order);
		return this;
	}

	/**
	 * Assert mlId
	 * @param mlId mlId
	 * @return fluent API
	 */
	public PartAssert hasMlId(int mlId) {
		assertThat(actual.getMlId()).as(descriptionText() + " mlId").isEqualTo(mlId);
		return this;
	}

	/**
	 * Assert visible
	 * @param flag flag
	 * @return fluent API
	 */
	public PartAssert isVisible(boolean flag) {
		assertThat(actual.isVisible()).as(descriptionText() + " visible").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert required
	 * @param flag flag
	 * @return fluent API
	 */
	public PartAssert isRequired(boolean flag) {
		assertThat(actual.isRequired()).as(descriptionText() + " required").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert inline editable
	 * @param flag flag
	 * @return fluent API
	 */
	public PartAssert isInlineEditable(boolean flag) {
		assertThat(actual.isInlineEditable()).as(descriptionText() + " inlineEditable").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert hideInEditor flag
	 * @param flag flag
	 * @return fluent API
	 */
	public PartAssert isHideInEditor(boolean flag) {
		assertThat(actual.isHideInEditor()).as(descriptionText() + " hideInEditor").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert external editor url
	 * @param externalEditorUrl expected
	 * @return fluent API
	 */
	public PartAssert hasExternalEditorUrl(String externalEditorUrl) {
		assertThat(actual.getExternalEditorUrl()).as(descriptionText() + " externalEditorUrl").isEqualTo(externalEditorUrl);
		return this;
	}

	/**
	 * Assert infoInt
	 * @param infoInt infoInt
	 * @return fluent API
	 */
	public PartAssert hasInfoInt(int infoInt) {
		assertThat(actual.getInfoInt()).as(descriptionText() + " infoInt").isEqualTo(infoInt);
		return this;
	}
}
