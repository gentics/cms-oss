package com.gentics.contentnode.rest;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.Uberspect;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelMethod;

/**
 * Implementation of an {@link Uberspect} that restricts access to methods.
 */
public class RestrictiveDiffUberspector extends UberspectImpl {

	/* (non-Javadoc)
	 * @see org.apache.velocity.util.introspection.UberspectImpl#getMethod(java.lang.Object, java.lang.String, java.lang.Object[], org.apache.velocity.util.introspection.Info)
	 */
	public VelMethod getMethod(Object arg0, String arg1, Object[] arg2, Info arg3) throws Exception {
		throw new Exception("Illegal access to method");
	}
}
