package com.gentics.contentnode.rest.model.request;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The REST request connection a set of input linkable entities to the selected targets, in bulk.
 * 
 * @author plyhun
 *
 */
public class BulkLinkUpdateRequest implements Serializable {

	private static final long serialVersionUID = 3899750054256133786L;

	/**
	 * Node IDs
	 */
	protected Set<Integer> ids = new HashSet<Integer>();

	/**
	 * Target IDs
	 */
	protected Set<String> targetIds = new HashSet<String>();

	public Set<Integer> getIds() {
		return ids;
	}

	public BulkLinkUpdateRequest setIds(Set<Integer> ids) {
		this.ids = ids;
		return this;
	}

	public Set<String> getTargetIds() {
		return targetIds;
	}

	public BulkLinkUpdateRequest setTargetIds(Set<String> ids) {
		this.targetIds = ids;
		return this;
	}
}
