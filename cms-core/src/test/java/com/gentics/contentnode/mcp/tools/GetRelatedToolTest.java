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

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.GetRelatedTool.Usage;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.response.PageUsageListResponse;
import com.gentics.contentnode.rest.model.response.TemplateUsageListResponse;

/**
 * Unit tests for {@link GetRelatedTool}. The usage lookups are covered by the live check.
 */
public class GetRelatedToolTest {
	private final GetRelatedTool tool = new GetRelatedTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_related", "Get related objects", "ref");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("ref", Map.of("type", "page", "id", 4711, "name", "Home"), "relations",
				List.of("links_to_page", "total"), "size", 10));
		assertRejected(tool, Map.of("ref", Map.of("type", "page")));
		assertRejected(tool, Map.of("ref", Map.of("type", "page", "id", 4711), "relations", List.of()));
		assertRejected(tool, Map.of("ref", Map.of("type", "page", "id", 4711), "relations", List.of("links_from")));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("ref", Map.of("type", "page", "id", 4711)));
	}

	@Test
	public void testRelationTable() {
		assertThat(GetRelatedTool.usage(Type.PAGE, "links_to_page")).isEqualTo(Usage.PAGES_LINKING_PAGE);
		assertThat(GetRelatedTool.usage(Type.PAGE, "uses_tag")).isEqualTo(Usage.PAGES_LINKING_PAGE_TAG);
		assertThat(GetRelatedTool.usage(Type.PAGE, "variants")).isEqualTo(Usage.PAGE_VARIANTS);
		assertThat(GetRelatedTool.usage(Type.PAGE, "uses_template")).isEqualTo(Usage.TEMPLATES_LINKING_PAGE);
		assertThat(GetRelatedTool.usage(Type.PAGE, "links_to_file")).isNull();
		assertThat(GetRelatedTool.usage(Type.PAGE, "links_to_image")).isNull();

		assertThat(GetRelatedTool.usage(Type.FILE, "links_to_file")).isEqualTo(Usage.PAGES_LINKING_FILE);
		assertThat(GetRelatedTool.usage(Type.FILE, "uses_template")).isEqualTo(Usage.TEMPLATES_LINKING_FILE);
		assertThat(GetRelatedTool.usage(Type.FILE, "links_to_page")).isNull();
		assertThat(GetRelatedTool.usage(Type.FILE, "variants")).isNull();

		assertThat(GetRelatedTool.usage(Type.IMAGE, "links_to_image")).isEqualTo(Usage.PAGES_LINKING_IMAGE);
		assertThat(GetRelatedTool.usage(Type.IMAGE, "uses_template")).isEqualTo(Usage.TEMPLATES_LINKING_IMAGE);
		assertThat(GetRelatedTool.usage(Type.IMAGE, "links_to_file")).isNull();
		assertThat(GetRelatedTool.usage(Type.IMAGE, "uses_tag")).isNull();
	}

	@Test
	public void testOtherTypesRejected() {
		assertThatThrownBy(() -> GetRelatedTool.supportedType(Type.FOLDER))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("folder");
		assertThatThrownBy(() -> GetRelatedTool.usage(Type.TEMPLATE, "links_to_page"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("template");
		assertThat(GetRelatedTool.supportedType(Type.IMAGE)).isEqualTo(Type.IMAGE);
	}

	@Test
	public void testGroups() {
		Page page = new Page();
		page.setId(1);
		page.setName("Start");
		PageUsageListResponse pages = new PageUsageListResponse();
		pages.setPages(List.of(page));
		pages.setTotal(3);
		Template template = new Template();
		template.setId(9);
		template.setName("Article");
		TemplateUsageListResponse templates = new TemplateUsageListResponse();
		templates.setTemplates(List.of(template));
		templates.setTotal(1);

		GetRelatedTool.RelatedGroup pageGroup = GetRelatedTool.group("links_to_page", pages, 3);
		GetRelatedTool.RelatedGroup templateGroup = GetRelatedTool.group("uses_template", templates, null);

		assertThat(pageGroup.total()).isEqualTo(3);
		assertThat(pageGroup.items()).containsExactly(new ObjectRef(Type.PAGE, 1, null, 3, "Start", null, null, null,
				null));
		assertThat(templateGroup.items()).extracting(ObjectRef::type).containsExactly(Type.TEMPLATE);
		assertValidOutput(tool, new GetRelatedTool.Result(ObjectRef.of(Type.PAGE, 4711),
				List.of(pageGroup, templateGroup, new GetRelatedTool.RelatedGroup("links_to_file", 0, List.of())), 4));
	}
}
