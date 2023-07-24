/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: Overview.java,v 1.1.6.1 2011-03-08 12:30:15 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Overview object, representing an overview in GCN
 * @author norbert
 */
@XmlRootElement
public class Overview implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -7435752235802356633L;

	/**
	 * Enumeration of the type of objects in the overview
	 */
	@XmlEnum(String.class)
	public static enum ListType {
		/**
		 * Overview lists pages
		 */
		PAGE,

		/**
		 * Overview lists folders
		 */
		FOLDER,

		/**
		 * Overview lists files
		 */
		FILE,

		/**
		 * Overview lists images
		 */
		IMAGE,

		/**
		 * Overview is not defined
		 */
		UNDEFINED
	}

	/**
	 * Enumeration of the selection type
	 */
	@XmlEnum(String.class)
	public static enum SelectType {
		/**
		 * Listed objects are fetched from selected folders
		 */
		FOLDER,

		/**
		 * Listed objects are selected individually
		 */
		MANUAL,

		/**
		 * Listed objects are fetched from the folder of the currently rendered object
		 */
		AUTO,

		/**
		 * Overview is not defined
		 */
		UNDEFINED
	}

	/**
	 * Enumeration for the order directions
	 */
	@XmlEnum(String.class)
	public static enum OrderDirection {
		/**
		 * Listed objects are sorted in ascending order
		 */
		ASC,

		/**
		 * Listed objects are sorted in descending order
		 */
		DESC,

		/**
		 * Overview is not defined
		 */
		UNDEFINED
	}

	/**
	 * Enumeration for the 'order by' setting
	 */
	@XmlEnum(String.class)
	public static enum OrderBy {
		/**
		 * Listed objects are sorted by name
		 */
		ALPHABETICALLY,

		/**
		 * Listed objects are sorted by priority (only if listType is PAGE)
		 */
		PRIORITY,

		/**
		 * Listed objects are sorted by publish date (only if listType is PAGE)
		 */
		PDATE,

		/**
		 * Listed objects are sorted by edit date
		 */
		EDATE,

		/**
		 * Listed objects are sorted by creation date
		 */
		CDATE,

		/**
		 * Listed objects are sorted by file size (only if listType is FILE or IMAGE)
		 */
		FILESIZE,

		/**
		 * Listed objects are sorted manually
		 */
		SELF,

		/**
		 * Overview is not defined
		 */
		UNDEFINED
	}

	/**
	 * Global UUID
	 */
	private String globalId;

	/**
	 * ID
	 */
	private Integer id;

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
	 * List of the id's of the selected items
	 */
	private List<Integer> selectedItemIds;

	/**
	 * List of object id's with node id's
	 */
	private List<NodeIdObjectId> selectedNodeItemIds;

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
	 * Constructor used by JAXB
	 */
	public Overview() {}

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
	 * List of selected item IDs
	 * @return the selectedItemIds
	 */
	public List<Integer> getSelectedItemIds() {
		return selectedItemIds;
	}

	/**
	 * List of selected item IDs with node IDs
	 * @return list of nodeId/objectId tuples
	 */
	public List<NodeIdObjectId> getSelectedNodeItemIds() {
		return selectedNodeItemIds;
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
	 */
	public void setListType(ListType listType) {
		this.listType = listType;
	}

	/**
	 * @param selectType the selectType to set
	 */
	public void setSelectType(SelectType selectType) {
		this.selectType = selectType;
	}

	/**
	 * @param orderDirection the orderDirection to set
	 */
	public void setOrderDirection(OrderDirection orderDirection) {
		this.orderDirection = orderDirection;
	}

	/**
	 * @param orderBy the orderBy to set
	 */
	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	/**
	 * @param selectedItemIds the selectedItemIds to set
	 */
	public void setSelectedItemIds(List<Integer> selectedItemIds) {
		this.selectedItemIds = selectedItemIds;
	}

	/**
	 * Set the item IDs with node IDs
	 * @param selectedNodeItemIds
	 */
	public void setSelectedNodeItemIds(List<NodeIdObjectId> selectedNodeItemIds) {
		this.selectedNodeItemIds = selectedNodeItemIds;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
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
	 */
	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
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
	 */
	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + ((listType == null) ? 0 : listType.hashCode());
		result = prime * result + ((maxItems == null) ? 0 : maxItems.hashCode());
		result = prime * result + ((orderBy == null) ? 0 : orderBy.hashCode());
		result = prime * result + ((orderDirection == null) ? 0 : orderDirection.hashCode());
		result = prime * result + ((recursive == null) ? 0 : recursive.hashCode());
		result = prime * result + ((selectType == null) ? 0 : selectType.hashCode());
		result = prime * result + ((selectedItemIds == null) ? 0 : selectedItemIds.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Overview other = (Overview) obj;

		if (listType == null) {
			if (other.listType != null) {
				return false;
			}
		} else if (!listType.equals(other.listType)) {
			return false;
		}
		if (maxItems == null) {
			if (other.maxItems != null) {
				return false;
			}
		} else if (!maxItems.equals(other.maxItems)) {
			return false;
		}
		if (orderBy == null) {
			if (other.orderBy != null) {
				return false;
			}
		} else if (!orderBy.equals(other.orderBy)) {
			return false;
		}
		if (orderDirection == null) {
			if (other.orderDirection != null) {
				return false;
			}
		} else if (!orderDirection.equals(other.orderDirection)) {
			return false;
		}
		if (recursive == null) {
			if (other.recursive != null) {
				return false;
			}
		} else if (!recursive.equals(other.recursive)) {
			return false;
		}
		if (selectType == null) {
			if (other.selectType != null) {
				return false;
			}
		} else if (!selectType.equals(other.selectType)) {
			return false;
		}
		if (selectedItemIds == null) {
			if (other.selectedItemIds != null) {
				return false;
			}
		} else if (!selectedItemIds.equals(other.selectedItemIds)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
			return false;
		}
		return true;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}
}
