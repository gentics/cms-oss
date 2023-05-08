package com.gentics.contentnode.rest.resource;


import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.GroupsPermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for reading and writing permissions.<P>
 *
 * The following table can be used to look up the bit positions (starting at 0) of the folder permissions:
 *
 *<table>
 *	<thead>
 *		<tr><th>Type</th><th>Short</th><th>Description</th><th>Bit</th></tr>
 *	</thead>
 *	<tbody>
 *		<tr><td>Folder</td><td>s</td><td>Show</td><td>0</td></tr>
 *		<tr><td></td><td>r</td><td>Assign user permissions</td><td>1</td></tr>
 *		<tr><td></td><td>c</td><td>Create</td><td>8</td></tr>
 *		<tr><td></td><td>e</td><td>Edit</td><td>9</td></tr>
 *		<tr><td></td><td>d</td><td>Delete</td><td>10</td></tr>
 *		<tr><td>Pages/Images/Files</td><td>s</td><td>Show</td><td>11</td></tr>
 *		<tr><td></td><td>c</td><td>Create</td><td>12</td></tr>
 *		<tr><td></td><td>e</td><td>Edit</td><td>13</td></tr>
 *		<tr><td></td><td>d</td><td>Delete</td><td>14</td></tr>
 *		<tr><td></td><td>i</td><td>Import</td><td>23</td></tr>
 *		<tr><td>Pages</td><td>p</td><td>Publish</td><td>19</td></tr>
 *		<tr><td>Templates</td><td>s</td><td>Show</td><td>15</td></tr>
 *		<tr><td></td><td>c</td><td>Create</td><td>16</td></tr>
 *		<tr><td></td><td>l</td><td>Link</td><td>21</td></tr>
 *		<tr><td></td><td>e</td><td>Edit</td><td>17</td></tr>
 *		<tr><td></td><td>d</td><td>Delete</td><td>18</td></tr>
 *		<tr><td>Workflow</td><td>v</td><td>Link</td><td>22</td></tr>
 *	</tbody>
 *</table>
 *<P>
 * The following table can be used to look up the bit positions (starting at 0) of the role permissions:
 *
 *<table>
 *	<thead>
 *		<tr><th>Type</th><th>Description</th><th>Bit</th></tr>
 *	</thead>
 *	<tbody>
 *		<tr><td>Role permissions</td><td>Show</td><td>10</td></tr>
 *		<tr><td></td><td>Create</td><td>11</td></tr>
 *		<tr><td></td><td>Modify</td><td>12</td></tr>
 *		<tr><td></td><td>Delete</td><td>13</td></tr>
 *		<tr><td></td><td>Publishing</td><td>14</td></tr>
 *		<tr><td></td><td>Translate</td><td>15</td></tr>
 *	</tbody>
 *</table>
 */
@Path("/perm")
@Produces(MediaType.APPLICATION_JSON)
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions."),
})
public interface PermResource {
	/**
	 * Get the permission bits valid for the current user and the given type
	 * @param objType object type
	 * @param privilegeMap true if the privileges should also be returned as map
	 * @return permission bits response
	 * @throws Exception
	 */
	@GET
	@Path("/{type}")
	PermBitsResponse getPermissions(@PathParam("type") String objType, @QueryParam("map") @DefaultValue("false") boolean privilegeMap) throws Exception;

	/**
	 * Get the permission bits valid for the current user on the given object (and optionally for the given node).<p>
	 * See the class description for the permission bits.<br>
	 * To get the folder permissions you need to provide the type (10002 = Folder, 10001 = Node) and the object id.<br>
	 * To get the role permissions you need to provide the languageId and the type to check for (10007 = Pages, 10008 = Files).<br>
	 *
	 * @param objType object type (10002 = Folder, 10001 = Node)
	 * @param objId object id
	 * @param nodeId optional node id
	 * @param checkType optional type for which the role permission should be returned (10007 = Pages, 10008 = Files)
	 * @param languageId optional language id for which the role permissions should be returned
	 * @param privilegeMap true if the privileges should also be returned as map
	 * @return permission bits response
	 * @throws Exception
	 */
	@GET
	@Path("/{type}/{id}")
	PermBitsResponse getPermissions(@PathParam("type") String objType, @PathParam("id") int objId, @QueryParam("nodeId") @DefaultValue("0") int nodeId,
			@QueryParam("type") @DefaultValue("-1") int checkType, @QueryParam("lang") @DefaultValue("0") int languageId,
			@QueryParam("map") @DefaultValue("false") boolean privilegeMap) throws Exception;

	/**
	 * Check whether the user has permission perm on the object defined by type and id
	 * @param perm permission
	 * @param objType object type
	 * @param objId object id
	 * @param nodeId node id
	 * @return response containing the result
	 * @throws Exception
	 */
	@GET
	@Path("/{perm}/{type}/{id}")
	PermResponse getObjectPermission(@PathParam("perm") Permission perm, @PathParam("type") String objType, @PathParam("id") int objId,
			@QueryParam("nodeId") @DefaultValue("0") int nodeId) throws Exception;

	/**
	 * Set the permissions on the type
	 * @param type type
	 * @param waitMs wait timeout in milliseconds
	 * @param req request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/{type}")
	GenericResponse setPermissions(@PathParam("type") String type, @QueryParam("wait") @DefaultValue("0") long waitMs, SetPermsRequest req) throws Exception;

	/**
	 * Set the permissions on the identified object according to the posted request
	 * @param objType object type
	 * @param objId object id
	 * @param waitMs wait timeout in milliseconds
	 * @param req request
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/{type}/{id}")
	GenericResponse setPermissions(@PathParam("type") String objType, @PathParam("id") int objId, @QueryParam("wait") @DefaultValue("0") long waitMs,
			SetPermsRequest req) throws Exception;

	/**
	 * List all groups with with their permission bits for the given object type
	 * This only lists groups that the current user actually
	 * has permission to view (his own groups and their sub groups)
	 *
	 * @param objType  Type of the object
	 * @return generic GroupsPermBitsResponse object
	 * @throws Exception
	 */
	@GET
	@Path("/list/{type}")
	GroupsPermBitsResponse list(@PathParam("type") String objType) throws Exception;

	/**
	 * List all groups with with their permission bits for the given object
	 * This only lists groups that the current user actually
	 * has permission to view (his own groups and their sub groups)
	 *
	 * @param objType  Type of the object
	 * @param objId    ID of the object
	 * @return generic GroupsPermBitsResponse object
	 * @throws Exception
	 */
	@GET
	@Path("/list/{type}/{id}")
	GroupsPermBitsResponse list(@PathParam("type") String objType, @PathParam("id") int objId) throws Exception;
}
