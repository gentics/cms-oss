package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import com.gentics.contentnode.rest.model.response.MaintenanceResponse;

/**
 * Resource for access to general info about the system, which are available without a valid session.
 */
@Path("/info")
public interface InfoResource {
	/**
	 * Get info about the maintenance mode
	 * @return info about maintenance mode
	 */
	@GET
	@Path("/maintenance")
	MaintenanceResponse getMaintenance();
}
