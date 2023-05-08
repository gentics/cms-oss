package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.DatasourceEntryModel;

/**
 * Response containing a list of datasource entries
 */
@XmlRootElement
public class DatasourceEntryListResponse extends AbstractListResponse<DatasourceEntryModel> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2134563281252356818L;
}
