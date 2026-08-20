package com.gentics.contentnode.tests.publish.mesh.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.Blocks;

/**
 * Test cases for merging ui-schema changes with modifications made with formgrid
 */
@RunWith(Parameterized.class)
public class UiSchemaMergeTest {
	protected static ObjectMapper mapper = new ObjectMapper();

	protected static FormTypeConfiguration config;

	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = List
				.of("empty", "add_element", "remove_element", "reorder_elements", "reorder_elements_with_block",
						"move_to_page", "change_label", "change_description", "aggregate/add_element",
						"aggregate/change_description", "aggregate/change_label", "aggregate/remove_element",
						"aggregate/reorder_elements", "aggregate/reorder_elements_with_block", "complex")
				.stream().map(name -> new Object[] { name }).collect(Collectors.toList());
		return data;
	}

	@BeforeClass
	public static void setupOnce() {
		config = new FormTypeConfiguration(Map.of(), "test_plugin");
		config.blocks = new Blocks(Map.of("spacer", new BlockConfig(Map.of())));
	}

	@Parameter(0)
	public String test;

	protected JsonNode old;

	protected JsonNode stored;

	protected JsonNode merge;

	protected JsonNode expected;

	@Before
	public void setup() throws IOException {
		try (InputStream in = getClass().getResourceAsStream("merge/%s/old.json".formatted(test))) {
			old = mapper.readTree(in);
		}
		try (InputStream in = getClass().getResourceAsStream("merge/%s/stored.json".formatted(test))) {
			stored = mapper.readTree(in);
		}
		try (InputStream in = getClass().getResourceAsStream("merge/%s/merge.json".formatted(test))) {
			merge = mapper.readTree(in);
		}
		try (InputStream in = getClass().getResourceAsStream("merge/%s/expected.json".formatted(test))) {
			expected = mapper.readTree(in);
		}
	}

	@Test
	public void test() {
		ObjectNode merged = new FormTransformer().mergeUiSchemaAndFormGrid(config,
				old.deepCopy().withObjectProperty("ui-schema"), merge.deepCopy().withObjectProperty("ui-schema"),
				stored.deepCopy().withObjectProperty("ui-schema"));

		assertThat(merged).as("Merged ui-schema").isEqualTo(expected.deepCopy().withObjectProperty("ui-schema"));
	}
}
