package com.gentics.contentnode.rest.model.response.devtools;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.TemplateInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of templates in packages
 */
@XmlRootElement
public class PagedTemplateInPackageListResponse extends AbstractListResponse<TemplateInPackage> {
}
