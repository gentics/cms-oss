package com.gentics.contentnode.rest.model.migration;

import java.util.List;

public class TemplateMigrationEditableTagMapping extends TemplateMigrationTagMapping {

	private static final long serialVersionUID = 6128197822188210735L;

	private List<MigrationPartMapping> partMappings;

	/**
	 * Default constructor
	 */
	public TemplateMigrationEditableTagMapping() {}

	public List<MigrationPartMapping> getPartMappings() {
		return partMappings;
	}

	public void setPartMappings(List<MigrationPartMapping> partMappings) {
		this.partMappings = partMappings;
	}

}
