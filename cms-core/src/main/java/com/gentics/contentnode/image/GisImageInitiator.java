package com.gentics.contentnode.image;

/**
 * A contract for GIS image file lookup.
 * 
 * @author plyhun
 *
 * @param <T> an image file variant foreign key
 */
public interface GisImageInitiator<T> {

	/**
	 * Get the lookup foreign key.
	 * 
	 * @return
	 */
	public T getInitiatorForeignKey();

	/**
	 * Should the GIS proceed with the variant initialization, if the existing image is not found?
	 * 
	 * @return
	 */
	public boolean initiateIfNotFound();

	/**
	 * Should the GIS not attempting loading image data from DB, if no cached data found?
	 * 
	 * @return
	 */
	public boolean useOnlyCachedImageData();

	/**
	 * Set the image variant data
	 * 
	 * @param webrootPath image path
	 * @param transform variant transformation
	 */
	public default void setImageData(String webrootPath, String transform) {}
}
