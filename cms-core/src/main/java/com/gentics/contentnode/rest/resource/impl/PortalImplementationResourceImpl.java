package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.PortalImplementationFileFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.object.PortalImplementationFile;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.PortalImplementationFileListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationFileModel;
import com.gentics.contentnode.rest.model.PortalImplementationFolderModel;
import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PortalImplementationFileResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationFolderResponse;
import com.gentics.contentnode.rest.model.response.PortalImplementationResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.PortalImplementationResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;

/**
 * Implementation of {@link PortalImplementationResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/portalimplementations")
@Authenticated
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, bit = PermHandler.PERM_VIEW)
public class PortalImplementationResourceImpl implements PortalImplementationResource {
	@Override
	@GET
	public PortalImplementationListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			trx.success();
			Set<Integer> ids = DBUtils.select("SELECT id FROM " + C.Tables.PORTAL_IMPLEMENTATION, DBUtils.IDS);
			return ListBuilder.from(trx.getTransaction().getObjects(PortalImplementation.class, ids), PortalImplementation.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "keyword", "name", "description", "type"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "keyword", "name", "description", "type", "cdate", "edate"))
					.page(paging).to(new PortalImplementationListResponse());
		}
	}

	@Override
	@POST
	@RequiredPerm(type = PortalImplementation.TYPE_PORTAL_IMPLEMENTATIONS, bit = PermHandler.PERM_CONTENTREPOSITORY_CREATE)
	public PortalImplementationResponse add(PortalImplementationModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = trx.getTransaction().createObject(PortalImplementation.class);
			if (!StringUtils.isBlank(item.getGlobalId())) {
				GlobalId globalId = new GlobalId(item.getGlobalId());
				if (globalId.getLocalId(PortalImplementation.class) != null) {
					throw new RestMappedException(I18NHelper.get("portal_implementation.duplicate.globalid", item.getGlobalId()))
							.setMessageType(Message.Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
				}
				impl.setGlobalId(globalId);
			}
			PortalImplementation.REST2NODE.apply(item, impl);
			impl.save();
			trx.success();
			return new PortalImplementationResponse(PortalImplementation.TRANSFORM2REST.apply(impl),
					ResponseInfo.ok("Successfully created Portal Implementation"));
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public PortalImplementationResponse get(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			trx.success();
			return new PortalImplementationResponse(PortalImplementation.TRANSFORM2REST.apply(impl),
					ResponseInfo.ok("Successfully loaded Portal Implementation"));
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	public PortalImplementationResponse update(@PathParam("id") String id, PortalImplementationModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			impl = trx.getTransaction().getObject(impl, true);
			PortalImplementation.REST2NODE.apply(item, impl);
			impl.save();
			impl = trx.getTransaction().getObject(impl);
			trx.success();
			return new PortalImplementationResponse(PortalImplementation.TRANSFORM2REST.apply(impl),
					ResponseInfo.ok("Successfully updated Portal Implementation"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public Response delete(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.delete);
			impl = trx.getTransaction().getObject(impl, true);
			impl.delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/nodes")
	public NodeList getNodes(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			List<com.gentics.contentnode.rest.model.Node> nodes = new ArrayList<>();
			for (Node node : impl.getNodes()) {
				if (ObjectPermission.view.checkObject(node.getFolder())) {
					nodes.add(Node.TRANSFORM2REST.apply(node));
				}
			}
			NodeList response = new NodeList();
			response.setItems(nodes);
			response.setResponseInfo(ResponseInfo.ok("Successfully loaded nodes of the Portal Implementation"));
			trx.success();
			return response;
		}
	}
	@Override
	@GET
	@Path("/{id}/folders{path: (/.*)?}")
	public PortalImplementationFolderResponse getFolder(@PathParam("id") String id, @PathParam("path") String path) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			PortalImplementationFolderModel folder = getFolderModel(id, impl, PortalImplementationFileFactory.normalize(path));
			trx.success();
			return new PortalImplementationFolderResponse(folder, ResponseInfo.ok("Successfully loaded folder"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/folders/{path: .+}")
	public PortalImplementationFolderResponse moveFolder(@PathParam("id") String id, @PathParam("path") String path, PortalImplementationFolderModel folder)
			throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			String from = PortalImplementationFileFactory.normalize(path);
			List<PortalImplementationFile> files = loadFolderFiles(id, impl, from);

			String to = PortalImplementationFileFactory.normalize(folder != null ? folder.getPath() : null);
			PortalImplementationFileFactory.validatePath(to);

			if (!to.equals(from)) {
				// a folder cannot be moved into itself
				if (to.startsWith(from + PortalImplementationFile.SEPARATOR)) {
					throw new RestMappedException(I18NHelper.get("portal_implementation_file.invalidpath", to)).setMessageType(Message.Type.CRITICAL)
							.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
				}
				// the target folder must not exist
				if (impl.getFile(to) != null || !impl.getFiles(to).isEmpty()) {
					throw new RestMappedException(I18NHelper.get("portal_implementation_file.conflict", to, to)).setMessageType(Message.Type.CRITICAL)
							.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
				}

				for (PortalImplementationFile file : files) {
					PortalImplementationFile editable = t.getObject(file, true);
					editable.setPath(to + file.getPath().substring(from.length()));
					editable.save();
				}
			}

			PortalImplementationFolderModel moved = getFolderModel(id, impl, to);
			trx.success();
			return new PortalImplementationFolderResponse(moved, ResponseInfo.ok("Successfully moved folder"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/folders/{path: .+}")
	public Response deleteFolder(@PathParam("id") String id, @PathParam("path") String path) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			for (PortalImplementationFile file : loadFolderFiles(id, impl, PortalImplementationFileFactory.normalize(path))) {
				trx.getTransaction().getObject(file, true).delete();
			}
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/files")
	public PortalImplementationFileListResponse listFiles(@PathParam("id") String id, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			trx.success();
			return ListBuilder.from(impl.getFiles(null), PortalImplementationFile.TRANSFORM2REST)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "path", "name", "folder", "mimetype"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "path", "name", "folder", "mimetype", "filesize", "cdate", "edate"))
					.page(paging).to(new PortalImplementationFileListResponse());
		}
	}

	@Override
	@GET
	@Path("/{id}/files/{path: .+}")
	public PortalImplementationFileResponse getFile(@PathParam("id") String id, @PathParam("path") String path) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			PortalImplementationFile file = loadFile(id, impl, path);
			trx.success();
			return new PortalImplementationFileResponse(PortalImplementationFile.TRANSFORM2REST.apply(file), ResponseInfo.ok("Successfully loaded file"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/files/{path: .+}")
	public PortalImplementationFileResponse updateFile(@PathParam("id") String id, @PathParam("path") String path, PortalImplementationFileModel file)
			throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			PortalImplementationFile editable = t.getObject(loadFile(id, impl, path), true);
			if (file != null) {
				if (file.getPath() != null) {
					editable.setPath(PortalImplementationFileFactory.normalize(file.getPath()));
				}
				if (file.getMimetype() != null) {
					editable.setMimetype(file.getMimetype());
				}
			}
			editable.save();
			PortalImplementationFile updated = t.getObject(PortalImplementationFile.class, editable.getId());
			trx.success();
			return new PortalImplementationFileResponse(PortalImplementationFile.TRANSFORM2REST.apply(updated), ResponseInfo.ok("Successfully updated file"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/files/{path: .+}")
	public Response deleteFile(@PathParam("id") String id, @PathParam("path") String path) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			trx.getTransaction().getObject(loadFile(id, impl, path), true).delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/content/{path: .+}")
	@Produces(MediaType.WILDCARD)
	public Response getContent(@PathParam("id") String id, @PathParam("path") String path) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id);
			PortalImplementationFile file = loadFile(id, impl, path);
			java.io.File binFile = file.getBinFile();
			if (!binFile.exists()) {
				throw new NodeException("Contents of " + file + " not found in " + binFile.getAbsolutePath());
			}
			trx.success();
			return Response.ok(binFile, file.getMimetype()).build();
		}
	}

	@Override
	@PUT
	@Path("/{id}/content/{path: .+}")
	@Consumes(MediaType.WILDCARD)
	public PortalImplementationFileResponse putContent(@PathParam("id") String id, @PathParam("path") String path,
			@HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType, InputStream content) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			PortalImplementation impl = MiscUtils.load(PortalImplementation.class, id, ObjectPermission.edit);
			String normalizedPath = PortalImplementationFileFactory.normalize(path);

			PortalImplementationFile file = impl.getFile(normalizedPath);
			if (file == null) {
				file = t.createObject(PortalImplementationFile.class);
				file.setPortalImplementationId(impl.getId());
				file.setPath(normalizedPath);
			} else {
				file = t.getObject(file, true);
			}

			file.setContents(content != null ? content : new ByteArrayInputStream(new byte[0]));
			// without a specific content type, the mimetype will be detected
			if (StringUtils.isBlank(contentType) || contentType.startsWith(MediaType.APPLICATION_OCTET_STREAM)) {
				file.setMimetype(null);
			} else {
				file.setMimetype(contentType);
			}
			file.save();

			PortalImplementationFile saved = t.getObject(PortalImplementationFile.class, file.getId());
			trx.success();
			return new PortalImplementationFileResponse(PortalImplementationFile.TRANSFORM2REST.apply(saved), ResponseInfo.ok("Successfully stored file"));
		}
	}

	/**
	 * Load the file with given path
	 * @param id ID of the portal implementation (as given in the request)
	 * @param impl portal implementation
	 * @param path path of the file
	 * @return file
	 * @throws NodeException if the file does not exist
	 */
	protected PortalImplementationFile loadFile(String id, PortalImplementation impl, String path) throws NodeException {
		PortalImplementationFile file = impl.getFile(path);
		if (file == null) {
			throw new EntityNotFoundException(I18NHelper.get("portal_implementation_file.notfound", PortalImplementationFileFactory.normalize(path), id));
		}
		return file;
	}

	/**
	 * Load the files contained in the folder (including subfolders)
	 * @param id ID of the portal implementation (as given in the request)
	 * @param impl portal implementation
	 * @param folder normalized path of the folder
	 * @return non-empty list of files
	 * @throws NodeException if the folder does not exist
	 */
	protected List<PortalImplementationFile> loadFolderFiles(String id, PortalImplementation impl, String folder) throws NodeException {
		List<PortalImplementationFile> files = impl.getFiles(folder);
		if (files.isEmpty()) {
			throw new EntityNotFoundException(I18NHelper.get("portal_implementation_folder.notfound", folder, id));
		}
		return files;
	}

	/**
	 * Get the model of the folder, containing the direct subfolders and files
	 * @param id ID of the portal implementation (as given in the request)
	 * @param impl portal implementation
	 * @param folder normalized path of the folder (empty for the root folder)
	 * @return folder model
	 * @throws NodeException if the folder does not exist
	 */
	protected PortalImplementationFolderModel getFolderModel(String id, PortalImplementation impl, String folder) throws NodeException {
		List<PortalImplementationFile> files = StringUtils.isEmpty(folder) ? impl.getFiles(null) : loadFolderFiles(id, impl, folder);
		int prefixLength = StringUtils.isEmpty(folder) ? 0 : folder.length() + 1;

		Set<String> subfolders = new TreeSet<>();
		List<PortalImplementationFile> directFiles = new ArrayList<>();
		for (PortalImplementationFile file : files) {
			String relativePath = file.getPath().substring(prefixLength);
			if (relativePath.contains(PortalImplementationFile.SEPARATOR)) {
				subfolders.add(StringUtils.substringBefore(relativePath, PortalImplementationFile.SEPARATOR));
			} else {
				directFiles.add(file);
			}
		}

		List<PortalImplementationFileModel> fileModels = new ArrayList<>();
		for (PortalImplementationFile file : directFiles) {
			fileModels.add(PortalImplementationFile.TRANSFORM2REST.apply(file));
		}
		fileModels.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

		return new PortalImplementationFolderModel().setPath(folder)
				.setName(StringUtils.substringAfterLast(PortalImplementationFile.SEPARATOR + folder, PortalImplementationFile.SEPARATOR))
				.setFolders(new ArrayList<>(subfolders)).setFiles(fileModels);
	}
}
