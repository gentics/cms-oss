package com.gentics.contentnode.mcp.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;

/**
 * Unit tests for {@link ObjectRef}.
 */
public class ObjectRefTest {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	public void testTypeSerializesToLowercaseValue() throws Exception {
		assertThat(MAPPER.writeValueAsString(Type.NODE)).isEqualTo("\"node\"");
		assertThat(MAPPER.writeValueAsString(Type.CONSTRUCT)).isEqualTo("\"construct\"");
	}

	@Test
	public void testTypeDeserializesFromValue() throws Exception {
		assertThat(MAPPER.readValue("\"node\"", Type.class)).isEqualTo(Type.NODE);
		assertThat(MAPPER.readValue("\"construct\"", Type.class)).isEqualTo(Type.CONSTRUCT);
		assertThat(MAPPER.readValue("\"PAGE\"", Type.class)).isEqualTo(Type.PAGE);
	}

	@Test
	public void testEveryTypeRoundTrips() throws Exception {
		for (Type type : Type.values()) {
			assertThat(MAPPER.readValue(MAPPER.writeValueAsString(type), Type.class)).isEqualTo(type);
		}
	}

	@Test
	public void testUnknownTypeFails() {
		assertThatThrownBy(() -> Type.fromValue("bogus")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("bogus").hasMessageContaining("node");
		assertThatThrownBy(() -> MAPPER.readValue("\"bogus\"", Type.class))
				.isInstanceOf(ValueInstantiationException.class).hasMessageContaining("bogus");
	}

	@Test
	public void testForNode() {
		Node node = new Node();
		node.setId(42);
		node.setGlobalId("A547.12345");
		node.setName("Example Node");
		node.setHost("www.example.com");

		ObjectRef ref = ObjectRef.forNode(node);

		assertThat(ref).isEqualTo(new ObjectRef(Type.NODE, 42, "A547.12345", null, "Example Node", null, null, null, null));
	}

	@Test
	public void testForPage() {
		Page page = new Page();
		page.setId(7);
		page.setGlobalId("A547.99");
		page.setName("Home");
		page.setPath("/News/2026/");
		page.setLanguage("en");
		page.setNiceUrl("/news/home");
		page.setUrl("/preview/home.html");
		page.setLiveUrl("https://www.example.com/News/2026/home.html");

		assertThat(ObjectRef.forPage(page, 3)).isEqualTo(new ObjectRef(Type.PAGE, 7, "A547.99", 3, "Home",
				"/News/2026/", "en", "/news/home", "/preview/home.html"));
	}

	@Test
	public void testForPageFallsBackToLiveUrl() {
		Page page = new Page();
		page.setId(7);
		page.setUrl(" ");
		page.setLiveUrl("https://www.example.com/home.html");

		ObjectRef ref = ObjectRef.forPage(page, null);

		assertThat(ref.url()).isEqualTo("https://www.example.com/home.html");
		assertThat(ref.nodeId()).isNull();
	}

	@Test
	public void testForPageTakesNodeIdFromFolder() {
		Folder folder = new Folder();
		folder.setNodeId(3);
		Page page = new Page();
		page.setId(7);
		page.setFolder(folder);

		assertThat(ObjectRef.forPage(page).nodeId()).isEqualTo(3);
	}

	@Test
	public void testForPageWithoutFolderHasNoNodeId() {
		Page page = new Page();
		page.setId(7);

		assertThat(ObjectRef.forPage(page)).isEqualTo(ObjectRef.forPage(page, null));
	}

	@Test
	public void testJsonSchemaDescription() {
		assertThat(ObjectRef.jsonSchema("Reference.")).containsEntry("description", "Reference.");
		assertThat(ObjectRef.jsonSchema(null)).doesNotContainKey("description");
	}

	@Test
	public void testOf() {
		assertThat(ObjectRef.of(Type.PAGE, 7))
				.isEqualTo(new ObjectRef(Type.PAGE, 7, null, null, null, null, null, null, null));
	}

	@Test
	public void testFullRoundTrip() throws Exception {
		ObjectRef ref = new ObjectRef(Type.PAGE, 7, "A547.99", 3, "Home", "/News/2026/", "en", "/news/home",
				"https://www.example.com/News/2026/home.html");

		String json = MAPPER.writeValueAsString(ref);
		Map<String, Object> map = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
		});

		assertThat(map).containsOnlyKeys("type", "id", "globalId", "nodeId", "name", "path", "language", "niceUrl",
				"url");
		assertThat(map).containsEntry("type", "page").containsEntry("id", 7).containsEntry("nodeId", 3);
		assertThat(MAPPER.readValue(json, ObjectRef.class)).isEqualTo(ref);
	}

	@Test
	public void testUnsetFieldsAreOmitted() throws Exception {
		Map<String, Object> map = MAPPER.convertValue(ObjectRef.of(Type.FOLDER, 5),
				new TypeReference<Map<String, Object>>() {
				});

		assertThat(map).containsOnlyKeys("type", "id");
	}

	@Test
	public void testInputIgnoresUnknownFields() throws Exception {
		ObjectRef ref = MAPPER.readValue("{\"type\":\"node\",\"id\":1,\"somethingElse\":true}", ObjectRef.class);

		assertThat(ref).isEqualTo(ObjectRef.of(Type.NODE, 1));
	}

	@Test
	public void testForFolder() {
		Folder folder = new Folder();
		folder.setId(57);
		folder.setGlobalId("A547.57");
		folder.setName("News");
		folder.setNodeId(3);
		folder.setPath("/Home/News/");

		assertThat(ObjectRef.forFolder(folder))
				.isEqualTo(new ObjectRef(Type.FOLDER, 57, "A547.57", 3, "News", "/Home/News/", null, null, null));
	}

	@Test
	public void testForFile() {
		File file = new File();
		file.setId(12);
		file.setGlobalId("A547.12");
		file.setName("report.pdf");
		file.setPath("/Home/Downloads/");
		file.setNiceUrl("/downloads/report");
		file.setLiveUrl("https://www.example.com/Downloads/report.pdf");

		assertThat(ObjectRef.forFile(file, 3)).isEqualTo(new ObjectRef(Type.FILE, 12, "A547.12", 3, "report.pdf",
				"/Home/Downloads/", null, "/downloads/report", "https://www.example.com/Downloads/report.pdf"));
	}

	@Test
	public void testForImage() {
		Image image = new Image();
		image.setId(77);
		image.setName("logo.png");
		image.setUrl("/preview/logo.png");
		image.setLiveUrl("https://www.example.com/logo.png");

		assertThat(ObjectRef.forImage(image, null))
				.isEqualTo(new ObjectRef(Type.IMAGE, 77, null, null, "logo.png", null, null, null, "/preview/logo.png"));
	}

	@Test
	public void testForTemplate() {
		Template template = new Template();
		template.setId(9);
		template.setGlobalId("A547.9");
		template.setName("Article");

		assertThat(ObjectRef.forTemplate(template, 3))
				.isEqualTo(new ObjectRef(Type.TEMPLATE, 9, "A547.9", 3, "Article", null, null, null, null));
	}
}
