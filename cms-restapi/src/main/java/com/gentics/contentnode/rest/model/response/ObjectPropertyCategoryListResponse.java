package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ObjectPropertyCategory;

/**
 * Response containing a list of object property categories
 */
@XmlRootElement
public class ObjectPropertyCategoryListResponse extends AbstractListResponse<ObjectPropertyCategory> {

	private static final long serialVersionUID = -6086235529550216671L;
}
