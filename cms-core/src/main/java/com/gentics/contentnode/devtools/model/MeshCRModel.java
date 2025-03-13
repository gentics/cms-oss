package com.gentics.contentnode.devtools.model;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for Mesh CRs
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown=true)
public class MeshCRModel extends AbstractCRModel {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6827828993634306388L;

	@Override
	public String getDbType() {
		return null;
	}

	@Override
	public void setDbType(String dbType) {
	}

	@Override
	public Boolean getDiffDelete() {
		return null;
	}

	@Override
	public void setDiffDelete(Boolean diffDelete) {
	}

	@Override
	public Boolean getLanguageInformation() {
		return null;
	}

	@Override
	public void setLanguageInformation(Boolean languageInformation) {
	}

	@Override
	public Boolean getPermissionInformation() {
		return null;
	}

	@Override
	public void setPermissionInformation(Boolean permissionInformation) {
	}

	@Override
	public String getBasepath() {
		return null;
	}

	@Override
	public void setBasepath(String basepath) {
	}

	@Override
	public String getBasepathProperty() {
		return null;
	}

	@Override
	public void setBasepathProperty(String basepathProperty) {
	}
}
