package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.response.FolderExternalLinksResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;

/**
 * Tests for getting external links over the REST API
 */
public class GetExternalLinksSandboxTest {

	private static final String[] URLS_INSIDE_PAGE = new String[] {"http://www.gentics.com", "http://www.gentics.com", "", "#"};

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private static final String URL_PARTNAME = "url";

	/**
	 * ID of the root folder
	 */
	private static int ROOT_FOLDER_ID = 56;

	private static int MAX_NUM_PAGE = 30;

	private Integer subFolder1Id;

	/**
	 * Creates folders, subfolders and pages.
	 */
	private void createPages(int numPages) throws Exception {
		FolderResource folderResource = getFolderResource();
		FolderCreateRequest request = new FolderCreateRequest();
		FolderLoadResponse loadResponse;

		request.setMotherId(Integer.toString(ROOT_FOLDER_ID));

		request.setName("subFolder1");
		loadResponse = folderResource.create(request);

		subFolder1Id = loadResponse.getFolder().getId();

		request.setMotherId(String.valueOf(subFolder1Id));
		request.setName("subsubfolder");
		loadResponse = folderResource.create(request);

		Integer subsubfolderId = loadResponse.getFolder().getId();
		Template template = createTemplate(ROOT_FOLDER_ID);

		int construct = createConstruct(null, PageURLPartType.class, URL_PARTNAME, URL_PARTNAME);

		for (int i = 0; i < numPages; i++) {
			createUrlPage(construct, ROOT_FOLDER_ID, template, "Page number - " + i, new String[] {"http://root.com", "http://root.link.es", "", "   ", "asdf"});
		}

		createUrlPage(construct, subFolder1Id, template, "subfolder1 one page", URLS_INSIDE_PAGE);
		createUrlPage(construct, subsubfolderId, template, "subsubfolderId one page", new String[] {"http://subsubfolderId.com", "", "ftp://sdfsa"});
		createUrlPage(construct, subsubfolderId, template, "subsubfolderId another page", new String[] {"hp/:sd/d.com", "", "ftp://sdfsa"});
	}

	/**
	 * Tests to get the external links from all pages inside a folder
	 * @throws Exception
	 */
	@Test
	public void testGetExternalLinks() throws Exception {
		boolean recursive = false;
		createPages(MAX_NUM_PAGE);
		FolderExternalLinksResponse response = getFolderResource().getExternalLinks(ROOT_FOLDER_ID, recursive);

		assertEquals(MAX_NUM_PAGE, response.getPages().size());
	}

	/**
	 * Tests to get the external links from all pages inside a folder recursively
	 * @throws Exception
	 */
	@Test
	public void testGetExternalLinksRecursive() throws Exception {
		boolean recursive = true;
		int folderPages = 2;
		createPages(folderPages);
		FolderExternalLinksResponse response = getFolderResource().getExternalLinks(ROOT_FOLDER_ID, recursive);

		assertEquals(folderPages + 3, response.getPages().size());
	}

	/**
	 * Tests to get the external links from all pages inside a sufolder
	 * @throws Exception
	 */
	@Test
	public void testGetExternalLinksFromSubfolder() throws Exception {
		boolean recursive = false;
		int folderPages = 3;
		createPages(folderPages);
		FolderExternalLinksResponse response = getFolderResource().getExternalLinks(subFolder1Id, recursive);

		List<String> links = response.getPages().get(0).getLinks();

		assertEquals(1, response.getPages().size());
		assertEquals("subfolder1 one page", response.getPages().get(0).getPageName());
		assertArrayEquals(Arrays.asList(URLS_INSIDE_PAGE), links);
	}

	/**
	 * Asserts two list are equals: have the same size and same elements.
	 * @param expectedList
	 * @param actualList
	 */
	private void assertArrayEquals(List<String> expectedList, List<String> actualList) {
		assertEquals(expectedList.size(), actualList.size());
		for (String url : actualList) {
			assertTrue(expectedList.contains(url));
		}
	}

	/**
	 * Create the url page
	 * @param folder folder
	 * @param template template
	 * @param construcid 
	 * @param targetPage target page
	 * @return id of the url page
	 * @throws NodeException
	 */
	protected Page createUrlPage(int construcid, int folderId, Template template, String pageName, String[] strings) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setName(pageName);
		page.setTemplateId(template.getId());
		page.setFolderId(folderId);

		for (String string : strings) {
			ContentTag tag = page.getContent().addContentTag(construcid);
			getPartType(PageURLPartType.class, tag, URL_PARTNAME).setExternalTarget(string);
		}

		page.save();
		page.publish();
		t.commit(false);

		return page;
	}

	/**
	 * Create a construct with a single visible editable part
	 * @param node node
	 * @param clazz parttype class
	 * @param constructKeyword keyword of the construct
	 * @param partKeyword keyword of the part
	 * @return id of the construct
	 * @throws NodeException
	 */
	protected <T> int createConstruct(Node node, Class<T> clazz, String constructKeyword, String partKeyword) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.createObject(Construct.class);
		construct.setAutoEnable(true);
		construct.setKeyword(constructKeyword);
		construct.setName(constructKeyword, 1);
//		construct.getNodes().add(node);

		Part part = t.createObject(Part.class);
		part.setEditable(1);
		part.setHidden(false);
		part.setKeyname(partKeyword);
		part.setName(partKeyword, 1);
		part.setPartTypeId(getPartTypeId(clazz));
		part.setDefaultValue(t.createObject(Value.class));

		construct.getParts().add(part);

		construct.save();
		t.commit(false);

		return ObjectTransformer.getInt(construct.getId(), 0);
	}

	/**
	 * Get the parttype id for the given parttype class
	 * @param clazz class
	 * @return parttype id
	 * @throws NodeException if the parttype class was not found
	 */
	protected <T> int getPartTypeId(final Class<T> clazz) throws NodeException {
		final int[] id = new int[1];
		DBUtils.executeStatement("SELECT id FROM type WHERE javaclass = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setString(1, clazz.getName());
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				if (rs.next()) {
					id[0] = rs.getInt("id");
				} else {
					fail("Could not find type for " + clazz);
				}
			}
		});

		return id[0];
	}

	/**
	 * Get the parttype implementation of the given part from the container
	 * @param clazz expected parttype implementation class
	 * @param container container
	 * @param partKeyword keyword of the part
	 * @return parttype implementation
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected <T extends PartType> T getPartType(Class<T> clazz, ValueContainer container, String partKeyword) throws NodeException {
		Value value = container.getValues().getByKeyname(partKeyword);
		if (value == null) {
			throw new NodeException(container + " does not contain part " + partKeyword);
		}
		return (T) value.getPartType();
	}

	private Template createTemplate(Integer folderId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);

		template.setFolderId(folderId);
		template.setName("Template");
		template.setSource("This is the template");
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Get a folder resource, that can be used to test REST calls
	 * The folder resource will have the current transaction set
	 * @return folder resource
	 * @throws NodeException
	 */
	protected FolderResource getFolderResource() throws NodeException {
		FolderResourceImpl folderResource = new FolderResourceImpl();

		folderResource.setTransaction(TransactionManager.getCurrentTransaction());
		return folderResource;
	}

	/**
	 * Get a page resource, that can be used to test REST calls
	 * The page resource will have the current transaction set
	 * @return page resource
	 * @throws NodeException
	 */
	protected PageResource getPageResource() throws NodeException {
		PageResourceImpl pageResource = new PageResourceImpl();

		pageResource.setTransaction(TransactionManager.getCurrentTransaction());
		return pageResource;
	}
}
