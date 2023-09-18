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
	 * Set the image variant data
	 * 
	 * @param webrootPath image path
	 */
	public default void setImageData(String webrootPath) {}
}
