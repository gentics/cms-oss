package com.gentics.contentnode.rest.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.gentics.contentnode.rest.model.request.DaisyDiffRequest;
import com.gentics.contentnode.rest.model.request.DiffRequest;
import com.gentics.contentnode.rest.model.response.DiffResponse;

/**
 * This resource implements the diff tool in the REST API, which can be used to
 * calculate the diff between html contents.
 */
@Produces( { "application/json; charset=UTF-8",
	"application/xml; charset=UTF-8" })
@Path("/diff")
public interface DiffResource {

	/**
	 * Annotates a newer HTML document with differences to an older HTML document.
	 * 
	 * <p>
	 * The annotations will be done by the DaisyDiff Algorithm.
	 * Please see the <a href="http://code.google.com/p/daisydiff/">DaisyDiff
	 * project site</a> for more information and examples.
	 * 
	 * <p>
	 * The resulting HTML will need some CSS and (optionally) some Javascript includes
	 * from version 1.1 of the DaisyDiff distribution.
	 * 
	 * <p>
	 * The CSS file in "css/diff.css" of the DaisyDiff distribution archive provides basic
	 * styles for inserted/deleted/changed elements and is sufficient by itself to provide
	 * basic visualization of changes.
	 * 
	 * <p>
	 * The following JavaScript files need to be included to get an extended interface
	 * that allows navigation between changes and provides more information about a change
	 * when it is clicked:
	 * 
	 * <ul>
	 * <li>lib/js/daisydiff-1.1/tooltip/wz_tooltip.js
	 * <li>lib/js/daisydiff-1.1/tooltip/tip_balloon.js
	 * <li>lib/js/daisydiff-1.1/dojo/dojo.js
	 * <li>lib/js/daisydiff-1.1/diff.js
	 * </ul>
	 * 
	 * <p>
	 * The included JavaScript must be explicitly initialized:
	 * &lt;script type='text/javascript'&gt;htmlDiffInit();&lt;/script&gt;
	 * 
	 * <p>
	 * The DaisyDiff algorithm differs from the standard diff algorithm in that it
	 * tries to preserve the HTML tree structure, while the standard HTML diff
	 * alogrithm may break it.
	 * 
	 * <p>
	 * If the given HTML documents contain &lt;body> tags, only content inside the tags will
	 * be diffed. Differences outside the body tags will be ignored. The resulting HTML
	 * document will contain the same header (substring from start of document until and
	 * including the body tag) as the newer document.  
	 * 
	 * <p>
	 * Please note that the HTML diff will not reflect the changes exactly. The HTML
	 * will be cleaned up and some content may be lost in this process. The result
	 * is meant for human consumption, not to generate exact differences between HTML
	 * documents.
	 * 
	 * @param request
	 * 			contains the parameters for the DaisyDiff algorithm
	 * @return
	 * 		 	a response containing the diff for the two HTML documents
	 */
	@POST
	@Path("/daisyDiff")
	DiffResponse daisyDiff(DaisyDiffRequest request);

	/**
	 * Method to calculate the diff in HTML
	 * 
	 * @param request
	 *            request containing the HTML contents to be diffed
	 * @return response containing the diff
	 */
	@POST
	@Path("/html")
	DiffResponse diffHTML(DiffRequest request);

	/**
	 * Method to calculate the diff between two HTML contents in source
	 * 
	 * @param request
	 *            request containing the HTML contents to be diffed
	 * @return response containing the diff
	 */
	@POST
	@Path("/source")
	DiffResponse diffSource(DiffRequest request);
}
