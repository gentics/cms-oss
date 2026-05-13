package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
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
 * Tests for staging folders
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.WASTEBIN })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FolderStagingTest extends AbstractBaseContentStagingTest<Folder> {
	/**
	 * Create instance
	 */
	public FolderStagingTest() {
		super(Folder.class, "folder");
	}

	/**
	 * Test that template assignment is staged
	 * @throws NodeException
	 */
	@Test
	public void testTemplateAssignment() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Staged Folder");
			f.getTemplates().add(template);
		}).build();
		GlobalId folderGlobalId = execute(Folder::getGlobalId, folder);

		Template otherTemplate = execute(f -> create(Template.class, t -> {
			t.setMlId(1);
			t.setName("Template");
			t.setSource("");
			t.addFolder(f);
		}).build(), folder);

		// create package and add folder
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, "folder", folderGlobalId.toString(), false));

		// delete other template and folder
		consume(t -> t.delete(true), otherTemplate);
		consume(f -> f.delete(true), folder);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		folder = supply(t -> t.getObject(Folder.class, folderGlobalId));
		try (Trx trx = new Trx()) {
			assertThat(folder).as("Staged folder")
				.isNotNull()
				.hasTemplates(template);
			trx.success();
		}
	}

	@Override
	protected Folder createObject(Folder folder, int timestamp) throws NodeException {
		return create(Folder.class, f -> {
			f.setMotherId(folder.getId());
			f.setName("Staged Folder");
			f.setDescription("This is the staged folder");
			f.setNameI18n(new I18nMap().put("de", "Gestufter Ordner").put("en", "Staged Folder"));
			f.setDescriptionI18n(new I18nMap().put("de", "Das ist der gestufte Ordner").put("en", "This is the staged folder"));
			f.setPublishDir("/staged/");
			f.setPublishDirI18n(new I18nMap().put("de", "/gestuft/").put("en", "/staged/"));
		}).at(timestamp).build();
	}

	@Override
	protected Folder updateObject(Folder object, int timestamp) throws NodeException {
		return update(object, f -> {
			f.setName("Modified Folder @" + timestamp);
			f.setDescription("This is the modified folder");
			f.setNameI18n(new I18nMap().put("de", "Geänderter Ordner").put("en", "Modified Folder"));
			f.setDescriptionI18n(new I18nMap().put("de", "Das ist der geänderte Ordner").put("en", "This is the modified folder"));
			f.setPublishDir("/modified/");
			f.setPublishDirI18n(new I18nMap().put("de", "/geaendert/").put("en", "/modified/"));
		}).at(timestamp).build();
	}

	@Override
	protected Folder moveObject(Folder object, Folder targetFolder) throws NodeException {
		return update(object, o -> {
			OpResult result = o.move(targetFolder, 0);
			assertThat(result.isOK()).as("move succeeded").isTrue();
		}).build();
	}

	@Override
	protected Folder setReference(Folder object, Folder referenced) throws NodeException {
		return update(object, o -> {
			FolderURLPartType partType = getPartType(FolderURLPartType.class, o.getObjectTag("reference"), "folder");
			partType.setTargetFolder(referenced);
			partType.setNode(referenced.getNode());
		}).build();
	}

	@Override
	protected void assertStagedObject(Folder object, Folder parent) throws NodeException {
		assertThat(object).as("Imported folder")
			.isNotNull()
			.hasParent(parent)
			.hasFieldOrPropertyWithValue("name", "Staged Folder")
			.hasFieldOrPropertyWithValue("description", "This is the staged folder")
			.hasFieldOrPropertyWithValue("nameI18n", supply(() -> new I18nMap().put("de", "Gestufter Ordner").put("en", "Staged Folder")))
			.hasFieldOrPropertyWithValue("descriptionI18n", supply(() -> new I18nMap().put("de", "Das ist der gestufte Ordner").put("en", "This is the staged folder")))
			.hasFieldOrPropertyWithValue("publishDir", "/staged/")
			.hasFieldOrPropertyWithValue("publishDirI18n", supply(() -> new I18nMap().put("de", "/gestuft/").put("en", "/staged/")));
	}

	@Override
	protected Folder setForeignReference(Folder folder, Folder object) throws NodeException {
		return update(folder, f -> {
			FolderURLPartType partType = getPartType(FolderURLPartType.class, f.getObjectTag("reference"), "folder");
			partType.setTargetFolder(object);
			partType.setNode(object.getNode());
		}).build();
	}

	@Override
	protected void assertForeignReference(Folder folder, Folder object) throws NodeException {
		assertThat(folder).as("Folder with reference").isNotNull();
		assertThat(object).as("Referenced folder").isNotNull();
		assertThat(getPartType(FolderURLPartType.class, folder.getObjectTag("reference"), "folder")).as("Reference part")
				.isNotNull()
				.hasFieldOrPropertyWithValue("targetFolder", object)
				.hasFieldOrPropertyWithValue("node", object.getNode());
	}

	@Override
	protected void assertReference(Folder object, Folder referenced) throws NodeException {
		assertThat(object).as("Object with reference").isNotNull();
		assertThat(referenced).as("Referenced object").isNotNull();
		assertThat(getPartType(FolderURLPartType.class, object.getObjectTag("reference"), "folder")).as("Reference part")
			.isNotNull()
			.hasFieldOrPropertyWithValue("targetFolder", referenced)
			.hasFieldOrPropertyWithValue("node", referenced.getNode());
	}

	@Override
	protected StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new FolderResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(folder.getGlobalId().toString())
					.setStagingPackageName(packageName),
				new FolderListParameterBean(),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean()));
	}

	@Override
	protected StagingResponse<String> getLegacyStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new FolderResourceImpl().getFolders(folder.getGlobalId().toString(), null, false,
				new InFolderParameterBean()
					.setFolderId(folder.getGlobalId().toString())
					.setStagingPackageName(packageName),
				new FolderListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean()));
	}
}
