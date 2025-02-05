package com.gentics.contentnode.rest.resource;


import com.gentics.contentnode.rest.model.PublishLogDto;
import com.gentics.contentnode.rest.model.response.GenericItemList;
import com.gentics.contentnode.rest.resource.parameter.FilterPublishableObjectBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * Interface for managing publish protocol resources.
 */
@Path("/publish/state")
public interface PublishProtocolResource {


	/**
	 * Retrieves a publish protocol entry by its object ID.
	 *
	 * @param objId the object ID of the publish protocol entry
	 * @return the publish protocol entry
	 * @throws Exception if an error occurs during retrieval
	 */
	@GET
	@Path("/{type}/{objId}")
	@StatusCodes({
			@ResponseCode(code = 200, condition = "Publish protocol entry is returned."),
			@ResponseCode(code = 404, condition = "Not found")

	})
	PublishLogDto get(
			@PathParam("type") String type,
			@PathParam("objId") Integer objId) throws Exception;

	/**
	 * Retrieves a list of publish protocol entries with pagination.
	 *
	 * @param paging the paging parameters
	 * @return a list of publish protocol entries
	 * @throws Exception if an error occurs during retrieval
	 */
	@GET
	@Path("/")
	@StatusCodes({
			@ResponseCode(code = 200, condition = "Publish protocol list is returned.")
	})
	GenericItemList<PublishLogDto> list(
			@BeanParam PagingParameterBean paging,
			@BeanParam FilterPublishableObjectBean filter) throws Exception;

}
