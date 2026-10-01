package com.gentics.contentnode.mcp.model;

/**
 * Mapping of the unix timestamps of the REST model to the MCP output records
 */
public final class Timestamps {
	private Timestamps() {
	}

	/**
	 * Map a timestamp that is 0 or negative when not set (e.g. {@code pdate} of a page that was
	 * never published), so that it is omitted from the output
	 * @param timestamp timestamp
	 * @return timestamp, or null if not set
	 */
	public static Integer orNull(int timestamp) {
		return timestamp > 0 ? timestamp : null;
	}
}
