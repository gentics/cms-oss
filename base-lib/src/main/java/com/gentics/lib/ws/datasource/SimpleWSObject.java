/*
 * @author norbert
 * @date 28.08.2006
 * @version $Id: SimpleWSObject.java,v 1.2 2006-09-07 11:33:14 laurin Exp $
 */
package com.gentics.lib.ws.datasource;

import com.gentics.lib.datasource.simple.SimpleObject;

/*
 * Implementation notice: keep this class synchronized with the interface
 * SimpleObject
 */

/**
 * Wrapper around the interface
 * {@link com.gentics.lib.datasource.simple.SimpleObject}. This wrapper is
 * used as return value for webservice calls.
 */
public class SimpleWSObject {

	/**
	 * wrapped object
	 */
	private SimpleObject object;

	/**
	 * Create wrapper object
	 * @param object wrapped object
	 */
	public SimpleWSObject(SimpleObject object) {
		this.object = object;
	}

	/**
	 * Get the objects id
	 * @return objects id
	 */
	public String getId() {
		return object.getId();
	}

	/**
	 * Get the attributes of the object
	 * @return array of attributes
	 */
	public SimpleWSAttribute[] getAttributes() {
		return ObjectWrapperHelper.wrapAttributes(object.getAttributes());
	}
}
