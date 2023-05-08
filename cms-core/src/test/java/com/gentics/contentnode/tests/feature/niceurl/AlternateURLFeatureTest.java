package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
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
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for setting alternate URLs
 */
@GCNFeature(set = { Feature.NICE_URLS, Feature.MULTITHREADED_PUBLISHING })
@RunWith(value = Parameterized.class)
public class AlternateURLFeatureTest extends AbstractNiceURLTest {
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
	 * Test create object with alternate URLs
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/one", "/two", "/three", "/four");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/four", "/one", "/three", "/two");
	}

	/**
	 * Test create page with alternate URLs and publish it.
	 * @throws Exception
	 */
	@Test(timeout = 30_000)
	public void testPublish() throws Exception {
		// This test only applies to pages.
		if (test != TestedType.page) {
			return;
		}

		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/one", "/two", "/three", "/four");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
			.containsExactly("/four", "/one", "/three", "/two");

		if (object instanceof Page) {
			update((Page) object, Page::publish);

			operate(() -> {
				try {
					testContext.publish(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	/**
	 * Test setting alternate URLs with an update
	 * @throws NodeException
	 */
	@Test
	public void testSetWithUpdate() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template);
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs").isEmpty();

		object = update(object, o -> {
			o.setAlternateUrls("/five", "/six", "/seven", "/eight");
		});
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/eight", "/five", "/seven", "/six");
	}

	/**
	 * Test changing alternate URLs with an update
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/one", "/two");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/one", "/two");

		object = update(object, o -> {
			o.setAlternateUrls("/two", "/three");
		});
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/three", "/two");
	}

	/**
	 * Test setting an alternate URL to /
	 * @throws NodeException
	 */
	@Test
	public void testSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "/", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/ok");
	}

	/**
	 * Test setting an alternate URL to an empty String
	 * @throws NodeException
	 */
	@Test
	public void testEmpty() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/ok");
	}

	/**
	 * Test setting an alternate URL to null
	 * @throws NodeException
	 */
	@Test
	public void testNull() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", null, "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/ok");
	}

	/**
	 * Test setting an alternate URL that does not begin with a /
	 * @throws NodeException
	 */
	@Test
	public void testStartSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "bla", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/bla", "/ok");
	}

	/**
	 * Test setting an alternate URL that ends with a /
	 * @throws NodeException
	 */
	@Test
	public void testEndSlash() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "/bla/", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/bla", "/ok");
	}

	/**
	 * Test setting an alternate URL that contains a space
	 * @throws NodeException
	 */
	@Test
	public void testSpace() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "/bla bla", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/bla-bla", "/ok");
	}

	/**
	 * Test setting a nice URL with leading and trailing whitespace
	 * @throws NodeException
	 */
	@Test
	public void testTrim() throws NodeException {
		NodeObjectWithAlternateUrls object = createObjectWithAlternateUrls(test, folder, template, "/ok", "    bla   ", "/also/ok");
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, object)).as("Alternate URLs")
				.containsExactly("/also/ok", "/bla", "/ok");
	}

	/**
	 * Test versioning
	 * @throws NodeException
	 */
	@Test
	public void testVersioning() throws NodeException {
		if (test != TestedType.page) {
			return;
		}

		Page page = supply(t -> {
			t.setTimestamp(1000);
			return create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder.getId());
				p.setName("Page");
				p.setAlternateUrls("/one", "/two", "/three");
			});
		});

		page = execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(2000);
			return update(p, upd -> {
				upd.setAlternateUrls("/one", "/three");
			});
		}, page);

		page = execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(3000);
			return update(p, upd -> {
				upd.setAlternateUrls("/three", "/one", "/four");
			});
		}, page);

		page = execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(4000);
			return update(p, upd -> {
				upd.setAlternateUrls();
			});
		}, page);

		page = execute(p -> {
			TransactionManager.getCurrentTransaction().setTimestamp(5000);
			return update(p, upd -> {
				upd.setAlternateUrls("/four", "/three", "/two", "/one");
			});
		}, page);

		consume(p -> {
			assertThat(p).hasVersions(
				new NodeObjectVersion().setNumber("0.5").setDate(5).setCurrent(true),
				new NodeObjectVersion().setNumber("0.4").setDate(4),
				new NodeObjectVersion().setNumber("0.3").setDate(3),
				new NodeObjectVersion().setNumber("0.2").setDate(2),
				new NodeObjectVersion().setNumber("0.1").setDate(1)
			);
		}, page);

		assertPageVersionAlternateUrls(page, 1, "/one", "/two", "/three");
		assertPageVersionAlternateUrls(page, 2, "/one", "/three");
		assertPageVersionAlternateUrls(page, 3, "/one", "/three", "/four");
		assertPageVersionAlternateUrls(page, 4);
		assertPageVersionAlternateUrls(page, 5, "/one", "/two", "/three", "/four");
	}


}
