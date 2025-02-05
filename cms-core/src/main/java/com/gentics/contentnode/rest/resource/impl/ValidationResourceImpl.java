/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: ValidationResource.java,v 1.1.2.5 2011-03-07 18:42:01 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.resource.impl;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.resource.ValidationResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.validation.map.NodeInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FileDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FileNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FolderDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FolderNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.FsPathInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.GenericInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.GroupDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.GroupNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.HostNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.InputChannel;
import com.gentics.contentnode.validation.map.inputchannels.MimeTypeInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.NodeDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.NodeNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.PageDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.PageNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.RoleDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.RoleNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.TagPartInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.UserDescriptionInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.UserEmailInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.UserFirstLastNameInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.UserMessageInputChannel;
import com.gentics.contentnode.validation.map.inputchannels.UserNameInputChannel;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.contentnode.validation.validator.ValidationException;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;

/**
 * REST API for validating user input.
 * TODO: 
 */
@Path("/validate")
@Consumes({ "text/html; charset=UTF-8" })
public class ValidationResourceImpl extends AuthenticatedContentNodeResource implements ValidationResource {

	private boolean formattedError = false;

	/**
	 * @param formattedError If true, the response will include a formatted error
	 *   message.
	 */
	@QueryParam("formattedError")
	public void setFormattedError(boolean formattedError) {
		this.formattedError = formattedError;
	}

	@POST
	@Path("/tagPart/{partId}")
	public Response validateTagPart(
			@PathParam("partId")
	int partId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		Transaction transaction = TransactionManager.getCurrentTransaction();
		Part part = (Part) transaction.getObject(Part.class, new Integer(partId));

		if (null == part) {
			throw new EntityNotFoundException("Unable to find part with partId `" + partId + "'");
		}
		return validate(new TagPartInputChannel(part), unsafe);
	}
    
	@POST
	@Path("/genericInput")
	public Response validateGenericInput(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new GenericInputChannel(), unsafe);
	}
    
	@POST
	@Path("/userName")
	public Response validateUserName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserNameInputChannel(), unsafe);
	}
        
	@POST
	@Path("/userEmail")
	public Response validateUserEmail(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserEmailInputChannel(), unsafe);
	}

	@POST
	@Path("/userFirstName")
	public Response validateUserFirstName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserFirstLastNameInputChannel(), unsafe);
	}
    
	@POST
	@Path("/userLastName")
	public Response validateUserLastName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserFirstLastNameInputChannel(), unsafe);
	}

	@POST
	@Path("/userDescription")
	public Response validateUserDescription(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserDescriptionInputChannel(), unsafe);
	}

	@POST
	@Path("/nodeName")
	public Response validateNodeName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new NodeNameInputChannel(), unsafe);
	}

	@POST
	@Path("/hostName")
	public Response validateHostName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new HostNameInputChannel(), unsafe);
	}

	@POST
	@Path("/fsPath")
	public Response validateFsPath(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new FsPathInputChannel(), unsafe);
	}

	@POST
	@Path("/nodeDescription")
	public Response validateNodeDescription(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new NodeDescriptionInputChannel(), unsafe);
	}

	@POST
	@Path("/userMessage")
	public Response validateUserMessage(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new UserMessageInputChannel(), unsafe);
	}

	@POST
	@Path("/groupName")
	public Response validateGroupName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new GroupNameInputChannel(), unsafe);
	}

	@POST
	@Path("/groupDescription")
	public Response validateGroupDescription(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new GroupDescriptionInputChannel(), unsafe);
	}
    
	@POST
	@Path("/roleName")
	public Response validateRoleName(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new RoleNameInputChannel(), unsafe);
	}

	@POST
	@Path("/roleDescription")
	public Response validateRoleDescription(String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new RoleDescriptionInputChannel(), unsafe);
	}

	@POST
	@Path("/nodeInput/{nodeId}")
	public Response validateNodeInput(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new NodeInputChannel(nodeId), unsafe);
	}

	@POST
	@Path("/nodeInput/{nodeId}/folderName")
	public Response validateFolderName(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new FolderNameInputChannel(nodeId), unsafe);
	}

	@POST
	@Path("/nodeInput/{nodeId}/folderDescription")
	public Response validateFolderDescription(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new FolderDescriptionInputChannel(nodeId), unsafe);
	}

	@POST
	@Path("/nodeInput/{nodeId}/pageName")
	public Response validatePageName(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new PageNameInputChannel(nodeId), unsafe);
	}

	@POST
	@Path("/nodeInput/{nodeId}/pageDescription")
	public Response validatePageDescription(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new PageDescriptionInputChannel(nodeId), unsafe);
	}
    
	@POST
	@Path("/nodeInput/{nodeId}/fileDescription")
	public Response validateFileDescription(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new FileDescriptionInputChannel(nodeId), unsafe);
	}
    
	@POST
	@Path("/nodeInput/{nodeId}/fileName")
	public Response validateFileName(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new FileNameInputChannel(nodeId), unsafe);
	}
    
	@POST
	@Path("/nodeInput/{nodeId}/mimeType")
	public Response validateMimeType(
			@PathParam("nodeId")
	int nodeId,
			String unsafe) throws NodeException, ValidatorInstantiationException, ValidationException {
		return validate(new MimeTypeInputChannel(nodeId), unsafe);
	}

	protected Response validate(InputChannel inputChannel, String unsafe) throws ValidatorInstantiationException, NodeException, ValidationException {
		ValidationResult result = ValidationUtils.validate(inputChannel, unsafe);        

		return Response.ok().entity(ModelBuilder.getValidationResultResponse(result, formattedError)).build();
	}
}
