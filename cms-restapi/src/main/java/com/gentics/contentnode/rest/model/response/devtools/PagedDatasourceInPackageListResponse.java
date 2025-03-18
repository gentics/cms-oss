package com.gentics.contentnode.rest.model.response.devtools;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.devtools.DatasourceInPackage;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Paged list of datasources in packages
 */
@XmlRootElement
public class PagedDatasourceInPackageListResponse extends AbstractListResponse<DatasourceInPackage> {
}
