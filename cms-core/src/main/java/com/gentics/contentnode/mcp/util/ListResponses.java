package com.gentics.contentnode.mcp.util;

import java.util.List;

import com.gentics.contentnode.rest.model.response.AbstractListResponse;

/**
 * Helpers for the list responses of the REST resource implementations
 */
public final class ListResponses {
	private ListResponses() {
	}

	/**
	 * Get the items of a list response, never null
	 * @param <T> item type
	 * @param response list response, may be null
	 * @return items, or an empty list if the response or its items are null
	 */
	public static <T> List<T> items(AbstractListResponse<T> response) {
		if (response == null || response.getItems() == null) {
			return List.of();
		}
		return response.getItems();
	}
}
