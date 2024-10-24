package com.gentics.contentnode.rest.resource.impl.cluster;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.gentics.contentnode.cluster.ClusterSupport;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.cluster.ClusterInfo;
import com.gentics.contentnode.rest.resource.cluster.ClusterResource;

@Produces({ MediaType.APPLICATION_JSON})
@Path("/cluster")
@Authenticated
@RequiredFeature(Feature.CLUSTER)
@RequiredPerm(type=PermHandler.TYPE_ADMIN, bit=PermHandler.PERM_VIEW)
public class ClusterResourceImpl implements ClusterResource {
	@Override
	@GET
	@Path("/info")
	public ClusterInfo getInfo() {
		ClusterInfo response = ClusterSupport.getSingleton().getInfo();
		response.setResponseInfo(new ResponseInfo(ResponseCode.OK, null));
		return response;
	}

	@Override
	@PUT
	@Path("/master")
	public ClusterInfo setMaster() {
		ClusterSupport.getSingleton().setMaster();
		return getInfo();
	}
}
