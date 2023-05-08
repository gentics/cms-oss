package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTagDefinition;


/**
 * Assert for object tag definitions
 */
public class ObjectTagDefinitionAssert extends AbstractNodeObjectAssert<ObjectTagDefinitionAssert, ObjectTagDefinition> {
	/**
	 * Create an instance
	 * @param actual actual definition
	 */
	protected ObjectTagDefinitionAssert(ObjectTagDefinition actual) {
		super(actual, ObjectTagDefinitionAssert.class);
	}

	/**
	 * Assert keyword
	 * @param keyword keyword
	 * @return fluent API
	 * @throws NodeException 
	 */
	public ObjectTagDefinitionAssert hasKeyword(String keyword) throws NodeException {
		assertThat(actual.getObjectTag().getName()).as(descriptionText() + " keyword").isEqualTo(keyword);
		return this;
	}

	/**
	 * Assert name
	 * @param code language code
	 * @param name name
	 * @return fluent API
	 * @throws NodeException
	 */
	public ObjectTagDefinitionAssert hasName(String code, String name) throws NodeException {
		try (LangTrx lang = new LangTrx(code)) {
			assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
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
	public ObjectTagDefinitionAssert hasDescription(String code, String description) throws NodeException {
		try (LangTrx lang = new LangTrx(code)) {
			assertThat(actual.getDescription()).as(descriptionText() + " description").isEqualTo(description);
		}
		return this;
	}

	/**
	 * Assert construct global ID
	 * @param globalId construct global ID
	 * @return fluent API
	 * @throws NodeException
	 */
	public ObjectTagDefinitionAssert hasConstruct(GlobalId globalId) throws NodeException {
		assertThat(actual.getObjectTag().getConstruct()).as(descriptionText() + " construct").isNotNull().has(globalId);
		return this;
	}

	/**
	 * Assert target type
	 * @param targetType target type
	 * @return fluent API
	 */
	public ObjectTagDefinitionAssert isTargetType(int targetType) {
		assertThat(actual.getTargetType()).as(descriptionText() + " target type").isEqualTo(targetType);
		return this;
	}
}
