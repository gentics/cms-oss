package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.publish.mesh.MeshPortalImplementationPublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.PasswordType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

/**
 * Test cases for publishing Portal Implementations into Mesh CRs
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY })
@Category(MeshTest.class)
public class MeshPublishPortalImplementationTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "portalproject";

	/**
	 * Expected schema name
	 */
	public final static String SCHEMA_NAME = MESH_PROJECT_NAME + "_" + MeshPortalImplementationPublisher.SCHEMA_SUFFIX;

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	/**
	 * Counter for unique keywords
	 */
	private static AtomicInteger counter = new AtomicInteger();

	private static Node node;

	private static Node otherNode;

	private static Integer crId;

	private static String meshCrUrl;

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();
		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		otherNode = supply(() -> createNode("othernode", "Other Node", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		meshCrUrl = supply(t -> t.getObject(ContentRepository.class, crId).getEffectiveUrl());

		operate(() -> update(node, n -> n.setContentrepositoryId(crId)));
		operate(() -> update(otherNode, n -> n.setContentrepositoryId(crId)));
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		operate(t -> {
			for (PortalImplementation existing : t.getObjects(PortalImplementation.class,
					DBUtils.select("SELECT id FROM portal_implementation", DBUtils.IDS))) {
				existing.delete(true);
			}
		});

		// no project per node, set credentials and URL
		ContentRepositoryModel crModel = new ContentRepositoryModel();
		crModel.setInstantPublishing(false);
		crModel.setProjectPerNode(false);
		crModel.setUsername("admin");
		crModel.setPassword("admin");
		crModel.setPasswordType(PasswordType.value);
		crModel.setUrl(meshCrUrl);
		crResource.update(Integer.toString(crId), crModel);
	}

	/**
	 * Test that repairing the CR creates the schema for portal implementations and assigns it to the project
	 * @throws Exception
	 */
	@Test
	public void testRepairCreatesSchema() throws Exception {
		repair();

		SchemaResponse schema = mesh.client().findSchemas().blockingGet().getData().stream().filter(s -> SCHEMA_NAME.equals(s.getName())).findFirst()
				.orElse(null);
		assertThat(schema).as("Schema " + SCHEMA_NAME).isNotNull();
		assertThat(schema.getFields().stream().collect(Collectors.toMap(FieldSchema::getName, FieldSchema::getType))).as("Schema fields")
				.containsOnly(Map.entry("keyword", "string"), Map.entry("name", "string"), Map.entry("description", "string"), Map.entry("package", "binary"));
		assertThat(schema.getContainer()).as("Container flag").isFalse();
		assertThat(schema.getNoIndex()).as("noIndex flag").isTrue();

		List<String> projectSchemas = mesh.client().findSchemas(MESH_PROJECT_NAME).blockingGet().getData().stream().map(SchemaResponse::getName)
				.collect(Collectors.toList());
		assertThat(projectSchemas).as("Schemas assigned to project").contains(SCHEMA_NAME);

		// a second check (without repair) must succeed
		assertThat(check().getContentRepository().getCheckStatus()).as("Check status").isEqualTo(Status.ok);
	}

	/**
	 * Test that the structure check detects missing, outdated and obsolete portal implementation nodes and that the repair publishes them
	 * @throws Exception
	 */
	@Test
	public void testCheckAndRepair() throws Exception {
		// create schemas and project
		repair();

		PortalImplementation impl = createWithFiles(Map.of("index.php", "index"));
		operate(t -> t.getObject(node).addPortalImplementation(impl));

		// missing node
		ContentRepositoryResponse checkResponse = check();
		assertThat(checkResponse.getContentRepository().getCheckStatus()).as("Check status with missing node").isEqualTo(Status.error);
		assertThat(checkResponse.getContentRepository().getCheckResult()).as("Check result with missing node")
				.contains(String.format("Portal implementation %s is missing", impl.getKeyword()));

		repair();
		assertPublished(impl);
		assertThat(downloadPackage(impl)).as("Package contents after repair").containsExactlyEntriesOf(Map.of("index.php", "index"));
		assertThat(check().getContentRepository().getCheckStatus()).as("Check status after repair").isEqualTo(Status.ok);

		// outdated node
		operate(t -> {
			PortalImplementationFile file = t.getObject(t.getObject(impl).getFile("index.php"), true);
			file.setContents(new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8)));
			file.save();
		});
		checkResponse = check();
		assertThat(checkResponse.getContentRepository().getCheckStatus()).as("Check status with outdated node").isEqualTo(Status.error);
		assertThat(checkResponse.getContentRepository().getCheckResult()).as("Check result with outdated node")
				.contains(String.format("Portal implementation %s is outdated", impl.getKeyword()));

		repair();
		assertThat(downloadPackage(impl)).as("Package contents after second repair").containsExactlyEntriesOf(Map.of("index.php", "changed"));
		assertThat(check().getContentRepository().getCheckStatus()).as("Check status after second repair").isEqualTo(Status.ok);

		// obsolete node
		String uuid = getMeshUuid(MESH_PROJECT_NAME, impl);
		operate(t -> t.getObject(node).removePortalImplementation(impl));
		checkResponse = check();
		assertThat(checkResponse.getContentRepository().getCheckStatus()).as("Check status with obsolete node").isEqualTo(Status.error);
		assertThat(checkResponse.getContentRepository().getCheckResult()).as("Check result with obsolete node")
				.contains(String.format("Obsolete portal implementation node %s", uuid));

		repair();
		assertThat(findNode(MESH_PROJECT_NAME, uuid)).as("Node after third repair").isNull();
		assertThat(check().getContentRepository().getCheckStatus()).as("Check status after third repair").isEqualTo(Status.ok);
	}

	/**
	 * Test publishing an assigned portal implementation
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		PortalImplementation impl = createWithFiles(Map.of("index.php", "<?php echo 'index'; ?>", "src/lib/a.php", "<?php echo 'a'; ?>"));
		PortalImplementation unassigned = createWithFiles(Map.of("x.txt", "x"));
		operate(t -> t.getObject(node).addPortalImplementation(impl));

		publish();

		NodeResponse meshNode = assertPublished(impl);
		assertThat(meshNode.getParentNode().getUuid()).as("Parent node").isEqualTo(getProjectRootUuid(MESH_PROJECT_NAME));
		assertThat(downloadPackage(impl)).as("Package contents")
				.containsExactlyEntriesOf(new TreeMap<>(Map.of("index.php", "<?php echo 'index'; ?>", "src/lib/a.php", "<?php echo 'a'; ?>")));

		assertThat(findNode(MESH_PROJECT_NAME, unassigned)).as("Node of unassigned portal implementation").isNull();
	}

	/**
	 * Test that publishing again without changes does not rewrite the portal implementation, and that changes are published
	 * @throws Exception
	 */
	@Test
	public void testChangeDetection() throws Exception {
		PortalImplementation impl = createWithFiles(Map.of("index.php", "index"));
		operate(t -> t.getObject(node).addPortalImplementation(impl));

		publish();
		String version = assertPublished(impl).getVersion();

		// publish without changes
		publish();
		assertThat(assertPublished(impl).getVersion()).as("Version after publishing without changes").isEqualTo(version);

		// change a file
		operate(t -> {
			PortalImplementationFile file = t.getObject(t.getObject(impl).getFile("index.php"), true);
			file.setContents(new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8)));
			file.save();
		});
		publish();
		NodeResponse afterFileChange = assertPublished(impl);
		assertThat(afterFileChange.getVersion()).as("Version after changing a file").isNotEqualTo(version);
		assertThat(downloadPackage(impl)).as("Package contents after changing a file").containsExactlyEntriesOf(Map.of("index.php", "changed"));

		// change the name
		operate(t -> {
			PortalImplementation editable = t.getObject(impl, true);
			editable.setName(editable.getName() + " changed");
			editable.save();
		});
		publish();
		NodeResponse afterNameChange = assertPublished(impl);
		assertThat(afterNameChange.getVersion()).as("Version after changing the name").isNotEqualTo(afterFileChange.getVersion());
		assertThat(afterNameChange.getFields().getBinaryField("package").getSha512sum()).as("Hash of unchanged package")
				.isEqualTo(afterFileChange.getFields().getBinaryField("package").getSha512sum());
	}

	/**
	 * Test that portal implementations are removed from Mesh, when they are unassigned or deleted
	 * @throws Exception
	 */
	@Test
	public void testRemove() throws Exception {
		PortalImplementation unassign = createWithFiles(Map.of("a.txt", "a"));
		PortalImplementation delete = createWithFiles(Map.of("b.txt", "b"));
		PortalImplementation keep = createWithFiles(Map.of("c.txt", "c"));
		operate(t -> {
			Node n = t.getObject(node);
			n.addPortalImplementation(unassign);
			n.addPortalImplementation(delete);
			n.addPortalImplementation(keep);
		});

		publish();
		assertPublished(unassign);
		assertPublished(delete);
		assertPublished(keep);

		String deleteUuid = getMeshUuid(MESH_PROJECT_NAME, delete);
		operate(t -> {
			t.getObject(node).removePortalImplementation(unassign);
			t.getObject(delete).delete(true);
		});

		publish();
		assertThat(findNode(MESH_PROJECT_NAME, unassign)).as("Node of unassigned portal implementation").isNull();
		assertThat(findNode(MESH_PROJECT_NAME, deleteUuid)).as("Node of deleted portal implementation").isNull();
		assertPublished(keep);
	}

	/**
	 * Test that portal implementations assigned to multiple nodes are published only once (when the nodes publish into the same project)
	 * and that with "project per node", the portal implementations are published into the project of the node, they are assigned to
	 * @throws Exception
	 */
	@Test
	public void testMultipleNodes() throws Exception {
		PortalImplementation shared = createWithFiles(Map.of("shared.txt", "shared"));
		PortalImplementation nodeOnly = createWithFiles(Map.of("node.txt", "node"));
		PortalImplementation otherOnly = createWithFiles(Map.of("other.txt", "other"));
		operate(t -> {
			Node n = t.getObject(node);
			n.addPortalImplementation(shared);
			n.addPortalImplementation(nodeOnly);
			Node o = t.getObject(otherNode);
			o.addPortalImplementation(shared);
			o.addPortalImplementation(otherOnly);
		});

		// one project for both nodes
		publish();
		assertPublished(shared);
		assertPublished(nodeOnly);
		assertPublished(otherOnly);

		// remember the uuids in the common project
		List<String> commonUuids = List.of(getMeshUuid(MESH_PROJECT_NAME, shared), getMeshUuid(MESH_PROJECT_NAME, nodeOnly),
				getMeshUuid(MESH_PROJECT_NAME, otherOnly));

		// switch to project per node (without cleaning Mesh)
		operate(t -> update(t.getObject(ContentRepository.class, crId), cr -> cr.setProjectPerNode(true)));
		publish();

		// project names are derived from the names of the root folders (sanitized)
		String nodeProject = supply(() -> MeshPublisher.getMeshName(node));
		String otherProject = supply(() -> MeshPublisher.getMeshName(otherNode));

		// the portal implementations must have been removed from the (former) common project
		for (String uuid : commonUuids) {
			assertThat(findNode(MESH_PROJECT_NAME, uuid)).as("Node " + uuid + " in former common project").isNull();
		}

		// the shared portal implementation is published into both projects (with different uuids)
		assertThat(getMeshUuid(nodeProject, shared)).as("Uuid of shared portal implementation in projects").isNotEqualTo(getMeshUuid(otherProject, shared));
		assertThat(findNode(nodeProject, shared)).as("Shared in project of node").isNotNull();
		assertThat(findNode(nodeProject, nodeOnly)).as("Node only in project of node").isNotNull();
		assertThat(findNode(nodeProject, otherOnly)).as("Other only in project of node").isNull();
		assertThat(findNode(otherProject, shared)).as("Shared in project of other node").isNotNull();
		assertThat(findNode(otherProject, nodeOnly)).as("Node only in project of other node").isNull();
		assertThat(findNode(otherProject, otherOnly)).as("Other only in project of other node").isNotNull();
	}

	/**
	 * Repair the CR
	 * @throws Exception
	 */
	protected void repair() throws Exception {
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
	}

	/**
	 * Check the CR (without repairing)
	 * @return response
	 * @throws Exception
	 */
	protected ContentRepositoryResponse check() throws Exception {
		ContentRepositoryResponse response = crResource.check(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		return response;
	}

	/**
	 * Run a publish process
	 * @throws Exception
	 */
	protected void publish() throws Exception {
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Create a portal implementation with the given files
	 * @param files map of paths to contents
	 * @return portal implementation
	 * @throws NodeException
	 */
	protected PortalImplementation createWithFiles(Map<String, String> files) throws NodeException {
		int index = counter.incrementAndGet();
		PortalImplementation impl = supply(() -> Builder.create(PortalImplementation.class, created -> {
			created.setKeyword("portal" + index);
			created.setName("Portal " + index);
			created.setDescription("Description " + index);
			created.setType(PortalImplementationModel.Type.PHP);
		}).build());
		operate(() -> {
			for (Map.Entry<String, String> entry : files.entrySet()) {
				Builder.create(PortalImplementationFile.class, file -> {
					file.setPortalImplementationId(impl.getId());
					file.setPath(entry.getKey());
					file.setContents(new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
				}).build();
			}
		});
		return impl;
	}

	/**
	 * Assert that the portal implementation is published into the project (with current keyword, name and description)
	 * @param impl portal implementation
	 * @return mesh node
	 * @throws NodeException
	 */
	protected NodeResponse assertPublished(PortalImplementation impl) throws NodeException {
		NodeResponse meshNode = findNode(MESH_PROJECT_NAME, impl);
		assertThat(meshNode).as("Mesh node of " + impl).isNotNull();
		PortalImplementation current = supply(t -> t.getObject(PortalImplementation.class, impl.getId()));
		assertThat(meshNode.getSchema().getName()).as("Schema").isEqualTo(SCHEMA_NAME);
		assertThat(meshNode.getFields().getStringField("keyword").getString()).as("Keyword").isEqualTo(current.getKeyword());
		assertThat(meshNode.getFields().getStringField("name").getString()).as("Name").isEqualTo(current.getName());
		assertThat(meshNode.getFields().getStringField("description").getString()).as("Description").isEqualTo(current.getDescription());
		BinaryField packageField = meshNode.getFields().getBinaryField("package");
		assertThat(packageField).as("Package").isNotNull();
		assertThat(packageField.getFileName()).as("Package file name").isEqualTo(current.getKeyword() + ".zip");
		assertThat(packageField.getMimeType()).as("Package mimetype").isEqualTo("application/zip");
		assertThat(meshNode.getAvailableLanguages()).as("Available languages").containsKey("en");
		assertThat(meshNode.getAvailableLanguages().get("en")).as("English version").hasFieldOrPropertyWithValue("published", true);
		return meshNode;
	}

	/**
	 * Find the mesh node of the portal implementation in the project
	 * @param projectName project name
	 * @param impl portal implementation
	 * @return mesh node or null
	 * @throws NodeException
	 */
	protected NodeResponse findNode(String projectName, PortalImplementation impl) throws NodeException {
		String uuid = getMeshUuid(projectName, impl);
		return uuid != null ? findNode(projectName, uuid) : null;
	}

	/**
	 * Get the uuid of the Mesh node for the portal implementation in the project
	 * @param projectName project name
	 * @param impl portal implementation
	 * @return uuid or null, if the project does not exist
	 * @throws NodeException
	 */
	protected String getMeshUuid(String projectName, PortalImplementation impl) throws NodeException {
		ProjectResponse project = mesh.client().findProjectByName(projectName).toMaybe().onErrorComplete().blockingGet();
		if (project == null) {
			return null;
		}
		return supply(() -> MeshPortalImplementationPublisher.getMeshUuid(impl, project.getUuid()));
	}

	/**
	 * Find the mesh node with given uuid in the project
	 * @param projectName project name
	 * @param uuid uuid
	 * @return mesh node or null
	 */
	protected NodeResponse findNode(String projectName, String uuid) {
		return mesh.client().findNodeByUuid(projectName, uuid, new NodeParametersImpl().setLanguages("en")).toMaybe().onErrorComplete()
				.blockingGet();
	}

	/**
	 * Get the uuid of the project root node
	 * @param projectName project name
	 * @return root node uuid
	 */
	protected String getProjectRootUuid(String projectName) {
		ProjectResponse project = mesh.client().findProjectByName(projectName).blockingGet();
		return project.getRootNode().getUuid();
	}

	/**
	 * Download and unzip the package of the portal implementation
	 * @param impl portal implementation
	 * @return sorted map of paths to contents
	 * @throws Exception
	 */
	protected Map<String, String> downloadPackage(PortalImplementation impl) throws Exception {
		String uuid = getMeshUuid(MESH_PROJECT_NAME, impl);
		MeshBinaryResponse response = mesh.client().downloadBinaryField(MESH_PROJECT_NAME, uuid, "en", "package").blockingGet();
		Map<String, String> contents = new TreeMap<>();
		try (InputStream in = response.getStream(); ZipInputStream zip = new ZipInputStream(in)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				contents.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new Exception(e);
		} finally {
			response.close();
		}
		return contents;
	}
}
