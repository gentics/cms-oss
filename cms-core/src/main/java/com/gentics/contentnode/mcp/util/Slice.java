package com.gentics.contentnode.mcp.util;

import java.util.List;

/**
 * Bounds of an offset-based page {@code [from, from + size)} within a list of {@code total}
 * items, for tools that page by offset while the underlying REST resource is page-number based
 * (or not paged at all): fetch all items, then {@link #apply} the slice.
 * @param fromIndex inclusive start index (always within {@code [0, total]})
 * @param toIndex exclusive end index (always within {@code [fromIndex, total]})
 * @param total total number of items
 */
public record Slice(int fromIndex, int toIndex, int total) {
	/**
	 * Compute the slice {@code [from, from + size)} of a list of {@code total} items, cut off at
	 * {@code total}
	 * @param from requested offset (non-negative)
	 * @param size requested page size (positive)
	 * @param total total number of items (non-negative)
	 * @return slice
	 * @throws IllegalArgumentException if an argument is out of range
	 */
	public static Slice of(int from, int size, int total) {
		if (from < 0 || size < 1 || total < 0) {
			throw new IllegalArgumentException(
					"Invalid slice from %d, size %d of %d items".formatted(from, size, total));
		}
		int fromIndex = Math.min(from, total);
		// long arithmetic, so from + size cannot overflow
		int toIndex = (int) Math.min((long) fromIndex + size, total);
		return new Slice(fromIndex, toIndex, total);
	}

	/**
	 * Get the items of this slice
	 * @param <T> item type
	 * @param all all items, of which this slice was computed
	 * @return view of the items in this slice
	 * @throws IllegalArgumentException if the list does not have {@link #total()} items
	 */
	public <T> List<T> apply(List<T> all) {
		if (all.size() != total) {
			throw new IllegalArgumentException(
					"Slice was computed for %d items, but the list has %d".formatted(total, all.size()));
		}
		return all.subList(fromIndex, toIndex);
	}

	/**
	 * Whether there are more items after this slice
	 * @return true iff truncated
	 */
	public boolean truncated() {
		return toIndex < total;
	}

	/**
	 * Offset to request the next page with, if there is one
	 * @return offset of the next page, or null
	 */
	public Integer nextFrom() {
		return truncated() ? toIndex : null;
	}
}
