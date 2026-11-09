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
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Multichannelling test for staging of files
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FolderMultichannellingStagingTest extends AbstractMultichannellingContentStagingTest<Folder> {
	/**
	 * Create test instance
	 */
	public FolderMultichannellingStagingTest() {
		super(Folder.class, "folder");
	}

	@Override
	protected Folder createMasterObject(Folder folder) throws NodeException {
		return create(Folder.class, f -> {
			f.setMotherId(folder.getId());
			f.setName("Master Folder");
		}).build();
	}

	@Override
	protected Folder localizeObject(Folder master) throws NodeException {
		return execute(m -> localize(m, channel), master);
	}

	@Override
	protected Folder createLocalObject(Folder folder) throws NodeException {
		return create(Folder.class, f -> {
			f.setMotherId(folder.getId());
			f.setChannelInfo(channel.getId(), null);
			f.setName("Master Folder");
		}).build();
	}

	@Override
	protected Folder createExcludedObject(Folder folder) throws NodeException {
		Folder object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), false)).build();
	}

	@Override
	protected Folder createDisinheritedObject(Folder folder) throws NodeException {
		Folder object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(false, Collections.singleton(channel), false))
				.build();
	}

	@Override
	protected Folder createDefaultDisinherited(Folder folder) throws NodeException {
		Folder object = createMasterObject(folder);
		return update(object, f -> f.setDisinheritDefault(true, false)).build();
	}

	@Override
	protected void assertMasterObject(Folder object, Folder folder) throws NodeException {
		assertThat(object).as("Staged master folder")
			.isNotNull()
			.isMaster()
			.isInheritedIn(channel)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalizedObject(Folder masterObject, Folder localizedObject, Folder folder) throws NodeException {
		assertThat(localizedObject).as("Staged localized folder")
			.isNotNull()
			.isNotMaster()
			.hasChannel(channel)
			.hasMaster(masterObject)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalObject(Folder object, Node channel, Folder folder) throws NodeException {
		assertThat(object).as("Staged local folder")
			.isNotNull()
			.hasChannel(channel)
			.isMaster()
			.hasParent(folder);
	}

	@Override
	protected void assertExcluded(Folder object) throws NodeException {
		assertThat(object).as("Staged folder")
			.isNotNull()
			.isExcluded()
			.isDisinheritedIn()
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDisinherited(Folder object) throws NodeException {
		assertThat(object).as("Staged folder")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn(channel)
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDefaultDisinherited(Folder object) throws NodeException {
		assertThat(object).as("Staged folder")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn()
			.isDisinheritedByDefault();
	}

}
