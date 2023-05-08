/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: XmlDerefView.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

import java.util.AbstractList;
import java.util.List;

/**
 * Supposed to be used together with a list of {@link XmlRef}s - makes it very
 * convenient to handle a list of refs without an intermediary method call to
 * convert the refs to the objects they point to. just initialize the refs so
 * that JAXB uses the already present list to add objects, and use the view
 * to convert any refs to objects:
 * 
 * XmlElement(name="obj")
 * List<RefT> refs = new ArrayList<RefT>();
 * XmlTransient
 * List<T>    objs = new XmlDerefView<T,RefT>(refs);
 * 
 * This list is immutable.
 */
public class XmlDerefView<T, RefT extends XmlRef<T>> extends AbstractList<T> {

	public List<? extends RefT> refs;
    
	public XmlDerefView(List<? extends RefT> refs) {
		this.refs = refs;
	}
    
	@Override
	public T get(int index) {
		return refs.get(index).getRef();
	}

	@Override
	public int size() {
		return refs.size();
	}
}
