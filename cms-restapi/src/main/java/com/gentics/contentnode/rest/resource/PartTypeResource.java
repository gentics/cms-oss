package com.gentics.contentnode.rest.resource;

import com.gentics.contentnode.rest.model.response.PartTypeListResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PartTypeListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Resource for part types
 */
@Path("/parttype")
public interface PartTypeResource {

	/**
	 * List Part types
	 *
	 * @param filter filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param partTypeFilter part type filter parameters
	 * @return list of part type
	 * @throws Exception in case of errors
	 */
	@GET
	PartTypeListResponse list(@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam PartTypeListParameterBean partTypeFilter) throws Exception;
}
