package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.tools.UpdateConstructTool.Merge;
import com.gentics.contentnode.mcp.tools.UpdateConstructTool.Request;
import com.gentics.contentnode.mcp.util.PartSpecs;
import com.gentics.contentnode.mcp.util.PartSpecs.PartSpec;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.Property;

/**
 * Unit tests for {@link UpdateConstructTool}. Updating, keeping tag values and permissions are covered by the live
 * check.
 */
public class UpdateConstructToolTest {
	private final UpdateConstructTool tool = new UpdateConstructTool();

	private static final List<String> UI_LANGUAGES = List.of("de", "en");

	private static final Map<String, String> NAME = Map.of("en", "Text");

	@Test
	public void testDefinition() {
		assertDefinition(tool, "update_construct", "Update construct");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 12, "handlebarsTemplate", "<i>x</i>"));
		assertAccepted(tool, Map.of("keyword", "teaser", "nodeIds", List.of(1, 2), "mayBeSubtag", false));
		assertRejected(tool, Map.of("id", 12, "nodeIds", List.of(1, 1)));
		assertRejected(tool, Map.of("id", 12, "keyword", "teaser", "newKeyword", "x"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 12));
	}

	@Test
	public void testRequest() {
		Request request = Request.of(Map.of("name", Map.of("en", "Teaser"), "autoEnable", false), UI_LANGUAGES);

		assertThat(request.changes().getNameI18n()).isEqualTo(Map.of("en", "Teaser"));
		assertThat(request.changes().getAutoEnable()).isFalse();
		assertThat(request.changes().getMayBeSubtag()).isNull();
		assertThat(request.changes().getEditorControlStyle()).isNull();
		assertThat(request.source()).isNull();
		assertThat(request.parts()).isNull();
		assertThat(request.nodeIds()).isNull();
	}

	@Test
	public void testRequestRejections() {
		assertThatThrownBy(() -> Request.of(Map.of("nodeIds", List.of()), UI_LANGUAGES))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not be empty");
		assertThatThrownBy(() -> Request.of(Map.of("visibleInMenu", false), UI_LANGUAGES))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot be false");
	}

	@Test
	public void testMergeNothing() {
		assertThat(UpdateConstructTool.merge(current(), null, null)).isEqualTo(new Merge(null, List.of()));
	}

	@Test
	public void testMergeTemplateOnly() {
		List<Part> current = current();
		Merge merge = UpdateConstructTool.merge(current, "<i>new</i>", null);

		assertThat(merge.parts()).extracting(Part::getGlobalId).containsExactly("G.hb", "G.text", "G.flag");
		assertThat(merge.parts().get(0).getDefaultProperty().getStringValue()).isEqualTo("<i>new</i>");
		assertThat(merge.parts().get(1)).isSameAs(current.get(1));
		assertThat(merge.removedKeywords()).isEmpty();
	}

	@Test
	public void testMergeParts() {
		Merge merge = UpdateConstructTool.merge(current(), null, List.of(spec("text", 1), spec("flag", 1),
				spec("teaser", 2)));

		assertThat(merge.parts()).extracting(Part::getKeyword).containsExactly("handlebars", "text", "flag",
				"teaser");
		// same keyword and type keeps the identity, a changed type does not
		assertThat(merge.parts()).extracting(Part::getGlobalId).containsExactly("G.hb", "G.text", null, null);
		assertThat(merge.parts()).extracting(Part::getPartOrder).containsExactly(1, 2, 3, 4);
		assertThat(merge.removedKeywords()).isEmpty();

		Merge removal = UpdateConstructTool.merge(current(), null, List.of(spec("teaser", 2)));
		assertThat(removal.removedKeywords()).containsExactly("text", "flag");
	}

	@Test
	public void testMergeAddsTemplate() {
		Merge merge = UpdateConstructTool.merge(List.of(part("text", 1, "G.text", 2)), "<i>x</i>", null);

		assertThat(merge.parts()).extracting(Part::getKeyword).containsExactly("handlebars", "text");
		assertThat(merge.parts().get(0).getGlobalId()).isNull();
	}

	@Test
	public void testChangedFields() {
		Construct construct = new Construct().setId(12).setKeyword("teaser").setParts(current());
		ConstructInfo before = ConstructInfo.of(construct);
		construct.setParts(UpdateConstructTool.merge(current(), "<i>new</i>", null).parts());

		assertThat(UpdateConstructTool.CHANGED_FIELDS.snapshot(before).diff(ConstructInfo.of(construct)))
				.containsExactly("handlebarsTemplate");
	}

	@Test
	public void testOutput() {
		Construct construct = new Construct().setId(12).setKeyword("teaser").setName("Teaser").setParts(current());

		assertValidOutput(tool, new UpdateConstructTool.Result(ConstructInfo.of(construct), List.of("parts"), true,
				List.of("flag"), List.of()));
	}

	private static List<Part> current() {
		return List.of(PartSpecs.templatePart("handlebars", "<b>{{cms.tag.parts.text}}</b>", null).setGlobalId("G.hb"),
				part("text", 1, "G.text", 2), part("flag", 31, "G.flag", 3));
	}

	private static Part part(String keyword, int typeId, String globalId, int order) {
		return new Part().setKeyword(keyword).setTypeId(typeId).setType(Property.Type.get(typeId))
				.setGlobalId(globalId).setPartOrder(order).setNameI18n(NAME);
	}

	private static PartSpec spec(String keyword, int typeId) {
		return new PartSpec(keyword, NAME, typeId, null, true, false, false, false, null, null);
	}
}
