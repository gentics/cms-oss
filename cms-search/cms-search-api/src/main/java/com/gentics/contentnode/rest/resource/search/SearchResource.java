package com.gentics.contentnode.rest.resource.search;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import com.gentics.contentnode.rest.resource.parameter.SearchParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for endpoints that are forwarded to elasticsearch for performing searches
 */
@Path("/elastic")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 405, condition = "Feature elasticsearch is not activated.")
})
public interface SearchResource {
	/**
	 * Perform a search in indices for a specific type
	 * @param type object type (one of "page", "image", "file" or "folder")
	 * @param search search parameter bean
	 * @param body request body
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/{searchType}/_search")
	Response searchType(@PathParam("searchType") String type, @BeanParam SearchParameterBean search, String body) throws Exception;

	/**
	 * Perform a search by forwarding the posted search request to elasticsearch under the given path.
	 * The path will be prefixed with the index name.
	 * @param path path (e.g. /_search)
	 * @param search search parameter bean
	 * @param body request body
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("{path: .*}")
	Response search(@PathParam("path") String path, @BeanParam SearchParameterBean search, String body) throws Exception;
}
