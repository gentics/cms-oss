package com.gentics.contentnode.rest.model.response.devtools;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.ObjectPropertyInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of object property definitions in packages
 */
@XmlRootElement
public class PagedObjectPropertyInPackageListResponse extends AbstractListResponse<ObjectPropertyInPackage> {
}
