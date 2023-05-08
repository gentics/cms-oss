package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;

import java.util.List;

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

import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.RolePermissions;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.RoleModel;
import com.gentics.contentnode.rest.model.RolePermissionsModel;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.role.RoleListResponse;
import com.gentics.contentnode.rest.model.response.role.RolePermResponse;
import com.gentics.contentnode.rest.model.response.role.RoleResponse;
import com.gentics.contentnode.rest.resource.RoleResource;
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
 * Resource implementation for roles
 */
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Authenticated
@RequiredPerm(type = Role.TYPE_ROLE, bit = PermHandler.PERM_VIEW)
@Path("/role")
public class RoleResourceImpl implements RoleResource {

	@GET
	@Override
	public RoleListResponse list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging,
			@BeanParam PermsParameterBean perms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			List<Role> roles = t.getObjects(Role.class, DBUtils.select("SELECT id FROM role", DBUtils.IDS));

			RoleListResponse response = ListBuilder.from(roles, Role.TRANSFORM2REST)
					.filter(PermFilter.get(ObjectPermission.view))
					.filter(ResolvableFilter.get(filter, "id", "name", "description"))
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "name", "description"))
					.page(paging)
					.to(new RoleListResponse());

			trx.success();
			return response;
		}
	}

	@PUT
	@Override
	public RoleResponse create(RoleModel role) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, Role.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get(String.format("%s.nopermission", t.getTable(Role.class))), null, null, Role.TYPE_ROLE,
						0, PermType.create);
			}

			Role nodeRole = Role.REST2NODE.apply(role, t.createObject(Role.class));
			nodeRole.save();

			RoleResponse response = new RoleResponse(null, ResponseInfo.ok("Successfully created role"), Role.TRANSFORM2REST.apply(t.getObject(nodeRole)));

			trx.success();
			return response;
		}
	}

	@GET
	@Path("/{id}")
	@Override
	public RoleResponse get(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Role role = MiscUtils.load(Role.class, id);

			RoleResponse response = new RoleResponse(null, ResponseInfo.ok("Successfully loaded role"), Role.TRANSFORM2REST.apply(role));
			trx.success();
			return response;
		}
	}

	@POST
	@Path("/{id}")
	@Override
	public RoleResponse update(@PathParam("id") String id, RoleModel role) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Role nodeRole = MiscUtils.load(Role.class, id, ObjectPermission.edit);

			nodeRole = Role.REST2NODE.apply(role, t.getObject(nodeRole, true));
			nodeRole.save();

			RoleResponse response = new RoleResponse(null, ResponseInfo.ok("Successfully updated role"), Role.TRANSFORM2REST.apply(t.getObject(nodeRole)));

			trx.success();
			return response;
		}
	}

	@DELETE
	@Path("/{id}")
	@Override
	public Response delete(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Role nodeRole = MiscUtils.load(Role.class, id, ObjectPermission.delete);

			t.getObject(nodeRole, true).delete();

			trx.success();
			return Response.noContent().build();
		}
	}

	@GET
	@Path("/{id}/perm")
	@Override
	public RolePermResponse getPerm(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Role role = MiscUtils.load(Role.class, id);

			RolePermissions rolePerm = PermissionStore.getInstance().getRolePerm(role.getId());
			RolePermissionsModel perms = RolePermissions.TRANSFORM2REST.apply(rolePerm);

			RolePermResponse response = new RolePermResponse().setPerm(perms);
			response.setResponseInfo(ResponseInfo.ok("Successfully fetched role permissions"));
			trx.success();
			return response;
		}
	}

	@POST
	@Path("/{id}/perm")
	@Override
	public RolePermResponse updatePerm(@PathParam("id") String id, RolePermissionsModel updatedPerms) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Role role = MiscUtils.load(Role.class, id, ObjectPermission.edit);

			RolePermissions rolePerm = RolePermissions.REST2NODE.apply(updatedPerms, PermissionStore.getInstance().getRolePerm(role.getId()).clone());
			PermHandler.setRolePermissions(role.getId(), rolePerm);
			trx.success();
		} catch (CloneNotSupportedException e) {
			if (e.getCause() instanceof NodeException) {
				throw (NodeException)e.getCause();
			} else {
				throw new NodeException(e);
			}
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			Role role = MiscUtils.load(Role.class, id);

			RolePermissions rolePerm = PermissionStore.getInstance().getRolePerm(role.getId());
			RolePermissionsModel perms = RolePermissions.TRANSFORM2REST.apply(rolePerm);

			RolePermResponse response = new RolePermResponse().setPerm(perms);
			response.setResponseInfo(ResponseInfo.ok("Successfully updated role permissions"));
			trx.success();
			return response;
		}
	}
}
