package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;

/**
 * Page tests for inherit page content and properties
 * 
 */
public class InheritPageRenderChannelSandboxTest extends AbstractMultichannellingSandboxTest {

	private Template template;
	private Node masterNode;
	private Folder masterFolder;
	private Page masterPage;
	private Page localizedPage;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		Transaction transaction = TransactionManager.getCurrentTransaction();

		masterNode = transaction.getObject(Node.class, MASTER_ID);

		initContentRepository(transaction.getObject(Node.class, channelId));

		masterFolder = masterNode.getFolder();
		template = createTemplate(masterNode);

		masterPage = createPage("Master Page");
		localizedPage = localize(masterPage, "Channel Page localized");
	}

	/**
	 * Test inherited Content of pages.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageInheritedContent() throws Exception {
		boolean inherited = true;
		boolean tagmap = true;
		PageRenderResponse response = createRenderResponse(tagmap, inherited);

		assertEquals("Check inherited content response", "Master Page -- Test Channel", response.getInheritedContent());
		assertEquals("Check content response", "Channel Page localized -- Test Channel", response.getContent());
	}
	
	/**
	 * Test inherited Properties pages.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageInheritedProperties() throws Exception {
		boolean inherited = true;
		boolean tagmap = true;
		PageRenderResponse response = createRenderResponse(tagmap, inherited);

		assertEquals("Checking number of tagmap entries", 2, response.getProperties().size());
		assertEquals("Checking number of inherited tagmap entries", 2, response.getInheritedProperties().size());

		assertEquals("Node name of localized page", "Test Channel", response.getProperties().get("test"));
		assertEquals("Node name of inherited page", "Test Channel", response.getInheritedProperties().get("test"));

		assertEquals("Name of localized page", "Channel Page localized", response.getProperties().get("name"));
		assertEquals("Name of inherited page", "Master Page", response.getInheritedProperties().get("name"));
	}
	
	/**
	 * Test no inherited Content is present when 'inherited' parameter is set to true
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageNoInheritedContent() throws Exception {
		boolean inherited = false;
		boolean tagmap = false;
		PageRenderResponse response = createRenderResponse(tagmap, inherited);

		assertEquals("Inherited content response not expected", null, response.getInheritedContent());
		assertEquals("Check content response", "Channel Page localized -- Test Channel", response.getContent());
		
		assertEquals("Inherited properties entries not expected", null, response.getProperties());
	}
	
	/**
	 * Test tagmap entries when 'inherited' parameter is set to false
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageNoInheritedContentWhenTagmap() throws Exception {
		boolean inherited = false;
		boolean tagmap = true;
		PageRenderResponse response = createRenderResponse(tagmap, inherited);

		assertEquals("Checking number of tagmap entries", 2, response.getProperties().size());
		assertEquals("Inherited properties entries not expected", null, response.getInheritedProperties());
	}


	/**
	 * Test no inherited properties is not set when 'inherited' property is set to true
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRenderPageNoInheritedProperties() throws Exception {
		boolean inherited = false;
		boolean tagmap = true;
		PageRenderResponse response = createRenderResponse(tagmap, inherited);

		assertEquals("Checking number of tagmap entries", 2, response.getProperties().size());
		assertEquals("Inherited tagmap entries not expected", null, response.getInheritedProperties());

		assertEquals("Node name of localized page", "Test Channel", response.getProperties().get("test"));
		assertEquals("Name of localized page", "Channel Page localized", response.getProperties().get("name"));
	}
	
	
	/**
	 * Create render response from the REST-API
	 * 
	 * @param tagmap True to include the tagmap entries
	 * @param inherited True to include the inherited content and properties
	 * @return Response
	 * @throws Exception
	 */
	private PageRenderResponse createRenderResponse(boolean tagmap, boolean inherited) throws Exception {
		PageRenderResponse response = getPageResource().render(localizedPage.getId().toString(), channelId, null, false, null, LinksType.frontend, tagmap, inherited, false, 0);
		assertResponseCode(response);
		return response;
	}

	/**
	 * Initializes the content repository for a specific node.
	 * 
	 * @param node
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws InsufficientPrivilegesException
	 * @throws TransactionException
	 */
	private void initContentRepository(Node node) throws NodeException, ReadOnlyException, InsufficientPrivilegesException, TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName("test");
		cr.setMultichannelling(false);

		createTagmapEntries(cr);

		cr.save();
		t.commit(false);

		DBUtils.executeUpdate("UPDATE contentrepository SET dbtype = ?, url = ?, username = ?, password = ? WHERE id = ?", new Object[] { "mysql",
				"jdbc:mariadb://localhost/test", "test", "", cr.getId() });

		node = t.getObject(Node.class, node.getId(), true);
		node.setContentrepositoryId(cr.getId());
		node.setPublishContentmap(true);
		
		node.save();
		t.commit(false);
	}

	/** 
	 * Create tag map entries for a content repository
	 * 
	 * @param cr Content repository
	 * @throws NodeException
	 * @throws ReadOnlyException
	 */
	private void createTagmapEntries(ContentRepository cr) throws NodeException, ReadOnlyException {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		TagmapEntry contentEntry = t.createObject(TagmapEntry.class);

		contentEntry.setMapname("test");
		contentEntry.setTagname("node.folder.name");
		contentEntry.setObject(10007);
		contentEntry.setAttributeTypeId(5);

		TagmapEntry pageNameEntry = t.createObject(TagmapEntry.class);

		pageNameEntry.setMapname("name");
		pageNameEntry.setTagname("page.name");
		pageNameEntry.setObject(10007);
		pageNameEntry.setOptimized(true);
		pageNameEntry.setAttributeTypeId(1);

		cr.getEntries().add(contentEntry);
		cr.getEntries().add(pageNameEntry);
	}

	/**
	 * Localize a page with the name of 'name'
	 * 
	 * @param masterPage Master Page
	 * @param name Name of the localized page
	 * @return
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws InsufficientPrivilegesException
	 * @throws TransactionException
	 */
	private Page localize(Page masterPage, String name) throws NodeException, ReadOnlyException, InsufficientPrivilegesException, TransactionException {
		Page localizedPage = (Page) masterPage.copy();

		localizedPage.setChannelInfo(channelId, masterPage.getChannelSetId());
		localizedPage.setName(name);
		localizedPage.save();
		TransactionManager.getCurrentTransaction().commit(false);
		return localizedPage;
	}

	/**
	 * Create a page with name 'name'
	 * 
	 * @param name Name of the page
	 * @return
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws InsufficientPrivilegesException
	 * @throws TransactionException
	 */
	private Page createPage(String name) throws NodeException, ReadOnlyException, InsufficientPrivilegesException, TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page masterPage = t.createObject(Page.class);

		masterPage.setFolderId(masterFolder.getId());
		masterPage.setName(name);
		masterPage.setTemplateId(template.getId());
		masterPage.save();
		t.commit(false);
		return masterPage;
	}

	/**
	 * Create Template for a node
	 * 
	 * @param master Node where to create the template.
	 * @return
	 * @throws NodeException
	 * @throws ReadOnlyException
	 * @throws InsufficientPrivilegesException
	 * @throws TransactionException
	 */
	private Template createTemplate(Node master) throws NodeException, ReadOnlyException, InsufficientPrivilegesException, TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		Template template = t.createObject(Template.class);

		template.setFolderId(masterFolder.getId());
		template.setMlId(1);
		template.setName("test render template");
		template.setSource("<node page.name> -- <node node.folder.name>");
		template.save();
		t.commit(false);

		return template;
	}
}
