package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.PageInfo;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;

/**
 * Unit tests for {@link CreatePageTool}. Creating a real page is covered by the live check.
 */
public class CreatePageToolTest {
	private static final Map<String, Object> VALID = Map.of("folderId", 57, "templateId", 9, "name", "Acme terms",
			"language", "de");

	private final CreatePageTool tool = new CreatePageTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "create_page", "Create page", "folderId", "templateId", "name", "language");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, VALID);
		assertAccepted(tool, with("fileName", "terms", "forceExtension", true, "idempotencyKey", "k1"));
		assertRejected(tool, Map.of("folderId", 57, "templateId", 9, "name", "Acme terms"));
		assertRejected(tool, with("name", ""));
		assertRejected(tool, with("language", "d"));
		assertRejected(tool, with("priority", 101));
		assertRejected(tool, with("pageName", "x"));
	}

	@Test
	public void testRejectsChannel() {
		assertThatThrownBy(() -> tool.invoke(with("nodeId", 2), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("create_page does not yet support nodeId");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, VALID);
	}

	@Test
	public void testRequest() {
		PageCreateRequest request = CreatePageTool.request(VALID);
		assertThat(request.getFolderId()).isEqualTo("57");
		assertThat(request.getTemplateId()).isEqualTo(9);
		assertThat(request.getPageName()).isEqualTo("Acme terms");
		assertThat(request.getLanguage()).isEqualTo("de");
		assertThat(request.getFailOnDuplicate()).isTrue();
		assertThat(request.isForceExtension()).isFalse();
		assertThat(request.getNodeId()).isNull();

		assertThat(CreatePageTool.request(with("failOnDuplicate", false)).getFailOnDuplicate()).isFalse();
		assertThatThrownBy(() -> CreatePageTool.request(Map.of("folderId", 57, "templateId", 9, "name", "x")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("language");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResult() {
		Page page = new Page();
		page.setId(4711);
		page.setName("Acme terms");
		page.setFolderId(57);
		page.setLanguage("de");

		Map<String, Object> json = assertValidOutput(tool, new CreatePageTool.Result(PageInfo.of(page), true));
		assertThat(json).containsEntry("created", true);
		assertThat((Map<String, Object>) json.get("page")).containsEntry("online", false)
				.containsEntry("locked", false);
	}

	/**
	 * Get the valid arguments with additional ones
	 * @param keyValues additional arguments, alternating name and value
	 * @return arguments
	 */
	private static Map<String, Object> with(Object... keyValues) {
		Map<String, Object> arguments = new HashMap<>(VALID);
		for (int i = 0; i < keyValues.length; i += 2) {
			arguments.put((String) keyValues[i], keyValues[i + 1]);
		}
		return arguments;
	}
}
