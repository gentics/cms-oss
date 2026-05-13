package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.DatasourcePartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.rest.model.response.PageListResponse;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for staging pages
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.WASTEBIN })
@ConfigBy(FormTypeConfigurationModifier.class)
public class PageStagingTest extends AbstractVersionedContentStagingTest<Page> {
	/**
	 * Create instance
	 */
	public PageStagingTest() {
		super(Page.class, "page");
	}

	@Override
	protected Page createObject(Folder folder, int timestamp) throws NodeException {
		return create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Staged page");
			p.setFilename("staged_page.html");
			p.setDescription("This is the staged page");
			p.setNiceUrl("/nice/staged/page.html");
			p.setAlternateUrls("/very/nice/page.html", "/extremely/nice/page.html");
			NormalTextPartType partType = getPartType(NormalTextPartType.class, p.getObjectTag("reference"), "normaltext");
			partType.setText("Random text " + System.currentTimeMillis());
		}).at(timestamp).unlock().build();
	}

	@Override
	protected Page updateObject(Page object, int timestamp) throws NodeException {
		return update(object, p -> {
			p.setName("Modified page @" + timestamp);
			p.setFilename("modified_page.html");
			p.setDescription("This is the modified page");
			p.setNiceUrl("/nice/modified/page.html");
			p.setAlternateUrls("/very/modified/page.html", "/extremely/modified/page.html");
		}).at(timestamp).unlock().build();
	}

	@Override
	protected Page moveObject(Page object, Folder targetFolder) throws NodeException {
		return update(object, o -> {
			OpResult result = o.move(targetFolder, 0, true);
			assertThat(result.isOK()).as("move succeeded").isTrue();
		}).unlock().build();
	}

	@Override
	protected Page setReference(Page object, Page referenced) throws NodeException {
		return update(object, o -> {
			PageURLPartType partType = getPartType(PageURLPartType.class, o.getObjectTag("reference"), "page");
			partType.setTargetPage(referenced);
			partType.setNode(referenced.getNode());
		}).unlock().build();
	}

	@Override
	protected void assertStagedObject(Page object, Folder parent) throws NodeException {
		assertThat(object).as("Staged page")
			.isNotNull()
			.hasFieldOrPropertyWithValue("name", "Staged page")
			.hasFieldOrPropertyWithValue("filename", "staged_page.html")
			.hasFieldOrPropertyWithValue("description", "This is the staged page")
			.hasFieldOrPropertyWithValue("niceUrl", "/nice/staged/page.html")
			.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/very/nice/page.html", "/extremely/nice/page.html")))
			.hasParent(parent);
	}

	@Override
	protected Folder setForeignReference(Folder folder, Page object) throws NodeException {
		return update(folder, f -> {
			PageURLPartType partType = getPartType(PageURLPartType.class, f.getObjectTag("reference"), "page");
			partType.setTargetPage(object);
			partType.setNode(object.getNode());
		}).build();
	}

	@Override
	protected void assertForeignReference(Folder folder, Page object) throws NodeException {
		assertThat(folder).as("Folder with reference").isNotNull();
		assertThat(object).as("Referenced page").isNotNull();
		assertThat(getPartType(PageURLPartType.class, folder.getObjectTag("reference"), "page")).as("Reference part")
				.isNotNull()
				.hasFieldOrPropertyWithValue("targetPage", object)
				.hasFieldOrPropertyWithValue("node", object.getNode());
	}

	@Override
	protected void assertReference(Page object, Page referenced) throws NodeException {
		assertThat(object).as("Object with reference").isNotNull();
		assertThat(referenced).as("Referenced object").isNotNull();
		assertThat(getPartType(PageURLPartType.class, object.getObjectTag("reference"), "page")).as("Reference part")
			.isNotNull()
			.hasFieldOrPropertyWithValue("targetPage", referenced)
			.hasFieldOrPropertyWithValue("node", referenced.getNode());
	}

	@Override
	protected StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean()
					.setStagingPackageName(packageName)
					.setFolderId(folder.getGlobalId().toString()),
				new PageListParameterBean(),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean()));
	}

	@Override
	protected StagingResponse<String> getLegacyStagingResponse(String packageName, Folder folder) throws NodeException {
		return supply(() -> new FolderResourceImpl().getPages(folder.getGlobalId().toString(),
				new InFolderParameterBean()
					.setStagingPackageName(packageName)
					.setFolderId(folder.getGlobalId().toString()),
				new PageListParameterBean(),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean()));
	}

	/**
	 * Test importing two pages, which are language variants of each other
	 * @throws NodeException
	 */
	@Test
	public void testImportLanguageVariants() throws NodeException {
		Page german = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("German Page");
			p.setLanguage(getLanguage("de"));
		}).unlock().build();
		String germanGlobalId = execute(p -> p.getGlobalId().toString(), german);

		int contentsetId = execute(Page::getContentsetId, german);

		Page english = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("English Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("en"));
		}).unlock().build();
		String englishGlobalId = execute(p -> p.getGlobalId().toString(), english);

		// assert that pages are language variants of each other
		try (Trx trx = new Trx()) {
			assertThat(german).as("German page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("English page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			trx.success();
		}

		// create package and add pages
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanGlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishGlobalId, false));

		// delete pages in CMS
		consume(p -> p.delete(true), german);
		consume(p -> p.delete(true), english);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		german = supply(t -> t.getObject(clazz, germanGlobalId));
		english = supply(t -> t.getObject(clazz, englishGlobalId));

		try (Trx trx = new Trx()) {
			assertThat(german).as("Staged german page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("Staged english page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			trx.success();
		}
	}

	/**
	 * Test importing a language variant of an existing page
	 * @throws NodeException
	 */
	@Test
	public void testImportLanguageVariantOfExistingPage() throws NodeException {
		Page german = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("German Page");
			p.setLanguage(getLanguage("de"));
		}).unlock().build();
		String germanGlobalId = execute(p -> p.getGlobalId().toString(), german);

		int contentsetId = execute(Page::getContentsetId, german);

		Page english = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("English Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("en"));
		}).unlock().build();

		// assert that pages are language variants of each other
		try (Trx trx = new Trx()) {
			assertThat(german).as("German page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("English page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			trx.success();
		}

		// create package and add german page
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanGlobalId, false));

		// delete german page in CMS
		consume(p -> p.delete(true), german);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		german = supply(t -> t.getObject(clazz, germanGlobalId));
		english = execute(Page::reload, english);

		try (Trx trx = new Trx()) {
			assertThat(german).as("Staged german page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("English page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			trx.success();
		}
	}

	/**
	 * Test importing a language variant of an existing page, which has a conflicting language variant
	 * @throws NodeException
	 */
	@Test
	public void testImportLanguageVariantConflict() throws NodeException {
		Page german = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("German Page");
			p.setLanguage(getLanguage("de"));
		}).unlock().build();
		String germanGlobalId = execute(p -> p.getGlobalId().toString(), german);

		int contentsetId = execute(Page::getContentsetId, german);

		Page english = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("English Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("en"));
		}).unlock().build();

		// assert that pages are language variants of each other
		try (Trx trx = new Trx()) {
			assertThat(german).as("German page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("English page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			trx.success();
		}

		// create package and add german page
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanGlobalId, false));

		// delete german page in CMS
		consume(p -> p.delete(true), german);

		// create conflicting german page
		Page conflictingPage = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Conflicting german Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("de"));
		}).unlock().build();

		// import the package
		importContentPackage(packageName);

		// reload and assert
		german = supply(t -> t.getObject(clazz, germanGlobalId));
		english = execute(Page::reload, english);
		conflictingPage = execute(Page::reload, conflictingPage);

		try (Trx trx = new Trx()) {
			assertThat(german).as("Staged german page")
				.isNotNull()
				.hasLanguage("de")
				.hasLanguageVariant("en", english);
			assertThat(english).as("English page")
				.isNotNull()
				.hasLanguage("en")
				.hasLanguageVariant("de", german);

			assertThat(conflictingPage).as("Conflicting language variant").isNull();

			trx.success();
		}
	}

	/**
	 * Test that when importing a page, it will have the same global content ID
	 * @throws NodeException
	 */
	@Test
	public void testImportContentId() throws NodeException {
		Page page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
		}).unlock().build();
		GlobalId globalContentId = execute(p -> p.getContent().getGlobalId(), page);
		GlobalId globalPageId = execute(p -> p.getGlobalId(), page);

		// create package and add page
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, globalPageId.toString(), false));

		// delete page in CMS
		consume(p -> p.delete(true), page);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		page = supply(t -> t.getObject(Page.class, globalPageId));
		try (Trx trx = new Trx()) {
			assertThat(page).as("Staged page")
				.isNotNull()
				.has(globalPageId);
			assertThat(page.getContent()).as("Page content")
				.isNotNull()
				.has(globalContentId);
			trx.success();
		}
	}

	/**
	 * Test importing pages, which are page variants of each other
	 * @throws NodeException
	 */
	@Test
	public void testImportPageVariants() throws NodeException {
		Page variant1 = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Variant 1");
		}).unlock().build();
		int contentId = execute(p -> p.getContent().getId(), variant1);
		String variant1GlobalId = execute(p -> p.getGlobalId().toString(), variant1);

		Page variant2 = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Variant 2");
			p.setContentId(contentId);
		}).unlock().build();
		String variant2GlobalId = execute(p -> p.getGlobalId().toString(), variant2);

		// assert that pages are page variants of each other
		try (Trx trx = new Trx()) {
			assertThat(variant1).as("Variant 1")
				.isNotNull()
				.hasPageVariant(variant2);
			assertThat(variant2).as("Variant 2")
				.isNotNull()
				.hasPageVariant(variant1);
			trx.success();
		}

		// create package and add pages
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, variant1GlobalId, false));
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, variant2GlobalId, false));

		// delete pages in CMS
		consume(p -> p.delete(true), variant1);
		consume(p -> p.delete(true), variant2);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		variant1 = supply(t -> t.getObject(clazz, variant1GlobalId));
		variant2 = supply(t -> t.getObject(clazz, variant2GlobalId));

		// assert that pages are page variants of each other
		try (Trx trx = new Trx()) {
			assertThat(variant1).as("Variant 1")
				.isNotNull()
				.hasPageVariant(variant2);
			assertThat(variant2).as("Variant 2")
				.isNotNull()
				.hasPageVariant(variant1);
			trx.success();
		}
	}

	/**
	 * Test importing a page, which is a page variant of an existing page
	 * @throws NodeException
	 */
	@Test
	public void testImportPageVariantOfExistingPage() throws NodeException {
		Page variant1 = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Variant 1");
		}).unlock().build();
		int contentId = execute(p -> p.getContent().getId(), variant1);
		String variant1GlobalId = execute(p -> p.getGlobalId().toString(), variant1);

		Page variant2 = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Variant 2");
			p.setContentId(contentId);
		}).unlock().build();
		String variant2GlobalId = execute(p -> p.getGlobalId().toString(), variant2);

		// assert that pages are page variants of each other
		try (Trx trx = new Trx()) {
			assertThat(variant1).as("Variant 1")
				.isNotNull()
				.hasPageVariant(variant2);
			assertThat(variant2).as("Variant 2")
				.isNotNull()
				.hasPageVariant(variant1);
			trx.success();
		}

		// create package and add variant1
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, variant1GlobalId, false));

		// delete variant1 in CMS
		consume(p -> p.delete(true), variant1);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		variant1 = supply(t -> t.getObject(clazz, variant1GlobalId));
		variant2 = supply(t -> t.getObject(clazz, variant2GlobalId));

		// assert that pages are page variants of each other
		try (Trx trx = new Trx()) {
			assertThat(variant1).as("Variant 1")
				.isNotNull()
				.hasPageVariant(variant2);
			assertThat(variant2).as("Variant 2")
				.isNotNull()
				.hasPageVariant(variant1);
			trx.success();
		}
	}

	/**
	 * Test importing a single language variant of a page in multiple languages, which has been moved to another folder
	 * @throws NodeException
	 */
	@Test
	public void testImportMovedLanguage() throws NodeException {
		Folder sourceFolder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Source Folder");
			f.setPublishDir("/source");
		}).build();
		Integer sourceFolderId = execute(NodeObject::getId, sourceFolder);

		Folder targetFolder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Target Folder");
			f.setPublishDir("/target");
		}).build();

		Page german = create(Page.class, p -> {
			p.setFolderId(sourceFolderId);
			p.setTemplateId(template.getId());
			p.setName("German Page");
			p.setLanguage(getLanguage("de"));
		}).unlock().build();
		GlobalId germanGlobalId = execute(NodeObject::getGlobalId, german);

		int contentsetId = execute(Page::getContentsetId, german);

		Page english = create(Page.class, p -> {
			p.setFolderId(sourceFolderId);
			p.setTemplateId(template.getId());
			p.setName("English Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("en"));
		}).unlock().build();

		// create package and add german page
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanGlobalId.toString(), false));

		// move all language variants to target folder
		moveObject(german, targetFolder);

		// reload and assert
		german = execute(NodeObject::reload, german);
		english = execute(NodeObject::reload, english);

		try (Trx trx = new Trx()) {
			assertThat(german).as("Moved german page").hasParent(targetFolder);
			assertThat(english).as("Moved english page").hasParent(targetFolder);
			trx.success();
		}

		// import the package
		importContentPackage(packageName);

		// reload and assert
		german = execute(NodeObject::reload, german);
		english = execute(NodeObject::reload, english);

		try (Trx trx = new Trx()) {
			assertThat(german).as("Moved german page").hasParent(sourceFolder);
			assertThat(english).as("Moved english page").hasParent(sourceFolder);
			trx.success();
		}
	}

	/**
	 * Test importing a page containing a datasource part type
	 * @throws NodeException
	 */
	@Test
	public void testDatasourcePartType() throws NodeException {
		int constructId = supply(() -> createConstruct(node, DatasourcePartType.class, "construct", "part"));
		AtomicReference<String> tagName = new AtomicReference<>();

		Page stagedPage = create(Page.class, p -> {
			p.setFolderId(nodeRootFolder.getId());
			p.setTemplateId(template.getId());
			p.setName("Staged Page");
			ContentTag tag = p.getContent().addContentTag(constructId);
			List<DatasourceEntry> entries = getPartType(DatasourcePartType.class, tag, "part").getItems();
			entries.add(create(DatasourceEntry.class, e -> {
				e.setKey("one");
				e.setValue("Value One");
			}).doNotSave().build());
			entries.add(create(DatasourceEntry.class, e -> {
				e.setKey("two");
				e.setValue("Value Two");
			}).doNotSave().build());
			entries.add(create(DatasourceEntry.class, e -> {
				e.setKey("three");
				e.setValue("Value Three");
			}).doNotSave().build());

			tagName.set(tag.getName());
		}).unlock().build();
		GlobalId pageGlobalId = execute(Page::getGlobalId, stagedPage);

		// create package and add page
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, pageGlobalId.toString(), false));

		// delete page
		consume(p -> p.delete(true), stagedPage);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		stagedPage = supply(t -> t.getObject(Page.class, pageGlobalId));
		try (Trx trx = new Trx()) {
			assertThat(stagedPage).as("Staged Page").isNotNull();
			assertThat(stagedPage.getContentTag(tagName.get())).as("Staged tag").isNotNull();
			assertThat(getPartType(DatasourcePartType.class, stagedPage.getContentTag(tagName.get()), "part").getKeys())
					.as("Staged entry keys").containsExactly("one", "two", "three");
			assertThat(getPartType(DatasourcePartType.class, stagedPage.getContentTag(tagName.get()), "part").getValues())
					.as("Staged entry values").containsExactly("Value One", "Value Two", "Value Three");
			trx.success();
		}
	}

	/**
	 * Test the staging information for language variants
	 * @throws NodeException
	 */
	@Test
	public void testLanguageVariantsStagingInfo() throws NodeException {
		Page german = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("German Page");
			p.setLanguage(getLanguage("de"));
		}).build();
		String germanGlobalId = execute(p -> p.getGlobalId().toString(), german);

		int contentsetId = execute(Page::getContentsetId, german);

		Page english = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("English Page");
			p.setContentsetId(contentsetId);
			p.setLanguage(getLanguage("en"));
		}).build();
		String englishGlobalId = execute(p -> p.getGlobalId().toString(), english);

		String globalFolderId = supply(() -> node.getFolder().getGlobalId().toString());

		String packageName = "TestPackage";
		createContentPackage(packageName);

		PageListResponse response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(globalFolderId)
					.setStagingPackageName(packageName),
				new PageListParameterBean()
					.setLanguageVariants(true)
					.setLangFallback(true)
					.setLanguage("de"),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean()));
		assertResponseOK(response);

		assertThat(response.getStagingStatus()).as("Staging status")
			.containsEntry(germanGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false))
			.containsEntry(englishGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false));

		// add english
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, englishGlobalId, false));

		response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(globalFolderId)
					.setStagingPackageName(packageName),
				new PageListParameterBean()
					.setLanguageVariants(true)
					.setLangFallback(true)
					.setLanguage("de"),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean()));
		assertResponseOK(response);

		assertThat(response.getStagingStatus()).as("Staging status")
			.containsEntry(germanGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(false))
			.containsEntry(englishGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true));

		// add german
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, type, germanGlobalId, false));

		response = supply(() -> new PageResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(globalFolderId)
					.setStagingPackageName(packageName),
				new PageListParameterBean()
					.setLanguageVariants(true)
					.setLangFallback(true)
					.setLanguage("de"),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new PublishableParameterBean(),
				new WastebinParameterBean()));
		assertResponseOK(response);

		assertThat(response.getStagingStatus()).as("Staging status")
			.containsEntry(germanGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true))
			.containsEntry(englishGlobalId, new StagingStatus().setPackageName(packageName).setIncluded(true));
	}
}
