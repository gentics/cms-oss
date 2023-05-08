package com.gentics.contentnode.tests.devtools;

import static com.gentics.contentnode.factory.Trx.consume;
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
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import com.gentics.contentnode.devtools.MainPackageSynchronizer;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.factory.object.OverviewPartSetting.ObjectType;
import com.gentics.contentnode.factory.object.OverviewPartSetting.SelectionType;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
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
