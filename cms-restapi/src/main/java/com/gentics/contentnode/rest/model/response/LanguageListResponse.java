package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ContentLanguage;

/**
 * Response for a language list request
 * @author norbert
 */
@XmlRootElement
public class LanguageListResponse extends GenericResponse {

	/**
	 * list of languages
	 */
	private List<ContentLanguage> languages;

	/**
	 * True if more items are available (paging)
	 */
	private boolean hasMoreItems;

	/**
	 * Total number of items present (paging)
	 */
	private Integer numItems;

	/**
	 * Empty constructor
	 */
	public LanguageListResponse() {}

	/**
	 * Create an instance with given message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public LanguageListResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * List of languages
	 * @return list of languages
	 */
	public List<ContentLanguage> getLanguages() {
		return languages;
	}

	/**
	 * True if more items are present
	 * @return true if more items are present
	 */
	public boolean isHasMoreItems() {
		return hasMoreItems;
	}

	/**
	 * Total number of items present
	 * @return total number of items present
	 */
	public Integer getNumItems() {
		return numItems;
	}

	/**
	 * Set the list of languages
	 * @param languages list of languages
	 */
	public void setLanguages(List<ContentLanguage> languages) {
		this.languages = languages;
	}

	/**
	 * Set true when more items are available
	 * @param hasMoreItems true if more items are available
	 */
	public void setHasMoreItems(boolean hasMoreItems) {
		this.hasMoreItems = hasMoreItems;
	}

	/**
	 * Set the total number of items present
	 * @param numItems total number of items present
	 */
	public void setNumItems(Integer numItems) {
		this.numItems = numItems;
	}
}
