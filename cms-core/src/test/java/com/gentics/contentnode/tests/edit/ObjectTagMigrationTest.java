package com.gentics.contentnode.tests.edit;

import static com.gentics.testutils.GenericTestUtils.getRandomStr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.TestedObjectTagContainer;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for automatic migration of object tags to new constructs
 */
@RunWith(value = Parameterized.class)
public class ObjectTagMigrationTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Template template;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedObjectTagContainer type : TestedObjectTagContainer.values()) {
			data.add(new Object[] {type});
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Template"));
	}

	@Parameter(0)
	public TestedObjectTagContainer type;

	private int oldConstructId;

	private int newConstructId;

	private ObjectTagDefinition definition;

	@Before
	public void setup() throws NodeException {
		oldConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, getRandomStr(10), "part"));
		newConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, getRandomStr(10), "part"));
		definition = Trx.supply(() -> ContentNodeTestDataUtils.createObjectPropertyDefinition(type.getTType(), oldConstructId, getRandomStr(10),
				getRandomStr(10)));
	}

	@Test
	public void test() throws NodeException {
		// create object of tested type
		ObjectTagContainer testedObject = Trx.supply(() -> type.create(node.getFolder(), template));

		// object tag must exist now
		ObjectTag objectTag = Trx.supply(() -> testedObject.getObjectTag(definition.getObjectTag().getName().substring("object.".length())));
		assertNotNull("Object Tag must exist", objectTag);
		assertEquals("Check construct ID", oldConstructId, objectTag.getConstructId().intValue());

		// change the construct of the definition
		Trx.operate(() -> {
			definition = TransactionManager.getCurrentTransaction().getObject(definition, true);
			definition.getObjectTag().setConstructId(newConstructId);
			definition.save();

			definition = TransactionManager.getCurrentTransaction().getObject(definition);
		});

		// object tag from original tested object
		objectTag = Trx.supply(() -> type.reload(testedObject, false).getObjectTag(definition.getObjectTag().getName().substring("object.".length())));
		assertNotNull("Object Tag must exist", objectTag);
		// assert that it still uses the old construct
		assertEquals("Check construct ID", oldConstructId, objectTag.getConstructId().intValue());

		// object tag from editable copy of tested object
		objectTag = Trx.supply(() -> type.reload(testedObject, true).getObjectTag(definition.getObjectTag().getName().substring("object.".length())));
		assertNotNull("Object Tag must exist", objectTag);
		// assert that it uses the new construct now
		assertEquals("Check construct ID", newConstructId, objectTag.getConstructId().intValue());
	}
}
