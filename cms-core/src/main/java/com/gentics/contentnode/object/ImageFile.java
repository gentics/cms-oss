/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ImageFile.java,v 1.4 2010-09-27 08:21:57 johannes2 Exp $
 */
package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;

/**
 * This is the image object. It extends the methods of a {@link File} by image-specific methods.
 */
@TType(ImageFile.TYPE_IMAGE)
public interface ImageFile extends File {

	/**
	 * This is the ttype of the image object.
	 */
	public static final int TYPE_IMAGE = 10011;
    
	public static final Integer TYPE_IMAGE_INTEGER = new Integer(TYPE_IMAGE);

	/**
	 * Get the width of the image in pixels.
	 * @return the width of the image in pixels.
	 */
	@FieldGetter("sizex")
	int getSizeX();
    
	/**
	 * Set the width of the image in pixels.
	 * @return the old width of the image in pixels.
	 */
	@FieldSetter("sizex")
	int setSizeX(int dimension) throws ReadOnlyException;
    
	/**
	 * get the height of the image in pixels.
	 * @return the height of the image in pixels.
	 */
	@FieldGetter("sizey")
	int getSizeY();

	/**
	 * Set the height of the image in pixels.
	 * @return the old height of the image in pixels.
	 */
	@FieldSetter("sizey")
	int setSizeY(int dimension) throws ReadOnlyException;
    
	/**
	 * get the resolution of the image in X direction in dpi.
	 * @return the resolution of the image in X direction.
	 */
	@FieldGetter("dpix")
	int getDpiX();

	/**
	 * set the resolution of the image in X direction in dpi.
	 * @return the old resolution of the image in X direction.
	 */
	@FieldSetter("dpix")
	int setDpiX(int dpix) throws ReadOnlyException;

	/**
	 * get the resolution of the image in Y direction in dpi.
	 * @return the resolution of the image in Y direction.
	 */
	@FieldGetter("dpiy")
	int getDpiY();
    
	/**
	 * set the resolution of the image in Y direction in dpi.
	 * @return the old resolution of the image in Y direction.
	 */
	@FieldSetter("dpiy")
	int setDpiY(int dpiy) throws ReadOnlyException;

	@FieldGetter("fpx")
	float getFpX();

	@FieldSetter("fpx")
	float setFpX(float fpx) throws ReadOnlyException;

	@FieldGetter("fpy")
	float getFpY();

	@FieldSetter("fpy")
	float setFpY(float fpy) throws ReadOnlyException;

	/**
	 * whether the image is resizable by Gentics Image Store
	 * @return true or false
	 */
	@FieldGetter("gis_resizable")
	boolean isGisResizable();
}
