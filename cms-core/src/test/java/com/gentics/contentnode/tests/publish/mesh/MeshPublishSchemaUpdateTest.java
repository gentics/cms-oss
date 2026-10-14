package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertNewJobs;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.jobs;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.newJobs;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.Job;
import com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;

/**
 * Test cases for schema updates
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.INSTANT_CR_PUBLISHING })
@Category(MeshTest.class)
public class MeshPublishSchemaUpdateTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	/**
	 * Schema prefix
	 */
	public final static String SCHEMA_PREFIX = "test";

	public final static String projectName = "Node";

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Trx.operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = Trx.supply(() -> createNode("node.com", projectName, PublishTarget.CONTENTREPOSITORY));
	}

	private Integer crId;

	@Before
	public void setup() throws NodeException {
		crId = createMeshCR(mesh, SCHEMA_PREFIX);
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.setProjectPerNode(true);
			cr.setInstantPublishing(true);
		}));

		Trx.operate(() -> update(node, update -> {
			update.setPublishDir("/prefix");
			update.setContentrepositoryId(crId);
		}));
	}

	@After
	public void tearDown() throws NodeException {
		cleanMesh(mesh.client());

		if (crId != null) {
			Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), ContentRepository::delete));
			crId = null;
		}
	}

	/**
	 * Test that adding tagmap entries updates the schema and adds migration jobs (which succeed)
	 * @throws Exception
	 */
	@Test
	public void testAddEntry() throws Exception {
		// repair CR (which will generate schemas) and assert that no migration jobs are generated
		List<JobResponse> initialJobs = jobs(mesh.client());
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertThat(newJobs(mesh.client(), initialJobs)).isEmpty();

		// update the CR by adding an entry, repair again
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.addEntry("bla", "bla", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false);
		}));

		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertNewJobs(mesh.client(), initialJobs, new Job(JobType.schema, SCHEMA_PREFIX + "_content", projectName, "1.0", "2.0"));

		// update the CR by adding an entry, repair again
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.addEntry("bla2", "bla2", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false);
		}));

		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertNewJobs(mesh.client(), initialJobs, new Job(JobType.schema, SCHEMA_PREFIX + "_content", projectName, "1.0", "2.0"),
				new Job(JobType.schema, SCHEMA_PREFIX + "_content", projectName, "2.0", "3.0"));
	}

	/**
	 * Test updating multiple schemas at once
	 * @throws Exception
	 */
	@Test
	public void testModifyMultipleSchemas() throws Exception {
		// repair CR (which will generate schemas) and assert that no migration jobs are generated
		List<JobResponse> initialJobs = jobs(mesh.client());
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertThat(newJobs(mesh.client(), initialJobs)).isEmpty();

		// update the CR by adding entries for each type
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.addEntry("bla", "bla", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false);
			cr.addEntry("bla", "bla", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false);
			cr.addEntry("bla", "bla", File.TYPE_FILE, 0, AttributeType.text, false, false, false, false, false);
		}));

		// repair again
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));
		assertNewJobs(mesh.client(), initialJobs, new Job(JobType.schema, SCHEMA_PREFIX + "_content", projectName, "1.0", "2.0"),
				new Job(JobType.schema, SCHEMA_PREFIX + "_folder", projectName, "1.0", "2.0"),
				new Job(JobType.schema, SCHEMA_PREFIX + "_binary_content", projectName, "1.0", "2.0"));
	}

	/**
	 * Test that only updating the order of the fields will not invalidate the schema
	 * @throws Exception
	 */
	@Test
	public void testChangeFieldOrder() throws Exception {
		// repair CR (which will generate the schemas)
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// get the folder schema
		Optional<SchemaResponse> optFolderSchema = mesh.client().findSchemas(projectName).blockingGet().getData().stream().filter(schema -> StringUtils
				.equals(schema.getName(), ContentNodeMeshCRUtils.getFolderSchemaName(SCHEMA_PREFIX))).findFirst();
		assertThat(optFolderSchema).as("Folder schema").isPresent();
		SchemaResponse folderSchema = optFolderSchema.get();

		// update the folder schema by changing the field order
		assertThat(folderSchema.getFields().size()).as("Number of Fields").isGreaterThanOrEqualTo(2);
		Collections.reverse(folderSchema.getFields());
		mesh.client()
				.updateSchema(folderSchema.getUuid(),
						new SchemaUpdateRequest().setName(folderSchema.getName()).setFields(folderSchema.getFields()))
				.blockingAwait();

		ContentRepositoryResponse response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		assertThat(response.getContentRepository())
				.as(String.format("Check status: %s", response.getContentRepository().getCheckResult()))
				.hasFieldOrPropertyWithValue("checkStatus", ContentRepositoryModel.Status.ok);
	}

	/**
	 * Test that only updating the order of the urlfields will not invalidate the schema
	 * @throws Exception
	 */
	@Test
	public void testChangeUrlFieldsOrder() throws Exception {
		// update the CR by adding two urlfield entries
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.addEntry("url1", "url1", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, true);
			cr.addEntry("url2", "url2", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, true);
		}));

		// repair CR (which will generate the schemas)
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// get the page schema
		Optional<SchemaResponse> optPageSchema = mesh.client().findSchemas(projectName).blockingGet().getData().stream().filter(schema -> StringUtils
				.equals(schema.getName(), ContentNodeMeshCRUtils.getPageSchemaName(SCHEMA_PREFIX))).findFirst();
		assertThat(optPageSchema).as("Page schema").isPresent();
		SchemaResponse pageSchema = optPageSchema.get();

		// update the page schema by changing the url fields property (order of the values)
		assertThat(pageSchema.getUrlFields().size()).as("Number of URL fields Fields").isGreaterThanOrEqualTo(2);
		Collections.reverse(pageSchema.getUrlFields());
		mesh.client().updateSchema(pageSchema.getUuid(), new SchemaUpdateRequest().setName(pageSchema.getName())
				.setFields(pageSchema.getFields()).setUrlFields(pageSchema.getUrlFields())).blockingAwait();

		ContentRepositoryResponse response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		assertThat(response.getContentRepository())
				.as(String.format("Check status: %s", response.getContentRepository().getCheckResult()))
				.hasFieldOrPropertyWithValue("checkStatus", ContentRepositoryModel.Status.ok);
	}

	/**
	 * Test that updating the url fields property (other than just changing the order) will invalidate the schema
	 * @throws Exception
	 */
	@Test
	public void testChangeUrlFields() throws Exception {
		// update the CR by adding two urlfield entries
		Trx.operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> {
			cr.addEntry("url1", "url1", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, true);
			cr.addEntry("url2", "url2", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, true);
		}));

		// repair CR (which will generate the schemas)
		ContentNodeRESTUtils.assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// get the page schema
		Optional<SchemaResponse> optPageSchema = mesh.client().findSchemas(projectName).blockingGet().getData().stream().filter(schema -> StringUtils
				.equals(schema.getName(), ContentNodeMeshCRUtils.getPageSchemaName(SCHEMA_PREFIX))).findFirst();
		assertThat(optPageSchema).as("Page schema").isPresent();
		SchemaResponse pageSchema = optPageSchema.get();

		// update the page schema by changing the url fields property (order of the values)
		assertThat(pageSchema.getUrlFields().size()).as("Number of URL fields Fields").isGreaterThanOrEqualTo(2);
		Collections.reverse(pageSchema.getUrlFields());
		pageSchema.getUrlFields().remove(0);
		mesh.client().updateSchema(pageSchema.getUuid(), new SchemaUpdateRequest().setName(pageSchema.getName())
				.setFields(pageSchema.getFields()).setUrlFields(pageSchema.getUrlFields())).blockingAwait();

		ContentRepositoryResponse response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		assertThat(response.getContentRepository())
				.as(String.format("Check status: %s", response.getContentRepository().getCheckResult()))
				.hasFieldOrPropertyWithValue("checkStatus", ContentRepositoryModel.Status.error);
	}
}
