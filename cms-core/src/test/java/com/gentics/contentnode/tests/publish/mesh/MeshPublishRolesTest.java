package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertMeshProject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.isDefaultBranch;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.request.MeshRolesRequest;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.MeshRolesResponse;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.contentnode.testutils.mesh.MeshTestRule;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.RolePermissionParameters;
import com.gentics.mesh.parameter.client.RolePermissionParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Test cases for setting permissions on roles
 */
@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.ATTRIBUTE_DIRTING })
@Category(MeshTest.class)
public class MeshPublishRolesTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	/**
	 * Mesh roles
	 */
	public final static List<String> ROLES = Arrays.asList("role_a", "role_b", "role_c");

	private final static String VTL_OBJECT_TAG_KEYWORD = "vtlroles";
	private final static String VTL_PART_KEYWORD = "vtl";
	private final static String VTL_ROLES_FIELD = "roles";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Node node;

	private static Integer crId;

	private static Template template;

	private static Datasource rolesDs;

	private static Construct rolesConstruct;

	private static ObjectTagDefinition rolesProperty;
	private static ObjectTagDefinition velocityRolesProperty;

	@Rule
	public MeshTestRule meshTestRule = new MeshTestRule(mesh);

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws Exception {
		ROLES.forEach(role -> {
			mesh.client().createRole(new RoleCreateRequest().setName(role)).blockingAwait();
		});
		node = Trx.supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);

		Trx.operate(() -> update(node, n -> {
			n.setContentrepositoryId(crId);
		}));

		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));

		rolesDs = Trx.supply(() -> create(Datasource.class, ds -> {
			ds.setSourceType(SourceType.staticDS);
			ds.setName("Mesh Roles");
		}));

		rolesConstruct = Trx.supply(() -> create(Construct.class, construct -> {
			construct.setAutoEnable(true);
			construct.setIconName("icon");
			construct.setKeyword("roles");
			construct.setName("roles", 1);
			construct.getNodes().add(node);

			construct.getParts().add(create(Part.class, part -> {
				part.setEditable(1);
				part.setHidden(false);
				part.setKeyname("roles");
				part.setName("roles", 1);
				part.setPartTypeId(getPartTypeId(MultiSelectPartType.class));
				part.setDefaultValue(create(Value.class, value -> {}, false));
				part.setInfoInt(rolesDs.getId());
			}, false));
		}));

		int velocityRolesConstructId = Trx.supply(t -> ContentNodeTestDataUtils.createVelocityConstruct(node, "roleConstruct", VTL_PART_KEYWORD));

		rolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, rolesConstruct.getId(), "Roles", "roles");
		velocityRolesProperty = createObjectPropertyDefinition(Folder.TYPE_FOLDER, velocityRolesConstructId, "VTLRoles", VTL_OBJECT_TAG_KEYWORD);
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());

		Trx.operate(t -> {
			update(t.getObject(ContentRepository.class, crId, true), cr -> {
				cr.setPermissionProperty("object.roles");
				cr.setDefaultPermission("role_b");
			});
		});

		Trx.operate(t -> {
			for (Folder folder : node.getFolder().getChildFolders()) {
				t.getObject(folder, true).delete(true);
			}
		});

		Trx.operate(t -> {
			update(rolesDs, ds -> {
				ds.getEntries().clear();
			});
		});
	}

	/**
	 * Test synchronizing all available roles
	 * @throws Exception
	 */
	@Test
	public void testSyncAll() throws Exception {
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		if (response.getContentRepository().getCheckStatus() != Status.ok) {
			fail(response.getContentRepository().getCheckResult());
		}
		setAllRoles();

		assertMeshProject(mesh.client(), MESH_PROJECT_NAME);

		rolesDs = Trx.execute(ds -> ds.reload(), rolesDs);

		Trx.operate(() -> {
			List<DatasourceEntry> expectedRoles = getExpectedEntries();
			List<DatasourceEntry> roles = rolesDs.getEntries();
			assertThat(roles).as("Roles Datasource entries").usingElementComparatorOnFields("key", "value").containsExactlyElementsOf(expectedRoles);
		});
	}

	/**
	 * Test synchronizing some of the available roles
	 * @throws Exception
	 */
	@Test
	public void testSetSomeRoles() throws Exception {
		doSetTest("role_a", "role_c");
	}

	/**
	 * Test Changing set roles
	 * @throws Exception
	 */
	@Test
	public void testChangeRoles() throws Exception {
		doSetTest("role_a", "role_c");
		doSetTest("role_b", "anonymous");
		doSetTest("role_b", "role_c");
		doSetTest();
	}

	/**
	 * Test setting invalid (unknown) roles
	 * @throws Exception
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Die Rollen [invalid] sind ungültig und können nicht gesetzt werden.")
	public void testSetInvalidRoles() throws Exception {
		crResource.setRoles(Integer.toString(crId), new MeshRolesRequest().setRoles(Arrays.asList("invalid")));
	}

	/**
	 * Test getting available roles
	 * @throws Exception
	 */
	@Test
	public void testGetAvailableRoles() throws Exception {
		MeshRolesResponse getResponse = crResource.getAvailableRoles(Integer.toString(crId));
		assertResponseOK(getResponse);

		assertThat(getResponse.getRoles()).as("Available roles").containsOnly("anonymous", "role_a", "role_b", "role_c");
	}

	/**
	 * Test that permissions on project are set
	 * @throws Exception
	 */
	@Test
	public void testProjectPermissions() throws Exception {
		setAllRoles();

		Trx.operate(() -> update(node.getFolder(), f -> {
			setRoles(f, "anonymous", "role_b");
		}));

		Trx.operate(() -> PublishQueue.dirtObject(node.getFolder(), Action.MODIFY, 0));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertProjectPermission(MESH_PROJECT_NAME, Permission.READ, "admin", "anonymous", "role_b");
		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		Optional<BranchResponse> optionalDefaultBranch = branches.stream().filter(isDefaultBranch()).findFirst();
		assertThat(optionalDefaultBranch).isPresent();
		assertBranchPermission(MESH_PROJECT_NAME, optionalDefaultBranch.get().getUuid(), Permission.READ, "admin", "anonymous", "role_b");
	}

	/**
	 * Test that permissions on project are set, even if not yet created on Mesh.
	 * @throws Exception
	 */
	@Test
	public void testProjectPermissionsAutoCreate() throws Exception {
		MeshPublisher mp = new MeshPublisher(MiscUtils.load(ContentRepository.class, crId.toString()), true);

		Trx.operate(() -> {
			update(rolesDs, ds -> {
				mp.setRoles(Collections.singleton(rolesDs), Arrays.asList("role_auto"));
			});
			update(node.getFolder(), f -> {
				setRoles(f, "role_auto");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertProjectPermission(MESH_PROJECT_NAME, Permission.READ, "admin", "role_auto");
		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		Optional<BranchResponse> optionalDefaultBranch = branches.stream().filter(isDefaultBranch()).findFirst();
		assertThat(optionalDefaultBranch).isPresent();
		assertBranchPermission(MESH_PROJECT_NAME, optionalDefaultBranch.get().getUuid(), Permission.READ, "admin", "role_auto");
	}
	/**
	 * Test permissions on root node
	 * @throws Exception
	 */
	@Test
	public void testProjectRootNodePermissions() throws Exception {
		setAllRoles();

		Trx.operate(() -> update(node.getFolder(), f -> {
			setRoles(f, "anonymous", "role_b");
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		String rootNodeUuid = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet().getRootNode().getUuid();
		assertRoles(param -> mesh.client().findNodeByUuid(MESH_PROJECT_NAME, rootNodeUuid, param), Permission.READ_PUBLISHED, "admin", "anonymous", "role_b");
	}


	/**
	 * Test permissions on root node when role is not yet created on Mesh.
	 * @throws Exception
	 */
	@Test
	public void testProjectRootNodePermissionsAutoCreate() throws Exception {
		MeshPublisher mp = new MeshPublisher(MiscUtils.load(ContentRepository.class, crId.toString()), true);

		Trx.operate(() -> {
			update(rolesDs, ds -> {
				mp.setRoles(Collections.singleton(rolesDs), Arrays.asList("role_auto"));
			});
			update(node.getFolder(), f -> {
				setRoles(f, "role_auto");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		String rootNodeUuid = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet().getRootNode().getUuid();
		assertRoles(param -> mesh.client().findNodeByUuid(MESH_PROJECT_NAME, rootNodeUuid, param), Permission.READ_PUBLISHED, "admin", "role_auto");
	}

	/**
	 * Test permissions on folder
	 * @throws Exception
	 */
	@Test
	public void testFolderPermissions() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "anonymous", "role_c");
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "anonymous", "role_c");
	}

	/**
	 * Test permissions on folder when role is not yet created on Mesh.
	 * @throws Exception
	 */
	@Test
	public void testFolderPermissionsAutoCreate() throws Exception {
		MeshPublisher mp = new MeshPublisher(MiscUtils.load(ContentRepository.class, crId.toString()), true);

		Folder folder = Trx.supply(() -> {
			update(rolesDs, ds -> {
				mp.setRoles(Collections.singleton(rolesDs), Arrays.asList("role_auto"));
			});

			return update(createFolder(node.getFolder(), "Folder"), f -> {
				setRoles(f, "role_auto");
			});
		});

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "role_auto");
	}

	/**
	 * Test permissions on folder after attribute dirting
	 * @throws Exception
	 */
	@Test
	public void testFolderPermissionsAfterAttributeDirting() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "anonymous", "role_c");
		}));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "anonymous", "role_c");

		// dirt "name" on all folders
		Trx.operate(() -> PublishQueue.dirtFolders(new int[] { node.getId() }, null, 0, 0, Action.DEPENDENCY,
				"name"));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "anonymous", "role_c");
	}

	/**
	 * Test default permission on folder
	 * @throws Exception
	 */
	@Test
	public void testDefaultFolderPermission() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "role_b");

	}

	/**
	 * Test permissions on page
	 * @throws Exception
	 */
	@Test
	public void testPagePermissions() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "role_c", "role_a");
		}));

		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");
	}


	/**
	 * Test permissions on page when role is not yet created on Mesh.
	 * @throws Exception
	 */
	@Test
	public void testPagePermissionsAutoCreate() throws Exception {
		MeshPublisher mp = new MeshPublisher(MiscUtils.load(ContentRepository.class, crId.toString()), true);

		Folder folder = Trx.supply(() -> {
			update(rolesDs, ds -> {
				mp.setRoles(Collections.singleton(rolesDs), Arrays.asList("role_auto"));
			});

			return update(createFolder(node.getFolder(), "Folder"), f -> {
				setRoles(f, "role_auto");
			});
		});

		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_auto");
	}

	/**
	 * Test permissions on page after attribute was dirted
	 * @throws Exception
	 */
	@Test
	public void testPagePermissionsAfterAttributeDirting() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "role_c", "role_a");
		}));

		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");

		// dirt "name" on all pages
		Trx.operate(() -> PublishQueue.dirtPublishedPages(new int[] { node.getId() }, null, 0, 0, Action.DEPENDENCY,
				"name"));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");
	}

	/**
	 * Test default permissions on page
	 * @throws Exception
	 */
	@Test
	public void testDefaultPagePermissions() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		Page page = Trx.supply(() -> update(createPage(folder, template, "Page"), Page::publish));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_b");
	}

	/**
	 * Test permissions on file
	 * @throws Exception
	 */
	@Test
	public void testFilePermissions() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "role_c", "role_a", "role_b");
		}));

		File file = Trx.supply(() -> createFile(folder, "file.txt", "File contents".getBytes()));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_a", "role_b", "role_c");
	}

	/**
	 * Test permissions on file when role is not yet created on Mesh.
	 * @throws Exception
	 */
	@Test
	public void testFilePermissionsAutoCreate() throws Exception {
		MeshPublisher mp = new MeshPublisher(MiscUtils.load(ContentRepository.class, crId.toString()), true);

		Folder folder = Trx.supply(() -> {
			update(rolesDs, ds -> {
				mp.setRoles(Collections.singleton(rolesDs), Arrays.asList("role_auto"));
			});

			return update(createFolder(node.getFolder(), "Folder"), f -> {
				setRoles(f, "role_auto");
			});
		});

		File file = Trx.supply(() -> createFile(folder, "file.txt", "File contents".getBytes()));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_auto");
	}

	/**
	 * Test permissions on file after attribute dirting
	 * @throws Exception
	 */
	@Test
	public void testFilePermissionsAfterAttributeDirting() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Folder"), f -> {
			setRoles(f, "role_c", "role_a", "role_b");
		}));

		File file = Trx.supply(() -> createFile(folder, "file.txt", "File contents".getBytes()));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_a", "role_b", "role_c");

		// dirt "name" on all files
		Trx.operate(() -> PublishQueue.dirtImagesAndFiles(new int[] { node.getId() }, null, 0, 0, Action.DEPENDENCY,
				"name"));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_a", "role_b", "role_c");
	}

	/**
	 * Test default permissions on file
	 * @throws Exception
	 */
	@Test
	public void testDefaultFilePermissions() throws Exception {
		setAllRoles();

		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		File file = Trx.supply(() -> createFile(folder, "file.txt", "File contents".getBytes()));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_b");
	}

	/**
	 * Test the permissions on a folder when using values from a Velocity context.
	 * @throws Exception
	 */
	@Test
	public void testFolderPermissionsWithoutDs() throws Exception {
		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));

		setVelocityPermissionProperty();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// Assert that the page has the default permission set.
		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "role_b");

		Trx.operate(() -> update(folder, f -> setVelocityRoles(f, "role_a", "role_c")));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, folder, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");
	}

	/**
	 * Test the permissions on a page when using values from a Velocity context.
	 * @throws Exception
	 */
	@Test
	public void testPagePermissionsWithoutDs() throws Exception {
		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		Page page = Trx.supply(() -> update(createPage(folder, template, "testpage"), Page::publish));

		setVelocityPermissionProperty();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// Assert that the page has the default permission set.
		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_b");

		update(folder, f -> setVelocityRoles(f, "role_a", "role_c"));
		update(page, Page::publish);

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, page, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");
	}

	/**
	 * Test the permissions on a file when using values from a Velocity context.
	 * @throws Exception
	 */
	@Test
	public void testFilePermissionsWithoutDs() throws Exception {
		Folder folder = Trx.supply(() -> createFolder(node.getFolder(), "Folder"));
		File file = Trx.supply(() -> createFile(folder, "file.txt", "File contents".getBytes()));

		setVelocityPermissionProperty();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		// Assert that the page has the default permission set.
		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_b");

		Trx.operate(() -> update(folder, f -> setVelocityRoles(f, "role_a", "role_c")));

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertNodePermission(MESH_PROJECT_NAME, file, Permission.READ_PUBLISHED, "admin", "role_a", "role_c");
	}


	/**
	 * Test that permissions on project are set
	 * @throws Exception
	 */
	@Test
	public void testProjectPermissionsWithoutDs() throws Exception {
		setVelocityPermissionProperty();
		setVelocityRoles(node.getFolder(), "anonymous", "role_b");

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		assertProjectPermission(MESH_PROJECT_NAME, Permission.READ, "admin", "anonymous", "role_b");
		List<BranchResponse> branches = mesh.client().findBranches(MESH_PROJECT_NAME).blockingGet().getData();
		Optional<BranchResponse> optionalDefaultBranch = branches.stream().filter(isDefaultBranch()).findFirst();
		assertThat(optionalDefaultBranch).isPresent();
		assertBranchPermission(MESH_PROJECT_NAME, optionalDefaultBranch.get().getUuid(), Permission.READ, "admin", "anonymous", "role_b");
	}

	/**
	 * Test permissions on root node
	 * @throws Exception
	 */
	@Test
	public void testProjectRootNodePermissionsWithoutDs() throws Exception {
		setVelocityPermissionProperty();
		setVelocityRoles(node.getFolder(), "anonymous", "role_b");

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		String rootNodeUuid = mesh.client().findProjectByName(MESH_PROJECT_NAME).blockingGet().getRootNode().getUuid();
		assertRoles(param -> mesh.client().findNodeByUuid(MESH_PROJECT_NAME, rootNodeUuid, param), Permission.READ_PUBLISHED, "admin", "anonymous", "role_b");
	}

	/**
	 * Get the expected datasource entries for the roles datasource (only keys and values filled)
	 * @return list of expected datasource entries
	 * @throws NodeException
	 */
	protected List<DatasourceEntry> getExpectedEntries() throws NodeException {
		RoleListResponse rolesResponse = mesh.client().findRoles().blockingGet();
		return getExpectedEntries(
				rolesResponse.getData().stream().map(RoleResponse::getName).filter(role -> !"admin".equals(role)).collect(Collectors.toList()));
	}

	/**
	 * Get the expected datasource entries for the given list of roles
	 * @param roles role list
	 * @return list of expected datasource entries
	 * @throws NodeException
	 */
	protected List<DatasourceEntry> getExpectedEntries(List<String> roles) throws NodeException {
		return getExpectedEntries(roles.toArray(new String[roles.size()]));
	}

	/**
	 * Get the expected datasource entries for the given list of roles
	 * @param roles role list
	 * @return list of expected datasource entries
	 * @throws NodeException
	 */
	protected List<DatasourceEntry> getExpectedEntries(String...roles) throws NodeException {
		List<DatasourceEntry> entries = new ArrayList<>();
		for (String role : roles) {
			entries.add(create(DatasourceEntry.class, entry -> {
				entry.setKey(role);
				entry.setValue(role);
			}, false));
		}

		entries.sort((e1, e2) -> {
			return e1.getKey().compareTo(e2.getKey());
		});
		return entries;
	}

	/**
	 * The the {@link ContentRepository#setPermissionProperty(String) permission property} of the Mesh Contentrepository
	 * to the {@code roles} field in the Velocity context of the object tag.
	 */
	protected void setVelocityPermissionProperty() throws NodeException {
		Trx.operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId, true);

			cr.setPermissionProperty(String.format("object.%s.parts.%s.%s", VTL_OBJECT_TAG_KEYWORD, VTL_PART_KEYWORD, VTL_ROLES_FIELD));
			cr.save();
		});
	}

	/**
	 * Set the given roles via the velocity object tag.
	 *
	 * @param container The container to set the roles for.
	 * @param roles The roles to set.
	 */
	protected void setVelocityRoles(ObjectTagContainer container, String... roles) throws NodeException {
		ObjectTag objTag = container.getObjectTag(VTL_OBJECT_TAG_KEYWORD);
		StringBuilder valueText = new StringBuilder(String.format("#set($%s = [])\n", VTL_ROLES_FIELD));

		for (String role : roles) {
			valueText.append(String.format("$%s.add(\"%s\")\n", VTL_ROLES_FIELD, role));
		}

		update(objTag, tag -> {
			tag.setEnabled(true);

			getPartType(LongHTMLPartType.class, tag, ContentNodeTestDataUtils.TEMPLATE_PARTNAME)
				.getValueObject()
				.setValueText(valueText.toString());
		});
	}

	/**
	 * Set the given roles into the roles object property of the container.
	 * This method expects an open transaction and that the container is editable
	 * @param container editable container
	 * @param roles roles to be set
	 * @throws NodeException
	 */
	protected void setRoles(ObjectTagContainer container, String... roles) throws NodeException {
		Set<String> roleNames = new HashSet<>(Arrays.asList(roles));
		AtomicInteger rolesCount = new AtomicInteger();
		Datasource ds = rolesDs.reload();
		String valueText = ds.getEntries().stream().filter(e -> roleNames.contains(e.getKey())).map(e -> {
			rolesCount.incrementAndGet();
			return Integer.toString(e.getDsid());
		}).collect(Collectors.joining("|-|"));

		assertThat(rolesCount.get()).as("Roles set to object property").isEqualTo(roles.length);
		getPartType(MultiSelectPartType.class, container.getObjectTag("roles"), "roles").getValueObject().setValueText(valueText);
		container.getObjectTag("roles").setEnabled(true);
	}

	/**
	 * Assert that exactly the given roles have the permission on the project
	 * @param projectName project name
	 * @param permission queried permission
	 * @param roles roles
	 * @throws NodeException
	 */
	protected void assertProjectPermission(String projectName, Permission permission, String... roles) throws NodeException {
		assertRoles(param -> mesh.client().findProjectByName(projectName, param), permission, roles);
	}

	/**
	 * Assert that exactly the given roles have the permission on the branch
	 * @param projectName project name
	 * @param branchUuid branch uuid
	 * @param permission queried permission
	 * @param roles roles
	 * @throws NodeException
	 */
	protected void assertBranchPermission(String projectName, String branchUuid, Permission permission, String... roles) throws NodeException {
		assertRoles(param -> mesh.client().findBranchByUuid(projectName, branchUuid, param), permission, roles);
	}

	/**
	 * Assert that exactly the given roles have the permission on the object
	 * @param projectName project name
	 * @param object object
	 * @param permission queried permission
	 * @param roles roles
	 * @throws NodeException
	 */
	protected void assertNodePermission(String projectName, NodeObject object, Permission permission, String... roles) throws NodeException {
		Trx.operate(() -> {
			assertRoles(param -> mesh.client().findNodeByUuid(projectName, MeshPublisher.getMeshUuid(object), param), permission, roles);
		});
	}

	/**
	 * Assert that the object with given path has the permission set on exactly the given roles
	 * @param request function that returns the MeshRequest to get the object with permission
	 * @param permission queried permission
	 * @param roles roles
	 * @throws NodeException
	 */
	protected void assertRoles(Function<RolePermissionParameters, MeshRequest<? extends AbstractGenericRestResponse>> request, Permission permission, String... roles) throws NodeException {
		Set<String> roleNames = new HashSet<>(Arrays.asList(roles));
		Set<String> neededRoles = new HashSet<>(roleNames);
		RoleListResponse rolesResponse = mesh.client().findRoles().blockingGet();
		for (RoleResponse role : rolesResponse.getData()) {
			AbstractGenericRestResponse response = request.apply(new RolePermissionParametersImpl().setRoleUuid(role.getUuid())).blockingGet();

			assertThat(response.getRolePerms().get(permission)).as(String.format("Flag for %s on %s", permission, role.getName()))
					.isEqualTo(roleNames.contains(role.getName()));

			neededRoles.remove(role.getName());
		}

		assertThat(neededRoles)
			.as("Roles left to check")
			.isEmpty();
	}

	/**
	 * Set all available roles to the datasource
	 * @throws Exception
	 */
	protected void setAllRoles() throws Exception {
		MeshRolesResponse getResponse = crResource.getAvailableRoles(Integer.toString(crId));
		ContentNodeRESTUtils.assertResponseOK(getResponse);

		List<String> roles = getResponse.getRoles();
		ContentNodeRESTUtils.assertResponseOK(crResource.setRoles(Integer.toString(crId), new MeshRolesRequest().setRoles(roles)));
	}

	/**
	 * Set the given roles and do some assertions
	 * @param roles roles to set
	 * @throws Exception
	 */
	protected void doSetTest(String...roles) throws Exception {
		assertResponseOK(crResource.setRoles(Integer.toString(crId), new MeshRolesRequest().setRoles(Arrays.asList(roles))));

		MeshRolesResponse rolesResponse = crResource.getRoles(Integer.toString(crId));
		assertResponseOK(rolesResponse);
		assertThat(rolesResponse.getRoles()).as("Set roles").containsOnly(roles);

		rolesDs = Trx.execute(ds -> ds.reload(), rolesDs);
		Trx.operate(() -> {
			List<DatasourceEntry> expectedRoles = getExpectedEntries(roles);
			List<DatasourceEntry> rolesEntries = rolesDs.getEntries();
			assertThat(rolesEntries).as("Roles Datasource entries").usingElementComparatorOnFields("key", "value").containsExactlyElementsOf(expectedRoles);
		});
	}
}
