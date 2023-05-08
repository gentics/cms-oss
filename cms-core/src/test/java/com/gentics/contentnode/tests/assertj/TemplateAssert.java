package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;

/**
 * Assert for templates
 */
public class TemplateAssert extends AbstractNodeObjectAssert<TemplateAssert, Template> {
	/**
	 * Create an instance
	 * @param actual actual template
	 */
	protected TemplateAssert(Template actual) {
		super(actual, TemplateAssert.class);
	}

	/**
	 * Assert name
	 * @param name name
	 * @return fluent API
	 */
	public TemplateAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return this;
	}

	/**
	 * Assert description
	 * @param description description
	 * @return fluent API
	 */
	public TemplateAssert hasDescription(String description) {
		assertThat(actual.getDescription()).as(descriptionText() + " description").isEqualTo(description);
		return this;
	}

	/**
	 * Assert markup language
	 * @param type markupLanguage name
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert hasType(String type) throws NodeException {
		assertThat(actual.getMarkupLanguage().getName()).as(descriptionText() + " type").isEqualTo(type);
		return this;
	}

	/**
	 * Assert source
	 * @param source source
	 * @return fluent API
	 */
	public TemplateAssert hasSource(String source) {
		assertThat(actual.getSource()).as(descriptionText() + " source").isEqualTo(source);
		return this;
	}

	/**
	 * Assert editor
	 * @param editor editor
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert hasEditor(SystemUser editor) throws NodeException {
		assertThat(actual.getEditor()).as(descriptionText() + " editor").isEqualTo(editor);
		return this;
	}

	/**
	 * Assert edate
	 * @param edate edate
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert hasEDate(int edate) throws NodeException {
		assertThat(actual.getEDate().getIntTimestamp()).as(descriptionText() + " edate").isEqualTo(edate);
		return this;
	}

	/**
	 * Assert that the template is locked
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert isLocked() throws NodeException {
		assertThat(actual.isLocked()).as(descriptionText() + " locked").isTrue();
		return myself;
	}

	/**
	 * Assert that the template is locked by the given user
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert isLockedBy(SystemUser user) throws NodeException {
		assertThat(actual.getLockedBy()).as(descriptionText() + " locked by").isEqualTo(user);
		return myself;
	}

	/**
	 * Assert that the template is not locked
	 * @return fluent API
	 * @throws NodeException
	 */
	public TemplateAssert isNotLocked() throws NodeException {
		assertThat(actual.isLocked()).as(descriptionText() + " locked").isFalse();
		return myself;
	}
}
