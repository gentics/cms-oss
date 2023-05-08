package com.gentics.contentnode.rest.model.response.devtools;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.ConstructInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of constructs in packages
 */
@XmlRootElement
public class PagedConstructInPackageListResponse extends AbstractListResponse<ConstructInPackage> {
}
