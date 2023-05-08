package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ItemVersion;

/**
 * Response containing a list of versions
 */
@XmlRootElement
public class ItemVersionListResponse extends AbstractListResponse<ItemVersion> {
	/**
	 * Serial Version UId
	 */
	private static final long serialVersionUID = 2009556649667950391L;
}
