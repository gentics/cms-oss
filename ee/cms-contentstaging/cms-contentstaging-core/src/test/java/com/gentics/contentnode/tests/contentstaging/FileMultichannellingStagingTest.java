package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
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
public class FileMultichannellingStagingTest extends AbstractMultichannellingContentStagingTest<File> {
	/**
	 * Create test instance
	 */
	public FileMultichannellingStagingTest() {
		super(File.class, "file");
	}

	@Override
	protected File createMasterObject(Folder folder) throws NodeException {
		return create(File.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
		}).build();
	}

	@Override
	protected File localizeObject(File master) throws NodeException {
		return execute(m -> localize(m, channel), master);
	}

	@Override
	protected File createLocalObject(Folder folder) throws NodeException {
		return create(File.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testfile.txt");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
			f.setChannelInfo(channel.getId(), null);
		}).build();
	}

	@Override
	protected File createExcludedObject(Folder folder) throws NodeException {
		File object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), false)).build();
	}

	@Override
	protected File createDisinheritedObject(Folder folder) throws NodeException {
		File object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(false, Collections.singleton(channel), false))
				.build();
	}

	@Override
	protected File createDefaultDisinherited(Folder folder) throws NodeException {
		File object = createMasterObject(folder);
		return update(object, f -> f.setDisinheritDefault(true, false)).build();
	}

	@Override
	protected void assertMasterObject(File object, Folder folder) throws NodeException {
		assertThat(object).as("Staged master file")
			.isNotNull()
			.isMaster()
			.isInheritedIn(channel)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalizedObject(File masterObject, File localizedObject, Folder folder) throws NodeException {
		assertThat(localizedObject).as("Staged localized file")
			.isNotNull()
			.isNotMaster()
			.hasChannel(channel)
			.hasMaster(masterObject)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalObject(File object, Node channel, Folder folder) throws NodeException {
		assertThat(object).as("Staged local file")
			.isNotNull()
			.hasChannel(channel)
			.isMaster()
			.hasParent(folder);
	}

	@Override
	protected void assertExcluded(File object) throws NodeException {
		assertThat(object).as("Staged file")
			.isNotNull()
			.isExcluded()
			.isDisinheritedIn()
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDisinherited(File object) throws NodeException {
		assertThat(object).as("Staged file")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn(channel)
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDefaultDisinherited(File object) throws NodeException {
		assertThat(object).as("Staged file")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn()
			.isDisinheritedByDefault();
	}
}
