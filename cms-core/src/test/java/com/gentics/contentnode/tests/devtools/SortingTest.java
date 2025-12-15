package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.getStructureFile;
import static com.gentics.contentnode.tests.devtools.DevToolTestUtils.jsonToFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.factory.object.OverviewPartSetting.ObjectType;
import com.gentics.contentnode.factory.object.OverviewPartSetting.SelectionType;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for sorting of list entries
 */
@GCNFeature(set = { Feature.DEVTOOLS })
public class SortingTest {
	/**
	 * Name of the testpackage
	 */
	public final static String PACKAGE_NAME = "testpackage";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Integer constructId;

	private static Construct overviewConstruct;

	@Rule
	public PackageSynchronizerContext syncContext = new PackageSynchronizerContext();

	private MainPackageSynchronizer pack;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		// delete all existing object property categories
		operate(t -> {
			for (ObjectTagDefinitionCategory category : t.getObjects(ObjectTagDefinitionCategory.class, DBUtils.select("SELECT id FROM objprop_category", DBUtils.IDS))) {
				category.delete(true);
			}
		});

		node = supply(() -> createNode());
		constructId = supply(() -> createConstruct(node, HTMLPartType.class, "html", "html"));
		supply(() -> createObjectTagDefinition("object.tag1", Template.TYPE_TEMPLATE, constructId, node));
		supply(() -> createObjectTagDefinition("object.tag2", Template.TYPE_TEMPLATE, constructId, node));
		supply(() -> createObjectTagDefinition("object.tag3", Template.TYPE_TEMPLATE, constructId, node));

		overviewConstruct = supply(t -> update(t.getObject(Construct.class, createConstruct(node, OverviewPartType.class, "overview", "ds")), upd -> {
			Part part = upd.getParts().get(0);
			OverviewPartSetting setting = new OverviewPartSetting(part);

			// restricted object types
			setting.getRestrictedObjectTypes().add(ObjectType.page);
			setting.getRestrictedObjectTypes().add(ObjectType.file);
			setting.getRestrictedObjectTypes().add(ObjectType.folder);

			// restricted selection types
			setting.getRestrictedSelectionTypes().add(SelectionType.single);
			setting.getRestrictedSelectionTypes().add(SelectionType.parent);

			setting.setTo(part);
		}));
	}

	@Before
	public void setup() throws NodeException {
		Synchronizer.addPackage(PACKAGE_NAME);

		pack = Synchronizer.getPackage(PACKAGE_NAME);
		assertThat(pack).as("package synchronizer").isNotNull();
	}

	@After
	public void teardown() throws NodeException {
		Synchronizer.removePackage(PACKAGE_NAME);
	}

	@Test
	public void testTemplateTagSorting() throws NodeException, JsonProcessingException, IOException {
		// create template with three tags
		Template template = supply(() -> create(Template.class, create -> {
			create.setName("Template");
			create.setSource("");
			create.addFolder(node.getFolder());

			for (String name : Arrays.asList("html1", "html2", "html3")) {
				create.getTags().put(name, create(TemplateTag.class, tTag -> {
					tTag.setConstructId(constructId);
					tTag.setEnabled(true);
					tTag.setName(name);
				}, false));
			}
		}));

		// synchronize to package
		consume(t -> pack.synchronize(t, true), template);

		// change sorting of tags in FS
		File templateStructureFile = getStructureFile(pack.getPackagePath().toFile(), template);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonFile = mapper.readTree(templateStructureFile);

		if (jsonFile.get("templateTags").isArray()) {
			reverse((ArrayNode) jsonFile.get("templateTags"));
		}
		jsonToFile(jsonFile, templateStructureFile);
		String jsonFileContents = FileUtils.readFileToString(templateStructureFile, "UTF-8");

		// synchronize again
		consume(t -> pack.synchronize(t, true), template);

		// check that sorting in FS was not changed
		assertThat(FileUtils.readFileToString(templateStructureFile, "UTF-8")).as("Structure file after sync").isEqualTo(jsonFileContents);
	}

	@Test
	public void testTemplateObjectTagSorting() throws NodeException, JsonProcessingException, IOException {
		// create template with three object tags
		Template template = supply(() -> create(Template.class, create -> {
			create.setName("Template");
			create.setSource("");
			create.addFolder(node.getFolder());
		}));

		// synchronize to package
		consume(t -> pack.synchronize(t, true), template);

		// change sorting of tags in FS
		File templateStructureFile = getStructureFile(pack.getPackagePath().toFile(), template);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonFile = mapper.readTree(templateStructureFile);

		if (jsonFile.get("objectTags").isArray()) {
			reverse((ArrayNode) jsonFile.get("objectTags"));
		}
		jsonToFile(jsonFile, templateStructureFile);
		String jsonFileContents = FileUtils.readFileToString(templateStructureFile, "UTF-8");

		// synchronize again
		consume(t -> pack.synchronize(t, true), template);

		// check that sorting in FS was not changed
		assertThat(FileUtils.readFileToString(templateStructureFile, "UTF-8")).as("Structure file after sync").isEqualTo(jsonFileContents);
	}

	@Test
	public void testOverviewConstruct() throws NodeException, JsonProcessingException, IOException {
		// sync construct to package
		consume(c -> pack.synchronize(c, true), overviewConstruct);

		// change sortings in FS
		File constructStructureFile = getStructureFile(pack.getPackagePath().toFile(), overviewConstruct);
		File partFile = new File(constructStructureFile.getParentFile(), "part.ds.ds.json");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonFile = mapper.readTree(partFile);

		reverse((ArrayNode)jsonFile.get("restrictedObjectTypes"));
		reverse((ArrayNode)jsonFile.get("restrictedSelectionTypes"));
		jsonToFile(jsonFile, partFile);
		String jsonFileContents = FileUtils.readFileToString(partFile, "UTF-8");

		// synchronize again
		consume(c -> pack.synchronize(c, true), overviewConstruct);

		// check that sorting in FS was not changed
		assertThat(FileUtils.readFileToString(partFile, "UTF-8")).as("Part file after sync").isEqualTo(jsonFileContents);
	}

	@Test
	public void testCrFragment() throws NodeException, JsonProcessingException, IOException {
		// create CR Fragment with three entries
		CrFragment crFragment = supply(() -> create(CrFragment.class, fr -> {
			fr.setName("Test Fragment");
			for (String text : Arrays.asList("one", "two", "three")) {
				fr.getEntries().add(create(CrFragmentEntry.class, entry -> {
					entry.setObjType(Page.TYPE_PAGE);
					entry.setAttributeTypeId(AttributeType.text.getType());
					entry.setMapname(text);
					entry.setTagname(text);
				}, false));
			}
		}));

		// synchronize to package
		consume(f -> pack.synchronize(f, true), crFragment);

		// change sorting of entries in FS
		File fragmentStructureFile = getStructureFile(pack.getPackagePath().toFile(), crFragment);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonFile = mapper.readTree(fragmentStructureFile);

		if (jsonFile.get("entries").isArray()) {
			reverse((ArrayNode) jsonFile.get("entries"));
		}
		jsonToFile(jsonFile, fragmentStructureFile);
		String jsonFileContents = FileUtils.readFileToString(fragmentStructureFile, "UTF-8");

		// synchronize again
		consume(f -> pack.synchronize(f, true), crFragment);

		// check that sorting in FS was not changed
		assertThat(FileUtils.readFileToString(fragmentStructureFile, "UTF-8")).as("Structure file after sync").isEqualTo(jsonFileContents);
	}

	@Test
	public void testContentRepository() throws NodeException, JsonProcessingException, IOException {
		// create CR with three entries
		ContentRepository contentRepository = supply(() -> create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setName("Mesh CR");
			for (String text : Arrays.asList("one", "two", "three")) {
				cr.getEntries().add(create(TagmapEntry.class, entry -> {
					entry.setObject(Page.TYPE_PAGE);
					entry.setAttributeTypeId(AttributeType.text.getType());
					entry.setMapname(text);
					entry.setTagname(text);
				}, false));
			}
		}));

		// synchronize to package
		consume(c -> pack.synchronize(c, true), contentRepository);

		// change sorting of entries in FS
		File crStructureFile = getStructureFile(pack.getPackagePath().toFile(), contentRepository);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonFile = mapper.readTree(crStructureFile);

		if (jsonFile.get("entries").isArray()) {
			reverse((ArrayNode) jsonFile.get("entries"));
		}
		jsonToFile(jsonFile, crStructureFile);
		String jsonFileContents = FileUtils.readFileToString(crStructureFile, "UTF-8");

		// synchronize again
		consume(c -> pack.synchronize(c, true), contentRepository);

		// check that sorting in FS was not changed
		assertThat(FileUtils.readFileToString(crStructureFile, "UTF-8")).as("Structure file after sync").isEqualTo(jsonFileContents);
	}

	/**
	 * Test the sort orders of construct categories when importing from a devtool package
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testConstructCategory() throws NodeException {
		// create some constructs with categories
		Construct firstInPackage = createConstructAndCategory("first_in_package", "First in Package", 1);
		Construct secondInPackage = createConstructAndCategory("second_in_package", "Second in Package", 2);
		Construct thirdInPackage = createConstructAndCategory("third_in_package", "Third in Package", 3);
		Construct fourthInPackage = createConstructAndCategory("fourth_in_package", "Fourth in Package", 4);

		// put constructs (with categories) into the package
		consume(c -> pack.synchronize(c, true), firstInPackage);
		consume(c -> pack.synchronize(c, true), secondInPackage);
		consume(c -> pack.synchronize(c, true), thirdInPackage);
		consume(c -> pack.synchronize(c, true), fourthInPackage);

		String thirdCatInPackageGlobalId = execute(cons -> cons.getConstructCategory().getGlobalId().toString(), thirdInPackage);

		// delete the constructs and categories
		Synchronizer.disable();
		deleteCategoryAndConstruct(firstInPackage);
		deleteCategoryAndConstruct(secondInPackage);
		deleteCategoryAndConstruct(thirdInPackage);
		deleteCategoryAndConstruct(fourthInPackage);

		// create new constructs with categories, with same sort orders as the first set
		createConstructAndCategory("first_other", "First other", 1);
		createConstructAndCategory("second_other", "Second other", 2);
		createConstructAndCategory("third_other", "Third other", 3);
		createConstructAndCategory("fourth_other", "Fourth other", 4);

		// synchronize from the package
		operate(() -> pack.syncAllFromFilesystem(Construct.class));

		// all sort orders should be unique and continuous. imported categories sorted after the existing ones
		assertThat(getNamesAndSortOrdersOfConstructCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Fourth in Package", 4),
				Pair.of("First other", 5),
				Pair.of("Second other", 6),
				Pair.of("Third other", 7),
				Pair.of("Fourth other", 8));

		// delete one imported category
		operate(t -> t.getObject(ConstructCategory.class, thirdCatInPackageGlobalId).delete(true));

		// sort orders of remaining categories did not change
		assertThat(getNamesAndSortOrdersOfConstructCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Fourth in Package", 3),
				Pair.of("First other", 4),
				Pair.of("Second other", 5),
				Pair.of("Third other", 6),
				Pair.of("Fourth other", 7));

		// import from the package
		operate(() -> pack.syncAllFromFilesystem(Construct.class));

		// the re-imported category should be sorted to the same position
		assertThat(getNamesAndSortOrdersOfConstructCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Fourth in Package", 4),
				Pair.of("First other", 5),
				Pair.of("Second other", 6),
				Pair.of("Third other", 7),
				Pair.of("Fourth other", 8));

		// change sorting in CMS
		List<String> ids = supply(() -> {
			return new ConstructResourceImpl()
					.listCategories(new SortParameterBean().setSort("sortorder"), null, null, null, null).getItems().stream()
					.map(com.gentics.contentnode.rest.model.ConstructCategory::getId).map(id -> Integer.toString(id)).collect(Collectors.toList());
		});

		// Initial order is
		// 0 First in Package
		// 1 Second in Package
		// 2 Third in Package
		// 3 Fourth in Package
		// 4 First other
		// 5 Second other
		// 6 Third other
		// 7 Fourth other

		// Put "Second other" into 2nd place
		String id = ids.remove(5);
		ids.add(1, id);

		// Order is now
		// 0 First in Package
		// 1 Second other
		// 2 Second in Package
		// 3 Third in Package
		// 4 Fourth in Package
		// 5 First other
		// 6 Third other
		// 7 Fourth other

		// Put "Third other" into 1st place
		id = ids.remove(6);
		ids.add(0, id);

		// Order is now
		// 0 Third other
		// 1 First in Package
		// 2 Second other
		// 3 Second in Package
		// 4 Third in Package
		// 5 Fourth in Package
		// 6 First other
		// 7 Fourth other

		// Put "Third in Package" into 3rd place
		id = ids.remove(4);
		ids.add(2, id);

		// Order is now
		// 0 Third other
		// 1 First in Package
		// 2 Third in Package
		// 3 Second other
		// 4 Second in Package
		// 5 Fourth in Package
		// 6 First other
		// 7 Fourth other

		consume(newlist -> {
			IdSetRequest request = new IdSetRequest();
			request.setIds(newlist);
			new ConstructResourceImpl().sortCategories(request);
		}, ids);

		assertThat(getNamesAndSortOrdersOfConstructCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("Third other", 1),
				Pair.of("First in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Second other", 4),
				Pair.of("Second in Package", 5),
				Pair.of("Fourth in Package", 6),
				Pair.of("First other", 7),
				Pair.of("Fourth other", 8));

		// re-import
		operate(() -> pack.syncAllFromFilesystem(Construct.class));

		// the re-imported category should be sorted to the same position
		assertThat(getNamesAndSortOrdersOfConstructCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Fourth in Package", 4),
				Pair.of("Third other", 5),
				Pair.of("Second other", 6),
				Pair.of("First other", 7),
				Pair.of("Fourth other", 8));
	}

	/**
	 * Test the sort orders of object property categories when importing from a devtool package
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testObjectPropertyCategory() throws NodeException {
		// create some object properties with categories
		ObjectTagDefinition firstInPackage = createObjectPropertyAndCategory("first_in_package", "First in Package", 1);
		ObjectTagDefinition secondInPackage = createObjectPropertyAndCategory("second_in_package", "Second in Package", 2);
		ObjectTagDefinition thirdInPackage = createObjectPropertyAndCategory("third_in_package", "Third in Package", 3);
		ObjectTagDefinition fourthInPackage = createObjectPropertyAndCategory("fourth_in_package", "Fourth in Package", 4);

		// put object properties (with categories) into the package
		consume(c -> pack.synchronize(c, true), firstInPackage);
		consume(c -> pack.synchronize(c, true), secondInPackage);
		consume(c -> pack.synchronize(c, true), thirdInPackage);
		consume(c -> pack.synchronize(c, true), fourthInPackage);

		String thirdCatInPackageGlobalId = execute(cons -> cons.getCategory().getGlobalId().toString(), thirdInPackage);

		// delete the object properties and categories
		Synchronizer.disable();
		deleteCategoryAndObjectProperty(firstInPackage);
		deleteCategoryAndObjectProperty(secondInPackage);
		deleteCategoryAndObjectProperty(thirdInPackage);
		deleteCategoryAndObjectProperty(fourthInPackage);

		// create new object properties with categories, with same sort orders as the first set
		createObjectPropertyAndCategory("first_other", "First other", 1);
		createObjectPropertyAndCategory("second_other", "Second other", 2);
		createObjectPropertyAndCategory("third_other", "Third other", 3);
		createObjectPropertyAndCategory("fourth_other", "Fourth other", 4);

		// synchronize from the package
		operate(() -> pack.syncAllFromFilesystem(ObjectTagDefinition.class));

		// all sort orders should be unique and continuous. imported categories sorted after the existing ones
		assertThat(getNamesAndSortOrdersOfObjectPropertyCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Fourth in Package", 4),
				Pair.of("First other", 5),
				Pair.of("Second other", 6),
				Pair.of("Third other", 7),
				Pair.of("Fourth other", 8));

		// delete one imported category
		operate(t -> t.getObject(ObjectTagDefinitionCategory.class, thirdCatInPackageGlobalId).delete(true));

		// sort orders of remaining categories did not change
		assertThat(getNamesAndSortOrdersOfObjectPropertyCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Fourth in Package", 3),
				Pair.of("First other", 4),
				Pair.of("Second other", 5),
				Pair.of("Third other", 6),
				Pair.of("Fourth other", 7));

		// import from the package
		operate(() -> pack.syncAllFromFilesystem(ObjectTagDefinition.class));

		// the re-imported category should be sorted to the same position
		assertThat(getNamesAndSortOrdersOfObjectPropertyCategories()).as("Names and Sort Orders").containsExactly(
				Pair.of("First in Package", 1),
				Pair.of("Second in Package", 2),
				Pair.of("Third in Package", 3),
				Pair.of("Fourth in Package", 4),
				Pair.of("First other", 5),
				Pair.of("Second other", 6),
				Pair.of("Third other", 7),
				Pair.of("Fourth other", 8));
	}

	protected List<Pair<String, Integer>> getNamesAndSortOrdersOfConstructCategories() throws NodeException {
		List<ConstructCategory> categories = supply(t -> t.getObjects(ConstructCategory.class,
				DBUtils.select("SELECT id FROM construct_category", DBUtils.IDS)));

		operate(() -> Collections.sort(categories, (c1, c2) -> Integer.compare(c1.getSortorder(), c2.getSortorder())));

		return execute(cats -> cats.stream()
				.map(cat -> Pair.of(cat.getName().toString(), cat.getSortorder())).collect(Collectors.toList()),
				categories);
	}

	protected Construct createConstructAndCategory(String keyword, String name, int sortOrder) throws NodeException {
		ConstructCategory category = Builder.create(ConstructCategory.class, cat -> {
			cat.setName(name, 1);
			cat.setSortorder(sortOrder);
		}).build();

		return Builder.create(Construct.class, constr -> {
			constr.setKeyword(keyword);
			constr.setName(name, 1);
			constr.setConstructCategoryId(category.getId());
		}).build();
	}

	protected void deleteCategoryAndConstruct(Construct construct) throws NodeException {
		consume(c -> {
			c.getConstructCategory().delete(true);
			c.delete(true);
		}, construct);
	}

	protected List<Pair<String, Integer>> getNamesAndSortOrdersOfObjectPropertyCategories() throws NodeException {
		List<ObjectTagDefinitionCategory> categories = supply(t -> t.getObjects(ObjectTagDefinitionCategory.class,
				DBUtils.select("SELECT id FROM objprop_category", DBUtils.IDS)));

		operate(() -> Collections.sort(categories, (c1, c2) -> Integer.compare(c1.getSortorder(), c2.getSortorder())));

		return execute(cats -> cats.stream()
				.map(cat -> Pair.of(cat.getName().toString(), cat.getSortorder())).collect(Collectors.toList()),
				categories);
	}

	protected ObjectTagDefinition createObjectPropertyAndCategory(String keyword, String name, int sortOrder) throws NodeException {
		ObjectTagDefinitionCategory category = Builder.create(ObjectTagDefinitionCategory.class, cat -> {
			cat.setName(name, 1);
			cat.setSortorder(sortOrder);
		}).build();

		return Builder.update(supply(() -> createObjectTagDefinition(keyword, Folder.TYPE_FOLDER, constructId)), upd -> {
			upd.setCategoryId(category.getId());
		}).build();
	}

	protected void deleteCategoryAndObjectProperty(ObjectTagDefinition def) throws NodeException {
		consume(d -> {
			d.getCategory().delete(true);
			d.delete(true);
		}, def);
	}

	/**
	 * Reverse the order of the elements in the array node
	 * @param arrayNode array node
	 */
	protected void reverse(ArrayNode arrayNode) {
		List<JsonNode> elements = new ArrayList<>();
		arrayNode.elements().forEachRemaining(e -> elements.add(0, e));
		arrayNode.removeAll();
		arrayNode.addAll(elements);
	}
}
