package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for staging images
 *
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.WASTEBIN })
@ConfigBy(FormTypeConfigurationModifier.class)
public class ImageStagingTest extends AbstractBaseContentStagingTest<ImageFile> {
	/**
	 * Create instance
	 */
	public ImageStagingTest() {
		super(ImageFile.class, "image");
	}

	@Override
	protected ImageFile createObject(Folder folder, int timestamp) throws NodeException {
		return Builder.create(ImageFile.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testimage.jpg");
			f.setDescription("This is the staged image");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
			f.setNiceUrl("/nice/staged/image.jpg");
			f.setAlternateUrls("/very/nice/image.jpg", "/extremely/nice/image.jpg");
			f.setForceOnline(true);
		}).at(timestamp).build();
	}

	@Override
	protected ImageFile updateObject(ImageFile object, int timestamp) throws NodeException {
		return Builder.update(object, f -> {
			f.setName("modified_testimage_" + timestamp + ".jpg");
			f.setDescription("This is the modified image");
			f.setFileStream(GenericTestUtils.getPictureResource("bild2.jpg"));
			f.setNiceUrl("/nice/modified/image.jpg");
			f.setAlternateUrls("/very/modified/image.jpg", "/extremely/modified/image.jpg");
			f.setForceOnline(false);
		}).at(timestamp).build();
	}

	@Override
	protected ImageFile moveObject(ImageFile object, Folder targetFolder) throws NodeException {
		return update(object, o -> {
			OpResult result = o.move(targetFolder, 0);
			assertThat(result.isOK()).as("move succeeded").isTrue();
		}).build();
	}

	@Override
	protected ImageFile setReference(ImageFile object, ImageFile referenced) throws NodeException {
		return update(object, o -> {
			ImageURLPartType partType = getPartType(ImageURLPartType.class, o.getObjectTag("reference"), "image");
			partType.setTargetImage(referenced);
			partType.setNode(referenced.getNode());
		}).build();
	}

	@Override
	protected void assertStagedObject(ImageFile object, Folder parent) throws NodeException {
		assertThat(object).as("Staged image")
			.isNotNull()
			.hasFieldOrPropertyWithValue("name", "testimage.jpg")
			.hasFieldOrPropertyWithValue("description", "This is the staged image")
			.hasFieldOrPropertyWithValue("niceUrl", "/nice/staged/image.jpg")
			.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/very/nice/image.jpg", "/extremely/nice/image.jpg")))
			.hasFieldOrPropertyWithValue("forceOnline", true)
			.hasParent(parent);
		ByteArrayOutputStream expected = new ByteArrayOutputStream();
		try (InputStream in = GenericTestUtils.getPictureResource("blume.jpg")) {
			IOUtils.copy(in, expected);
		} catch (IOException e) {
			throw new NodeException(e);
		}

		ByteArrayOutputStream is = new ByteArrayOutputStream();
		try (InputStream in = object.getFileStream()) {
			IOUtils.copy(in, is);
		} catch (IOException e) {
			throw new NodeException(e);
		}

		assertThat(is.toByteArray()).as("Binary data").isEqualTo(expected.toByteArray());
	}

	@Override
	protected Folder setForeignReference(Folder folder, ImageFile object) throws NodeException {
		return Builder.update(folder, f -> {
			ImageURLPartType partType = getPartType(ImageURLPartType.class, f.getObjectTag("reference"), "image");
			partType.setTargetImage(object);
			partType.setNode(object.getNode());
		}).build();
	}

	@Override
	protected void assertForeignReference(Folder folder, ImageFile object) throws NodeException {
		assertThat(folder).as("Folder with reference").isNotNull();
		assertThat(object).as("Referenced image").isNotNull();
		assertThat(getPartType(ImageURLPartType.class, folder.getObjectTag("reference"), "image")).as("Reference part")
				.isNotNull()
				.hasFieldOrPropertyWithValue("targetImage", object)
				.hasFieldOrPropertyWithValue("node", object.getNode());
	}

	@Override
	protected void assertReference(ImageFile object, ImageFile referenced) throws NodeException {
		assertThat(object).as("Object with reference").isNotNull();
		assertThat(referenced).as("Referenced object").isNotNull();
		assertThat(getPartType(ImageURLPartType.class, object.getObjectTag("reference"), "image")).as("Reference part")
			.isNotNull()
			.hasFieldOrPropertyWithValue("targetImage", referenced)
			.hasFieldOrPropertyWithValue("node", referenced.getNode());
	}

	@Override
	protected StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new ImageResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(folder.getGlobalId().toString())
					.setStagingPackageName(packageName),
				new FileListParameterBean(),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean()));
	}

	@Override
	protected StagingResponse<String> getLegacyStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new FolderResourceImpl().getImages(folder.getGlobalId().toString(),
				new InFolderParameterBean()
					.setFolderId(folder.getGlobalId().toString())
					.setStagingPackageName(packageName),
				new FileListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean()));
	}
}
