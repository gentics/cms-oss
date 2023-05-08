package com.gentics.contentnode.rest.model.request.page;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Tag;

/**
 * Created tag
 */
@XmlRootElement
public class CreatedTag {
	private Tag tag;

	private String html;

	/**
	 * Create an empty instance
	 */
	public CreatedTag() {}

	/**
	 * Create an instance with tag and html code
	 * @param tag tag
	 * @param html rendered tag
	 */
	public CreatedTag(Tag tag, String html) {
		setTag(tag);
		setHtml(html);
	}

	/**
	 * Tag object
	 * @return tag object
	 */
	public Tag getTag() {
		return tag;
	}

	/**
	 * Tag rendered in edit mode
	 * @return rendered tag
	 */
	public String getHtml() {
		return html;
	}

	/**
	 * Set the tag
	 * @param tag tag
	 */
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	/**
	 * Set the html code
	 * @param html html code
	 */
	public void setHtml(String html) {
		this.html = html;
	}
}
