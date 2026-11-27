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

import com.gentics.contentnode.mcp.util.Guidelines;
import com.gentics.contentnode.mcp.util.Guidelines.Guideline;

/**
 * Unit tests for the guideline tools and {@link Guidelines}. Reading and writing real package files, and the
 * permissions, are covered by the live check.
 */
public class GuidelineToolsTest {
	@Test
	public void testDefinitions() {
		assertDefinition(new ListGuidelinesTool(), "list_guidelines", "List guidelines", "nodeId");
		assertDefinition(new GetGuidelineTool(), "get_guideline", "Get guideline", "id");
		assertDefinition(new CreateGuidelineTool(), "create_guideline", "Create guideline", "package", "name",
				"content");
		assertDefinition(new UpdateGuidelineTool(), "update_guideline", "Update guideline", "id");
		assertDefinition(new DeleteGuidelineTool(), "delete_guideline", "Delete guideline", "id");
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(new ListGuidelinesTool(), Map.of("nodeId", 1));
		assertRejectsWithoutCredentials(new GetGuidelineTool(), Map.of("id", "p/n"));
		assertRejectsWithoutCredentials(new CreateGuidelineTool(), Map.of("package", "p", "name", "n", "content", "c"));
		assertRejectsWithoutCredentials(new UpdateGuidelineTool(), Map.of("id", "p/n", "content", "c"));
		assertRejectsWithoutCredentials(new DeleteGuidelineTool(), Map.of("id", "p/n"));
	}

	@Test
	public void testInputValidation() {
		assertAccepted(new CreateGuidelineTool(),
				Map.of("package", "p", "name", "tone", "default", true, "priority", 10, "content", "c"));
		assertRejected(new CreateGuidelineTool(), Map.of("package", "p", "name", "tone"));
		assertRejected(new CreateGuidelineTool(), Map.of("package", "p", "name", "../tone", "content", "c"));
		assertRejected(new GetGuidelineTool(), Map.of("id", "no-slash"));
	}

	@Test
	public void testNameRule() {
		for (String name : List.of("10_tone", "../tone", "tone.default", "a/b", ".hidden")) {
			assertThatThrownBy(() -> CreateGuidelineTool.Request
					.of(Map.of("package", "p", "name", name, "content", "c"))).as(name)
							.isInstanceOf(IllegalArgumentException.class);
		}
		assertThat(CreateGuidelineTool.Request.of(Map.of("package", "p", "name", "tone_of-voice1", "content", "c"))
				.name()).isEqualTo("tone_of-voice1");
	}

	@Test
	public void testUpdateNeedsAChange() {
		assertThatThrownBy(() -> new UpdateGuidelineTool().invoke(Map.of("id", "p/n"), Optional.empty()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Nothing to change");
	}

	@Test
	public void testFileNames() {
		Guideline guideline = Guidelines.ofFile("corporate", "10_tone-of-voice.default.md");
		assertThat(guideline.id()).isEqualTo("corporate/tone-of-voice.default");
		assertThat(guideline.name()).isEqualTo("tone-of-voice");
		assertThat(guideline.isDefault()).isTrue();
		assertThat(guideline.priority()).isEqualTo(10);

		Guideline plain = Guidelines.ofFile("corporate", "seo.md");
		assertThat(plain.id()).isEqualTo("corporate/seo");
		assertThat(plain.isDefault()).isFalse();
		assertThat(plain.priority()).isNull();

		assertThat(Guidelines.ofFile("corporate", "notes.txt")).isNull();
		assertThat(Guidelines.ofFile("corporate", ".hidden.md")).isNull();
		assertThat(Guidelines.fileName("tone", true, 10)).isEqualTo("10_tone.default.md");
		assertThat(Guidelines.fileName("tone", false, null)).isEqualTo("tone.md");
	}

	@Test
	public void testPackageOf() {
		assertThat(Guidelines.packageOf("corporate/seo")).isEqualTo("corporate");
		for (String id : List.of("seo", "/seo", "corporate/", "a/b/c")) {
			assertThatThrownBy(() -> Guidelines.packageOf(id)).as(id).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	public void testSortedKeepsTheFirstOfOneId() {
		List<Guideline> sorted = Guidelines.sorted(List.of(Guidelines.ofFile("p", "seo.md"),
				Guidelines.ofFile("p", "10_seo.md"), Guidelines.ofFile("p", "5_tone.md"),
				Guidelines.ofFile("p", "a.md")));
		assertThat(sorted).extracting(Guideline::fileName).containsExactly("5_tone.md", "10_seo.md", "a.md");
	}

	@Test
	public void testResults() {
		Map<String, Object> list = assertValidOutput(new ListGuidelinesTool(),
				new ListGuidelinesTool.Result(List.of(Guidelines.ofFile("p", "10_tone.default.md"))));
		@SuppressWarnings("unchecked")
		Map<String, Object> item = ((List<Map<String, Object>>) list.get("items")).get(0);
		assertThat(item).containsEntry("package", "p").containsEntry("default", true).containsEntry("priority", 10)
				.doesNotContainKey("fileName");

		assertValidOutput(new GetGuidelineTool(), new GetGuidelineTool.Result("p/n", "p", "n", false, null, "text"));
		assertValidOutput(new CreateGuidelineTool(), new CreateGuidelineTool.Result("p/n"));
		assertValidOutput(new UpdateGuidelineTool(), new UpdateGuidelineTool.Result("p/n"));
		assertValidOutput(new DeleteGuidelineTool(), new DeleteGuidelineTool.Result("p/n", true));
	}
}
