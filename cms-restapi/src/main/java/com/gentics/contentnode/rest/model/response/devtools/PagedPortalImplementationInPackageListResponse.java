package com.gentics.contentnode.rest.model.response.devtools;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.PortalImplementationInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of Portal Implementations in packages
 */
@XmlRootElement
public class PagedPortalImplementationInPackageListResponse extends AbstractListResponse<PortalImplementationInPackage> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 4626004127745640151L;
}
