package com.gentics.contentnode.mcp.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.model.TranslationStatusInfo.LatestVersionInfo;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.ItemVersion;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.PageVersion;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectSetting;
import com.gentics.contentnode.rest.model.User;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator.ValidationResponse;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;

/**
 * Checks that the hand-built output schema of every MCP model record matches the record, i.e.
 * describes exactly its components, and is itself a valid schema. The mapping of
 * {@link PageInfo} is covered by {@code UpdatePagePropertiesToolTest}.
 */
public class ModelJsonSchemaTest {
	@Test
	public void testObjectRef() {
		assertMatches(ObjectRef.class, ObjectRef.jsonSchema(null));
	}

	@Test
	public void testUserRef() {
		assertMatches(UserRef.class, UserRef.jsonSchema());
	}

	@Test
	public void testVersionInfo() {
		assertMatches(VersionInfo.class, VersionInfo.jsonSchema());
	}

	@Test
	public void testTranslationStatusInfo() {
		assertMatches(TranslationStatusInfo.class, TranslationStatusInfo.jsonSchema());
	}

	@Test
	public void testLatestVersionInfo() {
		assertMatches(LatestVersionInfo.class, LatestVersionInfo.jsonSchema());
	}

	@Test
	public void testPageInfo() {
		Map<String, Object> schema = PageInfo.jsonSchema("The page.");

		assertMatches(PageInfo.class, schema);
		assertThat(schema).containsEntry("description", "The page.").containsEntry("required", List.of("ref"));
	}

	@Test
	public void testLanguageInfo() {
		assertMatches(LanguageInfo.class, LanguageInfo.jsonSchema());
	}

	@Test
	public void testWhoamiUser() {
		assertMatches(WhoamiUser.class, WhoamiUser.jsonSchema());
	}

	@Test
	public void testSearchHit() {
		assertMatches(SearchHit.class, SearchHit.jsonSchema());
	}

	@Test
	public void testFolderItem() {
		assertMatches(FolderItem.class, FolderItem.jsonSchema());
	}

	@Test
	public void testFileInfo() {
		assertMatches(FileInfo.class, FileInfo.jsonSchema(null));
	}

	@Test
	public void testTagInfo() {
		assertMatches(TagInfo.class, TagInfo.jsonSchema());
	}

	@Test
	public void testNodeInfo() {
		assertMatches(NodeInfo.class, NodeInfo.jsonSchema());
	}

	@Test
	public void testConstructInfo() {
		assertMatches(ConstructInfo.class, ConstructInfo.jsonSchema(null));
		assertMatches(ConstructInfo.Category.class, ConstructInfo.Category.jsonSchema());
		assertMatches(ConstructInfo.Template.class, ConstructInfo.Template.jsonSchema());
		assertMatches(ConstructInfo.PartInfo.class, ConstructInfo.PartInfo.jsonSchema());
		assertMatches(ConstructInfo.Summary.class, ConstructInfo.Summary.jsonSchema());
	}

	@Test
	public void testConstructInfoOf() {
		Property template = new Property();
		template.setType(Property.Type.RICHTEXT);
		template.setStringValue("<b>{{cms.tag.parts.text}}</b>");
		Property text = new Property();
		text.setType(Property.Type.STRING);
		text.setStringValue("Hi");
		SelectSetting select = new SelectSetting();
		select.setDatasourceId(7);
		ConstructCategory category = new ConstructCategory();
		category.setId(5);
		category.setNameI18n(Map.of("en", "Teasers"));

		Construct construct = new Construct();
		construct.setId(12);
		construct.setGlobalId("A547.12");
		construct.setName("Teaser");
		construct.setKeyword("teaser");
		construct.setCategoryId(5);
		construct.setCategory(category);
		construct.setCdate(1000);
		construct.setParts(List.of(
				new Part().setKeyword("text").setTypeId(1).setType(Property.Type.STRING).setPartOrder(2)
						.setEditable(true).setDefaultProperty(text),
				new Part().setKeyword("handlebars").setTypeId(43).setType(Property.Type.RICHTEXT).setPartOrder(1)
						.setDefaultProperty(template),
				new Part().setKeyword("choice").setTypeId(29).setType(Property.Type.SELECT).setPartOrder(3)
						.setSelectSettings(select)));

		ConstructInfo info = ConstructInfo.of(construct);

		assertThat(info.ref()).isEqualTo(new ObjectRef(Type.CONSTRUCT, 12, "A547.12", null, "Teaser", null, null,
				null, null));
		assertThat(info.template()).isEqualTo(new ConstructInfo.Template("handlebars",
				"<b>{{cms.tag.parts.text}}</b>", 43, "RICHTEXT"));
		assertThat(info.parts()).extracting(ConstructInfo.PartInfo::keyword).containsExactly("text", "choice");
		assertThat(info.parts().get(0).defaultValue()).isEqualTo("Hi");
		assertThat(info.parts().get(1).datasourceId()).isEqualTo(7);
		assertThat(info.category()).isEqualTo(new ConstructInfo.Category(5, null, Map.of("en", "Teasers"), null,
				null));
		assertThat(info.created()).isEqualTo(1000);
		assertThat(info.edited()).isNull();

		ConstructInfo.Summary summary = ConstructInfo.Summary.of(construct);
		assertThat(summary.partKeywords()).containsExactly("text", "choice");
		assertThat(summary.hasHandlebarsPart()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testListResult() {
		Map<String, Object> itemSchema = NodeInfo.jsonSchema();
		Map<String, Object> schema = ListResult.jsonSchema(itemSchema, "nodes");

		assertMatches(ListResult.class, schema);
		assertThat(schema).containsEntry("required", List.of("items"));
		Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
		assertThat((Map<String, Object>) properties.get("items")).containsEntry("items", itemSchema);
		assertThat((Map<String, Object>) properties.get("total")).containsEntry("description",
				"Total number of matching nodes.");
	}

	@Test
	public void testListResultOfSlice() {
		assertThat(ListResult.of(Slice.of(0, 2, 5), List.of("a", "b")))
				.isEqualTo(new ListResult<>(5, true, 2, true, List.of("a", "b")));
		assertThat(ListResult.of(Slice.of(4, 2, 5), List.of("e")))
				.isEqualTo(new ListResult<>(5, true, null, false, List.of("e")));
	}

	@Test
	public void testLanguageInfoOf() {
		ContentLanguage language = new ContentLanguage().setId(1).setCode("de").setName("Deutsch");

		assertThat(LanguageInfo.of(language)).isEqualTo(new LanguageInfo(1, "de", "Deutsch"));
		assertThat(LanguageInfo.of(null)).isNull();
	}

	@Test
	public void testNodeInfoOf() {
		Node node = new Node();
		node.setId(42);
		node.setGlobalId("A547.12345");
		node.setName("Example Node");
		node.setHost("https://www.example.com");
		node.setPublishDir("/");
		node.setDefaultFileFolderId(10);
		node.setContentRepositoryId(3);
		List<LanguageInfo> languages = List.of(new LanguageInfo(1, "de", "Deutsch"));

		assertThat(NodeInfo.of(node, languages)).isEqualTo(new NodeInfo(
				new ObjectRef(Type.NODE, 42, "A547.12345", null, "Example Node", null, null, null, null),
				"https://www.example.com", "/", languages, 10, null, 3));
	}

	@Test
	public void testUserItem() {
		assertMatches(UserItem.class, UserItem.jsonSchema(null));
	}

	@Test
	public void testUserItemHasNoPassword() {
		User user = new User();
		user.setId(35);
		user.setLogin("mcp.test");
		user.setLastName("Test");
		user.setPassword("secret-password");

		Map<?, ?> json = new ObjectMapper().convertValue(UserItem.of(user, List.of()), Map.class);

		assertThat(json).isEqualTo(Map.of("ref", Map.of("type", "user", "id", 35, "name", "mcp.test"), "login",
				"mcp.test", "lastName", "Test", "groups", List.of()));
		assertThat(json.toString()).doesNotContain("password", "secret");
	}

	@Test
	public void testUserRefOf() {
		User user = new User();
		user.setId(11);
		user.setLogin("editor");

		assertThat(UserRef.of(user)).isEqualTo(new UserRef(11, "editor"));
		assertThat(UserRef.of(null)).isNull();
	}

	@Test
	public void testVersionInfoOfAnyItemVersion() {
		User editor = new User();
		editor.setId(11);
		editor.setLogin("editor");
		ItemVersion version = new ItemVersion().setNumber("1.0").setTimestamp(2000).setEditor(editor);
		PageVersion pageVersion = new PageVersion();
		pageVersion.setNumber("0.1");

		assertThat(VersionInfo.of(version)).isEqualTo(new VersionInfo("1.0", 2000, new UserRef(11, "editor")));
		assertThat(VersionInfo.of(pageVersion)).isEqualTo(new VersionInfo("0.1", 0, null));
		assertThat(VersionInfo.of(null)).isNull();
	}

	@Test
	public void testTimestampsOrNull() {
		assertThat(Timestamps.orNull(1000)).isEqualTo(1000);
		assertThat(Timestamps.orNull(0)).isNull();
		assertThat(Timestamps.orNull(-1)).isNull();
	}

	@SuppressWarnings("unchecked")
	private static void assertMatches(Class<? extends Record> recordClass, Map<String, Object> schema) {
		String[] components = Arrays.stream(recordClass.getRecordComponents()).map(RecordComponent::getName)
				.toArray(String[]::new);

		assertThat(schema).containsEntry("type", "object");
		assertThat((Map<String, Object>) schema.get("properties")).as("properties of %s", recordClass.getSimpleName())
				.containsOnlyKeys(components);

		ValidationResponse response = new DefaultJsonSchemaValidator().validateSchema(schema);
		assertThat(response.valid()).as(response.errorMessage()).isTrue();
	}
}
