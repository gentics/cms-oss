package com.gentics.contentnode.tests.rendering;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test cases for rendering corrupted node tags in templates
 * corrupted node tags are tags which are opened but never closed. eg: ('<node ')
 */
@RunWith(Parameterized.class)
public class CorruptedNodeTagsRenderTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();


	private static final String CONSTRUCT_PART_NAME = "testpart";
	private static final String TEMPLATE_TAG_NAME_1 = "templatetag1";
	private static final String TEMPLATE_TAG_NAME_2 = "templatetag2";
	private static final String VALUE_1 = "This is the first value ";
	private static final String VALUE_2 = "and this is the second";

	private static Page page;

	private static Template template;

	/**
	 * Get the test parameters.
	 * The first parameter describes the test, the second is the source for the template and the third
	 * is the expected output of the rendered page.
	 *
	 * @return collection of test parameter sets
	 */
	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{
				"No corrupted NodeTag",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + ">",
				VALUE_1 + VALUE_2
			}, {
				"Corrupted NodeTag at the end with space",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + "><node ",
				VALUE_1 + VALUE_2 + "<node "
			}, {
				"Corrupted NodeTag at the end with multiple spaces",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + "><node    ",
				VALUE_1 + VALUE_2 + "<node    "
			}, {
				"Corrupted NodeTag at the end without closing bracket",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + "><node notexistingtagname",
				VALUE_1 + VALUE_2 + "<node notexistingtagname"
			}, {
				"Corrupted NodeTag at the end without space",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + "><node",
				VALUE_1 + VALUE_2 + "<node"
			}, {
				"Corrupted NodeTag at the beginning with space",
				"<node <node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + ">",
				VALUE_2
			}, {
				"Corrupted NodeTag at the beginning without space",
				"<node<node " + TEMPLATE_TAG_NAME_1 + "><node " + TEMPLATE_TAG_NAME_2 + ">",
				"<node" + VALUE_1 + VALUE_2

			}, {
				"corrupted NodeTag in the middle with space",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node <node " + TEMPLATE_TAG_NAME_2 + ">",
				VALUE_1
			}, {
				"corrupted NodeTag in the middle without space",
				"<node " + TEMPLATE_TAG_NAME_1 + "><node<node " + TEMPLATE_TAG_NAME_2 + ">",
				VALUE_1 + "<node" + VALUE_2
			}
		});
	}

	@Parameterized.Parameter
	public String testDescription;

	@Parameterized.Parameter(1)
	public String templateSource;

	@Parameterized.Parameter(2)
	public String expectedOutput;

	@BeforeClass
	public static void setupClass() throws NodeException {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = ContentNodeTestDataUtils.createNode();
			int constructId = ContentNodeTestDataUtils.createConstruct(node, NormalTextPartType.class, "testconstruct", CONSTRUCT_PART_NAME);
			TemplateTag templateTag1 = ContentNodeTestDataUtils.createTemplateTag(constructId, TEMPLATE_TAG_NAME_1, true, true);
			TemplateTag templateTag2 = ContentNodeTestDataUtils.createTemplateTag(constructId, TEMPLATE_TAG_NAME_2, true, true);
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "", "test", templateTag1, templateTag2);
			Template editableTemplate = t.getObject(template, true);

			Value val = editableTemplate.getTag(TEMPLATE_TAG_NAME_1).getValues().getByKeyname(CONSTRUCT_PART_NAME);
			val.setValueText(VALUE_1);

			val = editableTemplate.getTag(TEMPLATE_TAG_NAME_2).getValues().getByKeyname(CONSTRUCT_PART_NAME);
			val.setValueText(VALUE_2);
			editableTemplate.save();
			page = ContentNodeTestDataUtils.createPage(node.getFolder(), template, "TestPage");
		});
	}

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Template editableTemplate = t.getObject(template, true);
			editableTemplate.setSource(templateSource);
			editableTemplate.save();
		});
	}

	@Test
	public void renderNodeTagsTest() throws NodeException {
		String renderedContent = Trx.supply(() -> {
			try (RenderTypeTrx rtx = new RenderTypeTrx(RenderType.EM_PREVIEW)){
				return page.render();
			}
		});
		Assert.assertEquals("The rendered output should match the expected output for the current template.", expectedOutput, renderedContent);
	}
}
