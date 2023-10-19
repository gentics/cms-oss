package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TagmapEntry;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.exceptions.DuplicateEntityException;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.MeshRolesRequest;
import com.gentics.contentnode.rest.model.TagmapEntryConsistencyResponse;
import com.gentics.contentnode.rest.model.TagmapEntryInconsistencyModel;
import com.gentics.contentnode.rest.model.TagmapEntryListResponse;
import com.gentics.contentnode.rest.model.TagmapEntryModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.MeshRolesResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TagmapEntryResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.lib.i18n.CNI18nString;

import io.reactivex.Flowable;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/contentrepositories")
@Authenticated
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = ContentRepository.TYPE_CONTENTREPOSITORIES, bit = PermHandler.PERM_VIEW)
public class ContentRepositoryResourceImpl implements ContentRepositoryResource {
	@Override
	@GET
	public ContentRepositoryListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			trx.success();
			Set<Integer> ids = DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS);
			return ListBuilder.from(trx.getTransaction().getObjects(ContentRepository.class, ids), ContentRepository.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "crType", "dbType", "username", "url", "basepath"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name", "crType", "dbType", "username", "url", "basepath",
							"instantPublishing", "languageInformation", "permissionInformation", "diffDelete", "checkDate", "checkStatus",
							"statusDate", "dataStatus"))
					.page(paging).to(new ContentRepositoryListResponse());
		}
	}

	@Override
	@POST
	@RequiredPerm(type = ContentRepository.TYPE_CONTENTREPOSITORIES, bit = PermHandler.PERM_CONTENTREPOSITORY_CREATE)
	public ContentRepositoryResponse add(ContentRepositoryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = trx.getTransaction().createObject(ContentRepository.class);
			ContentRepository.REST2NODE.apply(item, cr);
			addDefaultTagmapEntries(cr);
			cr.save();
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(cr), ResponseInfo.ok("Successfully created ContentRepository"));
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public ContentRepositoryResponse get(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(cr), ResponseInfo.ok("Successfully loaded ContentRepository"));
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	public ContentRepositoryResponse update(@PathParam("id") String id, ContentRepositoryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			// get editable copy of object
			cr = trx.getTransaction().getObject(cr, true);
			// change data and save
			ContentRepository.REST2NODE.apply(item, cr);
			cr.save();
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(cr), ResponseInfo.ok("Successfully updated ContentRepository"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public Response delete(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.delete);
			// get editable copy of object
			cr = trx.getTransaction().getObject(cr, true);
			// delete cr
			cr.delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@PUT
	@Path("/{id}/structure/check")
	public ContentRepositoryResponse check(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception {
		return internalCheck(id, false, waitMs);
		}

	@Override
	@PUT
	@Path("/{id}/structure/repair")
	public ContentRepositoryResponse repair(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) throws Exception {
		return internalCheck(id, true, waitMs);
		}

	@Override
	@PUT
	@Path("/{id}/data/check")
	public ContentRepositoryResponse checkData(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = internalCheckData(id, false);
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(cr), ResponseInfo.ok("Successfully queued datacheck for ContentRepository"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/data/repair")
	public ContentRepositoryResponse repairData(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = internalCheckData(id, true);
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(cr), ResponseInfo.ok("Successfully queued datacheck (with repair) for ContentRepository"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/copy")
	@RequiredPerm(type = ContentRepository.TYPE_CONTENTREPOSITORIES, bit = PermHandler.PERM_CONTENTREPOSITORY_CREATE)
	public ContentRepositoryResponse copy(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			ContentRepository newCr = trx.getTransaction().createObject(ContentRepository.class);
			newCr.copyFrom(cr);
			newCr.save();
			trx.success();
			return new ContentRepositoryResponse(ContentRepository.TRANSFORM2REST.apply(newCr), ResponseInfo.ok("Successfully created ContentRepository as copy"));
		}
	}

	@Override
	@GET
	@Path("/{id}/entries")
	public TagmapEntryListResponse listEntries(@PathParam("id") String id, @QueryParam("fragments") @DefaultValue("false") boolean fragments, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			trx.success();
			return ListBuilder.from(fragments ? cr.getAllEntries() : cr.getEntries(), TagmapEntry.TRANSFORM2REST)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "tagname", "mapname", "foreignlinkAttribute", "foreignlinkAttributeRule", "category", "fragmentName"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "tagname", "mapname", "object", "attributeType", "targetType", "multivalue", "optimized",
							"reserved", "filesystem", "foreignlinkAttribute", "foreignlinkAttributeRule", "category", "segmentfield", "displayfield", "urlfield", "noindex", "fragmentName"))
					.page(paging).to(new TagmapEntryListResponse());
		}
	}

	@Override
	@GET
	@Path("/{id}/entries/check")
	public TagmapEntryConsistencyResponse checkEntryConsistency(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			List<TagmapEntryInconsistencyModel> inconsistencies = cr.checkEntryConsistency();
			trx.success();
			return ListBuilder.from(inconsistencies, i -> i).to(new TagmapEntryConsistencyResponse());
		}
	}

	@Override
	@POST
	@Path("/{id}/entries")
	public TagmapEntryResponse addEntry(@PathParam("id") String id, TagmapEntryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			cr = trx.getTransaction().getObject(cr, true);
			TagmapEntry entry = TagmapEntry.REST2NODE.apply(item, trx.getTransaction().createObject(TagmapEntry.class));
			cr.getEntries().add(entry);
			cr.save();
			trx.success();
			return new TagmapEntryResponse(TagmapEntry.TRANSFORM2REST.apply(entry), new ResponseInfo(ResponseCode.OK, "Successfully created tagmap entry"));
		}
	}

	@Override
	@GET
	@Path("/{id}/entries/{entryId}")
	public TagmapEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			TagmapEntry entry = MiscUtils.get(TagmapEntry.class, cr.getEntries(), entryId);
			trx.success();
			return new TagmapEntryResponse(TagmapEntry.TRANSFORM2REST.apply(entry), new ResponseInfo(ResponseCode.OK, "Successfully fetched tagmap entry"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/entries/{entryId}")
	public TagmapEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId, TagmapEntryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			cr = trx.getTransaction().getObject(cr, true);

			TagmapEntry entry = MiscUtils.get(TagmapEntry.class, cr.getEntries(), entryId);
			TagmapEntry.REST2NODE.apply(item, entry);

			if (cr.getCrType() == Type.mesh) {
				// there may only be one segmentfield
				if (ObjectTransformer.getBoolean(item.getSegmentfield(), false)) {
					for (TagmapEntry otherEntry : cr.getEntries()) {
						if (otherEntry.getObject() == entry.getObject() && !otherEntry.equals(entry)) {
							otherEntry.setSegmentfield(false);
						}
					}
				}

				// there may only be one displayfield
				if (ObjectTransformer.getBoolean(item.getDisplayfield(), false)) {
					for (TagmapEntry otherEntry : cr.getEntries()) {
						if (otherEntry.getObject() == entry.getObject() && !otherEntry.equals(entry)) {
							otherEntry.setDisplayfield(false);
						}
					}
				}
			}

			cr.save();
			trx.success();

			return new TagmapEntryResponse(TagmapEntry.TRANSFORM2REST.apply(entry), new ResponseInfo(ResponseCode.OK, "Successfully updated tagmap entry"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/entries/{entryId}")
	public Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			cr = trx.getTransaction().getObject(cr, true);

			TagmapEntry entry = MiscUtils.get(TagmapEntry.class, cr.getEntries(), entryId);

			// not allowed to delete reserved tagmap entries
			if (entry.isStatic()) {
				throw new InsufficientPrivilegesException(I18NHelper.get("tagmap.static.delete", entry.getMapname()), entry, PermType.update);
			}

			cr.getEntries().remove(entry);

			cr.save();
			trx.success();

			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/cr_fragments")
	public ContentRepositoryFragmentListResponse listCrFragments(@PathParam("id") String id, @BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			trx.success();
			return ListBuilder.from(cr.getAssignedFragments(), CrFragment::getModel)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name"))
					.page(paging).to(new ContentRepositoryFragmentListResponse());
		}
	}

	@Override
	@GET
	@Path("/{id}/cr_fragments/{crFragmentId}")
	public ContentRepositoryFragmentResponse getCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			CrFragment crFragment = getCrFragment(cr, crFragmentId);
			trx.success();
			return new ContentRepositoryFragmentResponse(crFragment.getModel(), ResponseInfo.ok("Successfully loaded Cr Fragment"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/cr_fragments/{crFragmentId}")
	public Response addCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			CrFragment crFragment = MiscUtils.load(CrFragment.class, crFragmentId);

			// check for duplicates
			if (cr.getAssignedFragments().contains(crFragment)) {
				throw new DuplicateEntityException(I18NHelper.get("contentrepository.add_cr_fragment.duplicate", crFragment.getName(), cr.getName()));
			}

			cr = trx.getTransaction().getObject(cr, true);
			cr.getAssignedFragments().add(crFragment);
			cr.save();
			trx.success();
			return Response.created(null).build();
		}
	}

	@Override
	@DELETE
	@Path("/{id}/cr_fragments/{crFragmentId}")
	public Response removeCrFragment(@PathParam("id") String id, @PathParam("crFragmentId") String crFragmentId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
			CrFragment crFragment = MiscUtils.load(CrFragment.class, crFragmentId);

			cr = trx.getTransaction().getObject(cr, true);
			cr.getAssignedFragments().remove(crFragment);
			cr.save();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/roles")
	public MeshRolesResponse getRoles(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			assertMeshCr(cr);

			MeshRolesResponse response = new MeshRolesResponse();
			response.setResponseInfo(ResponseInfo.ok(""));
			try (MeshPublisher mp = new MeshPublisher(cr, false)) {
				Set<Datasource> datasources = mp.getRoleDatasources();
				response.setRoles(Flowable.fromIterable(datasources).flatMapIterable(ds -> ds.getEntries()).map(DatasourceEntry::getKey).distinct().toList()
						.blockingGet());
			}
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/availableroles")
	public MeshRolesResponse getAvailableRoles(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			assertMeshCr(cr);

			MeshRolesResponse response = new MeshRolesResponse();
			response.setResponseInfo(ResponseInfo.ok(""));
			try (MeshPublisher mp = new MeshPublisher(cr, true)) {
				List<String> available = new ArrayList<>(mp.getRoleMap().keySet());
				available.sort(String::compareTo);
				response.setRoles(available);
			}

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}/roles")
	public MeshRolesResponse setRoles(@PathParam("id") String id, MeshRolesRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepository cr = MiscUtils.load(ContentRepository.class, id);
			assertMeshCr(cr);

			try (MeshPublisher mp = new MeshPublisher(cr, true)) {
				// get the available roles and check whether all requested roles are available
				Set<String> available = mp.getRoleMap().keySet();

				List<String> diff = new ArrayList<>(request.getRoles());
				diff.removeAll(available);
				if (!diff.isEmpty()) {
					throw new RestMappedException(I18NHelper.get("meshcr.unknown.roles", diff.toString())).setResponseCode(ResponseCode.INVALIDDATA)
							.setStatus(Status.BAD_REQUEST);
				}

				Set<Datasource> datasources = mp.getRoleDatasources();
				for (Datasource datasource : datasources) {
					MiscUtils.check(datasource, PermType.update, (d, ph) -> ph.canEdit(d));
				}
				mp.setRoles(datasources, request.getRoles());
			}

			trx.success();
		}

		return getRoles(id);
	}

	/**
	 * Check/Repair a ContentRepository
	 * @param id ID
	 * @param repair true to repair
	 * @param waitMs wait time in ms
	 * @return ContentRepository instance
	 * @throws NodeException
	 */
	protected ContentRepositoryResponse internalCheck(String id, boolean repair, long waitMs) throws NodeException {
		CNI18nString description = new CNI18nString(repair ? "repair": "check");
		try (Trx trx = ContentNodeHelper.trx()) {
		// find the object
		ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);

			// set the check status to "running"
			DBUtils.update("UPDATE contentrepository SET checkstatus = ?, checkresult = ? WHERE id = ?",
					ContentRepository.DATACHECK_STATUS_RUNNING, "", cr.getId());

			trx.getTransaction().dirtObjectCache(ContentRepository.class, cr.getId());
			trx.success();
		}

		GenericResponse opResponse = null;
		try (Trx trx = ContentNodeHelper.trx()) {
			opResponse = Operator.executeLocked(description.toString(), waitMs, null, () -> {
		// check structure
				MiscUtils.load(ContentRepository.class, id).checkStructure(repair);
				return new GenericResponse(null, ResponseInfo.ok(repair ? "Successfully repaired ContentRepository" : "Successfully checked ContentRepository"));
			});
			trx.success();
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			ContentRepositoryResponse response = new ContentRepositoryResponse();
			ContentRepository checkedCr = MiscUtils.load(ContentRepository.class, id);
			response.setContentRepository(ContentRepository.TRANSFORM2REST.apply(checkedCr));
			response.setMessages(opResponse.getMessages());
			response.setResponseInfo(opResponse.getResponseInfo());
			trx.success();
			return response;
	}
	}

	/**
	 * Check/Repair data of a ContentRepository
	 * @param id ID
	 * @param repair true to repair
	 * @return ContentRepository instance
	 * @throws NodeException
	 */
	protected ContentRepository internalCheckData(String id, boolean repair) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// find the object
		ContentRepository cr = MiscUtils.load(ContentRepository.class, id, ObjectPermission.edit);
		// check data
		cr.checkData(repair);

		return t.getObject(cr);
	}

	/**
	 * Add the default tagmap entries to the (new) CR
	 * @param cr Cr
	 * @throws NodeException
	 */
	protected void addDefaultTagmapEntries(ContentRepository cr) throws NodeException {
		switch (cr.getCrType()) {
		case cr:
		case mccr:
			// folders
			cr.addEntry("folder.name", "name", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("folder.description", "description", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("folder.creator", "creator", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("folder.creationtimestamp", "createtimestamp", Folder.TYPE_FOLDER, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("folder.creator.email", "creatoremail", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("folder.editor", "editor", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("folder.edittimestamp", "edittimestamp", Folder.TYPE_FOLDER, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("folder.editor.email", "editoremail", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("folder.mother", "folder_id", Folder.TYPE_FOLDER, Folder.TYPE_FOLDER, AttributeType.link, false, true, true, "");
			cr.addEntry("", "obj_type", Folder.TYPE_FOLDER, 0, AttributeType.integer, false, true, false, "");
			cr.addEntry("", "contentid", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("folder.pub_dir", "pub_dir", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("", "subfolder_id", Folder.TYPE_FOLDER, Folder.TYPE_FOLDER, AttributeType.foreignlink, false, true, false, "folder_id");
			cr.addEntry("", "subpage_id", Folder.TYPE_FOLDER, Page.TYPE_PAGE, AttributeType.foreignlink, false, true, false, "folder_id");
			cr.addEntry("", "subfile_id", Folder.TYPE_FOLDER, File.TYPE_FILE, AttributeType.foreignlink, false, true, false, "folder_id");
			cr.addEntry("node.id", "node_id", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("folder.path", "url", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, false, "");

			// pages
			cr.addEntry("page.name", "name", Page.TYPE_PAGE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("page.description", "description", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("page.creator", "creator", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("page.creationtimestamp", "createtimestamp", Page.TYPE_PAGE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("page.creator.email", "creatoremail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("page.editor", "editor", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("page.edittimestamp", "edittimestamp", Page.TYPE_PAGE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("page.editor.email", "editoremail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("page.publisher", "publisher", Page.TYPE_PAGE, 0, AttributeType.text, false, false, true, "");
			cr.addEntry("page.publishtimestamp", "publishtimestamp", Page.TYPE_PAGE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("page.publisher.email", "publishermail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("folder.id", "folder_id", Page.TYPE_PAGE, Folder.TYPE_FOLDER, AttributeType.link, false, true, true, "");
			cr.addEntry("", "obj_type", Page.TYPE_PAGE, 0, AttributeType.integer, false, true, false, "");
			cr.addEntry("", "contentid", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("page.priority", "priority", Page.TYPE_PAGE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("", "content", Page.TYPE_PAGE, 0, AttributeType.longtext, false, true, false, "");
			cr.addEntry("page.language.id", "languageid", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("page.url", "url", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("page.language.code", "languagecode", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("node.id", "node_id", Page.TYPE_PAGE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("page.ml_id", "ml_id", Page.TYPE_PAGE, 0, AttributeType.integer, false, true, false, "");
			cr.addEntry("page.filename", "filename", Page.TYPE_PAGE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("page.folder.pub_dir", "pub_dir", Page.TYPE_PAGE, 0, AttributeType.text, false, true, true, "");

			// files
			cr.addEntry("file.description", "description", File.TYPE_FILE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("file.creator", "creator", File.TYPE_FILE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("file.createtimestamp", "createtimestamp", File.TYPE_FILE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("file.creator.email", "creatoremail", File.TYPE_FILE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("file.editor", "editor", File.TYPE_FILE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("file.edittimestamp", "edittimestamp", File.TYPE_FILE, 0, AttributeType.integer, false, false, false, "");
			cr.addEntry("file.editor.email", "editoremail", File.TYPE_FILE, 0, AttributeType.text, false, false, false, "");
			cr.addEntry("", "obj_type", File.TYPE_FILE, 0, AttributeType.integer, false, true, false, "");
			cr.addEntry("", "contentid", File.TYPE_FILE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("file.url", "url", File.TYPE_FILE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("folder.id", "folder_id", File.TYPE_FILE, Folder.TYPE_FOLDER, AttributeType.link, false, true, true, "");
			cr.addEntry("file.name", "name", File.TYPE_FILE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("file.type", "mimetype", File.TYPE_FILE, 0, AttributeType.text, false, true, false, "");
			cr.addEntry("binarycontent", "binarycontent", File.TYPE_FILE, 0, AttributeType.binary, false, true, false, "");
			cr.addEntry("node.id", "node_id", File.TYPE_FILE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("file.name", "filename", File.TYPE_FILE, 0, AttributeType.text, false, true, true, "");
			cr.addEntry("file.folder.pub_dir", "pub_dir", File.TYPE_FILE, 0, AttributeType.text, false, true, true, "");

			break;
		case mesh:
			// folders
			cr.addEntry("folder.id", "cms_id", Folder.TYPE_FOLDER, 0, AttributeType.integer, false, true, false, false, false, false);
			cr.addEntry("folder.name", "name", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, false, true, false, false);
			cr.addEntry("folder.description", "description", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, false, false, false, false);
			cr.addEntry("folder.pub_dir", "pub_dir", Folder.TYPE_FOLDER, 0, AttributeType.text, false, true, true, false, false, false);
			cr.addEntry("object.startpage.parts.url.target", "startpage", Folder.TYPE_FOLDER, Page.TYPE_PAGE, AttributeType.link, false, false, false, false, false, false);
			cr.addEntry("object.navhidden", "navhidden", Folder.TYPE_FOLDER, 0, AttributeType.bool, false, false, false, false, false, false);
			cr.addEntry("object.navsortorder", "navsortorder", Folder.TYPE_FOLDER, 0, AttributeType.integer, false, false, false, false, false, false);
			cr.addEntry("folder.creator", "creator", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("folder.creationtimestamp", "createdate", Folder.TYPE_FOLDER, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("folder.creator.email", "creatoremail", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("folder.editor", "editor", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("folder.edittimestamp", "editdate", Folder.TYPE_FOLDER, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("folder.editor.email", "editoremail", Folder.TYPE_FOLDER, 0, AttributeType.text, false, false, false, false, false, false);

			// pages
			cr.addEntry("page.id", "cms_id", Page.TYPE_PAGE, 0, AttributeType.integer, false, true, false, false, false, false);
			cr.addEntry("page.name", "name", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, true, false, false);
			cr.addEntry("page.description", "description", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, false, false, false);
			cr.addEntry("", "content", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.filename", "filename", Page.TYPE_PAGE, 0, AttributeType.text, false, true, true, false, false, false);
			cr.addEntry("page.template.ml.contenttype", "contenttype", Page.TYPE_PAGE, 0, AttributeType.text, false, true, false, false, false, false);
			cr.addEntry("page.creator", "creator", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.creationtimestamp", "createdate", Page.TYPE_PAGE, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("page.creator.email", "creatoremail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.editor", "editor", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.edittimestamp", "editdate", Page.TYPE_PAGE, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("page.editor.email", "editoremail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.publisher", "publisher", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("page.publishtimestamp", "publishtimestamp", Page.TYPE_PAGE, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("page.publisher.email", "publishermail", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("object.templateName", "templateName", Page.TYPE_PAGE, 0, AttributeType.text, false, false, false, false, false, false);

			// files
			cr.addEntry("file.id", "cms_id", File.TYPE_FILE, 0, AttributeType.integer, false, true, false, false, false, false);
			cr.addEntry("file.name", "name", File.TYPE_FILE, 0, AttributeType.text, false, true, false, true, false, false);
			cr.addEntry("file.type", "mimetype", File.TYPE_FILE, 0, AttributeType.text, false, true, false, false, false, false);
			cr.addEntry("file.description", "description", File.TYPE_FILE, 0, AttributeType.text, false, true, false, false, false, false);
			cr.addEntry("binarycontent", "binarycontent", File.TYPE_FILE, 0, AttributeType.binary, false, true, true, false, false, false);
			cr.addEntry("file.creator", "creator", File.TYPE_FILE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("file.createtimestamp", "createdate", File.TYPE_FILE, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("file.creator.email", "creatoremail", File.TYPE_FILE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("file.editor", "editor", File.TYPE_FILE, 0, AttributeType.text, false, false, false, false, false, false);
			cr.addEntry("file.edittimestamp", "editdate", File.TYPE_FILE, 0, AttributeType.date, false, false, false, false, false, false);
			cr.addEntry("file.editor.email", "editoremail", File.TYPE_FILE, 0, AttributeType.text, false, false, false, false, false, false);

			break;
		}
	}

	protected CrFragment getCrFragment(ContentRepository cr, String crFragmentId) throws NodeException {
		CrFragment crFragment = cr.getAssignedFragments().stream().filter(fr -> {
			return crFragmentId.equals(ObjectTransformer.getString(fr.getId(), null))
					|| crFragmentId.equals(ObjectTransformer.getString(fr.getGlobalId(), null));
		}).findFirst().orElseThrow(() -> new EntityNotFoundException(I18NHelper.get("cr_fragment.notfound", crFragmentId)));
		return MiscUtils.load(CrFragment.class, crFragment.getId().toString());
	}

	protected void assertMeshCr(ContentRepository cr) throws NodeException {
		if (cr.getCrType() != ContentRepositoryModel.Type.mesh) {
			String translatedType = I18NHelper.get("crtype." + cr.getCrType().name() + ".short");
			throw new RestMappedException(I18NHelper.get("contentrepository.invalidtype", cr.getName(), translatedType)).setResponseCode(ResponseCode.INVALIDDATA)
					.setStatus(Status.CONFLICT);
		}
	}
}
