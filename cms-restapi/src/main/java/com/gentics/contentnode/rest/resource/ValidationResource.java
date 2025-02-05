package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * REST API for validating user input.
 */
@Path("/validate")
@Consumes({ "text/html; charset=UTF-8" })
public interface ValidationResource extends AuthenticatedResource {

	/**
	 * @param formattedError
	 *            If true, the response will include a formatted error message.
	 */
	@QueryParam("formattedError")
	void setFormattedError(boolean formattedError);

	@POST
	@Path("/tagPart/{partId}")
	Response validateTagPart(@PathParam("partId") int partId, String unsafe) throws Exception;

	@POST
	@Path("/genericInput")
	Response validateGenericInput(String unsafe) throws Exception;

	@POST
	@Path("/userName")
	Response validateUserName(String unsafe) throws Exception;

	@POST
	@Path("/userEmail")
	Response validateUserEmail(String unsafe) throws Exception;

	@POST
	@Path("/userFirstName")
	Response validateUserFirstName(String unsafe) throws Exception;

	@POST
	@Path("/userLastName")
	Response validateUserLastName(String unsafe) throws Exception;

	@POST
	@Path("/userDescription")
	Response validateUserDescription(String unsafe) throws Exception;

	@POST
	@Path("/nodeName")
	Response validateNodeName(String unsafe) throws Exception;

	@POST
	@Path("/hostName")
	Response validateHostName(String unsafe) throws Exception;

	@POST
	@Path("/fsPath")
	Response validateFsPath(String unsafe) throws Exception;

	@POST
	@Path("/nodeDescription")
	Response validateNodeDescription(String unsafe) throws Exception;

	@POST
	@Path("/userMessage")
	Response validateUserMessage(String unsafe) throws Exception;

	@POST
	@Path("/groupName")
	Response validateGroupName(String unsafe) throws Exception;

	@POST
	@Path("/groupDescription")
	Response validateGroupDescription(String unsafe) throws Exception;

	@POST
	@Path("/roleName")
	Response validateRoleName(String unsafe) throws Exception;

	@POST
	@Path("/roleDescription")
	Response validateRoleDescription(String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}")
	Response validateNodeInput(@PathParam("nodeId") int nodeId, String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/folderName")
	Response validateFolderName(@PathParam("nodeId") int nodeId, String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/folderDescription")
	Response validateFolderDescription(@PathParam("nodeId") int nodeId,
			String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/pageName")
	Response validatePageName(@PathParam("nodeId") int nodeId, String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/pageDescription")
	Response validatePageDescription(@PathParam("nodeId") int nodeId,
			String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/fileDescription")
	Response validateFileDescription(@PathParam("nodeId") int nodeId,
			String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/fileName")
	Response validateFileName(@PathParam("nodeId") int nodeId, String unsafe) throws Exception;

	@POST
	@Path("/nodeInput/{nodeId}/mimeType")
	Response validateMimeType(@PathParam("nodeId") int nodeId, String unsafe) throws Exception;
}
