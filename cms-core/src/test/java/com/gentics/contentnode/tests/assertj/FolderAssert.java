package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Template;

/**
 * Assert for folders
 */
public class FolderAssert extends AbstractLocalizableNodeObjectAssert<FolderAssert, Folder> {
	protected FolderAssert(Folder actual) {
		super(actual, FolderAssert.class);
	}

	/**
	 * Assert that the folder has exactly the given templates assigned
	 * @param templates templates
	 * @return fluent API
	 * @throws NodeException
	 */
	public FolderAssert hasTemplates(Template...templates) throws NodeException {
		assertThat(actual.getTemplates()).as(descriptionText() + " templates").containsOnly(templates);
		return myself;
	}
}
