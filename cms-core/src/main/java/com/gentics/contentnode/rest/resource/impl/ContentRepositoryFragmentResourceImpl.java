package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import java.util.Set;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.object.cr.CrFragmentEntry;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentEntryModel;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentListResponse;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentEntryResponse;
import com.gentics.contentnode.rest.model.response.ContentRepositoryFragmentResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.ContentRepositoryFragmentResource;
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
 * Implementation of {@link ContentRepositoryFragmentResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/cr_fragments")
@Authenticated
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = CrFragment.TYPE_CR_FRAGMENTS, bit = PermHandler.PERM_VIEW)
public class ContentRepositoryFragmentResourceImpl implements ContentRepositoryFragmentResource {
	@Override
	@GET
	public ContentRepositoryFragmentListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			trx.success();
			Set<Integer> ids = DBUtils.select("SELECT id FROM cr_fragment", DBUtils.IDS);
			return ListBuilder.from(trx.getTransaction().getObjects(CrFragment.class, ids), CrFragment::getModel)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "name"))
					.page(paging).to(new ContentRepositoryFragmentListResponse());
		}
	}

	@Override
	@POST
	@RequiredPerm(type = CrFragment.TYPE_CR_FRAGMENTS, bit = PermHandler.PERM_CONTENTREPOSITORY_CREATE)
	public ContentRepositoryFragmentResponse add(ContentRepositoryFragmentModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = trx.getTransaction().createObject(CrFragment.class);
			fragment.fromModel(item);
			fragment.save();
			trx.success();
			return new ContentRepositoryFragmentResponse(fragment.getModel(), ResponseInfo.ok("Successfully created ContentRepository Fragment"));
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public ContentRepositoryFragmentResponse get(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id);
			trx.success();
			return new ContentRepositoryFragmentResponse(fragment.getModel(), ResponseInfo.ok("Successfully loaded ContentRepository Fragment"));
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	public ContentRepositoryFragmentResponse update(@PathParam("id") String id, ContentRepositoryFragmentModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id, ObjectPermission.edit);
			// get editable copy of object
			fragment = trx.getTransaction().getObject(fragment, true);
			// change data and save
			fragment.fromModel(item);
			fragment.save();
			trx.success();
			return new ContentRepositoryFragmentResponse(fragment.getModel(), ResponseInfo.ok("Successfully updated ContentRepository Fragment"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public Response delete(@PathParam("id") String id) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id, ObjectPermission.delete);
			// get editable copy of object
			fragment = trx.getTransaction().getObject(fragment, true);
			// delete cr
			fragment.delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/entries")
	public ContentRepositoryFragmentEntryListResponse listEntries(@PathParam("id") String id, @BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment cr = MiscUtils.load(CrFragment.class, id);
			trx.success();
			return ListBuilder.from(cr.getEntries(), CrFragmentEntry::getModel)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "tagname", "mapname", "foreignlinkAttribute", "foreignlinkAttributeRule", "category"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "tagname", "mapname", "objType", "attributeType", "targetType", "multivalue", "optimized",
							"filesystem", "foreignlinkAttribute", "foreignlinkAttributeRule", "category", "segmentfield", "displayfield", "urlfield", "noIndex"))
					.page(paging).to(new ContentRepositoryFragmentEntryListResponse());
		}
	}

	@Override
	@POST
	@Path("/{id}/entries")
	public ContentRepositoryFragmentEntryResponse addEntry(@PathParam("id") String id, ContentRepositoryFragmentEntryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id, ObjectPermission.edit);
			fragment = trx.getTransaction().getObject(fragment, true);
			CrFragmentEntry entry = trx.getTransaction().createObject(CrFragmentEntry.class);
			entry.fromModel(item);
			fragment.getEntries().add(entry);
			fragment.save();
			trx.success();
			return new ContentRepositoryFragmentEntryResponse(entry.getModel(), new ResponseInfo(ResponseCode.OK, "Successfully created fragment entry"));
		}
	}

	@Override
	@GET
	@Path("/{id}/entries/{entryId}")
	public ContentRepositoryFragmentEntryResponse getEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id);
			CrFragmentEntry entry = MiscUtils.get(CrFragmentEntry.class, fragment.getEntries(), entryId);
			trx.success();
			return new ContentRepositoryFragmentEntryResponse(entry.getModel(), new ResponseInfo(ResponseCode.OK, "Successfully fetched fragment entry"));
		}
	}

	@Override
	@PUT
	@Path("/{id}/entries/{entryId}")
	public ContentRepositoryFragmentEntryResponse updateEntry(@PathParam("id") String id, @PathParam("entryId") String entryId,
			ContentRepositoryFragmentEntryModel item) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id, ObjectPermission.edit);
			fragment = trx.getTransaction().getObject(fragment, true);

			CrFragmentEntry entry = MiscUtils.get(CrFragmentEntry.class, fragment.getEntries(), entryId);
			entry.fromModel(item);
			// there may only be one segmentfield
			if (ObjectTransformer.getBoolean(item.getSegmentfield(), false)) {
				for (CrFragmentEntry otherEntry : fragment.getEntries()) {
					if (otherEntry.getObjType() == entry.getObjType() && !otherEntry.equals(entry)) {
						otherEntry.setSegmentfield(false);
					}
				}
			}

			// there may only be one displayfield
			if (ObjectTransformer.getBoolean(item.getDisplayfield(), false)) {
				for (CrFragmentEntry otherEntry : fragment.getEntries()) {
					if (otherEntry.getObjType() == entry.getObjType() && !otherEntry.equals(entry)) {
						otherEntry.setDisplayfield(false);
					}
				}
			}

			fragment.save();
			trx.success();

			return new ContentRepositoryFragmentEntryResponse(entry.getModel(), new ResponseInfo(ResponseCode.OK, "Successfully updated fragment entry"));
		}
	}

	@Override
	@DELETE
	@Path("/{id}/entries/{entryId}")
	public Response deleteEntry(@PathParam("id") String id, @PathParam("entryId") String entryId) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			CrFragment fragment = MiscUtils.load(CrFragment.class, id, ObjectPermission.edit);
			fragment = trx.getTransaction().getObject(fragment, true);

			CrFragmentEntry entry = MiscUtils.get(CrFragmentEntry.class, fragment.getEntries(), entryId);

			fragment.getEntries().remove(entry);

			fragment.save();
			trx.success();

			return Response.noContent().build();
		}
	}
}
