package com.gentics.contentnode.rest.resource;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.gentics.contentnode.rest.model.response.MarkupLanguageListResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for getting markup languages
 */
@Path("/markupLanguage")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions.")
})
public interface MarkupLanguageResource {
	/**
	 * Get a list of markup languages.<br>
	 * The result can be filtered by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>extension</code></li>
	 * <li><code>contentType</code></li>
	 * <li><code>feature</code></li>
	 * </ul>
	 * and sorted by
	 * <ul>
	 * <li><code>id</code></li>
	 * <li><code>name</code></li>
	 * <li><code>extension</code></li>
	 * <li><code>contentType</code></li>
	 * <li><code>feature</code></li>
	 * <li><code>excludeFromPublishing</code></li>
	 * </ul>
	 * @param sort sorting parameter bean
	 * @param filter filter parameter bean
	 * @param paging paging parameter bean
	 * @return list of markup languages
	 * @throws Exception
	 */
	@GET
	MarkupLanguageListResponse list(@BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging)
			throws Exception;
}
