/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: ImageHeightPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;

/**
 * PartType 18 - Select (image-height)
 */
public class ImageHeightPartType extends StaticSelectPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -5510625589418599323L;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public ImageHeightPartType(Value value) throws NodeException {
		super(value, null);
	}
}
