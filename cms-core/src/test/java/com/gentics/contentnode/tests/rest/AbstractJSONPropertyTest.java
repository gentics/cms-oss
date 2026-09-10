package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.testutils.DBTestContext;

public abstract class AbstractJSONPropertyTest {

	protected static final String PART_KEYWORD = "json";
	protected static final String CONSTRUCT_KEYWORD = "construct";
	protected static final String TAG_KEYWORD = "json";
	protected static final String OBJPROP_KEYWORD = "object." + TAG_KEYWORD;
	protected static final String RANDOM_JSON = "{\"whatever\":\"wherever\"}";

	protected static Node node;
	protected static Template template;
	protected static Integer constructId;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Entry<String, String> schemaRestriction : Map.of(
				"", RANDOM_JSON,
				"{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", "[\"one\", \"two\", \"three\"]",
				"{\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"},\"lastName\":{\"type\":\"string\"},\"middleName\":{\"type\":\"string\"}},\"required\":[\"firstName\",\"lastName\"]}", "{\"firstName\":\"Mickey\", \"lastName\":\"Mouse\"}"
		).entrySet()) {
			data.add(new Object[] { schemaRestriction.getKey(), schemaRestriction.getValue() });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		// basic setup
		node = supply(() -> createNode());
		constructId = supply(() -> createConstruct(node, JSONPartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD));
		template = supply(() -> {
			Template template = createTemplate(node.getFolder(), "Template");
			template.save();
			return template;
		});		
	}
}
