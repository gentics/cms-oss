package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Node.UrlRenderWay;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.i18n.LanguageProviderFactory;

/**
 * Sandbox test for rendering page URLs in different szenarios
 */
@RunWith(value = Parameterized.class)
@GCNFeature(unset = { Feature.TAG_IMAGE_RESIZER })
public class CustomUrlRenderSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Linkway
	 */
	protected UrlRenderWay urlRenderWay;

	/**
	 * Source page
	 */
	protected Page sourcePage;

	/**
	 * Target page
	 */
	protected Page targetPage;


	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: urlRenderWay {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testData = new Vector<Object[]>();

		for (UrlRenderWay urlRenderWay : UrlRenderWay.values()) {
			testData.add(new Object[] { urlRenderWay });
		}

		return testData;
	}

	/**
	 * Create a test instance
	 * 
	 * @param urlRenderWay
	 *            The URL render way
	 */
	public CustomUrlRenderSandboxTest(UrlRenderWay urlRenderWay) {
		this.urlRenderWay = urlRenderWay;
	}

	@Before
	public void setup() throws Exception {
		LanguageProviderFactory.reset();
		setLinkWay("abs");
	}

	/**
	 * Test rendering URLs of pages and files in publish mode
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishModeUrlRendering() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create node, template, source and target page
		Node node = ContentNodeTestDataUtils.createNode(
				"node", "Node", PublishTarget.FILESYSTEM);
		node  = t.getObject(node, true);
		node.setUrlRenderWayPages(this.urlRenderWay.getValue());
		node.setUrlRenderWayFiles(this.urlRenderWay.getValue());
		node.save();
		t.commit(false);

		ContentFile file = (ContentFile)ContentNodeTestDataUtils.createFile(
				node.getFolder(), "testfile", "content".getBytes());

		Template template = createTemplate(node.getFolder());
		targetPage = createPageWithLink("target", node.getFolder(), template, null, file);
		sourcePage = createPageWithLink("source", node.getFolder(), template, targetPage, file);

		// run a publish process
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS,
				testContext.getContext().publish(false).getReturnCode());

		// get the published content of the source page
		final List<String> sources = new ArrayList<String>();

		DBUtils.executeStatement("SELECT source FROM publish WHERE page_id = ? AND active = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, ObjectTransformer.getInt(sourcePage.getId(), 0));
				stmt.setInt(2, 1);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					sources.add(rs.getString("source"));
				}
			}
		});

		// check whether we found the expected page source
		assertEquals("Check # of found page sources", 1, sources.size());

		String[] urls = sources.get(0).split("\\|");
		assertEquals("Check source page content", getExpectedPageURL(), urls[0]);
		assertEquals("Check source page content", getExpectedFileURL(file), urls[1]);
	}

	/**
	 * Get the expected page URL
	 * 
	 * @return expected page URL
	 */
	protected String getExpectedPageURL() throws Exception {
		boolean portalUrl = false;
		boolean includeHost = false;
		Node sourceNode = sourcePage.getFolder().getNode();
		Node targetNode = targetPage.getFolder().getNode();

		// if the target node publishes only into the contentmap, plinks will be rendered regardless of the linkway
		/*if (targetNode.doPublishContentmap() && !targetNode.doPublishFilesystem()) {
			return "<plink id=\"10007." + targetPage.getId() + "\" />";
		}*/

		switch (urlRenderWay) {
		case STATIC_WITH_DOMAIN:
			includeHost = true;
			break;
		case STATIC_WITHOUT_DOMAIN:
			includeHost = false;
			break;
		case STATIC_DYNAMIC:
			if (!sourceNode.equals(targetNode)) {
				includeHost = true;
			}
			break;
		case PORTAL:
			portalUrl = true;
			break;
		}

		if (portalUrl) {
			//check for dependency
			return "<plink id=\"10007." + targetPage.getId() + "\" />";
		} else {
			StringBuffer url = new StringBuffer();

			if (includeHost) {
				url.append("http://").append(targetNode.getHostname());
			}
			url.append(targetNode.getPublishDir()).append(targetPage.getFolder().getPublishDir()).append(targetPage.getFilename());
			return url.toString();
		}
	}

	/**
	 * Get the expected page URL
	 * 
	 * @return expected page URL
	 */
	protected String getExpectedFileURL(ContentFile file) throws Exception {
		boolean portalUrl = false;
		boolean includeHost = false;
		Node sourceNode = sourcePage.getFolder().getNode();
		Node targetNode = targetPage.getFolder().getNode();

		// if the target node publishes only into the contentmap, plinks will be rendered regardless of the linkway
		/*if (targetNode.doPublishContentmap() && !targetNode.doPublishFilesystem()) {
			return "<plink id=\"10008." + targetPage.getId() + "\" />";
		}*/

		switch (urlRenderWay) {
		case STATIC_WITH_DOMAIN:
			includeHost = true;
			break;
		case STATIC_WITHOUT_DOMAIN:
			includeHost = false;
			break;
		case STATIC_DYNAMIC:
			if (!sourceNode.equals(targetNode)) {
				includeHost = true;
			}
			break;
		case PORTAL:
			portalUrl = true;
			break;
		}

		if (portalUrl) {
			//check for dependency
			return "<plink id=\"10008." + file.getId() + "\" />";
		} else {
			StringBuffer url = new StringBuffer();

			if (includeHost) {
				url.append("http://").append(targetNode.getHostname());
			}
			url.append(targetNode.getPublishDir()).append(file.getFolder().getPublishDir()).append(file.getFilename());
			return url.toString();
		}
	}

	/**
	 * Set a new linkway
	 * 
	 * @param linkWay
	 *            linkway
	 * @throws NodeException
	 */
	public void setLinkWay(String linkWay) throws NodeException {
		NodeConfigRuntimeConfiguration.getPreferences().setProperty("contentnode.linkway", linkWay);
	}

	/**
	 * Create the template
	 * 
	 * @param folder
	 *            folder where to create the template
	 * @return template
	 * @throws Exception
	 */
	public Template createTemplate(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a template
		Template template = t.createObject(Template.class);

		template.setName("Template linking to a page");
		template.setMlId(1);
		template.setSource("<node pageurl>|<node fileurl>");

		int constructId = createUrlConstruct(FileURLPartType.class, folder.getNode());

		TemplateTag tag = t.createObject(TemplateTag.class);
		tag.setConstructId(constructId);
		tag.setEnabled(true);
		tag.setPublic(true);
		tag.setName("fileurl");
		template.getTemplateTags().put("fileurl", tag);

		constructId = createUrlConstruct(PageURLPartType.class, folder.getNode());

		tag = t.createObject(TemplateTag.class);
		tag.setConstructId(constructId);
		tag.setEnabled(true);
		tag.setPublic(true);
		tag.setName("pageurl");
		template.getTemplateTags().put("pageurl", tag);

		template.setFolderId(folder.getId());
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a page, possibly linking to another page.
	 * The page is published and the transaction is commited (but not closed)
	 * 
	 * @param name        Name of the page
	 * @param folder      Folder
	 * @param template    Template
	 * @param linkTarget  link target (may be null)
	 * @param contentFile File to create a link to
	 * @return
	 * @throws Exception
	 */
	public Page createPageWithLink(String name, Folder folder, Template template,
			Page linkTarget, ContentFile contentFile) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);

		page.setName(name);
		page.setFolderId(folder.getId());
		page.setTemplateId(template.getId(), true);

		// Page URL tag
		Tag tag = page.getContent().getTag("pageurl");
		Value value = tag.getValues().iterator().next();
		PartType partType = value.getPartType();

		if (partType instanceof UrlPartType) {
			((PageURLPartType) partType).setTargetPage(linkTarget);
		} else {
			fail("Part type is of wrong type");
		}

		// File URL tag
		tag = page.getContent().getTag("fileurl");
		value = tag.getValues().iterator().next();
		partType = value.getPartType();

		if (partType instanceof UrlPartType) {
			((FileURLPartType) partType).setTargetFile(contentFile);
		} else {
			fail("Part type is of wrong type");
		}

		page.save();
		page.publish();
		t.commit(false);

		return t.getObject(Page.class, page.getId());
	}

	public <T extends UrlPartType> int createUrlConstruct(
			Class<T> urlPartType, Node nodeToLink) throws NodeException {
		return ContentNodeTestDataUtils.createConstruct(nodeToLink, urlPartType, "url", "url");
	}
}
