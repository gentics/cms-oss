package com.gentics.contentnode.mcp.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.Page;

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
}
