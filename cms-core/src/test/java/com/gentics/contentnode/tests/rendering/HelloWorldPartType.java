/*
 * @author norbert
 * @date 07.03.2007
 * @version $Id: HelloWorldPartType.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.contentnode.tests.rendering;

import com.gentics.api.contentnode.parttype.AbstractExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;

/**
 * Hello World implementation as PartType
 */
public class HelloWorldPartType extends AbstractExtensiblePartType {

	/* (non-Javadoc)
	 * @see com.gentics.api.contentnode.parttype.ExtensiblePartType#render()
	 */
	public String render() throws NodeException {
		return "Hello World";
	}
}
