package com.gentics.contentnode.publish.mesh;

import static com.gentics.contentnode.publish.mesh.MeshPublishUtils.ifNotFound;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;

import io.reactivex.Completable;

/**
 * Helper class for publishing {@link PortalImplementation}s into a Mesh ContentRepository.
 * Portal Implementations are published as nodes of the schema [prefix]_implementation, which are direct children of the project root node.
 * The files of a Portal Implementation are published as zip file into the binary field {@link #PACKAGE_FIELD}.
 */
public class MeshPortalImplementationPublisher {
	/**
	 * Suffix of the schema name
	 */
	public final static String SCHEMA_SUFFIX = "implementation";

	/**
	 * Name of the keyword field
	 */
	public final static String KEYWORD_FIELD = "keyword";

	/**
	 * Name of the name field
	 */
	public final static String NAME_FIELD = "name";

	/**
	 * Name of the description field
	 */
	public final static String DESCRIPTION_FIELD = "description";

	/**
	 * Name of the binary field containing the zipped files
	 */
	public final static String PACKAGE_FIELD = "package";

	/**
	 * Mimetype of the package
	 */
	public final static String PACKAGE_MIMETYPE = "application/zip";

	/**
	 * Language of the nodes (Portal Implementations are language independent)
	 */
	public final static String LANGUAGE = "en";

	/**
	 * Fixed modification time of the zip entries (2000-01-01T00:00:00Z), so that the zip file only depends on the paths and contents of the files
	 */
	protected final static long ZIP_ENTRY_TIME = 946684800000L;

	/**
	 * Page size for reading the children of the project root node
	 */
	protected final static int PAGE_SIZE = 1000;

	/**
	 * Mesh publisher
	 */
	protected MeshPublisher publisher;

	/**
	 * Create an instance
	 * @param publisher Mesh publisher
	 */
	public MeshPortalImplementationPublisher(MeshPublisher publisher) {
		this.publisher = publisher;
	}

	/**
	 * Get the uuid of the Mesh node for the portal implementation in the project with given uuid.
	 * Since uuids in Mesh are unique over all projects, and a portal implementation can be published into several projects
	 * (e.g. when assigned to several nodes, which publish into separate projects), the uuid is derived from the project uuid and the
	 * global ID of the portal implementation (name based UUID).
	 * @param impl portal implementation
	 * @param projectUuid uuid of the Mesh project
	 * @return uuid of the Mesh node (without dashes)
	 * @throws NodeException
	 */
	public static String getMeshUuid(PortalImplementation impl, String projectUuid) throws NodeException {
		if (StringUtils.isEmpty(projectUuid)) {
			throw new NodeException(String.format("Cannot determine Mesh uuid for %s without project uuid", impl));
		}
		String name = String.format("%s:%s", projectUuid, MeshPublisher.getMeshUuid(impl));
		return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
	}

	/**
	 * Get the name of the schema for Portal Implementations
	 * @return schema name
	 */
	public String getSchemaName() {
		return String.format("%s_%s", publisher.getSchemaPrefix(), SCHEMA_SUFFIX);
	}

	/**
	 * Fill the given schema model with the expected schema for Portal Implementations
	 * @param schema schema model
	 * @return schema model
	 */
	public <T extends SchemaModel> T getSchema(T schema) {
		schema.setName(getSchemaName());
		schema.setContainer(false);
		// portal implementations are never indexed
		schema.setNoIndex(true);
		schema.setDisplayField(NAME_FIELD);
		schema.addField(new StringFieldSchemaImpl().setName(KEYWORD_FIELD).setLabel(KEYWORD_FIELD));
		schema.addField(new StringFieldSchemaImpl().setName(NAME_FIELD).setLabel(NAME_FIELD));
		schema.addField(new StringFieldSchemaImpl().setName(DESCRIPTION_FIELD).setLabel(DESCRIPTION_FIELD));
		schema.addField(new BinaryFieldSchemaImpl().setName(PACKAGE_FIELD).setLabel(PACKAGE_FIELD));
		return schema;
	}

	/**
	 * Check the Portal Implementations assigned to the given nodes in the branch of the project.
	 * If repair is true, Portal Implementations, which do not exist in Mesh or differ from the CMS, are written, implementation nodes, which do not correspond to
	 * an assigned Portal Implementation, are deleted. If repair is false, missing, outdated and obsolete implementation nodes are only logged.
	 * @param project Mesh project
	 * @param branch branch parameter
	 * @param nodes nodes publishing into the branch of the project
	 * @param repair true to repair (write and delete), false to only check
	 * @return true, if the implementation nodes are valid (or were repaired), false if not
	 * @throws NodeException
	 */
	public boolean check(MeshProject project, VersioningParameters branch, Set<Node> nodes, boolean repair) throws NodeException {
		// collect the assigned portal implementations
		Map<String, PortalImplementation> assigned = new LinkedHashMap<>();
		for (Node node : nodes) {
			for (PortalImplementation impl : node.getPortalImplementations()) {
				assigned.put(getMeshUuid(impl, project.uuid), impl);
			}
		}

		// read the existing implementation nodes
		Map<String, NodeResponse> existing = getExistingNodes(project, branch);

		boolean valid = true;
		for (Map.Entry<String, PortalImplementation> entry : assigned.entrySet()) {
			valid &= checkNode(project, branch, entry.getKey(), entry.getValue(), existing.get(entry.getKey()), repair);
		}

		// remove implementation nodes, which are not assigned (anymore)
		for (String uuid : existing.keySet()) {
			if (!assigned.containsKey(uuid)) {
				if (repair) {
					publisher.info("Removing portal implementation %s from project %s, branch %s", uuid, project.name, getBranchName(branch));
					publisher.client.deleteNode(project.name, uuid, new DeleteParametersImpl().setRecursive(true), branch).toCompletable()
							.onErrorResumeNext(throwable -> ifNotFound(throwable, () -> Completable.complete())).blockingAwait();
				} else {
					publisher.error("Obsolete portal implementation node %s in project %s, branch %s", uuid, project.name, getBranchName(branch));
					valid = false;
				}
			}
		}
		return valid;
	}

	/**
	 * Get the existing nodes of the implementation schema, which are direct children of the project root node
	 * @param project project
	 * @param branch branch parameter
	 * @return map of node uuids to nodes
	 */
	protected Map<String, NodeResponse> getExistingNodes(MeshProject project, VersioningParameters branch) {
		Map<String, NodeResponse> existing = new HashMap<>();
		String schemaName = getSchemaName();
		long page = 1;
		long pageCount;
		do {
			NodeListResponse children = publisher.client.findNodeChildren(project.name, project.rootNodeUuid, branch,
					new NodeParametersImpl().setLanguages(LANGUAGE), new PagingParametersImpl().setPage(page).setPerPage((long) PAGE_SIZE)).blockingGet();
			for (NodeResponse child : children.getData()) {
				if (child.getSchema() != null && StringUtils.equals(child.getSchema().getName(), schemaName)) {
					existing.put(child.getUuid(), child);
				}
			}
			PagingMetaInfo metaInfo = children.getMetainfo();
			pageCount = metaInfo != null ? metaInfo.getPageCount() : 1;
			page++;
		} while (page <= pageCount);
		return existing;
	}

	/**
	 * Check whether the node of the portal implementation exists and is up to date.
	 * If repair is true, the portal implementation is written into Mesh, if it does not exist or is different
	 * @param project project
	 * @param branch branch parameter
	 * @param uuid uuid of the node
	 * @param impl portal implementation
	 * @param existingNode existing node (may be null)
	 * @param repair true to write the portal implementation, if necessary
	 * @return true, if the node is valid (or was repaired), false if not
	 * @throws NodeException
	 */
	protected boolean checkNode(MeshProject project, VersioningParameters branch, String uuid, PortalImplementation impl, NodeResponse existingNode,
			boolean repair) throws NodeException {
		File zipFile = null;
		try {
			zipFile = File.createTempFile("portalimplementation", ".zip", new File(ConfigurationValue.TMP_PATH.get()));
			String sha512 = createPackage(impl, zipFile);

			boolean fieldsChanged = existingNode == null || fieldsDiffer(impl, existingNode.getFields());
			boolean packageChanged = existingNode == null || packageDiffers(sha512, existingNode.getFields());

			if (!fieldsChanged && !packageChanged) {
				publisher.info("Portal implementation %s in project %s, branch %s is up to date", impl.getKeyword(), project.name, getBranchName(branch));
				return true;
			}

			if (!repair) {
				if (existingNode == null) {
					publisher.error("Portal implementation %s is missing in project %s, branch %s", impl.getKeyword(), project.name, getBranchName(branch));
				} else {
					publisher.error("Portal implementation %s is outdated in project %s, branch %s", impl.getKeyword(), project.name, getBranchName(branch));
				}
				return false;
			}

			publisher.info("Writing portal implementation %s into project %s, branch %s", impl.getKeyword(), project.name, getBranchName(branch));
			if (fieldsChanged) {
				FieldMap fields = new FieldMapImpl();
				fields.put(KEYWORD_FIELD, new StringFieldImpl().setString(impl.getKeyword()));
				fields.put(NAME_FIELD, new StringFieldImpl().setString(impl.getName()));
				fields.put(DESCRIPTION_FIELD, new StringFieldImpl().setString(StringUtils.defaultString(impl.getDescription())));

				NodeUpsertRequest request = new NodeUpsertRequest();
				request.setLanguage(LANGUAGE);
				request.setParentNodeUuid(project.rootNodeUuid);
				request.setSchema(new SchemaReferenceImpl().setName(getSchemaName()));
				request.setFields(fields);
				request.setPublish(true);
				publisher.client.upsertNode(project.name, uuid, request, branch).blockingGet();
			}

			if (packageChanged) {
				try (InputStream in = Files.newInputStream(zipFile.toPath())) {
					publisher.client.updateNodeBinaryField(project.name, uuid, LANGUAGE, "draft", PACKAGE_FIELD, in, zipFile.length(),
							String.format("%s.zip", impl.getKeyword()), PACKAGE_MIMETYPE, true, branch).blockingGet();
				}
			}
			return true;
		} catch (IOException e) {
			throw new NodeException(String.format("Error while publishing %s into project %s", impl, project.name), e);
		} finally {
			if (zipFile != null) {
				zipFile.delete();
			}
		}
	}

	/**
	 * Check whether the string fields of the node differ from the portal implementation
	 * @param impl portal implementation
	 * @param fields fields of the node
	 * @return true, if the fields differ
	 */
	protected boolean fieldsDiffer(PortalImplementation impl, FieldMap fields) {
		return !Objects.equals(impl.getKeyword(), getString(fields, KEYWORD_FIELD))
				|| !Objects.equals(impl.getName(), getString(fields, NAME_FIELD))
				|| !Objects.equals(StringUtils.defaultString(impl.getDescription()), StringUtils.defaultString(getString(fields, DESCRIPTION_FIELD)));
	}

	/**
	 * Check whether the package stored in the node differs from the package with the given hash
	 * @param sha512 hash of the package
	 * @param fields fields of the node
	 * @return true, if the package differs
	 */
	protected boolean packageDiffers(String sha512, FieldMap fields) {
		BinaryField binaryField = fields != null ? fields.getBinaryField(PACKAGE_FIELD) : null;
		return binaryField == null || !StringUtils.equalsIgnoreCase(sha512, binaryField.getSha512sum());
	}

	/**
	 * Get the name of the branch for log output
	 * @param branch branch parameter (may be null)
	 * @return branch name (or "default", if no branch is set)
	 */
	protected String getBranchName(VersioningParameters branch) {
		return branch != null && !StringUtils.isEmpty(branch.getBranch()) ? branch.getBranch() : "default";
	}

	/**
	 * Get the value of a string field
	 * @param fields fields
	 * @param name field name
	 * @return field value or null
	 */
	protected String getString(FieldMap fields, String name) {
		if (fields == null) {
			return null;
		}
		StringFieldImpl field = fields.getStringField(name);
		return field != null ? field.getString() : null;
	}

	/**
	 * Create the package (zip file) containing the files of the portal implementation.
	 * The entries are sorted by path and have a fixed modification time, so that the zip file only depends on the paths and contents.
	 * @param impl portal implementation
	 * @param zipFile target file
	 * @return hex encoded SHA-512 hash of the zip file
	 * @throws NodeException
	 */
	public static String createPackage(PortalImplementation impl, File zipFile) throws NodeException {
		List<PortalImplementationFile> files = new ArrayList<>(impl.getFiles(null));
		files.sort(Comparator.comparing(PortalImplementationFile::getPath));

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			try (OutputStream out = new DigestOutputStream(Files.newOutputStream(zipFile.toPath()), digest);
					ZipOutputStream zip = new ZipOutputStream(out)) {
				for (PortalImplementationFile file : files) {
					ZipEntry entry = new ZipEntry(file.getPath());
					entry.setTime(ZIP_ENTRY_TIME);
					zip.putNextEntry(entry);
					Files.copy(file.getBinFile().toPath(), zip);
					zip.closeEntry();
				}
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new NodeException(String.format("Error while creating package for %s", impl), e);
		}
	}
}
