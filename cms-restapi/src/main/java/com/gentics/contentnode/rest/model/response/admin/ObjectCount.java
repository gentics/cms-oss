package com.gentics.contentnode.rest.model.response.admin;

import java.util.Objects;

/**
 * Object counts for the publish info
 */
public class ObjectCount {
	protected int toPublish;

	protected int delayed;

	protected int published;

	protected int remaining;

	/**
	 * Number of objects to publish
	 * @documentationExample 158
	 * @return to publish count
	 */
	public int getToPublish() {
		return toPublish;
	}

	/**
	 * Set number of object to publish
	 * @param toPublish count
	 * @return fluent API
	 */
	public ObjectCount setToPublish(int toPublish) {
		this.toPublish = toPublish;
		return this;
	}

	/**
	 * Number of delayed objects
	 * @documentationExample 0
	 * @return delay count
	 */
	public int getDelayed() {
		return delayed;
	}

	/**
	 * Set number of delayed objects
	 * @param delayed count
	 * @return fluent API
	 */
	public ObjectCount setDelayed(int delayed) {
		this.delayed = delayed;
		return this;
	}

	/**
	 * Number of objects, which are already published in the current publish process
	 * @documentationExample 22
	 * @return publish count
	 */
	public int getPublished() {
		return published;
	}

	/**
	 * Set number of published objects
	 * @param published count
	 * @return fluent API
	 */
	public ObjectCount setPublished(int published) {
		this.published = published;
		return this;
	}

	/**
	 * Number of objects remaining to be published in the current publish process
	 * @documentationExample 136
	 * @return remaining count
	 */
	public int getRemaining() {
		return remaining;
	}

	/**
	 * Set number of remaining objects
	 * @param remaining count
	 * @return fluent API
	 */
	public ObjectCount setRemaining(int remaining) {
		this.remaining = remaining;
		return this;
	}

	@Override
	public String toString() {
		return String.format("toPublish: %d, delayed: %d, published: %d, remaining: %d", toPublish, delayed, published,
				remaining);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectCount) {
			ObjectCount other = (ObjectCount) obj;
			return toPublish == other.toPublish && delayed == other.delayed && published == other.published
					&& remaining == other.remaining;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(toPublish, delayed, published, remaining);
	}
}
