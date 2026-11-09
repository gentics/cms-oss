package com.gentics.contentnode.rest.resource;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import com.gentics.contentnode.rest.model.linkchecker.CheckExternalLinkRequest;
import com.gentics.contentnode.rest.model.linkchecker.CheckResponse;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkList;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkResponse;
import com.gentics.contentnode.rest.model.linkchecker.ExternalLinkStatistics;
import com.gentics.contentnode.rest.model.linkchecker.PageList;
import com.gentics.contentnode.rest.model.linkchecker.ReplaceExternalLinkRequest;
import com.gentics.contentnode.rest.model.linkchecker.UpdateExternalLinkStatusRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LinkCheckerFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.webcohesion.enunciate.metadata.rs.ResponseCode;
import com.webcohesion.enunciate.metadata.rs.StatusCodes;

/**
 * Resource for the link checker
 */
@Path("/linkChecker")
@StatusCodes({
	@ResponseCode(code = 401, condition = "No valid sid and session secret cookie were provided."),
	@ResponseCode(code = 403, condition = "User has insufficient permissions."),
	@ResponseCode(code = 405, condition = "The feature link_checker is not activated.")
})
public interface LinkCheckerResource {
	/**
	 * Get link checker statistics
	 * @param nodeId optional node ID for getting node specific statistics
	 * @param online (optional) true to restrict to online pages, false to restrict to offline pages
	 * @return statistics response
	 * @throws Exception
	 */
	@GET
	@Path("/stats")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "LinkChecker statistics are returned."),
	})
	ExternalLinkStatistics getStats(@QueryParam("nodeId") Integer nodeId, @QueryParam("online") Boolean online) throws Exception;

	/**
	 * Get list of pages containing external links
	 * @param filter general filter parameters
	 * @param sorting sorting parameters
	 * @param paging paging parameters
	 * @param linkFilter link filter parameters
	 * @param perms permissions parameters
	 * @return response containing a list of pages with external links
	 * @throws Exception
	 */
	@GET
	@Path("/pages")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The pages containing external links are returned."),
	})
	PageList getPages(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging, @BeanParam LinkCheckerFilterParameterBean linkFilter, @BeanParam PermsParameterBean perms) throws Exception;

	/**
	 * Get the external links contained in a single page
	 * @param pageId page ID
	 * @return response containing the external links of the page
	 * @throws Exception
	 */
	@GET
	@Path("/pages/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The external links of the page are returned.")
	})
	ExternalLinkList getLinks(@PathParam("id") String pageId) throws Exception;

	/**
	 * Update the validity status for external links contained in the page. Update permissions on the page are required.
	 * @param pageId page ID
	 * @param request request containing external links with updated status
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/pages/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "The status of the posted external links has been updated.")
	})
	GenericResponse updateLinkStatus(@PathParam("id") String pageId, UpdateExternalLinkStatusRequest request) throws Exception;

	/**
	 * Check an external link for validity
	 * @param request request containing the link to check
	 * @return check response
	 * @throws Exception
	 */
	@POST
	@Path("/check")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Check result is returned.")
	})
	CheckResponse checkLink(CheckExternalLinkRequest request) throws Exception;

	/**
	 * Load a single link.
	 * Response contains additional information about occurrence of URL in the system
	 * @pathExample /pages/115/links/8
	 * @param pageId page ID
	 * @param linkId link ID
	 * @return link response
	 * @throws Exception
	 */
	@GET
	@Path("/pages/{pageId}/links/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Link information is returned.")
	})
	ExternalLinkResponse getLink(@PathParam("pageId") String pageId, @PathParam("id") int linkId) throws Exception;

	/**
	 * Update a link (optionally together with other occurrences) by replacing the URL with the given URL. 
	 * @pathExample /pages/115/links/8
	 * @responseExample application/json {"messages": [{"message": "Occurrences have been replaced in 12 pages.\n4 pages could not be edited due to missing permissions.\n1 page was locked by another user.","type": "INFO","timestamp": 1591689489516}],"responseInfo": {"responseCode": "OK","responseMessage": ""}}
	 * @param pageId page ID
	 * @param linkId link ID
	 * @param waitMs time in ms for the request to be done in foreground
	 * @param request request containing the new URL and the replace scope
	 * @return response
	 * @throws Exception
	 */
	@POST
	@Path("/pages/{pageId}/links/{id}")
	@StatusCodes({
		@ResponseCode(code = 200, condition = "Updating the links has been started.")
	})
	GenericResponse updateLink(@PathParam("pageId") String pageId, @PathParam("id") int linkId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, ReplaceExternalLinkRequest request) throws Exception;
}
