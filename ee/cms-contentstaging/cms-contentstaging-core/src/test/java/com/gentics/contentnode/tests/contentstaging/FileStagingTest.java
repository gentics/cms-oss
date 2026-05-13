package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
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
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for staging files
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.WASTEBIN })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FileStagingTest extends AbstractBaseContentStagingTest<File> {
	/**
	 * Create instance
	 */
	public FileStagingTest() {
		super(File.class, "file");
	}

	@Override
	protected File createObject(Folder folder, int timestamp) throws NodeException {
		return create(File.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("testfile.txt");
			f.setDescription("This is the staged file");
			f.setFileStream(new ByteArrayInputStream("contents".getBytes()));
			f.setNiceUrl("/nice/staged/file.txt");
			f.setAlternateUrls("/very/nice/file.txt", "/extremely/nice/file.txt");
			f.setForceOnline(true);
		}).at(timestamp).build();
	}

	@Override
	protected File updateObject(File object, int timestamp) throws NodeException {
		return update(object, f -> {
			f.setName("modified_testfile_" + timestamp + ".txt");
			f.setDescription("This is the modified file");
			f.setFileStream(new ByteArrayInputStream("modified contents".getBytes()));
			f.setNiceUrl("/nice/modified/file.txt");
			f.setAlternateUrls("/very/modified/file.txt", "/extremely/modified/file.txt");
			f.setForceOnline(false);
		}).at(timestamp).build();
	}

	@Override
	protected File moveObject(File object, Folder targetFolder) throws NodeException {
		return update(object, o -> {
			OpResult result = o.move(targetFolder, 0);
			assertThat(result.isOK()).as("move succeeded").isTrue();
		}).build();
	}

	@Override
	protected File setReference(File object, File referenced) throws NodeException {
		return update(object, o -> {
			FileURLPartType partType = getPartType(FileURLPartType.class, o.getObjectTag("reference"), "file");
			partType.setTargetFile(referenced);
			partType.setNode(referenced.getNode());
		}).build();
	}

	@Override
	protected void assertStagedObject(File object, Folder parent) throws NodeException {
		assertThat(object).as("Staged file")
			.isNotNull()
			.hasFieldOrPropertyWithValue("name", "testfile.txt")
			.hasFieldOrPropertyWithValue("description", "This is the staged file")
			.hasFieldOrPropertyWithValue("niceUrl", "/nice/staged/file.txt")
			.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/very/nice/file.txt", "/extremely/nice/file.txt")))
			.hasFieldOrPropertyWithValue("forceOnline", true)
			.hasParent(parent);
		try (InputStream in = object.getFileStream()) {
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			assertThat(writer.toString()).as("Staged file content").isEqualTo("contents");
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	protected Folder setForeignReference(Folder folder, File object) throws NodeException {
		return update(folder, f -> {
			FileURLPartType partType = getPartType(FileURLPartType.class, f.getObjectTag("reference"), "file");
			partType.setTargetFile(object);
			partType.setNode(object.getNode());
		}).build();
	}

	@Override
	protected void assertForeignReference(Folder folder, File object) throws NodeException {
		assertThat(folder).as("Folder with reference").isNotNull();
		assertThat(object).as("Referenced file").isNotNull();
		assertThat(getPartType(FileURLPartType.class, folder.getObjectTag("reference"), "file")).as("Reference part")
				.isNotNull()
				.hasFieldOrPropertyWithValue("targetFile", object)
				.hasFieldOrPropertyWithValue("node", object.getNode());
	}

	@Override
	protected void assertReference(File object, File referenced) throws NodeException {
		assertThat(object).as("Object with reference").isNotNull();
		assertThat(referenced).as("Referenced object").isNotNull();
		assertThat(getPartType(FileURLPartType.class, object.getObjectTag("reference"), "file")).as("Reference part")
			.isNotNull()
			.hasFieldOrPropertyWithValue("targetFile", referenced)
			.hasFieldOrPropertyWithValue("node", referenced.getNode());
	}

	@Override
	protected StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new FileResourceImpl().list(
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
		return supply(() -> new FolderResourceImpl().getFiles(folder.getGlobalId().toString(),
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
