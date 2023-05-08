package com.gentics.contentnode.image;

public class GenticsImageStoreResult {

	/**
	 * Number of images, that were resized
	 */
	protected int resized;

	/**
	 * Total number of resized images (including the images found in cache)
	 */
	protected int total;

	/**
	 * Create an instance
	 * 
	 * @param resized
	 *            number of images that were resized
	 * @param total
	 *            total number of resized images
	 */
	protected GenticsImageStoreResult(int resized, int total) {
		this.resized = resized;
		this.total = total;
	}

	/**
	 * Get the number of resized images
	 * 
	 * @return number of resized images
	 */
	public int getResized() {
		return resized;
	}

	/**
	 * Total number of images
	 * 
	 * @return total number of images
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * Sets the number of resized images
	 * @param resized
	 */
	void setResized(int resized) {
		this.resized = resized;
	}

	/**
	 * Sets the number of total images
	 * @param total
	 */
	void setTotal(int total) {
		this.total = total;
	}
}