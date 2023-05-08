package com.gentics.contentnode.rest.model.request;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request to load multiple objects.
 *
 * This request can be used to load files and images and acts
 * as a base for {@link MultiFolderLoadRequest} and
 * {@link MultiPageLoadRequest}.
 */
@XmlRootElement
public class MultiObjectLoadRequest {

	/**
	 * The ids of the folders to load.
	 */
	private List<Integer> ids = new ArrayList<>();

	/**
	 * Whether the folders are loaded for update.
	 */
	private Boolean forUpdate = false;

	/**
	 * The node id to load the folders from.
	 */
	private Integer nodeId = null;

	/**
	 * If a content staging package name is given, the object is checked against the package for a sync status.
	 */
	@XmlElement(name = "package")
	private String packageName = null;

	/**
	 * Constructor used by JAXB.
	 */
	public MultiObjectLoadRequest() {
	}

	/**
	 * The ids of the folders to load.
	 *
	 * @return The ids of the folders to load.
	 */
	public List<Integer> getIds() {
		return ids;
	}

	/**
	 * Set the ids of the folders to load.
	 *
	 * @param ids The ids of the folders to load.
	 */
	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}

	/**
	 * Whether the folders are to be loaded for update.
	 *
	 * @return <code>true</code> if the folders should be loaded
	 *		for update, <code>false</code> otherwise.
	 */
	public Boolean isForUpdate() {
		return forUpdate;
	}

	/**
	 * Set whether the folders are to be loaded for update.
	 * 
	 * @param forUpdate Set to <code>true</code> if the folders
	 *		should be loaded for update.
	 */
	public void setForUpdate(Boolean forUpdate) {
		this.forUpdate = forUpdate;
	}

	/**
	 * The id of the node to load the folders from.
	 *
	 * @return The id of the node to load the folders from.
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Set the id of the node to load the folders from.
	 *
	 * @return The id of the node to load the folders from.
	 */
	public void setNodeId(Integer nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get a content staging package name. If set, the object is checked against the package for a sync status.
	 * 
	 * @return
	 */
	public String getPackage() {
		return packageName;
	}

	/**
	 * Set a content staging package name, to check the object against the package for a sync status.
	 * 
	 * @param packageName
	 */
	public void setPackage(String packageName) {
		this.packageName = packageName;
	}
}
