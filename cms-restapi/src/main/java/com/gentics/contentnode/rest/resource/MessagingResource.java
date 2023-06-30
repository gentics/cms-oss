package com.gentics.contentnode.rest.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gentics.contentnode.rest.model.request.MessageSendRequest;
import com.gentics.contentnode.rest.model.request.MessagesReadRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Messaging resource to access the inbox (list, view, mark read, delete) and
 * send messages
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/msg")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface MessagingResource {
	/**
	 * Delete message with given ID
	 * @param id message id
	 * @return response
	 * @throws Exception
	 */
	@DELETE
	@Path("/{id}")
	@StatusCodes({
		@ResponseCode(code = 204, condition = "Message {id} was deleted."),
		@ResponseCode(code = 404, condition = "Message {id} does not exist.")
	})
	Response delete(@PathParam("id") int id) throws Exception;

	/**
	 * Send a message to users/groups
	 * 
	 * @param request
	 *            request to send messages
	 * @return generic response
	 */
	@POST
	@Path("/send")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Message was sent.")
	})
	GenericResponse send(MessageSendRequest request) throws Exception;

	/**
	 * List messages for the current user
	 * 
	 * @param unread
	 *            true if only unread messages shall be shown, false for all
	 *            messages (default)
	 * @return response containing the messages
	 */
	@GET
	@Path("/list")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Message list is returned.")
	})
	GenericResponse list(
			@QueryParam("unread") @DefaultValue("false") boolean unread) throws Exception;

	/**
	 * Set messages to be read
	 * 
	 * @param request
	 *            request containing the list of messages to be set read
	 * @return generic response
	 */
	@POST
	@Path("/read")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Messages were set to be read.")
	})
	GenericResponse read(MessagesReadRequest request) throws Exception;
}
