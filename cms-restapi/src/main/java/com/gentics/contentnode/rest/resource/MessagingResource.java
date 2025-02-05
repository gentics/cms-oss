package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
