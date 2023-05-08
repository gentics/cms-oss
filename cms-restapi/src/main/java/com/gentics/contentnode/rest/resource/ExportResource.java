package com.gentics.contentnode.rest.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.gentics.contentnode.rest.model.request.ExportSelectionRequest;
import com.gentics.contentnode.rest.model.response.ExportSelectionResponse;

/**
 * Export helper resource
 */
@Path("/export")
public interface ExportResource extends AuthenticatedResource {

	/**
	 * Get export selection data. The posted data contains the currently
	 * selected or excluded objects. This method will calculate the
	 * "subselected" folders and return them. This is needed by the export
	 * selection dialog to mark folders, that are not selected themselves, but
	 * contain selected items.
	 * 
	 * @param request
	 *            request containing the lists of selected objects
	 * @return response containing the lists of subselected folders
	 */
	@POST
	@Path("/selection")
	ExportSelectionResponse getExportSelection(
			ExportSelectionRequest request);
}
