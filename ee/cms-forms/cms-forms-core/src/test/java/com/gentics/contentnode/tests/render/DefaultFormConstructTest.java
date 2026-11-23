package com.gentics.contentnode.tests.render;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createForm;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.init.SyncDefaultPackage;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.rest.model.PortalType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for rendering the default form construct "gtxform" from the DefaultElements package,
 * which renders the form for the portal type of the node
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
@ConfigBy(FormTypeConfigurationModifier.class)
public class DefaultFormConstructTest {
	/**
	 * Start of the form rendered for the Java portal
	 */
	public final static String JAVA_START = "<eval>{";

	/**
	 * Start of the form rendered for the PHP portal
	 */
	public final static String PHP_START = "@";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	protected static Node master;

	protected static Node channel;

	protected static Page formPage;

	protected static Page emptyPage;

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		operate(() -> new SyncDefaultPackage().execute());
		Construct construct = supply(t -> t.getObject(Construct.class,
				DBUtils.select("SELECT id FROM construct WHERE keyword = 'gtxform'", DBUtils.firstInt("id"))));
		assertThat(construct).as("Default form construct").isNotNull();

		master = supply(() -> createNode("master.host", "Master", PublishTarget.NONE, getLanguage("en")));
		channel = supply(() -> createChannel(master, "Channel", "channel.host", "/"));
		operate(() -> master.addConstruct(construct));

		Form form = supply(() -> createForm(master.getFolder(), "Form"));
		consume(f -> f.publish(), form);

		Template template = supply(() -> update(createTemplate(master.getFolder(), "Template"), tmpl -> {
			tmpl.setSource("<node form>");
			tmpl.getTemplateTags().put("form", create(TemplateTag.class, tt -> {
				tt.setConstructId(construct.getId());
				tt.setEnabled(true);
				tt.setName("form");
				tt.setPublic(true);
			}, false));
		}));

		formPage = supply(() -> update(createPage(master.getFolder(), template, "Form Page"), upd -> {
			getPartType(CmsFormPartType.class, upd.getTag("form"), "form").setTarget(form);
		}));
		emptyPage = supply(() -> createPage(master.getFolder(), template, "Empty Page"));
	}

	@After
	public void cleanup() throws NodeException {
		setPortalType(master, null);
		setPortalType(channel, null);
	}

	/**
	 * Test rendering for a node with portal type Java
	 * @throws NodeException
	 */
	@Test
	public void testJava() throws NodeException {
		setPortalType(master, PortalType.Java);
		assertThat(render(formPage, null)).as("Rendered form").startsWith(JAVA_START);
	}

	/**
	 * Test rendering for a node with portal type PHP
	 * @throws NodeException
	 */
	@Test
	public void testPhp() throws NodeException {
		setPortalType(master, PortalType.PHP);
		assertThat(render(formPage, null)).as("Rendered form").startsWith(PHP_START).contains("$formsCookie");
	}

	/**
	 * Test that a node without portal type falls back to Java
	 * @throws NodeException
	 */
	@Test
	public void testNotSet() throws NodeException {
		assertThat(render(formPage, null)).as("Rendered form").startsWith(JAVA_START);
	}

	/**
	 * Test that a page rendered in a channel uses the portal type of the channel, not the one of the master
	 * @throws NodeException
	 */
	@Test
	public void testChannel() throws NodeException {
		setPortalType(master, PortalType.Java);
		setPortalType(channel, PortalType.PHP);

		assertThat(render(formPage, channel)).as("Form rendered in channel").startsWith(PHP_START);
		assertThat(render(formPage, null)).as("Form rendered in master").startsWith(JAVA_START);
	}

	/**
	 * Test that a tag without selected form renders the hint
	 * @throws NodeException
	 */
	@Test
	public void testNoForm() throws NodeException {
		setPortalType(master, PortalType.PHP);
		assertThat(render(emptyPage, null)).as("Rendered tag without form").isEqualTo("No form selected");
	}

	/**
	 * Set the portal type of the given node
	 * @param node node
	 * @param portalType portal type (may be null)
	 * @throws NodeException
	 */
	protected void setPortalType(Node node, PortalType portalType) throws NodeException {
		supply(() -> update(node, n -> {
			n.setPortalTypeProperty("");
			n.setPortalType(portalType);
		}));
	}

	/**
	 * Render the page in publish mode
	 * @param page page
	 * @param channel channel to render the page in, null for the master
	 * @return rendered content
	 * @throws NodeException
	 */
	protected String render(Page page, Node channel) throws NodeException {
		try (Trx trx = new Trx(); ChannelTrx cTrx = new ChannelTrx(channel); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			String rendered = trx.getTransaction().getObject(page).render();
			trx.success();
			return rendered;
		}
	}
}
