package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.request.page.CreatedTag;

/**
 * Response containing data about the created tags
 */
@XmlRootElement
public class MultiTagCreateResponse extends GenericResponse {
	/**
	 * Created tags
	 */
	private Map<String, CreatedTag> created;

	/**
	 * List of tags found in the rendered page
	 */
	private List<com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag> tags;

	/**
	 * Create empty instance
	 */
	public MultiTagCreateResponse() {}

	/**
	 * Create instance with message and response info
	 * @param message message (may be null)
	 * @param responseInfo response info
	 */
	public MultiTagCreateResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with message, response info and data
	 * @param message message (may be null)
	 * @param responseInfo response info
	 * @param created map of created tags
	 * @param tags tag data
	 */
	public MultiTagCreateResponse(Message message, ResponseInfo responseInfo, Map<String, CreatedTag> created, List<com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag> tags) {
		this(message, responseInfo);
		setCreated(created);
		setTags(tags);
	}

	/**
	 * Map of created tags. Keys are the IDs (from the request), values are objects containing tag data
	 * @return map of created tags
	 */
	public Map<String, CreatedTag> getCreated() {
		return created;
	}

	/**
	 * Tag data of the created tags
	 * @return tag data
	 */
	public List<com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag> getTags() {
		return tags;
	}

	/**
	 * Set the map of created tags
	 * @param created map od created tags
	 */
	public void setCreated(Map<String, CreatedTag> created) {
		this.created = created;
	}

	/**
	 * Set tag data
	 * @param tags tag data
	 */
	public void setTags(List<com.gentics.contentnode.rest.model.response.PageRenderResponse.Tag> tags) {
		this.tags = tags;
	}
}
