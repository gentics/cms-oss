package com.gentics.contentnode.rest.resource.impl;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.MaintenanceMode;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.InfoResource;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/info")
public class InfoResourceImpl implements InfoResource {
	/**
	 * Logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(InfoResourceImpl.class);

	@Override
	@GET
	@Path("/maintenance")
	public MaintenanceResponse getMaintenance() {
		try {
			return Trx.supply(() -> {
				return MaintenanceMode.TRANSFORM2REST.apply(MaintenanceMode.get());
			});
		} catch (NodeException e) {
			logger.error("Error while getting info", e);
			I18nString message = new CNI18nString("rest.general.error");
			return new MaintenanceResponse(new Message(Message.Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while getting info: " + e.getLocalizedMessage()));
		}
	}
}
