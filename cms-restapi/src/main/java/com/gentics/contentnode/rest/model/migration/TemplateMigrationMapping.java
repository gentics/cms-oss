package com.gentics.contentnode.rest.model.migration;

import java.io.Serializable;
import java.util.List;

/**
 * This class is a model for a template migration mapping.
 * 
 * @author johannes2
 * 
 */
public class TemplateMigrationMapping implements Serializable {

	private static final long serialVersionUID = 6778370587672580826L;
	private List<TemplateMigrationEditableTagMapping> editableTagMappings;
	private List<TemplateMigrationNonEditableTagMapping> nonEditableTagMappings;

	private Integer toTemplateId;
	private Integer fromTemplateId;
	private Integer nodeId;

	/**
	 * Default constructor
	 */
	public TemplateMigrationMapping() {}

	/**
	 * The node id for this mapping.
	 * @return
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node id for this mapping
	 * @param nodeId
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	public List<TemplateMigrationEditableTagMapping> getEditableTagMappings() {
		return editableTagMappings;
	}

	public void setEditableTagMappings(List<TemplateMigrationEditableTagMapping> editableTagMappings) {
		this.editableTagMappings = editableTagMappings;
	}

	public List<TemplateMigrationNonEditableTagMapping> getNonEditableTagMappings() {
		return nonEditableTagMappings;
	}

	public void setNonEditableTagMappings(List<TemplateMigrationNonEditableTagMapping> nonEditableTagMappings) {
		this.nonEditableTagMappings = nonEditableTagMappings;
	}

	public Integer getFromTemplateId() {
		return fromTemplateId;
	}

	public void setFromTemplateId(Integer fromTemplateId) {
		this.fromTemplateId = fromTemplateId;
	}

	public Integer getToTemplateId() {
		return toTemplateId;
	}

	public void setToTemplateId(Integer toTemplateId) {
		this.toTemplateId = toTemplateId;
	}

}
