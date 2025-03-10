package com.gentics.contentnode.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Simple Exception mapper that is used to catch unhandled exceptions.
 * 
 * @author johannes2
 * 
 */
@Provider
public class ExceptionRestModelMapper extends AbstractExceptionMapper implements ExceptionMapper<Exception> {
	@Override
	public Response toResponse(Exception e) {

		logger.error("Unhandled REST API error: ", e);
		tryRollback(e);

		GenericResponse response = new GenericResponse();

		// Add generic response info
		ResponseInfo responseInfo = new ResponseInfo();
		responseInfo.setResponseCode(ResponseCode.FAILURE);
		response.setResponseInfo(responseInfo);

		// Add i18n message
		Message message = null;
		if (TransactionManager.getCurrentTransactionOrNull() == null) {
			// start a new transaction, if no transaction is currently running
			try (Trx trx = ContentNodeHelper.trx()) {
				String messageText = I18NHelper.get("rest.general.error");
				message = new Message(Type.CRITICAL, messageText);
				responseInfo.setResponseMessage(messageText);
				trx.success();
			} catch (NodeException e1) {
				logger.error("Error while translating message", e1);
				message = new Message(Type.CRITICAL, I18NHelper.get("rest.general.error"));
			}
		} else {
			String messageText = I18NHelper.get("rest.general.error");
			message = new Message(Type.CRITICAL, messageText);
			responseInfo.setResponseMessage(messageText);
		}
		if (message != null) {
			response.addMessage(message);
		}

		return generate(response, Response.Status.INTERNAL_SERVER_ERROR);
	}
}
