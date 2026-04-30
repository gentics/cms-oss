package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.forms.FormTransformer.TransformationTarget.fromContentPackage;
import static com.gentics.contentnode.forms.FormTransformer.TransformationTarget.publish;
import static com.gentics.contentnode.forms.FormTransformer.TransformationTarget.toContentPackage;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.forms.FormTransformer.TransformationTarget;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for the {@link FormTransformer}
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@ConfigBy(FormTransformerTest.class)
public class FormTransformerTest implements ConfigModifier {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	protected static ObjectMapper mapper = new ObjectMapper();

	private static Node node;

	private static Form form;

	public final static String FORM_TYPE = "testing_type";

	private static Template template;

	private static Page germanPage;

	private static Page englishPage;

	private static Node channel;

	private static File file;

	private static File image;

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));

		ContentRepository contentRepository = create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setName("Mesh CR");
			cr.setProjectPerNode(true);
		}).build();

		node = update(node, n -> {
			n.setPublishContentmap(true);
			n.setContentrepositoryId(contentRepository.getId());
		}).build();

		consume(n -> {
			FormFactory.addFormTypeToNode(FORM_TYPE, n.getId());
		}, node);

		channel = supply(() -> createChannel(node, "Channel", "channel", "/Content.node"));

		Folder testFolder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Test Folder");
			f.setPublishDir("/test/folder");
			f.setPublishDirI18n(new I18nMap().put("de", "/test_de/folder").put("en", "/test_en/folder"));
		}).build();

		form = create(Form.class, f -> {
			f.setFolder(node, node.getFolder());
			f.setFormType(FORM_TYPE);
			f.setLanguages(Arrays.asList("de", "en"));
			f.setName("Test Form");
		}).build();

		template = create(Template.class, t -> {
			t.setName("Test Template");
			t.setSource("<node page.name> (<node page.language.code>)");
			t.setFolderId(node.getFolder().getId());
		}).build();

		germanPage = create(Page.class, p -> {
			p.setName("Seite");
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("de"));
		}).publish().build();

		englishPage = create(Page.class, p -> {
			p.setName("Page");
			p.setFolder(node, testFolder);
			p.setTemplateId(template.getId());
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanPage.getContentsetId());
		}).publish().build();

		update(supply(() -> localize(germanPage, channel)), p -> {
			p.setName("Seite im Channel");
			p.setFilename(null);
		}).publish().build();

		file = supply(() -> createFile(testFolder, "testfile.txt", "file contents".getBytes()));
		try (InputStream in = GenericTestUtils.getPictureResource("blume.jpg")) {
			image = supply(() -> createImage(testFolder, "blume.jpg", in));
		}
	}

	@Test
	public void testPublishMinimalFormData() throws NodeException {
		doTransformTest(mapper.createObjectNode(), publish, getMinimalRendered(form));
	}

	@Test
	public void testMinimalFormDataToContentPackage() throws NodeException {
		doTransformTest(mapper.createObjectNode(), toContentPackage, getMinimalContentPackage(form));
	}

	@Test
	public void testMinimalFormDataFromContentPackage() throws NodeException {
		doTransformTest(mapper.createObjectNode(), fromContentPackage, getMinimalContentPackage(form));
	}

	@Test
	public void testPublishSuccessPage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getId());
		data.put("successNodeId", node.getId());

		ObjectNode expected = getMinimalRendered(form);
		expected.withObjectProperty("successUrl")
			.put("de", "http://node/Content.node/test_de/folder/Seite.de.html")
			.put("en", "http://node/Content.node/test_en/folder/Page.en.html");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testPublishSuccessPageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getId());
		data.put("successNodeId", channel.getId());

		ObjectNode expected = getMinimalRendered(form);
		expected.withObjectProperty("successUrl")
			.put("de", "http://channel/Content.node/test_de/folder/Seite-im-Channel.de.html")
			.put("en", "http://channel/Content.node/test_en/folder/Page.en.html");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testSuccessPageToContentPackage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getId());
		data.put("successNodeId", node.getId());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("successPageId", germanPage.getGlobalId().toString())
			.put("successNodeId", node.getGlobalId().toString());

		doTransformTest(data, toContentPackage, expected);
	}

	@Test
	public void testSuccessPageToContentPackageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getId());
		data.put("successNodeId", channel.getId());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("successPageId", germanPage.getGlobalId().toString())
			.put("successNodeId", channel.getGlobalId().toString());

		doTransformTest(data, toContentPackage, expected);
	}

	@Test
	public void testSuccessPageFromContentPackage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getGlobalId().toString());
		data.put("successNodeId", node.getGlobalId().toString());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("successPageId", germanPage.getId())
			.put("successNodeId", node.getId());

		doTransformTest(data, fromContentPackage, expected);
	}

	@Test
	public void testSuccessPageFromContentPackageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("successPageId", germanPage.getGlobalId().toString());
		data.put("successNodeId", channel.getGlobalId().toString());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("successPageId", germanPage.getId())
			.put("successNodeId", channel.getId());

		doTransformTest(data, fromContentPackage, expected);
	}

	@Test
	public void testPublishAdminEmailPage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getId());
		data.put("adminEmailNodeId", node.getId());

		ObjectNode expected = getMinimalRendered(form);
		expected.withObjectProperty("adminEmailSource")
			.put("de", "Seite (de)")
			.put("en", "Page (en)");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testPublishAdminEmailPageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getId());
		data.put("adminEmailNodeId", channel.getId());

		ObjectNode expected = getMinimalRendered(form);
		expected.withObjectProperty("adminEmailSource")
			.put("de", "Seite im Channel (de)")
			.put("en", "Page (en)");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testAdminEmailPageToContentPackage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getId());
		data.put("adminEmailNodeId", node.getId());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("adminEmailPageId", englishPage.getGlobalId().toString())
			.put("adminEmailNodeId", node.getGlobalId().toString());

		doTransformTest(data, toContentPackage, expected);
	}

	@Test
	public void testAdminEmailPageToContentPackageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getId());
		data.put("adminEmailNodeId", channel.getId());

		ObjectNode expected = getMinimalContentPackage(form)
			.put("adminEmailPageId", englishPage.getGlobalId().toString())
			.put("adminEmailNodeId", channel.getGlobalId().toString());

		doTransformTest(data, toContentPackage, expected);
	}

	@Test
	public void testAdminEmailPageFromContentPackage() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getGlobalId().toString());
		data.put("adminEmailNodeId", node.getGlobalId().toString());

		ObjectNode expected = getMinimalContentPackage(form)
				.put("adminEmailPageId", englishPage.getId())
				.put("adminEmailNodeId", node.getId());

		doTransformTest(data, fromContentPackage, expected);
	}

	@Test
	public void testAdminEmailPageFromContentPackageInChannel() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		data.put("adminEmailPageId", englishPage.getGlobalId().toString());
		data.put("adminEmailNodeId", channel.getGlobalId().toString());

		ObjectNode expected = getMinimalContentPackage(form)
				.put("adminEmailPageId", englishPage.getId())
				.put("adminEmailNodeId", channel.getId());

		doTransformTest(data, fromContentPackage, expected);
	}

	@Test
	public void testContentReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("contentPage")
			.put("id", germanPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("contentPage")
			.put("de", "Seite (de)")
			.put("en", "Page (en)");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testContentReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("contentPageBackend")
			.put("id", englishPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("contentPageBackend")
			.put("de", "Seite (de)")
			.put("en", "Page (en)");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlPageReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("urlReference")
			.put("id", germanPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("urlReference")
			.put("de", "http://node/Content.node/test_de/folder/Seite.de.html")
			.put("en", "http://node/Content.node/test_en/folder/Page.en.html");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlPageReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("id", englishPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("de", "http://node/Content.node/test_de/folder/Seite.de.html")
			.put("en", "http://node/Content.node/test_en/folder/Page.en.html");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlFileReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("urlReference")
			.put("id", file.getId())
			.put("nodeId", node.getId())
			.put("type", "file");

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("urlReference")
			.put("de", "http://node/Content.node/test/folder/testfile.txt")
			.put("en", "http://node/Content.node/test/folder/testfile.txt");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlFileReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("id", file.getId())
			.put("nodeId", node.getId())
			.put("type", "file");

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("de", "http://node/Content.node/test/folder/testfile.txt")
			.put("en", "http://node/Content.node/test/folder/testfile.txt");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlImageReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("urlReference")
			.put("id", image.getId())
			.put("nodeId", node.getId())
			.put("type", "image");

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("urlReference")
			.put("de", "http://node/Content.node/test/folder/blume.jpg")
			.put("en", "http://node/Content.node/test/folder/blume.jpg");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testUrlImageReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("id", image.getId())
			.put("nodeId", node.getId())
			.put("type", "image");

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("urlReferenceBackend")
			.put("de", "http://node/Content.node/test/folder/blume.jpg")
			.put("en", "http://node/Content.node/test/folder/blume.jpg");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkPageReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("meshlinkReference")
			.put("id", germanPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(germanPage));

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReference")
			.put("de", "{{mesh.link(%s, de, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkPageReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("id", englishPage.getId())
			.put("nodeId", node.getId())
			.put("type", "page");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(englishPage));

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("de", "{{mesh.link(%s, de, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkFileReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("meshlinkReference")
			.put("id", file.getId())
			.put("nodeId", node.getId())
			.put("type", "file");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(file));

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReference")
			.put("de", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkFileReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("id", file.getId())
			.put("nodeId", node.getId())
			.put("type", "file");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(file));

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("de", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkImageReference() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.withObjectProperty("meshlinkReference")
			.put("id", image.getId())
			.put("nodeId", node.getId())
			.put("type", "image");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(image));

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReference")
			.put("de", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testMeshlinkImageReferenceBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("id", image.getId())
			.put("nodeId", node.getId())
			.put("type", "image");

		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(image));

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.withObjectProperty("meshlinkReferenceBackend")
			.put("de", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid))
			.put("en", "{{mesh.link(%s, en, Node)}}".formatted(meshUuid));

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testRichtext() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addUiSchemaElement(data, "control")
			.put("string", "Text with a link: {{ LINK|URL:https&col;//gentics.com|Go to Gentics &dollar;|_self }}");

		ObjectNode expected = getMinimalRendered(form);
		addUiSchemaElement(expected, "control")
			.put("string", "Text with a link: <a href='https://gentics.com' target='_self'>Go to Gentics $</a>");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testRichtextBackend() throws NodeException {
		ObjectNode data = mapper.createObjectNode();
		addSchemaElement(data, "control")
			.put("stringBackend", "Text with a link: {{ LINK|URL:https&col;//gentics.com|Go to Gentics &dollar;|_self }}");

		ObjectNode expected = getMinimalRendered(form);
		addSchemaElement(expected, "control")
			.put("stringBackend", "Text with a link: <a href='https://gentics.com' target='_self'>Go to Gentics $</a>");

		doTransformTest(data, publish, expected);
	}

	@Test
	public void testRichtextInternal() throws NodeException {
		String nodeId = node.getGlobalId().toString();
		String pageId = germanPage.getGlobalId().toString();
		String fileId = file.getGlobalId().toString();
		String imageId = image.getGlobalId().toString();

		List<Pair<String, String>> testCases = List.of(
			Pair.of("{{LINK|PAGE:%s:%s|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_de/folder/Seite.de.html' >Page</a>"),
			Pair.of("{{LINK|PAGE:%s:%s:de|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_de/folder/Seite.de.html' >Page</a>"),
			Pair.of("{{LINK|PAGE:%s:%s:en|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_en/folder/Page.en.html' >Page</a>"),
			Pair.of("{{LINK|FILE:%s:%s|File}}".formatted(nodeId, fileId), "<a href='http://node/Content.node/test/folder/testfile.txt' >File</a>"),
			Pair.of("{{LINK|FILE:%s:%s|Image}}".formatted(nodeId, imageId), "<a href='http://node/Content.node/test/folder/blume.jpg' >Image</a>"));

		for (Pair<String, String> testCase : testCases) {
			ObjectNode data = mapper.createObjectNode();
			addUiSchemaElement(data, "control")
				.put("string", testCase.getLeft());

			ObjectNode expected = getMinimalRendered(form);
			addUiSchemaElement(expected, "control")
				.put("string", testCase.getRight());

			doTransformTest(data, publish, expected);
		}
	}

	@Test
	public void testRichtextBackendInternal() throws NodeException {
		String nodeId = node.getGlobalId().toString();
		String pageId = germanPage.getGlobalId().toString();
		String fileId = file.getGlobalId().toString();
		String imageId = image.getGlobalId().toString();

		List<Pair<String, String>> testCases = List.of(
			Pair.of("{{LINK|PAGE:%s:%s|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_de/folder/Seite.de.html' >Page</a>"),
			Pair.of("{{LINK|PAGE:%s:%s:de|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_de/folder/Seite.de.html' >Page</a>"),
			Pair.of("{{LINK|PAGE:%s:%s:en|Page}}".formatted(nodeId, pageId), "<a href='http://node/Content.node/test_en/folder/Page.en.html' >Page</a>"),
			Pair.of("{{LINK|FILE:%s:%s|File}}".formatted(nodeId, fileId), "<a href='http://node/Content.node/test/folder/testfile.txt' >File</a>"),
			Pair.of("{{LINK|FILE:%s:%s|Image}}".formatted(nodeId, imageId), "<a href='http://node/Content.node/test/folder/blume.jpg' >Image</a>"));

		for (Pair<String, String> testCase : testCases) {
			ObjectNode data = mapper.createObjectNode();
			addSchemaElement(data, "control")
				.put("stringBackend", testCase.getLeft());

			ObjectNode expected = getMinimalRendered(form);
			addSchemaElement(expected, "control")
				.put("stringBackend", testCase.getRight());

			doTransformTest(data, publish, expected);
		}
	}

	/**
	 * Do the transformation test
	 * @param data original data
	 * @param transformationTarget transformation target
	 * @param expected expected transformed data
	 * @throws NodeException
	 */
	protected void doTransformTest(ObjectNode data, TransformationTarget transformationTarget, ObjectNode expected)
			throws NodeException {
		ObjectNode transformedData = transform(transformationTarget, form, data);
		assertThat(transformedData).as("Transformed data").isEqualTo(expected);
	}

	/**
	 * Transform the data for the form
	 * @param transformationTarget transformation target
	 * @param form form
	 * @param data data to be transformed
	 * @return transformed data
	 * @throws NodeException
	 */
	protected ObjectNode transform(TransformationTarget transformationTarget, Form form, ObjectNode data)
			throws NodeException {
		return supply(() -> new FormTransformer().withTransformationTarget(transformationTarget).transform(form, data));
	}

	/**
	 * Get the minimal form data, how it is expected to be transformed for publishing
	 * @param form form
	 * @return minimal expected data
	 */
	protected ObjectNode getMinimalRendered(Form form) {
		ObjectNode data = mapper.createObjectNode();
		data.put("cms_id", form.getId());
		data.put("type", form.getFormType());
		data.withObjectProperty("schema").withObjectProperty("properties");
		ObjectNode formName = data.withObjectProperty("ui-schema").withObjectProperty("formname");
		for (String language : form.getLanguages()) {
			formName.put(language, form.getName());
		}

		return data;
	}

	/**
	 * Get the minimal form data, how is is expected to be transformed for content packages
	 * @param form form
	 * @return minimal expected data
	 */
	protected ObjectNode getMinimalContentPackage(Form form) {
		return mapper.createObjectNode()
			.putNull("successPageId")
			.putNull("successNodeId")
			.putNull("adminEmailPageId")
			.putNull("adminEmailNodeId");
	}

	/**
	 * Add an element of the given type to the ui-schema
	 * @param data data
	 * @param elementType element type
	 * @return added element
	 */
	protected ObjectNode addUiSchemaElement(ObjectNode data, String elementType) {
		ArrayNode pages = data.withObjectProperty("ui-schema").withArrayProperty("pages");
		ObjectNode page = pages.addObject();
		ObjectNode element = page.withArrayProperty("elements").addObject();
		return element.withObjectProperty("formGridOptions")
			.put("type", elementType);
	}

	/**
	 * Add an element of the given type to the schema
	 * @param data data
	 * @param elementType element type
	 * @return added element
	 */
	protected ObjectNode addSchemaElement(ObjectNode data, String elementType) {
		return data
			.withObjectProperty("schema")
			.withObjectProperty("properties")
			.withObjectProperty(elementType)
			.put("type", elementType)
			.withObjectProperty("formGridOptions");
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("en", "Testing Type"), "forms");

		ControlConfig control = new ControlConfig(Map.of("de", "Kontrollelement"));

		StringSetting stringSetting = new StringSetting("string", Map.of());
		stringSetting.richContent = true;
		StringSetting stringSettingBackend = new StringSetting("stringBackend", Map.of());
		stringSettingBackend.backend = true;
		stringSettingBackend.richContent = true;

		ReferenceSetting pageContentReference = new ReferenceSetting("contentPage", Map.of(),
				Set.of(ReferenceType.page), PublishAsType.content);
		ReferenceSetting pageContentReferenceBackend = new ReferenceSetting("contentPageBackend", Map.of(),
				Set.of(ReferenceType.page), PublishAsType.content);
		pageContentReferenceBackend.backend = true;
		ReferenceSetting urlReference = new ReferenceSetting("urlReference", Map.of(),
				Set.of(ReferenceType.page, ReferenceType.file, ReferenceType.image), PublishAsType.url);
		ReferenceSetting urlReferenceBackend = new ReferenceSetting("urlReferenceBackend", Map.of(),
				Set.of(ReferenceType.page, ReferenceType.file, ReferenceType.image), PublishAsType.url);
		urlReferenceBackend.backend = true;
		ReferenceSetting meshlinkReference = new ReferenceSetting("meshlinkReference", Map.of(),
				Set.of(ReferenceType.page, ReferenceType.file, ReferenceType.image), PublishAsType.meshlink);
		ReferenceSetting meshlinkReferenceBackend = new ReferenceSetting("meshlinkReferenceBackend", Map.of(),
				Set.of(ReferenceType.page, ReferenceType.file, ReferenceType.image), PublishAsType.meshlink);
		meshlinkReferenceBackend.backend = true;

		control.settings = List.of(stringSetting, stringSettingBackend, pageContentReference,
				pageContentReferenceBackend, urlReference, urlReferenceBackend, meshlinkReference,
				meshlinkReferenceBackend);

		formsConfig.controls = Map.of("control", control);

		formTypeConfiguration.put(FORM_TYPE, formsConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}
}
