package com.gentics.contentnode.rest.resource.search;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.IndexList;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for maintenance of search indices
 */
@Path("/index")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions for index maintenance."),
	@ResponseCode(code = 405, condition = "Feature elasticsearch is not activated.")
})
public interface SearchIndexResource {
	/**
	 * List all required search indices
	 * @param filter filter bean
	 * @param sorting sort bean
	 * @param paging paging bean
	 * @return list response
	 * @throws Exception
	 */
	@GET
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Index list is returned.")
	})
	IndexList list(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting, @BeanParam PagingParameterBean paging) throws Exception;

	/**
	 * Schedule rebuilding of index
	 * @param name index name
	 * @param drop true, if the index shall be dropped first
	 * @return generic response
	 * @throws Exception
	 */
	@PUT
	@Path("/{name}/rebuild")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Rebuilding of index {name} is scheduled."),
		@ResponseCode(code = 404, condition = "Index {name} does not exist.")
	})
	GenericResponse rebuild(@PathParam("name") String name, @QueryParam("drop") @DefaultValue("false") boolean drop) throws Exception;
}
