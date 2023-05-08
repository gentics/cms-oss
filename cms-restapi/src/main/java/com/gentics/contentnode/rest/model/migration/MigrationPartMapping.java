package com.gentics.contentnode.rest.model.migration;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The tagtype migration part mapping model
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class MigrationPartMapping implements Serializable {

	private static final long serialVersionUID = -5078869600748377437L;

	public static final String NOT_MAPPED_TYPE_FLAG = "NOT_MAPPED";

	/**
	 * The ID of the source part
	 */
	private Integer fromPartId;

	/**
	 * The ID of the destination part
	 */
	private Integer toPartId;

	/**
	 * Type of part mapping
	 */
	private String partMappingType;

	/**
	 * Default constructor
	 */
	public MigrationPartMapping() {}

	public Integer getFromPartId() {
		return fromPartId;
	}

	public void setFromPartId(Integer fromPartId) {
		this.fromPartId = fromPartId;
	}

	public Integer getToPartId() {
		return toPartId;
	}

	public void setToPartId(Integer toPartId) {
		this.toPartId = toPartId;
	}

	public String getPartMappingType() {
		return partMappingType;
	}

	public void setPartMappingType(String partMappingType) {
		this.partMappingType = partMappingType;
	}

	/**
	 * Checks whether this part mapping was marked as not mapped. This is done by setting the correct part mapping type flag
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isMarkedAsNotMapped() {
		return NOT_MAPPED_TYPE_FLAG.equalsIgnoreCase(getPartMappingType());
	}

}
