package com.gentics.contentnode.devtools.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;

/**
 * Model of an overview containing global IDs of selected objects
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverviewModel {
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
	private List<OverviewItemModel> selection;

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
	 * List Type
	 * @return the listType
	 */
	public ListType getListType() {
		return listType;
	}

	/**
	 * Selection Type
	 * @return the selectType
	 */
	public SelectType getSelectType() {
		return selectType;
	}

	/**
	 * Order Direction
	 * @return the orderDirection
	 */
	public OrderDirection getOrderDirection() {
		return orderDirection;
	}

	/**
	 * Order By
	 * @return the orderBy
	 */
	public OrderBy getOrderBy() {
		return orderBy;
	}

	/**
	 * Get the selection
	 * @return selection
	 */
	public List<OverviewItemModel> getSelection() {
		return selection;
	}

	/**
	 * Overview Source
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param listType the listType to set
	 * @return fluent API
	 */
	public OverviewModel setListType(ListType listType) {
		this.listType = listType;
		return this;
	}

	/**
	 * @param selectType the selectType to set
	 * @return fluent API
	 */
	public OverviewModel setSelectType(SelectType selectType) {
		this.selectType = selectType;
		return this;
	}

	/**
	 * @param orderDirection the orderDirection to set
	 * @return fluent API
	 */
	public OverviewModel setOrderDirection(OrderDirection orderDirection) {
		this.orderDirection = orderDirection;
		return this;
	}

	/**
	 * @param orderBy the orderBy to set
	 * @return fluent API
	 */
	public OverviewModel setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	/**
	 * Set the item IDs with node IDs
	 * @param selection
	 * @return fluent API
	 */
	public OverviewModel setSelection(List<OverviewItemModel> selection) {
		this.selection = selection;
		return this;
	}

	/**
	 * @param source the source to set
	 * @return fluent API
	 */
	public OverviewModel setSource(String source) {
		this.source = source;
		return this;
	}

	/**
	 * Maximum number of items
	 * @return the maxItems
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * @param maxItems the maxItems to set
	 * @return fluent API
	 */
	public OverviewModel setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * True when objects shall be fetched also from subfolders (if
	 * {@link #selectType selectType} is {@link SelectType#FOLDER FOLDER})
	 * @return the recursive
	 */
	public Boolean isRecursive() {
		return recursive;
	}

	/**
	 * @param recursive the recursive to set
	 * @return fluent API
	 */
	public OverviewModel setRecursive(Boolean recursive) {
		this.recursive = recursive;
		return this;
	}
}
