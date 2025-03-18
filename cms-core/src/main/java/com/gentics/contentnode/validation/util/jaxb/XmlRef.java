/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: XmlRef.java,v 1.1.2.1 2011-02-10 13:43:27 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;

public abstract class XmlRef<T> implements XmlFinishedUnmarshalListener {
	@XmlIDREF
	@XmlAttribute(required = true)
	protected Object ref;

	protected abstract Class<T> getRefClass(); 
    
	protected void checkRefClass() {
		// TODO: will this also work for two classes that have different
		// generic parameters but the same base type (after erasure),
		// or will these return the same class and therefore be considered
		// the same?
		if (!getRefClass().isAssignableFrom(ref.getClass())) {
			throw new IllegalStateException(
					"XML IDREF points to instance of class `" + ref.getClass().getCanonicalName() + "'" + " but should point to instance of class `"
					+ getRefClass().getCanonicalName() + "'");
		}
	}
    
	public void finishedUnmarshal(Object parent) {
		checkRefClass();
	}
    
	public T getRef() {
		// may be a performance penalty, but it's best to fail early
		// in case the UnmarshalListener was not registered on the unmarshaller.
		checkRefClass();
        
		@SuppressWarnings("unchecked")
		T castRef = (T) ref;

		return castRef;
	}
}
