package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;

import java.util.Collections;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Multichannelling test for staging of files
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS })
public class PageMultichannellingStagingTest extends AbstractMultichannellingContentStagingTest<Page> {
	/**
	 * Create test instance
	 */
	public PageMultichannellingStagingTest() {
		super(Page.class, "page");
	}

	@Override
	protected Page createMasterObject(Folder folder) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Master Page");
		}).publish().build();
	}

	@Override
	protected Page localizeObject(Page master) throws NodeException {
		Page p = execute(m -> localize(m, channel), master);
		return update(p, upd -> {}).publish().build();
	}

	@Override
	protected Page createLocalObject(Folder folder) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Local Page");
			p.setChannelInfo(channel.getId(), null);
		}).publish().build();
	}

	@Override
	protected Page createExcludedObject(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), false)).build();
	}

	@Override
	protected Page createDisinheritedObject(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(false, Collections.singleton(channel), false))
				.build();
	}

	@Override
	protected Page createDefaultDisinherited(Folder folder) throws NodeException {
		Page object = createMasterObject(folder);
		return update(object, f -> f.setDisinheritDefault(true, false)).build();
	}

	@Override
	protected void assertMasterObject(Page object, Folder folder) throws NodeException {
		assertThat(object).as("Staged master page")
			.isNotNull()
			.isMaster()
			.isInheritedIn(channel)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalizedObject(Page masterObject, Page localizedObject, Folder folder) throws NodeException {
		assertThat(localizedObject).as("Staged localized page")
			.isNotNull()
			.isNotMaster()
			.hasChannel(channel)
			.hasMaster(masterObject)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalObject(Page object, Node channel, Folder folder) throws NodeException {
		assertThat(object).as("Staged local page")
			.isNotNull()
			.hasChannel(channel)
			.isMaster()
			.hasParent(folder);
	}

	@Override
	protected void assertExcluded(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isExcluded()
			.isDisinheritedIn()
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDisinherited(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn(channel)
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDefaultDisinherited(Page object) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn()
			.isDisinheritedByDefault();
	}
}
