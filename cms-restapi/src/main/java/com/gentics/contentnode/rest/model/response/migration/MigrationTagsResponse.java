package com.gentics.contentnode.rest.model.response.migration;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response to request to load tags for a list of objects
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class MigrationTagsResponse extends GenericResponse {

	/**
	 * Map of tag IDs and tags
	 */
	private Map<Integer, Construct> tagTypes = new HashMap<Integer, Construct>();

	/**
	 * Constructor used by JAXB
	 */
	public MigrationTagsResponse() {}

	/**
	 * Create an instance of the response with single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationTagsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Add a tag to the tags HashMap
	 * 
	 * @param id
	 *            id of the tag to add
	 * @param tag
	 *            tag to add
	 */
	public void addTagType(Integer id, Construct tag) {
		tagTypes.put(id, tag);
	}

	/**
	 * Returns the map of tagtypes.
	 * 
	 * @return
	 */
	public Map<Integer, Construct> getTagTypes() {
		return tagTypes;
	}

	/**
	 * Returns the map of tagtypes
	 * 
	 * @param tags
	 */
	public void setTagTypes(Map<Integer, Construct> tags) {
		this.tagTypes = tags;
	}

}
