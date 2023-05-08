package com.gentics.contentnode.factory.object;

import com.gentics.contentnode.object.OpResult;

/**
 * Result object for pages updated with the {@link MultiPageUpdater}
 */
public class UpdatePagesResult extends OpResult {
	protected int numPagesUpdated;

	protected int totalPages;

	protected int numErrors;

	protected int noPermission;

	protected int locked;

	/**
	 * Create result object
	 * @param numPagesUpdated number of pages updated
	 * @param totalPages number of total pages
	 * @param numErrors number of errors
	 */
	public UpdatePagesResult(int numPagesUpdated, int totalPages, int numErrors, int noPermission, int locked) {
		super(numErrors == 0 ? Status.OK : Status.FAILURE);
		this.numPagesUpdated = numPagesUpdated;
		this.totalPages = totalPages;
		this.numErrors = numErrors;
		this.noPermission = noPermission;
		this.locked = locked;
	}

	/**
	 * Get number of successfully updated pages
	 * @return number of updated pages
	 */
	public int getNumPagesUpdated() {
		return numPagesUpdated;
	}

	/**
	 * Get total number of pages
	 * @return total number of pages
	 */
	public int getTotalPages() {
		return totalPages;
	}

	/**
	 * Get number of pages that could not be updated due to errors
	 * @return number of pages
	 */
	public int getNumErrors() {
		return numErrors;
	}

	/**
	 * Get number of pages that could not be updated due to missing permissions
	 * @return number of pages
	 */
	public int getNoPermission() {
		return noPermission;
	}

	/**
	 * Get number of pages, which were locked by another user
	 * @return number of pages
	 */
	public int getLocked() {
		return locked;
	}

	/**
	 * Increase counts for the number of pages, where permission was missing
	 * @param noPermission number of pages without permission
	 */
	public void addNoPermission(int noPermission) {
		this.totalPages += noPermission;
		this.noPermission += noPermission;
		this.numErrors += noPermission;
	}
}
