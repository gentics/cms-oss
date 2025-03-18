package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Construct;

/**
 * Paged list of constructs
 */
@XmlRootElement
public class PagedConstructListResponse extends AbstractListResponse<Construct> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1450291013819581269L;
}
