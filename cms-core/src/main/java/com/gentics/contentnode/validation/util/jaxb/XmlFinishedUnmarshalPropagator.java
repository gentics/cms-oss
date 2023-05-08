/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: XmlFinishedUnmarshalPropagator.java,v 1.1.2.2 2011-02-26 08:51:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Unmarshaller;

/**
 * The SAXB afterUnmarshal() callback that can be implemented in classes that
 * are being unmarshalled have two limitations:
 * 
 * * XmlIDREFS are not initialized when the callback is valled (the whole
 *   document needs to be unmarshalled before IDREFS can be resolved),
 *   
 * * Parents will not have the child initialized for which the
 *   afterUnmarshal() call is made.
 *   
 * This class can be registered on an Unmarshaller and will intercept any
 * instances of XmlFinishedUnmarshalListener and remember them so that an
 * invocation of finishedUnmarshal() on the Listener can be propagated to any
 * sub-listeners.
 */
public class XmlFinishedUnmarshalPropagator extends Unmarshaller.Listener {
	protected final List<XmlFinishedUnmarshalListener> listeners = new ArrayList<XmlFinishedUnmarshalListener>();
	protected final List<Object> parents = new ArrayList<Object>();
    
	@Override
	public void afterUnmarshal(Object target, Object parent) {
		if (XmlFinishedUnmarshalListener.class.isInstance(target)) {
			listeners.add((XmlFinishedUnmarshalListener) target);
			parents.add(parent);
		}
	}
    
	/**
	 * This method must be invoked after unmarshalling a document (not part of the other
	 * JAXB callbacks (beforeUnmarshal(), afterUnmarshal()) that are invoked automatically).
	 * It will propagate the event to any listeners encountered during unmarshalling.
	 */
	public void finishedUnmarshal() {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).finishedUnmarshal(parents.get(i));
		}
	}
}
