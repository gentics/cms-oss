package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.MigrationTagsRequest;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.migration.MigrationResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationTagsResponse;
import com.gentics.contentnode.rest.resource.impl.migration.MigrationResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.factory.Transaction;

/**
 * Tests for performing migrations over the REST API
 * 
 * @author Taylor
 * 
 */
public class MigrationRestSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Valid page ID
	 */
	private static final Integer VALID_PAGE_ID = 45;

	/**
	 * Valid template ID
	 */
	private static final Integer VALID_TEMPLATE_ID = 85;

	/**
	 * Valid object definition ID
	 */
	private static final Integer VALID_OBJECTDEF_ID = 3;

	/**
	 * Invalid ID
	 */
	private static final Integer INVALID_ID = 89432483;

	/**
	 * Page
	 */
	private static final String PAGE = "page";

	/**
	 * Template
	 */
	private static final String TEMPLATE = "template";

	/**
	 * Object tag definition
	 */
	private static final String OBJECTDEFINITION = "objtagdef";

	/**
	 * Valid source construct for tag type migration
	 */
	private static final int VALID_FROMTAGTYPEID = 1;

	/**
	 * Valid destination construct for tag type migration
	 */
	private static final int VALID_TOTAGTYPEID = 3;

	/**
	 * Valid source part for tag type migration
	 */
	private static final int VALID_FROMPARTID = 6;

	/**
	 * Valid destination part for tag type migration
	 */
	private static final int VALID_TOPARTID = 7;

	/**
	 * TagTypeMigrationResource that is used to call the server
	 */
	private MigrationResourceImpl migrationResource = new MigrationResourceImpl();

	@Before
	public void setup() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		migrationResource.setTransaction(t);
	}

	/**
	 * Test an invalid getMigrationTagTypes request (invalid type)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTagTypesInvalidType() throws Exception {

		MigrationTagsRequest request = new MigrationTagsRequest();
		List<Integer> ids = new ArrayList<Integer>();

		ids.add(VALID_PAGE_ID);
		request.setIds(ids);
		request.setType("invalid type");

		MigrationTagsResponse response = migrationResource.getMigrationTagTypes(request);

		assertEquals("Check for the correct page response code", ResponseCode.FAILURE, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test an invalid getMigrationTagTypes request (invalid page id)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTagTypesInvalidId() throws Exception {

		MigrationTagsRequest request = new MigrationTagsRequest();
		List<Integer> ids = new ArrayList<Integer>();

		ids.add(INVALID_ID);
		request.setIds(ids);
		request.setType(PAGE);

		MigrationTagsResponse response = migrationResource.getMigrationTagTypes(request);

		assertEquals("Check for the correct page response code", ResponseCode.NOTFOUND, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test a valid getMigrationTagTypes request for a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTagTypesValidPage() throws Exception {

		MigrationTagsRequest request = new MigrationTagsRequest();
		List<Integer> ids = new ArrayList<Integer>();

		ids.add(VALID_PAGE_ID);
		request.setIds(ids);
		request.setType(PAGE);

		MigrationTagsResponse response = migrationResource.getMigrationTagTypes(request);

		assertNotNull("Check that the request returned a non-null map of tags", response.getTagTypes());
		assertEquals("Check for the correct page response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test a valid getMigrationTagTypes request for an object definition
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTagTypesValidObject() throws Exception {

		MigrationTagsRequest request = new MigrationTagsRequest();
		List<Integer> ids = new ArrayList<Integer>();

		ids.add(VALID_OBJECTDEF_ID);
		request.setIds(ids);
		request.setType(OBJECTDEFINITION);

		MigrationTagsResponse response = migrationResource.getMigrationTagTypes(request);

		assertNotNull("Check that the request returned a list of tags", response.getTagTypes());
		assertEquals("Check for the correct page response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test a valid getMigrationTagTypes request for a template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTagTypesValidTemplate() throws Exception {

		MigrationTagsRequest request = new MigrationTagsRequest();
		List<Integer> ids = new ArrayList<Integer>();

		ids.add(VALID_TEMPLATE_ID);
		request.setIds(ids);
		request.setType(TEMPLATE);

		MigrationTagsResponse response = migrationResource.getMigrationTagTypes(request);

		assertNotNull("Check that the request returned a list of tags", response.getTagTypes());
		assertEquals("Check for the correct page response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Test a valid tag type migration on a page
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPerformValidContentTagMigration() throws Exception {

		// Create mappings
		TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();

		mapping.setFromTagTypeId(VALID_FROMTAGTYPEID);
		mapping.setToTagTypeId(VALID_TOTAGTYPEID);

		MigrationPartMapping partMapping = new MigrationPartMapping();

		partMapping.setFromPartId(VALID_FROMPARTID);
		partMapping.setToPartId(VALID_TOPARTID);
		ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

		partMappings.add(partMapping);
		mapping.setPartMappings(partMappings);
		ArrayList<TagTypeMigrationMapping> mappings = new ArrayList<TagTypeMigrationMapping>();

		mappings.add(mapping);

		// Create list of objects to apply mappings to
		ArrayList<Integer> objectList = new ArrayList<Integer>();

		objectList.add(VALID_PAGE_ID);

		// Create request object
		TagTypeMigrationRequest request = new TagTypeMigrationRequest();

		request.setMappings(mappings);
		request.setType(PAGE);
		request.setObjectIds(objectList);

		MigrationResponse response = migrationResource.performTagTypeMigration(request);

		assertNotNull("Check that the job id was returned", response.getJobId());
	}

}
