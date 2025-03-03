package com.gentics.contentnode.tests.resolving;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.StackResolvableNodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.resolving.StackResolver;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for resolving properties
 */
@RunWith(value = Parameterized.class)
public class PropertyResolvingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Template template;

	private static Folder folder;

	private static Page germanTestPage;

	private static Page englishPage;

	/**
	 * Generate some test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("test.host", "Test Node", PublishTarget.NONE, getLanguage("de"), getLanguage("en")));
		template = supply(() -> createTemplate(node.getFolder(), "Test Template"));

		folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Test Folder");
			f.setPublishDir("/test/folder/path");
		}).build();

		germanTestPage = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("German Test Page");
			p.setFilename("german.html");
			p.setLanguage(getLanguage("de"));
		}).publish().build();

		englishPage = create(Page.class, p -> {
			p.setFolder(node, folder);
			p.setTemplateId(template.getId());
			p.setName("English Test Page");
			p.setFilename("english.html");
			p.setLanguage(getLanguage("en"));
			p.setContentsetId(germanTestPage.getContentsetId());
		}).publish().build();
	}

	@Parameters(name = "{index}: object {0}, property {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {TestObject.germanPage, "page.languageset.pages.de", TestObject.germanPage});
		data.add(new Object[] {TestObject.germanPage, "page.languageset.pages.en", TestObject.englishPage});
		data.add(new Object[] {TestObject.germanPage, "page.languageset.pages.fr", null});
		return data;
	}

	/**
	 * Tested object (base object for resolving)
	 */
	@Parameter(0)
	public TestObject object;

	/**
	 * Resolved property
	 */
	@Parameter(1)
	public String property;

	/**
	 * Expected value
	 */
	@Parameter(2)
	public Object expectedValue;

	/**
	 * When the {@link #expectedValue} is an instance of {@link TestObject}, get the object via {@link TestObject#get()}
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		if (expectedValue instanceof TestObject) {
			expectedValue = ((TestObject) expectedValue).get();
		}
	}

	/**
	 * Test resolving the property
	 * @throws NodeException
	 */
	@Test
	public void testResolving() throws NodeException {
		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish(object.get())) {
			StackResolver resolver = new StackResolver();
			resolver.push(object.get());

			assertThat(resolver.resolve(property)).as("Resolved property").isEqualTo(expectedValue);

			trx.success();
		}
	}

	/**
	 * Enum for tested objects
	 */
	protected static enum TestObject {
		/**
		 * German page
		 */
		germanPage(() -> PropertyResolvingTest.germanTestPage),

		/**
		 *  English page
		 */
		englishPage(() -> PropertyResolvingTest.englishPage)
		;

		/**
		 * Supplier for the tested object
		 */
		private Supplier<StackResolvableNodeObject> getter;

		/**
		 * Create instance
		 * @param getter getter
		 */
		private TestObject(Supplier<StackResolvableNodeObject> getter) {
			this.getter = getter;
		}

		/**
		 * Get the tested object
		 * @return tested object
		 * @throws NodeException
		 */
		protected StackResolvableNodeObject get() throws NodeException {
			return getter.supply();
		}
	}
}
