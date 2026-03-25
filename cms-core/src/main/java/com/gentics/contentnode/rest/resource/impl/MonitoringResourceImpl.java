package com.gentics.contentnode.rest.resource.impl;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.MonitoringException;
import com.gentics.contentnode.monitoring.CmsLivenessManager;
import com.gentics.contentnode.rest.resource.MonitoringResource;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class MonitoringResourceImpl implements MonitoringResource {

	@Override
	public Response live() throws Exception {
		if (CmsLivenessManager.getInstance().isLive()) {
			return Response.ok().build();
		}

		throw new MonitoringException("CMS is not live", Status.SERVICE_UNAVAILABLE);
	}

	@Override
	public Response ready() throws Exception {
		if (!CmsLivenessManager.getInstance().isLive()) {
			throw new MonitoringException("CMS is not ready", Status.SERVICE_UNAVAILABLE);
		}

		// Check if we can create a transaction.
		try (var trx = ContentNodeHelper.trx()) {
			return Response.ok().build();
		} catch (Exception e) {
			throw new MonitoringException("CMS is not ready", Status.SERVICE_UNAVAILABLE);
		}
	}
}
