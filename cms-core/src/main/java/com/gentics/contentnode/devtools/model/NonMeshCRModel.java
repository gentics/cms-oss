package com.gentics.contentnode.devtools.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.CRElasticsearchModel;

/**
 * Model for non-Mesh CRs
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class NonMeshCRModel extends AbstractCRModel {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8829556579189653431L;

	@Override
	public String getPermissionProperty() {
		return null;
	}

	@Override
	public void setPermissionProperty(String permissionProperty) {
	}

	@Override
	public String getVersion() {
		return null;
	}

	@Override
	public void setVersion(String version) {
	}

	@Override
	public CRElasticsearchModel getElasticsearch() {
		return null;
	}

	@Override
	public void setElasticsearch(CRElasticsearchModel elasticsearch) {
	}

	@Override
	public Boolean getProjectPerNode() {
		return null;
	}

	@Override
	public void setProjectPerNode(Boolean projectPerNode) {
	}
}
