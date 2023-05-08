package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Construct;

/**
 * Response containing a list of constructs
 */
@XmlRootElement
public class ConstructList extends AbstractListResponse<Construct> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6059215336165095068L;
}
