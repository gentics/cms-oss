/*
 * @author norbert
 * @date 04.01.2007
 * @version $Id: SelectClassPartType.java,v 1.1 2007-01-04 11:59:05 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;

/**
 * PartType 24 - Select (class)
 */
public class SelectClassPartType extends StaticSelectPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -2748401413577451926L;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public SelectClassPartType(Value value) throws NodeException {
		super(value, null);
	}
}
