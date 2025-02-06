package com.gentics.contentnode.staging;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;

/**
 * REST entity for the overview, contained in the Content Staging package.
 * 
 * @author plyhun
 *
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedOverview extends StagingBase {

	private static final long serialVersionUID = -7081104550205902317L;

	/**
	 * Type of listed objects
	 */
	private ListType listType;

	/**
	 * Type of the selection
	 */
	private SelectType selectType;

	/**
	 * Order direction
	 */
	private OrderDirection orderDirection;

	/**
	 * Order by
	 */
	private OrderBy orderBy;

	/**
	 * List of object id's with node id's
	 */
	private List<StagedNodeIdObjectId> selectedNodeItemIds;

	/**
	 * Source of the overview
	 */
	private String source;

	/**
	 * Maximum number of entries to be listed (0 for unlimited)
	 */
	private Integer maxItems;

	/**
	 * True when objects shall be fetched also from subfolders (if
	 * {@link #selectType selectType} is {@link SelectType#FOLDER FOLDER})
	 */
	private Boolean recursive;

	/**
	 * Get the overview type.
	 * 
	 * @return
	 */
	public ListType getListType() {
		return listType;
	}

	/**
	 * Set the overview type.
	 * 
	 * @param listType
	 */
	public void setListType(ListType listType) {
		this.listType = listType;
	}

	/**
	 * Get the select type.
	 * @return
	 */
	public SelectType getSelectType() {
		return selectType;
	}

	/**
	 * Set the select type.
	 * @param selectType
	 */
	public void setSelectType(SelectType selectType) {
		this.selectType = selectType;
	}

	/**
	 * Get the order direction.
	 * @return
	 */
	public OrderDirection getOrderDirection() {
		return orderDirection;
	}

	/**
	 * Set the order direction.
	 * @param orderDirection
	 */
	public void setOrderDirection(OrderDirection orderDirection) {
		this.orderDirection = orderDirection;
	}

	/**
	 * Get sort order-by.
	 * @return
	 */
	public OrderBy getOrderBy() {
		return orderBy;
	}

	/**
	 * Set sort order-by.
	 * @param orderBy
	 */
	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	/**
	 * Get selected UUIDs.
	 * @return
	 */
	public List<StagedNodeIdObjectId> getSelectedNodeItemIds() {
		return selectedNodeItemIds;
	}

	/**
	 * Set selected UUIDs.
	 * @param selectedNodeItemIds
	 */
	public void setSelectedNodeItemIds(List<StagedNodeIdObjectId> selectedNodeItemIds) {
		this.selectedNodeItemIds = selectedNodeItemIds;
	}

	/**
	 * Get the source.
	 * @return
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Set the source.
	 * @param source
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Get maximum item count.
	 * @return
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Set maximum item count.
	 * @param maxItems
	 */
	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
	}

	/**
	 * Get the recursive flag.
	 * @return
	 */
	public Boolean getRecursive() {
		return recursive;
	}

	/**
	 * Set the recursive flag.
	 * @param recursive
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	@Override
	@XmlTransient
	@JsonIgnore
	public String getTypeName() {
		return "overview";
	}
}
