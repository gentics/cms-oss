package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Page;

/**
 * Response containing information about multiple pages.
 */
@XmlRootElement
public class MultiPageLoadResponse extends GenericResponse {

	/**
	 * The list of found pages.
	 */
	private List<Page> pages;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiPageLoadResponse() {
	}

	/**
	 * Convenience constructor.
	 *
	 * Automatically adds a response info with a response code
	 * of <code>OK</code>.
	 *
	 * @param pages The pages the send with the response.
	 */
	public MultiPageLoadResponse(List<Page> pages) {
		super(
			null,
			new ResponseInfo(
				ResponseCode.OK,
				(pages == null ? 0 : pages.size()) + " page(s) loaded"));

		this.pages = pages;
	}

	/**
	 * Response with an empty page list.
	 *
	 * @param message The message that should be displayed to the user.
	 * @param response ResponseInfo with the status of the response.
	 */
	public MultiPageLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);

		this.pages = new ArrayList<>();
	}

	/**
	 * The list of found pages.
	 *
	 * @return The list of found pages.
	 */
	public List<Page> getPages() {
		return pages;
	}

	/**
	 * Set the list of pages to send with the response.
	 *
	 * @param pages The list of pages to send with the response.
	 */
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}
}
