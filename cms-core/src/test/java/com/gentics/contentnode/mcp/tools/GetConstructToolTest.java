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
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.Property;

/**
 * Unit tests for {@link GetConstructTool}. Loading by ID and keyword is covered by the live check.
 */
public class GetConstructToolTest {
	private final GetConstructTool tool = new GetConstructTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_construct", "Get construct");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 12));
		assertAccepted(tool, Map.of("keyword", "teaser", "embedCategory", false));
		assertRejected(tool, Map.of("id", 0));
		assertRejected(tool, Map.of("keyword", "k".repeat(101)));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 12));
	}

	@Test
	public void testExactlyOne() {
		assertThatThrownBy(() -> tool.invoke(Map.of(), Optional.empty())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exactly one of 'id' and 'keyword'");
		assertThatThrownBy(() -> tool.invoke(Map.of("id", 12, "keyword", "teaser"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(() -> tool.invoke(Map.of("keyword", " "), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");
	}

	@Test
	public void testOutput() {
		Property source = new Property();
		source.setType(Property.Type.RICHTEXT);
		source.setStringValue("<b>{{cms.tag.parts.text}}</b>");
		Construct construct = new Construct().setId(12).setKeyword("teaser").setName("Teaser")
				.setParts(List.of(new Part().setKeyword("handlebars").setTypeId(43).setDefaultProperty(source),
						new Part().setKeyword("text").setTypeId(1).setType(Property.Type.STRING)));

		Map<String, Object> json = assertValidOutput(tool, new GetConstructTool.Result(ConstructInfo.of(construct),
				List.of(ObjectRef.of(ObjectRef.Type.NODE, 1))));

		assertThat(json).containsKeys("construct", "nodeRefs");
	}
}
