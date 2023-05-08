package com.gentics.contentnode.rest.model.migration;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag Type Migration mapping object
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class TagTypeMigrationMapping implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5858887759764332796L;

	/**
	 * Id of TagType being mapped from
	 */
	private Integer fromTagTypeId;

	/**
	 * Id of TagType being mapped to
	 */
	private Integer toTagTypeId;

	/**
	 * Tag part mappings
	 */
	private List<MigrationPartMapping> partMappings;

	/**
	 * Create an empty instance
	 */
	public TagTypeMigrationMapping() {}

	public Integer getFromTagTypeId() {
		return fromTagTypeId;
	}

	public void setFromTagTypeId(Integer fromTagTypeId) {
		this.fromTagTypeId = fromTagTypeId;
	}

	public Integer getToTagTypeId() {
		return toTagTypeId;
	}

	public void setToTagTypeId(Integer toTagTypeId) {
		this.toTagTypeId = toTagTypeId;
	}

	public List<MigrationPartMapping> getPartMappings() {
		return partMappings;
	}

	public void setPartMappings(List<MigrationPartMapping> partMappings) {
		this.partMappings = partMappings;
	}
}
