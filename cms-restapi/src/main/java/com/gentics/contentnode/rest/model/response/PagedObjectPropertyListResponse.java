package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ObjectProperty;

/**
 * Paged list of object properties
 */
@XmlRootElement
public class PagedObjectPropertyListResponse extends AbstractListResponse<ObjectProperty> {

	private static final long serialVersionUID = 860790898020191531L;
}
