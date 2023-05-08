/*
 * @author herbert
 * @date Nov 12, 2007
 * @version $Id: PropertyStackResolver.java,v 1.2 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.contentnode.resolving;

import java.util.ArrayList;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;

/**
 * wrapper around a StackResolver to be used as a PropertyResolver
 * 
 * (used during publishing into tagmap to evaluate an expression using
 * a StackResolver)
 * 
 * @author herbert
 */
public class PropertyStackResolver extends PropertyResolver {

	private StackResolver stackResolver;

	public PropertyStackResolver(StackResolver stackResolver) {
		super(stackResolver);
		this.stackResolver = stackResolver;
	}

	public List resolvePath(String propertyPath) throws UnknownPropertyException {
		try {
			Object rs = stackResolver.resolve(propertyPath, true);

			if (rs instanceof List) {
				return (List) rs;
			}
			if (rs != null) {
				List list = new ArrayList();

				list.add(new PropertyPathEntry(propertyPath, "", rs, null));
				return list;
			}
		} catch (NodeException e) {
			return null;
		}
		return null;
	}
    
	public Object resolve(String propertyPath, boolean failIfUnresolvablePath) throws UnknownPropertyException {
		try {
			Object ret = stackResolver.resolve(propertyPath);

			if (ret != null || !failIfUnresolvablePath) {
				return ret;
			}
            
			// throw error if path could not be resolved and failIfUnresolvablePath is given.
			throw new UnknownPropertyException("Unable to resolve property {" + propertyPath + "}");
		} catch (NodeException e) {
			// always rethrow error ..
			throw new UnknownPropertyException("Error while trying to resolve property {" + propertyPath + "}", e);
		}
	}
}
