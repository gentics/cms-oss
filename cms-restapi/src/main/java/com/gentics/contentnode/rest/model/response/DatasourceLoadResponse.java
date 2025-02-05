package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Datasource;

/**
* Response for a datasource load request.
*/
@XmlRootElement
public class DatasourceLoadResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6857046837937514424L;

	/**
	 * Datasource
	 */
	private Datasource datasource;

	/**
	 * Create empty instance
	 */
	public DatasourceLoadResponse() {
	}

	/**
	 * Create instance with response info and datasource
	 * @param responseInfo response info
	 * @param datasource datasource
	 */
	public DatasourceLoadResponse(ResponseInfo responseInfo, Datasource datasource) {
		super(null, responseInfo);
		setDatasource(datasource);
	}

	/**
	 * Datasource
	 * @return datasource
	 */
	public Datasource getDatasource() {
		return datasource;
	}

	/**
	 * Set datasource
	 * @param datasource datasource
	 */
	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}
}
