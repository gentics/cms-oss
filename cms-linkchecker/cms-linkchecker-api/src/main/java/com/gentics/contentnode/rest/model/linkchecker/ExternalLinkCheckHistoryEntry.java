package com.gentics.contentnode.rest.model.linkchecker;

import java.io.Serializable;

/**
 * Item in the history of validity checks for an external link
 */
public class ExternalLinkCheckHistoryEntry implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	private int timestamp;

	private ExternalLinkStatus status;

	private String reason;

	/**
	 * Timestamp of the check
	 * @documentationExample 1591693475
	 * @return timestamp
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Set timestamp
	 * @param timestamp timestamp
	 * @return fluent API
	 */
	public ExternalLinkCheckHistoryEntry setTimestamp(int timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	/**
	 * Check status
	 * @documentationExample invalid
	 * @return status
	 */
	public ExternalLinkStatus getStatus() {
		return status;
	}

	/**
	 * Set check status
	 * @param status status
	 * @return fluent API
	 */
	public ExternalLinkCheckHistoryEntry setStatus(ExternalLinkStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Failure reason
	 * @documentationExample unknown host
	 * @return reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Set the reason
	 * @param reason reason
	 * @return fluent API
	 */
	public ExternalLinkCheckHistoryEntry setReason(String reason) {
		this.reason = reason;
		return this;
	}

	@Override
	public String toString() {
		String reasonText = "";
		if (reason != null) {
			reasonText = String.format(" (%s)", reason);
		}
		return String.format("@%d: %s%s", timestamp, status, reasonText);
	}
}
