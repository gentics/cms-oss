package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.PasswordType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryResourceImpl;
import com.gentics.contentnode.testutils.mesh.MeshContext;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchMicroschemaInfo;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Utilities for testing Mesh CR
 */
public class ContentNodeMeshCRUtils {
	/**
	 * Username in Mesh
	 */
	public final static String MESH_USERNAME = "admin";

	/**
	 * Password in Mesh
	 */
	public final static String MESH_PASSWORD = "admin";

	/**
	 * Default Port to Mesh API
	 */
	public final static int MESH_PORT = 8080;

	/**
	 * ContentRepository Resource Instance
	 */
	public static ContentRepositoryResource crResource = new ContentRepositoryResourceImpl();

	/**
	 * Create a mesh CR connecting to the mesh server
	 * @param mesh mesh docker server
	 * @param projectName project name
	 * @return ID of the Mesh CR
	 * @throws NodeException
	 */
	public static Integer createMeshCR(MeshContext mesh, String projectName) throws NodeException {
		return createMeshCR(mesh.getContainerIpAddress(), mesh.getMappedPort(8080), projectName);
	}

	/**
	 * Create a mesh CR connecting to the mesh server
	 * @param host mesh host
	 * @param port mesh port
	 * @param mesh mesh docker server
	 * @param projectName project name
	 * @return ID of the Mesh CR
	 * @throws NodeException
	 */
	public static Integer createMeshCR(String host, int port, String projectName) throws NodeException {
		try {
			ContentRepositoryModel crModel = new ContentRepositoryModel();
			crModel.setName("Test CR");
			crModel.setCrType(Type.mesh);
			crModel.setDbType("");
			crModel.setUrl(String.format("%s:%d/%s", host, port, projectName));
			crModel.setUsername(MESH_USERNAME);
			crModel.setPassword(MESH_PASSWORD);
			crModel.setPasswordType(PasswordType.value);
			ContentRepositoryResponse response = crResource.add(crModel);
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getContentRepository().getId();
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Get name of the schema for folders
	 * @param projectName project name
	 * @return schema name
	 */
	public static String getFolderSchemaName(String projectName) {
		return String.format("%s_folder", projectName);
	}

	/**
	 * Get name of the schema for files
	 * @param projectName project name
	 * @return schema name
	 */
	public static String getFileSchemaName(String projectName) {
		return String.format("%s_binary_content", projectName);
	}

	/**
	 * Get name of the schema for pages
	 * @param projectName project name
	 * @return schema name
	 */
	public static String getPageSchemaName(String projectName) {
		return String.format("%s_content", projectName);
	}

	/**
	 * Clean the mesh instance be deleting all projects and schemas
	 * @param client mesh rest client
	 */
	public static void cleanMesh(MeshRestClient client) {
		// delete all projects
		ProjectListResponse projects = client.findProjects().blockingGet();
		for (ProjectResponse project : projects.getData()) {
			client.deleteProject(project.getUuid()).toCompletable().blockingAwait();
		}

		// delete all schemas
		SchemaListResponse schemas = client.findSchemas().blockingGet();
		for (SchemaResponse schema : schemas.getData()) {
			client.deleteSchema(schema.getUuid()).toCompletable().blockingAwait();
		}

		// delete all microschemas
		client.findMicroschemas().toSingle().flatMapObservable(resp -> Observable.fromIterable(resp.getData())).flatMapCompletable(microschema -> {
			return client.deleteMicroschema(microschema.getUuid()).toCompletable();
		}).blockingAwait();
	}

	/**
	 * Assert that the Mesh CR is consistent
	 * @param client mesh rest client
	 */
	public static void assertConsistent(MeshRestClient client) {
		ConsistencyCheckResponse response = client.checkConsistency().blockingGet();
		assertThat(response.getInconsistencies()).as("Inconsistencies").isEmpty();
		assertThat(response.getResult()).as("Result").isEqualTo(ConsistencyRating.CONSISTENT);
	}

	/**
	 * Assert existence of the project and schemas in mesh (schema prefix is expected to be projectName)
	 * @param client mesh rest client
	 * @param projectName project name
	 * @return project response (never null)
	 */
	public static ProjectResponse assertMeshProject(MeshRestClient client, String projectName) {
		return assertMeshProject(client, projectName, projectName, null, null, true);
	}

	/**
	 * Assert existence of the project and schemas in mesh (schema prefix is expected to be projectName)
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param expected true if existence is expected
	 * @return project response (null, if project does not exist and expected is false)
	 */
	public static ProjectResponse assertMeshProject(MeshRestClient client, String projectName, boolean expected) {
		return assertMeshProject(client, projectName, projectName, null, null, expected);
	}

	/**
	 * Assert existence of the project and schemas in mesh
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @return project response (never null)
	 */
	public static ProjectResponse assertMeshProject(MeshRestClient client, String projectName, String schemaPrefix) {
		return assertMeshProject(client, projectName, schemaPrefix, null, null, true);
	}

	/**
	 * Assert existence of the project and schemas in mesh
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @param projectUuid project UUID
	 * @param expected true if existence is expected
	 * @return project response (null, if project does not exist and expected is false)
	 */
	public static ProjectResponse assertMeshProject(MeshRestClient client, String projectName, String schemaPrefix, String projectUuid, boolean expected) {
		return assertMeshProject(client, projectName, schemaPrefix, projectUuid, null, expected);
	}

	/**
	 * Assert existence of the project and schemas in mesh
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @param projectUuid project UUID
	 * @param implementationVersion implementation version
	 * @param expected true if existence is expected
	 * @return project response (null, if project does not exist and expected is false)
	 */
	public static ProjectResponse assertMeshProject(MeshRestClient client, String projectName, String schemaPrefix, String projectUuid, String implementationVersion, boolean expected) {
		String[] schemaNames = { getFolderSchemaName(schemaPrefix), getFileSchemaName(schemaPrefix), getPageSchemaName(schemaPrefix) };

		Optional<ProjectResponse> optionalProject = client.findProjectByName(projectName).toSingle().map(Optional::of)
				.onErrorResumeNext(t -> Single.just(Optional.empty())).blockingGet();
		if (expected) {
			assertThat(optionalProject.isPresent()).as("Project " + projectName + " exists").isTrue();
			ProjectResponse project = optionalProject.get();
			if (projectUuid != null) {
				assertThat(project.getUuid()).as("Project UUID").isEqualTo(projectUuid);
			}
			SchemaListResponse projectSchemas = client.findSchemas(project.getName()).blockingGet();
			assertThat(projectSchemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("Project schemas")
			.containsOnly(schemaNames);

			Optional<BranchResponse> defaultBranch = client.findBranches(projectName).blockingGet().getData().stream()
					.filter(branch -> {
						if (StringUtils.isEmpty(implementationVersion)) {
							return StringUtils.equals(branch.getName(), projectName);
						} else {
							return MeshPublisher.hasTag(branch, MeshPublisher.IMPLEMENTATION_VERSION_TAGFAMILY,
									implementationVersion)
									&& !MeshPublisher.hasTag(branch, MeshPublisher.CHANNEL_UUID_TAGFAMILY);
						}
					}).findFirst();
			assertThat(defaultBranch).as("Default branch").isPresent();
			assertThat(defaultBranch.get().getLatest()).as("Default branch is latest").isTrue();

			return project;
		} else {
			assertThat(optionalProject.isPresent()).as("Project " + projectName + " exists").isFalse();
			return null;
		}
	}

	/**
	 * Assert existence of a branch
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param filter predicate to filter for the requested branch
	 * @param expected true if existence is expected
	 * @return branch response (null, if branch does not exist and expected is false)
	 */
	public static BranchResponse assertBranch(MeshRestClient client, String projectName, Predicate<? super BranchResponse> filter, boolean expected) {
		return assertBranch(client, projectName, filter, expected, null, null, null, null);
	}

	/**
	 * Assert existence of a branch
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param filter predicate to filter for the requested branch
	 * @param expected true if existence is expected
	 * @param branchName optional expected branch name
	 * @param hostname optional expected branch hostname
	 * @param pathPrefix optional expected path prefix
	 * @param ssl optional expected branch SSL setting
	 * @return branch response (null, if branch does not exist and expected is false)
	 */
	public static BranchResponse assertBranch(MeshRestClient client, String projectName, Predicate<? super BranchResponse> filter, boolean expected,
			String branchName, String hostname, String pathPrefix, Boolean ssl) {
		BranchListResponse response = client.findBranches(projectName).blockingGet();
		Optional<BranchResponse> optionalBranch = response.getData().stream().filter(filter).findFirst();
		if (expected) {
			assertThat(optionalBranch.isPresent()).as("Branch " + branchName + " exists").isTrue();
			BranchResponse branch = optionalBranch.get();
			if (branchName != null) {
				assertThat(branch.getName()).as("Branch name").isEqualTo(branchName);
			}
			if (hostname != null) {
				assertThat(branch.getHostname()).as("Branch hostname").isEqualTo(hostname);
			}
			if (pathPrefix != null) {
				assertThat(branch.getPathPrefix()).as("Branch path prefix").isEqualTo(pathPrefix);
			}
			if (ssl != null) {
				assertThat(branch.getSsl()).as("Branch SSL").isEqualTo(ssl);
			}
			assertThat(branch.isMigrated()).as("Branch is migrated").isTrue();
			assertThat(branch.getLatest()).as("Branch is latest").isFalse();
			return branch;
		} else {
			assertThat(optionalBranch.isPresent()).as("Branch " + branchName + " exists").isFalse();
			return null;
		}
	}

	/**
	 * Assert that the parent node contains exactly the given set of subfolders
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param parentNodeUuid parent node UUID
	 * @param folders expected set of folders
	 */
	public static void assertFolders(MeshRestClient client, String projectName, String parentNodeUuid, Folder... folders) throws NodeException {
		assertFolders(client, projectName, projectName, parentNodeUuid, folders);
	}

	/**
	 * Assert that the parent node contains exactly the given set of subfolders
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @param parentNodeUuid parent node UUID
	 * @param folders expected set of folders
	 */
	public static void assertFolders(MeshRestClient client, String projectName, String schemaPrefix, String parentNodeUuid, Folder... folders) throws NodeException {
		assertFolders(client, projectName, null, schemaPrefix, parentNodeUuid, folders);
	}

	/**
	 * Assert that the parent node contains exactly the given set of subfolders
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param schemaPrefix schema prefix
	 * @param parentNodeUuid parent node UUID
	 * @param folders expected set of folders
	 */
	public static void assertFolders(MeshRestClient client, String projectName, String branchName, String schemaPrefix, String parentNodeUuid,
			Folder... folders) throws NodeException {
		assertContainsOnly(client, projectName, branchName, parentNodeUuid, getFolderSchemaName(schemaPrefix), "en", folders);
	}

	/**
	 * Assert that the parent node contains exactly the given set of files
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param parentNodeUuid parent node UUID
	 * @param files expected set of files
	 */
	public static void assertFiles(MeshRestClient client, String projectName, String parentNodeUuid, File... files) throws NodeException {
		assertFiles(client, projectName, projectName, parentNodeUuid, files);
	}

	/**
	 * Assert that the parent node contains exactly the given set of files
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @param parentNodeUuid parent node UUID
	 * @param files expected set of files
	 */
	public static void assertFiles(MeshRestClient client, String projectName, String schemaPrefix, String parentNodeUuid, File... files) throws NodeException {
		assertContainsOnly(client, projectName, null, parentNodeUuid, getFileSchemaName(schemaPrefix), "en", files);
	}

	/**
	 * Assert that the parent node contains exactly the given set of pages
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param parentNodeUuid parent node UUID
	 * @param language language
	 * @param pages expected set of pages
	 */
	public static void assertPages(MeshRestClient client, String projectName, String parentNodeUuid, String language, Page... pages) throws NodeException {
		assertPages(client, projectName, projectName, parentNodeUuid, language, pages);
	}

	/**
	 * Assert that the parent node contains exactly the given set of pages
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param schemaPrefix schema prefix
	 * @param parentNodeUuid parent node UUID
	 * @param language language
	 * @param pages expected set of pages
	 */
	public static void assertPages(MeshRestClient client, String projectName, String schemaPrefix, String parentNodeUuid, String language, Page... pages) throws NodeException {
		assertContainsOnly(client, projectName, null, parentNodeUuid, getPageSchemaName(schemaPrefix), language, pages);
	}

	/**
	 * Assert that the node in Mesh contains only the given subnodes of a specific schema
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param parentNodeUuid parent node UUID
	 * @param schemaName schema name
	 * @param language language
	 * @param expected expected objects
	 * @throws NodeException
	 */
	public static void assertContainsOnly(MeshRestClient client, String projectName, String branchName, String parentNodeUuid, String schemaName, String language, NodeObject... expected)
			throws NodeException {
		Set<String> foundNodes = getChildren(client, projectName, branchName, parentNodeUuid, schemaName, language);
		String[] expectedUuids = getUuids(expected);

		assertThat(foundNodes).as("Subnodes of " + parentNodeUuid).containsOnly(expectedUuids);
	}

	/**
	 * Assert that the node in Mesh contains the given subnodes of a specific schema
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param parentNodeUuid parent node UUID
	 * @param schemaName schema name
	 * @param language language
	 * @param expected expected objects
	 * @throws NodeException
	 */
	public static void assertContains(MeshRestClient client, String projectName, String branchName, String parentNodeUuid, String schemaName, String language,
			NodeObject... expected) throws NodeException {
		Set<String> foundNodes = getChildren(client, projectName, branchName, parentNodeUuid, schemaName, language);
		String[] expectedUuids = getUuids(expected);

		assertThat(foundNodes).as("Subnodes of " + parentNodeUuid).contains(expectedUuids);
	}

	/**
	 * Assert that the node in Mesh does not contain the given subnodes of a specific schema
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param parentNodeUuid parent node UUID
	 * @param schemaName schema name
	 * @param language language
	 * @param unexpected unexpected objects
	 * @throws NodeException
	 */
	public static void assertDoesNotContain(MeshRestClient client, String projectName, String branchName, String parentNodeUuid, String schemaName, String language,
			NodeObject... unexpected) throws NodeException {
		Set<String> foundNodes = getChildren(client, projectName, branchName, parentNodeUuid, schemaName, language);
		String[] expectedUuids = getUuids(unexpected);

		assertThat(foundNodes).as("Subnodes of " + parentNodeUuid).doesNotContain(expectedUuids);
	}

	/**
	 * Get the uuid of child nodes having a specific schema
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param parentNodeUuid parent node uuid
	 * @param schemaName schema name
	 * @param language language
	 * @return set of uuids
	 * @throws NodeException
	 */
	public static Set<String> getChildren(MeshRestClient client, String projectName, String branchName, String parentNodeUuid, String schemaName,
			String language) throws NodeException {
		NodeListResponse nodes = null;
		NodeParameters nodeParams = new NodeParametersImpl().setLanguages(language);
		VersioningParameters versionParams = new VersioningParametersImpl();
		if (branchName != null) {
			versionParams.setBranch(branchName);
		}
		nodes = client.findNodeChildren(projectName, parentNodeUuid, nodeParams, versionParams).blockingGet();
		return nodes.getData().stream().filter(node -> schemaName.equals(node.getSchema().getName())).map(NodeResponse::getUuid).collect(Collectors.toSet());
	}

	/**
	 * Transform the nodes into their mesh uuids
	 * @param nodes nodes
	 * @return mesh uuids
	 * @throws NodeException
	 */
	protected static String[] getUuids(NodeObject... nodes) throws NodeException {
		return Trx.supply(() -> {
			String[] uuids = new String[nodes.length];
			for (int i = 0; i < nodes.length; i++) {
				uuids[i] = MeshPublisher.getMeshUuid(nodes[i]);
			}
			return uuids;
		});
	}

	/**
	 * Assert existence of object in Mesh CR
	 * @param message additional part of the failure message
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param object asserted object
	 * @param expectExistence true if the object is expected to exist in mesh, false if not
	 * @param asserters optional asserters for the Mesh object (if object exists)
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void assertObject(String message, MeshRestClient client, String projectName, NodeObject object, boolean expectExistence,
			Consumer<NodeResponse>... asserters) throws NodeException {
		assertObject(message, client, projectName, null, object, expectExistence, asserters);
	}

	/**
	 * Assert existence of object in Mesh CR
	 * @param message additional part of the failure message
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param object asserted object
	 * @param expectExistence true if the object is expected to exist in mesh, false if not
	 * @param asserters optional asserters for the Mesh object (if object exists)
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void assertObject(String message, MeshRestClient client, String projectName, String branchName, NodeObject object, boolean expectExistence,
			Consumer<NodeResponse>... asserters) throws NodeException {
		assertObject(message, client, projectName, branchName, object, supply(() -> MeshPublisher.getMeshLanguage(object)), expectExistence, asserters);
	}

	/**
	 * Assert existence of object in Mesh CR
	 * @param message additional part of the failure message
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param branchName optional branch name
	 * @param object asserted object
	 * @param meshLanguage language of the mesh object
	 * @param expectExistence true if the object is expected to exist in mesh, false if not
	 * @param asserters optional asserters for the Mesh object (if object exists)
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void assertObject(String message, MeshRestClient client, String projectName, String branchName, NodeObject object, String meshLanguage, boolean expectExistence,
			Consumer<NodeResponse>... asserters) throws NodeException {
		String meshUuid = Trx.supply(() -> MeshPublisher.getMeshUuid(object));

		VersioningParameters versionParams = new VersioningParametersImpl();
		if (branchName != null) {
			versionParams.setBranch(branchName);
		}

		Optional<NodeResponse> nodeResponse = client.findNodeByUuid(projectName, meshUuid,
				new NodeParametersImpl().setLanguages(meshLanguage), versionParams).toSingle().map(element -> {
					if (element.getFields() == null || element.getFields().isEmpty()) {
						element = null;
					}
					return Optional.ofNullable(element);
				}).onErrorResumeNext(t -> {
					if (t instanceof MeshRestClientMessageException) {
						MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
						if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
							return Single.just(Optional.empty());
						}
					}
					throw new RuntimeException(t);
				}).blockingGet();

		String crDescription = branchName != null ? String.format("Mesh CR for Project %s (Branch %s)", projectName, branchName)
				: String.format("Mesh CR for project %s", projectName);
		if (nodeResponse == null || !nodeResponse.isPresent()) {
			if (expectExistence) {
				fail(String.format("Did not find expected %s in %s %s", object, crDescription, message));
			}
		} else {
			NodeResponse response = nodeResponse.get();
			if (!expectExistence) {
				fail(String.format("Found unexpected %s in %s %s", object, crDescription, message));
			} else {
				String description = String.format("CMS ID of object %s in %s %s", object, crDescription, message);
				assertThat(response.getFields().getNumberField("cms_id").getNumber()).as(description).isEqualTo(object.getId());
				for (Consumer<NodeResponse> asserter : asserters) {
					asserter.accept(response);
				}
			}
		}
	}

	/**
	 * Assert existence of version branches. The last specified version is expected to be the "latest" version (i.e. the branches must also be tagged as "latest")
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param expectVersionLess true if version less branches are expected
	 * @param channelsAndVersions list of channels and versions
	 * @throws NodeException
	 */
	public static void assertVersionBranches(MeshRestClient client, String projectName, boolean expectVersionLess, Object... channelsAndVersions) throws NodeException {
		List<Node> channels = new ArrayList<>();
		List<String> versions = new ArrayList<>();
		for (Object o : channelsAndVersions) {
			if (o instanceof Node) {
				channels.add((Node)o);
			}
			if (o instanceof String) {
				versions.add((String)o);
			}
		}

		List<BranchResponse> branches = client.findBranches(projectName).blockingGet().getData();

		List<BranchResponse> foundBranches = new ArrayList<>();

		// check branches for the node
		Optional<BranchResponse> found = null;

		// check the branch without version
		found = findBranch(branches);
		assertThat(found).as("Node Branch without version").isPresent();
		foundBranches.add(found.get());

		for (String version : versions) {
			if (version.equals(versions.get(versions.size() - 1))) {
				found = findBranch(branches, version, MeshPublisher.LATEST_TAG);
			} else {
				found = findBranch(branches, version);
			}
			assertThat(found).as("Node Branch for version " + version).isPresent();
			foundBranches.add(found.get());
		}

		// check branches for channels
		for (Node channel : channels) {
			if (expectVersionLess) {
				// check channel branch without version
				found = findBranch(branches, MeshPublisher.getMeshUuid(channel));
				assertThat(found).as("Channel Branch for " + channel + " without version").isPresent();
				foundBranches.add(found.get());
			}

			for (String version : versions) {
				if (version.equals(versions.get(versions.size() - 1))) {
					found = findBranch(branches, version, MeshPublisher.LATEST_TAG, MeshPublisher.getMeshUuid(channel));
				} else {
					found = findBranch(branches, version, MeshPublisher.getMeshUuid(channel));
				}
				assertThat(found).as("Branch for channel " + channel + " and version " + version).isPresent();
				foundBranches.add(found.get());
			}
		}

		assertThat(branches).as("Branch list").containsOnlyElementsOf(foundBranches);
	}

	/**
	 * Find the first branch that has exactly the given tags
	 * @param branches branches to search
	 * @param tags tags
	 * @return optional branch
	 */
	public static Optional<BranchResponse> findBranch(List<BranchResponse> branches, String... tags) {
		return branches.stream().filter(b -> {
			List<String> toFind = Arrays.asList(tags);
			List<String> found = b.getTags().stream().map(TagReference::getName).collect(Collectors.toList());
			return toFind.containsAll(found) && found.containsAll(toFind);
		}).findFirst();
	}

	/**
	 * Assert that the schema exists in the project and has certain properties for the branch
	 * @param client client
	 * @param projectName project name
	 * @param branchName branch name
	 * @param schemaName schema name
	 * @param asserters optional asserters
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void assertSchema(MeshRestClient client, String projectName, String branchName, String schemaName, Consumer<BranchSchemaInfo>... asserters)
			throws NodeException {
		Optional<BranchResponse> optionalBranch = client.findBranches(projectName).blockingGet().getData().stream()
				.filter(branch -> StringUtils.equals(branch.getName(), branchName)).findFirst();
		assertThat(optionalBranch).as("Branch " + branchName + " in project " + projectName).isPresent();

		Optional<BranchSchemaInfo> optionalSchemaInfo = client.getBranchSchemaVersions(projectName, optionalBranch.get().getUuid()).blockingGet()
				.getSchemas().stream().filter(schemaInfo -> StringUtils.equals(schemaInfo.getName(), schemaName)).findFirst();
		assertThat(optionalSchemaInfo).as("Schema " + schemaName + " in branch " + branchName).isPresent();

		for (Consumer<BranchSchemaInfo> asserter : asserters) {
			asserter.accept(optionalSchemaInfo.get());
		}
	}

	/**
	 * Get the Job List
	 * @param client client
	 * @return Job List
	 * @throws InterruptedException
	 */
	public static List<JobResponse> jobs(MeshRestClient client) throws InterruptedException {
		return client.findJobs().toSingle().test().await().assertComplete().assertValueCount(1).values().get(0).getData();
	}

	/**
	 * Get the list of new jobs
	 * @param client client
	 * @param oldJobs list of old jobs
	 * @return list of new jobs
	 * @throws InterruptedException
	 */
	public static List<JobResponse> newJobs(MeshRestClient client, List<JobResponse> oldJobs) throws InterruptedException {
		Set<String> oldJobUuids = oldJobs.stream().map(JobResponse::getUuid).collect(Collectors.toSet());
		return client.findJobs().toSingle().test().await().assertComplete().assertValueCount(1).values().get(0).getData().stream()
				.filter(job -> !oldJobUuids.contains(job.getUuid())).collect(Collectors.toList());
	}

	/**
	 * Assert that the current list of jobs contains exactly the expected new jobs (compared to the list of initial jobs)
	 * @param client client
	 * @param initial initial jobs
	 * @param expectedJobs expected new jobs
	 * @throws InterruptedException
	 */
	public static void assertNewJobs(MeshRestClient client, List<JobResponse> initial, Job... expectedJobs) throws InterruptedException {
		assertThat(newJobs(client, initial).stream().map(Job::new).collect(Collectors.toList())).usingFieldByFieldElementComparator()
				.containsOnly(expectedJobs);
	}

	/**
	 * Return predicate to filter branches for channel branch (checking for tag containing the uuid)
	 * @param channel requested channel
	 * @return predicate
	 * @throws NodeException
	 */
	public static Predicate<? super BranchResponse> isChannelBranch(Node channel) throws NodeException {
		return isChannelBranch(channel, null);
	}

	/**
	 * Return predicate to filter branches for channel branch (checking for tag containing the uuid)
	 * @param channel requested channel
	 * @param implementationVersion optional implementation version
	 * @return predicate
	 * @throws NodeException
	 */
	public static Predicate<? super BranchResponse> isChannelBranch(Node channel, String implementationVersion) throws NodeException {
		String tagName = MeshPublisher.getMeshUuid(channel);
		return branch -> {
			if (StringUtils.isEmpty(implementationVersion)) {
				return MeshPublisher.hasTag(branch, MeshPublisher.CHANNEL_UUID_TAGFAMILY, tagName)
						&& !MeshPublisher.hasTag(branch, MeshPublisher.IMPLEMENTATION_VERSION_TAGFAMILY);
			} else {
				return MeshPublisher.hasTag(branch, MeshPublisher.CHANNEL_UUID_TAGFAMILY, tagName) && MeshPublisher
						.hasTag(branch, MeshPublisher.IMPLEMENTATION_VERSION_TAGFAMILY, implementationVersion);
			}
		};
	}

	/**
	 * Return predicate to filter branches for the default brancht
	 * @return predicate
	 * @throws NodeException
	 */
	public static Predicate<? super BranchResponse> isDefaultBranch() throws NodeException {
		return branch -> {
			return !branch.getTags().stream().filter(tag -> MeshPublisher.CHANNEL_UUID_TAGFAMILY.equals(tag.getTagFamily())).findFirst().isPresent();
		};
	}

	/**
	 * Representation of schema/microschema migration Jobs, that can be used for assertions
	 */
	public static class Job {
		protected String status;
		protected String type;
		protected String schemaName;
		protected String branchName;
		protected String fromVersion;
		protected String toVersion;

		/**
		 * Create instance (with status COMPLETED)
		 * @param type type
		 * @param schemaName schema/microschema name
		 * @param branchName branch name
		 * @param fromVersion from version
		 * @param toVersion to version
		 */
		public Job(JobType type, String schemaName, String branchName, String fromVersion, String toVersion) {
			this.type = type.toString();
			this.status = JobStatus.COMPLETED.toString();
			this.schemaName = schemaName;
			this.branchName = branchName;
			this.fromVersion = fromVersion;
			this.toVersion = toVersion;
		}

		/**
		 * Create instance from job response
		 * @param response job response
		 */
		public Job(JobResponse response) {
			type = response.getType().toString();
			status = response.getStatus().toString();
			switch (response.getType()) {
			case schema:
				schemaName = response.getProperties().get("schemaName");
				break;
			case microschema:
				schemaName = response.getProperties().get("microschemaName");
				break;
			default:
				break;
			}
			branchName = response.getProperties().get("branchName");
			fromVersion = response.getProperties().get("fromVersion");
			toVersion = response.getProperties().get("toVersion");
		}

		@Override
		public String toString() {
			return String.format("migrate %s %s from %s to %s in branch %s (%s)", type, schemaName, fromVersion, toVersion, branchName, status);
		}
	}

	public static Observable<MicroschemaResponse> microschemas(MeshRestClient client) {
		return client.findMicroschemas().toSingle().flatMapObservable(resp -> Observable.fromIterable(resp.getData()));
	}

	public static Observable<MicroschemaResponse> microschemas(MeshRestClient client, String projectName) {
		return client.findMicroschemas(projectName).toSingle().flatMapObservable(resp -> Observable.fromIterable(resp.getData()));
	}

	public static Observable<BranchMicroschemaInfo> microschemas(MeshRestClient client, String projectName, String branchUuid) {
		return client.getBranchMicroschemaVersions(projectName, branchUuid).toSingle()
				.flatMapObservable(resp -> Observable.fromIterable(resp.getMicroschemas()));
	}

	/**
	 * Assert existence of a role in Mesh
	 * @param client client
	 * @param roleName role name
	 * @param expected true if role is expected to exist, false if it is expected to not exist
	 * @return role response if role is expected to exist, null otherwise
	 */
	public static RoleResponse assertRole(MeshRestClient client, String roleName, boolean expected) {
		Optional<RoleResponse> optionalRole = client.findRoles().blockingGet().getData().stream()
				.filter(role -> StringUtils.equals(roleName, role.getName())).findFirst();
		if (expected) {
			assertThat(optionalRole).as("Role").isNotEmpty();
			return optionalRole.get();
		} else {
			assertThat(optionalRole).as("Role").isEmpty();
			return null;
		}
	}

	/**
	 * Assert that hte given throwable is a {@link MeshRestClientMessageException} (or has one as it's cause) with the given expected status
	 * @param e throwable
	 * @param expected expected status
	 */
	public static void assertErrorCode(Throwable e, Response.Status expected) {
		if (e instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException mrcme = (MeshRestClientMessageException) e;
			assertThat(Response.Status.fromStatusCode(mrcme.getStatusCode())).as("Response Error Status").isEqualTo(expected);
		} else {
			if (e.getCause() != null && e.getCause() != e) {
				assertErrorCode(e.getCause(), expected);
			} else {
				fail(String.format(
						"Throwable is expected to be a MeshRestClientMessageException or have one at it's cause, but was a %s instead",
						e.getClass().getName()));
			}
		}
	}
}
