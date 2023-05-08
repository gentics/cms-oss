package com.gentics.contentnode.rest.model.linkchecker;

import java.io.Serializable;
import java.util.List;

/**
 * REST Model for an external Link
 */
public class ExternalLink implements Cloneable, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private int id;

	private int contentId;

	private int contenttagId;

	private String contenttagName;

	private int valueId;

	private String partName;

	private String url;

	private String text;

	private int lastCheckTimestamp;

	private ExternalLinkStatus lastStatus = ExternalLinkStatus.unchecked;

	private String lastReason;

	private List<ExternalLinkCheckHistoryEntry> history;

	/**
	 * ID
	 * @documentationExample 8
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the ID
	 * @param id ID
	 * @return fluent API
	 */
	public ExternalLink setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * ID of the content containing the external link
	 * @documentationExample 115
	 * @return content ID
	 */
	public int getContentId() {
		return contentId;
	}

	/**
	 * Set the content ID
	 * @param contentId content ID
	 * @return fluent API
	 */
	public ExternalLink setContentId(int contentId) {
		this.contentId = contentId;
		return this;
	}

	/**
	 * ID of the contenttag containing the external link
	 * @documentationExample 685
	 * @return contenttag ID
	 */
	public int getContenttagId() {
		return contenttagId;
	}

	/**
	 * Set the contenttag ID
	 * @param contenttagId contenttag ID
	 * @return fluent API
	 */
	public ExternalLink setContenttagId(int contenttagId) {
		this.contenttagId = contenttagId;
		return this;
	}

	/**
	 * Name of the contenttag
	 * @documentationExample gtxalohapagelink2
	 * @return tag name
	 */
	public String getContenttagName() {
		return contenttagName;
	}

	/**
	 * Set the contenttag name
	 * @param contenttagName name
	 * @return fluent API
	 */
	public ExternalLink setContenttagName(String contenttagName) {
		this.contenttagName = contenttagName;
		return this;
	}

	/**
	 * ID of the value containing the external link
	 * @documentationExample 2859
	 * @return value ID
	 */
	public int getValueId() {
		return valueId;
	}

	/**
	 * Set the value ID
	 * @param valueId value ID
	 * @return fluent API
	 */
	public ExternalLink setValueId(int valueId) {
		this.valueId = valueId;
		return this;
	}


	/**
	 * Name of the part
	 * @documentationExample URL
	 * @return part name
	 */
	public String getPartName() {
		return partName;
	}

	/**
	 * Set part name
	 * @param partName name
	 * @return fluent API
	 */
	public ExternalLink setPartName(String partName) {
		this.partName = partName;
		return this;
	}

	/**
	 * URL of the external link
	 * @documentationExample https://totally.invalid.url/
	 * @return URL
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL
	 * @param url URL
	 * @return fluent API
	 */
	public ExternalLink setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * Link text
	 * @documentationExample Webshop
	 * @return link text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set link text
	 * @param text link text
	 * @return fluent API
	 */
	public ExternalLink setText(String text) {
		this.text = text;
		return this;
	}

	/**
	 * Timestamp of the last validity check (0 if never checked)
	 * @documentationExample 1591693475
	 * @return timestamp
	 */
	public int getLastCheckTimestamp() {
		return lastCheckTimestamp;
	}

	/**
	 * Set timestamp
	 * @param lastCheckTimestamp timestamp
	 * @return fluent API
	 */
	public ExternalLink setLastCheckTimestamp(int lastCheckTimestamp) {
		this.lastCheckTimestamp = lastCheckTimestamp;
		return this;
	}

	/**
	 * Last check status
	 * @documentationExample invalid
	 * @return check status
	 */
	public ExternalLinkStatus getLastStatus() {
		return lastStatus;
	}

	/**
	 * Set check status
	 * @param lastStatus status
	 * @return fluent API
	 */
	public ExternalLink setLastStatus(ExternalLinkStatus lastStatus) {
		this.lastStatus = lastStatus;
		return this;
	}

	/**
	 * Last failure reason
	 * @documentationExample unknown host
	 * @return last reason
	 */
	public String getLastReason() {
		return lastReason;
	}

	/**
	 * Set last reason
	 * @param lastReason last reason
	 * @return fluent API
	 */
	public ExternalLink setLastReason(String lastReason) {
		this.lastReason = (lastReason == null || lastReason.length() <= 100) ? lastReason : lastReason.substring(0,99) + "...";
		return this;
	}

	/**
	 * History of last 5 validity checks
	 * @return list of checks
	 */
	public List<ExternalLinkCheckHistoryEntry> getHistory() {
		return history;
	}

	/**
	 * Set history
	 * @param history list of checks
	 * @return fluent API
	 */
	public ExternalLink setHistory(List<ExternalLinkCheckHistoryEntry> history) {
		this.history = history;
		return this;
	}

	@Override
	public ExternalLink clone() {
		return new ExternalLink()
				.setId(getId())
				.setContentId(getContentId())
				.setContenttagId(getContenttagId())
				.setContenttagName(getContenttagName())
				.setPartName(getPartName())
				.setValueId(getValueId())
				.setLastCheckTimestamp(getLastCheckTimestamp())
				.setLastStatus(getLastStatus())
				.setLastReason(getLastReason())
				.setText(getText())
				.setUrl(getUrl())
				.setHistory(getHistory());
	}

	@Override
	public String toString() {
		String lastReasonText = "";
		if (lastReason != null) {
			lastReasonText = String.format(" (%s)", lastReason);
		}
		return String.format("%s (%s). Content: %d, Tag: %d (%s), Value: %d (%s), @%d: %s%s", url, text, contentId, contenttagId, contenttagName, valueId,
				partName, lastCheckTimestamp, lastStatus, lastReasonText);
	}
}
