package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.object.Construct;

/**
 * Assert for constructs
 */
public class ConstructAssert extends AbstractNodeObjectAssert<ConstructAssert, Construct> {
	/**
	 * Create an instance
	 * @param actual actual construct
	 */
	protected ConstructAssert(Construct actual) {
		super(actual, ConstructAssert.class);
	}

	/**
	 * Assert keyword
	 * @param keyword keyword
	 * @return fluent API
	 */
	public ConstructAssert hasKeyword(String keyword) {
		assertThat(actual.getKeyword()).as(descriptionText() + " keyword").isEqualTo(keyword);
		return this;
	}

	/**
	 * Assert icon
	 * @param icon icon
	 * @return fluent API
	 */
	public ConstructAssert hasIcon(String icon) {
		assertThat(actual.getIconName()).as(descriptionText() + " icon").isEqualTo(icon);
		return this;
	}

	/**
	 * Assert name
	 * @param code language code
	 * @param name name
	 * @return fluent API
	 * @throws NodeException
	 */
	public ConstructAssert hasName(String code, String name) throws NodeException {
		try (LangTrx lang = new LangTrx(code)) {
			assertThat(actual.getName().toString()).as(descriptionText() + " name").isEqualTo(name);
		}
		return this;
	}

	/**
	 * Assert description
	 * @param code language code
	 * @param description description
	 * @return fluent API
	 * @throws NodeException
	 */
	public ConstructAssert hasDescription(String code, String description) throws NodeException {
		try (LangTrx lang = new LangTrx(code)) {
			assertThat(actual.getDescription().toString()).as(descriptionText() + " description").isEqualTo(description);
		}
		return this;
	}

	/**
	 * Assert mayBeSubtag
	 * @param flag flag
	 * @return fluent API
	 */
	public ConstructAssert mayBeSubtag(boolean flag) {
		assertThat(actual.mayBeSubtag()).as(descriptionText() + " mayBeSubtag").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert mayContainSubtags
	 * @param flag flag
	 * @return fuent API
	 */
	public ConstructAssert mayContainSubtags(boolean flag) {
		assertThat(actual.mayContainSubtags()).as(descriptionText() + " mayContainSubtags").isEqualTo(flag);
		return this;
	}

	/**
	 * Assert autoEnable
	 * @param flag flag
	 * @return fluent API
	 * @throws NodeException
	 */
	public ConstructAssert isAutoEnabled(boolean flag) throws NodeException {
		assertThat(actual.isAutoEnable()).as(descriptionText() + " autoEnable").isEqualTo(flag);
		return this;
	}
}
