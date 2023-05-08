package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for nice URL
 */
@GCNFeature(set = { Feature.NICE_URLS })
@RunWith(value = Parameterized.class)
public class NiceURLFeatureTest extends AbstractNiceURLTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test template
	 */
	private static Template template;

	/**
	 * Test folder
	 */
	private Folder folder;

	@Parameter(0)
	public TestedType test;

	@Parameters(name = "{index}: test {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType test : Arrays.asList(TestedType.page, TestedType.file, TestedType.image)) {
			data.add(new Object[] { test });
		}
		return data;
	}

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			t.commit();
		}
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Create test folder
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		folder = Trx.supply(() -> createFolder(node.getFolder(), "Testfolder"));
	}

	/**
	 * Delete the test folder (and everything in it)
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		Trx.operate(() -> folder.delete(true));
		folder = null;
	}

	/**
	 * Test creating an object with a nice URL
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "/feature/is/activated");

		assertThat(object.getNiceUrl()).as("Nice URL").isEqualTo("/feature/is/activated");
		assertThat(NodeObjectWithAlternateUrls.NAME.apply(object.getNiceUrl())).as("Nice URL name").isEqualTo("activated");
		assertThat(NodeObjectWithAlternateUrls.PATH.apply(object.getNiceUrl())).as("Nice URL path").isEqualTo("/feature/is/");
	}

	/**
	 * Test updating an object with a nice URL
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, null);

		object = update(object, p -> {
			p.setNiceUrl("/feature/is/activated");
		});

		assertThat(object.getNiceUrl()).as("Nice URL").isEqualTo("/feature/is/activated");
		assertThat(NodeObjectWithAlternateUrls.NAME.apply(object.getNiceUrl())).as("Nice URL name").isEqualTo("activated");
		assertThat(NodeObjectWithAlternateUrls.PATH.apply(object.getNiceUrl())).as("Nice URL path").isEqualTo("/feature/is/");
	}

	/**
	 * Test setting a nice URL to /
	 * @throws NodeException
	 */
	@Test
	public void testSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "/");

		assertThat(object.getNiceUrl()).as("Corrected nice URL").isNull();
	}

	/**
	 * Test setting a nice URL to an empty String
	 * @throws NodeException
	 */
	@Test
	public void testEmpty() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isNull();
	}

	/**
	 * Test setting a nice URL to null
	 * @throws NodeException
	 */
	@Test
	public void testNull() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "/bla");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isEqualTo("/bla");

		object = update(object, p -> {
			p.setNiceUrl(null);
		});

		assertThat(object.getNiceUrl()).as("Corrected nice URL").isNull();
	}

	/**
	 * Test setting a nice URL that does not begin with a /
	 * @throws NodeException
	 */
	@Test
	public void testStartSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "bla");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isEqualTo("/bla");
	}

	/**
	 * Test setting a nice URL that ends with a /
	 * @throws NodeException
	 */
	@Test
	public void testEndSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "/bla/");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isEqualTo("/bla");
	}

	/**
	 * Test setting a nice URL that contains a space
	 * @throws NodeException
	 */
	@Test
	public void testSpace() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "/bla bla");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isEqualTo("/bla-bla");
	}

	/**
	 * Test setting a nice URL with leading and trailing whitespace
	 * @throws NodeException
	 */
	@Test
	public void testTrim() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithNiceUrl(test, folder, template, "   bla   ");
		assertThat(object.getNiceUrl()).as("Corrected nice URL").isEqualTo("/bla");
	}
}
