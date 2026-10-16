package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.TemplateTag;

/**
 * Unit tests for {@link GetTemplateTool}. Loading a real template is covered by the live check.
 */
public class GetTemplateToolTest {
	private final GetTemplateTool tool = new GetTemplateTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_template", "Get template", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 9, "nodeId", 1, "includeSource", true));
		assertRejected(tool, Map.of("id", 9, "includeSource", "yes"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 9));
	}

	@Test
	public void testWithoutSource() {
		GetTemplateTool.TemplateInfo info = GetTemplateTool.info(template(), 1, false, List.of(folder()),
				Map.of(5, "richtext"));

		assertThat(info.source()).isNull();
		assertThat(info.templateTags()).extracting(GetTemplateTool.TemplateTagInfo::name).containsExactly("content",
				"title");
		assertThat(info.templateTags().get(0).constructKeyword()).isEqualTo("richtext");
		assertThat(info.templateTags().get(0).editableInPage()).isTrue();
		assertThat(info.templateTags().get(1).constructKeyword()).isEqualTo("loaded");
		assertThat(info.folderRefs()).extracting(ref -> ref.id()).containsExactly(57);
		assertThat(info.ref().nodeId()).isEqualTo(1);
		assertThat(assertValidOutput(tool, new GetTemplateTool.Result(info)).get("template").toString())
				.doesNotContain("source");
	}

	@Test
	public void testWithSource() {
		GetTemplateTool.TemplateInfo info = GetTemplateTool.info(template(), null, true, List.of(), Map.of());

		assertThat(info.source()).isEqualTo("<node content>");
		assertValidOutput(tool, new GetTemplateTool.Result(info));
	}

	private static Template template() {
		TemplateTag content = new TemplateTag();
		content.setName("content");
		content.setConstructId(5);
		content.setEditableInPage(true);
		content.setMandatory(false);
		Construct construct = new Construct();
		construct.setKeyword("loaded");
		TemplateTag title = new TemplateTag();
		title.setName("title");
		title.setConstructId(8);
		title.setConstruct(construct);
		Map<String, TemplateTag> tags = new LinkedHashMap<>();
		tags.put("title", title);
		tags.put("content", content);
		Template template = new Template();
		template.setId(9);
		template.setName("Article");
		template.setSource("<node content>");
		template.setTemplateTags(tags);
		return template;
	}

	private static Folder folder() {
		Folder folder = new Folder();
		folder.setId(57);
		folder.setName("News");
		return folder;
	}
}
