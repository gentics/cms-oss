package com.gentics.contentnode.rest.resource.impl;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.SimplePublishInfo;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.admin.PublishInfoResponse;

/**
 * Resource for management of the publish process (start/stop/status).
 * TODO: When the new scheduler REST API is done, think about migrating this endpoints to the scheduler
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/publisher")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
public class PublisherResourceImpl {
	/**
	 * Cancel the running publish process
	 * @return publish info response
	 * @throws NodeException
	 */
	@DELETE
	public GenericResponse stopPublish(@QueryParam("block") @DefaultValue("false") boolean block,
			@QueryParam("wait") @DefaultValue("0") long waitMs) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PublishController.stopPublish(block, waitMs);

			PublishInfoResponse response = getPublishInfo();

			// if we waited for the publish process to stop, but it is still running, something went wrong
			// so we log the states of the publish threads
			if (block && response.isRunning()) {
				PublishController.logStackTraces();
			}

			trx.success();
			return response;
		}
	}

	/**
	 * Get the publish info from the {@link PublishController}.
	 *
	 * <p>
	 *     <strong>IMPORTANT:</strong> This method must be called inside a
	 *     {@link ContentNodeHelper#trx()} block.
	 * </p>
	 *
	 * @return The publish info
	 * @throws NodeException
	 */
	static PublishInfoResponse getPublishInfo() throws NodeException {
		PublishInfoResponse response = new PublishInfoResponse();
		response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));
		PublishInfo currentInfo = PublishController.getPublishInfo();
		PublishInfo previousInfo = PublishController.getPreviousPublishInfo();

		if (currentInfo == null) {
			currentInfo = previousInfo;
			previousInfo = null;
		}

		if (currentInfo == null) {
			currentInfo = new SimplePublishInfo();
		}

		PublishInfo.NODE2REST.apply(currentInfo, response);

		if (previousInfo != null) {
			response.setLastFailed(previousInfo.getReturnCode() != PublishInfo.RETURN_CODE_SUCCESS);
		}

		return response;
	}
}
