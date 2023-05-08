package com.gentics.contentnode.rest.resource.cluster;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import com.gentics.contentnode.rest.model.response.cluster.ClusterInfo;

/**
 * Resource for clustering support
 */
@Path("/cluster")
public interface ClusterResource {
	/**
	 * Get clustering information
	 * @return clustering information response
	 */
	@GET
	@Path("/info")
	ClusterInfo getInfo();

	/**
	 * Set this instance as cluster master
	 * @return clustering information response
	 */
	@PUT
	@Path("/master")
	public ClusterInfo setMaster();
}
