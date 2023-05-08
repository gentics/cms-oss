package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Datasource;

/**
 * Paged list of datasources
 */
@XmlRootElement
public class PagedDatasourceListResponse extends AbstractListResponse<Datasource> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2266081592231414187L;
}
