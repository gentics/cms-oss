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
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Multichannelling test for staging of images
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class ImageMultichannellingStagingTest extends AbstractMultichannellingContentStagingTest<ImageFile> {
	/**
	 * Create test instance
	 */
	public ImageMultichannellingStagingTest() {
		super(ImageFile.class, "image");
	}

	@Override
	protected ImageFile createMasterObject(Folder folder) throws NodeException {
		return create(ImageFile.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testimage.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		}).build();
	}

	@Override
	protected ImageFile localizeObject(ImageFile master) throws NodeException {
		return execute(m -> localize(m, channel), master);
	}

	@Override
	protected ImageFile createLocalObject(Folder folder) throws NodeException {
		return create(ImageFile.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testimage.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			f.setChannelInfo(channel.getId(), null);
		}).build();
	}

	@Override
	protected ImageFile createExcludedObject(Folder folder) throws NodeException {
		ImageFile object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(true, Collections.emptySet(), false)).build();
	}

	@Override
	protected ImageFile createDisinheritedObject(Folder folder) throws NodeException {
		ImageFile object = createMasterObject(folder);
		return update(object, f -> f.changeMultichannellingRestrictions(false, Collections.singleton(channel), false))
				.build();
	}

	@Override
	protected ImageFile createDefaultDisinherited(Folder folder) throws NodeException {
		ImageFile object = createMasterObject(folder);
		return update(object, f -> f.setDisinheritDefault(true, false)).build();
	}

	@Override
	protected void assertMasterObject(ImageFile object, Folder folder) throws NodeException {
		assertThat(object).as("Staged master image")
			.isNotNull()
			.isMaster()
			.isInheritedIn(channel)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalizedObject(ImageFile masterObject, ImageFile localizedObject, Folder folder) throws NodeException {
		assertThat(localizedObject).as("Staged localized image")
			.isNotNull()
			.isNotMaster()
			.hasChannel(channel)
			.hasMaster(masterObject)
			.hasParent(folder);
	}

	@Override
	protected void assertLocalObject(ImageFile object, Node channel, Folder folder) throws NodeException {
		assertThat(object).as("Staged local image")
			.isNotNull()
			.hasChannel(channel)
			.isMaster()
			.hasParent(folder);
	}

	@Override
	protected void assertExcluded(ImageFile object) throws NodeException {
		assertThat(object).as("Staged image")
			.isNotNull()
			.isExcluded()
			.isDisinheritedIn()
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDisinherited(ImageFile object) throws NodeException {
		assertThat(object).as("Staged image")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn(channel)
			.isNotDisinheritedByDefault();
	}

	@Override
	protected void assertDefaultDisinherited(ImageFile object) throws NodeException {
		assertThat(object).as("Staged image")
			.isNotNull()
			.isNotExcluded()
			.isDisinheritedIn()
			.isDisinheritedByDefault();
	}
}
