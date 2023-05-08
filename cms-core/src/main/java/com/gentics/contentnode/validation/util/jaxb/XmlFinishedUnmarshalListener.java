/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: XmlFinishedUnmarshalListener.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

/**
 * @see XmlFinishedUnmarshalPropagator
 */
public interface XmlFinishedUnmarshalListener {
	void finishedUnmarshal(Object parent);
}
