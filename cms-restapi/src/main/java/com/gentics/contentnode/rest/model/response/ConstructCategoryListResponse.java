package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ConstructCategory;

/**
 * Response containing a list of construct categoriess
 */
@XmlRootElement
public class ConstructCategoryListResponse extends AbstractListResponse<ConstructCategory> {

	private static final long serialVersionUID = 4631758890136807432L;
}
