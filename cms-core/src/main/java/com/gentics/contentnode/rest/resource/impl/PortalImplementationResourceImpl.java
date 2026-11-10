package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import java.util.Set;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.PortalImplementationListResponse;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.Message;
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
}
