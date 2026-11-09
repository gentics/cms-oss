package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing localization info
 */
@XmlRootElement
public class LocalizationsResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1108851340029144516L;

	/**
	 * The ID of the master object.
	 */
	private int masterId;
	
	/**
	 * The ID of the node the master object resides in.
	 */
	private int masterNodeId;
	
	/**
	 * A map from object IDs to the respective channels of the objects.
	 */
	private Map<Integer, Integer> nodeIds;

	private List<Integer> online;

	private List<Integer> offline;

	/**
	 * Total number of localizations
	 */
	private int total;

	/**
	 * Number of localizations without permission
	 */
	private int hidden;

	/**
	 * Default constructor for an empty response.
	 */
	public LocalizationsResponse() {
	}

	/**
	 * Create a response containing only a message and response information.
	 *
	 * @param message The response message.
	 * @param responseInfo The response information.
	 */
	public LocalizationsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create a response with all fields set.
	 *
	 * @param message The response message.
	 * @param masterId The ID of the master object.
	 * @param masterNodeId The node ID of the master object.
	 * @param nodeIds A map of IDs of localized copies to their channel IDs.
	 */
	public LocalizationsResponse(
			Message message,
			int masterId,
			int masterNodeId,
			Map<Integer, Integer> nodeIds) {
		super(message, new ResponseInfo(ResponseCode.OK, "IDs of localized copies loaded"));
		
		this.masterId = masterId;
		this.masterNodeId = masterNodeId;
		this.nodeIds = nodeIds;
	}

	/**
	 * The ID of the master object.
	 *
	 * @return The ID of the master object.
	 */
	public int getMasterId() {
		return masterId;
	}

	/**
	 * Set the master object ID.
	 *
	 * @param masterId The new master object id.
	 * @return fluent API
	 */
	public LocalizationsResponse setMasterId(int masterId) {
		this.masterId = masterId;
		return this;
	}

	/**
	 * The ID of the node the master object resides in.
	 *
	 * @return The node the master object resides in.
	 */
	public int getMasterNodeId() {
		return masterNodeId;
	}

	/**
	 * Set the ID of the node the master object resides in.
	 *
	 * @param masterNodeId The ID of the node the master object resides in.
	 * @return fluent API
	 */
	public LocalizationsResponse setMasterNodeId(int masterNodeId) {
		this.masterNodeId = masterNodeId;
		return this;
	}

	/**
	 * A map from object IDs to the respective channels of the objects.
	 *
	 * @return A map from object IDs to the respective channels of the objects.
	 */
	public Map<Integer, Integer> getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the map from object IDs to the respective channels of the objects.
	 *
	 * @param nodeIds A map from object IDs to the respective channels of the objects.
	 * @return fluent API
	 */
	public LocalizationsResponse setNodeIds(Map<Integer, Integer> nodeIds) {
		this.nodeIds = nodeIds;
		return this;
	}

	/**
	 * Total number of localizations (including localizations in channels without permission and for folders localizations of subitems)
	 * @return total number
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Set total number of localizations
	 * @param total total number
	 * @return fluent API
	 */
	public LocalizationsResponse setTotal(int total) {
		this.total = total;
		return this;
	}

	/**
	 * Number of localizations the user has no permission to see
	 * @return hidden number
	 */
	public int getHidden() {
		return hidden;
	}

	/**
	 * Set number of hidden localizations
	 * @param hidden hidden number
	 * @return fluent API
	 */
	public LocalizationsResponse setHidden(int hidden) {
		this.hidden = hidden;
		return this;
	}

	/**
	 * IDs of pages, that are online
	 * @return list of page IDs
	 */
	public List<Integer> getOnline() {
		return online;
	}

	/**
	 * Set online pages
	 * @param online list of page IDs
	 * @return fluent API
	 */
	public LocalizationsResponse setOnline(List<Integer> online) {
		this.online = online;
		return this;
	}

	/**
	 * IDs of pages, that are offline
	 * @return list of page IDs
	 */
	public List<Integer> getOffline() {
		return offline;
	}

	/**
	 * Set offline pages
	 * @param offline list of page IDs
	 * @return fluent API
	 */
	public LocalizationsResponse setOffline(List<Integer> offline) {
		this.offline = offline;
		return this;
	}
}
