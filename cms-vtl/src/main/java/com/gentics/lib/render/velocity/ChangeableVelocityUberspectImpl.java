/*
 * @author norbert
 * @date 06.06.2008
 * @version $Id: ChangeableVelocityUberspectImpl.java,v 1.4.2.1 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.lib.render.velocity;

import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.node.SetExecutor;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelPropertySet;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.resolving.Changeable;

/**
 * @author norbert
 */
public class ChangeableVelocityUberspectImpl extends UberspectImpl {

	/*
	 * (non-Javadoc)
	 * @see org.apache.velocity.util.introspection.UberspectImpl#getPropertySet(java.lang.Object,
	 *      java.lang.String, java.lang.Object,
	 *      org.apache.velocity.util.introspection.Info)
	 */
	public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i) throws Exception {
		VelPropertySet propertySet = super.getPropertySet(obj, identifier, arg, i);

		if (propertySet == null && obj instanceof Changeable) {
			Class claz = obj.getClass();
			SetExecutor executor = new ChangeableSetExecutor(log, claz, identifier);

			if (executor.isAlive()) {
				propertySet = new VelSetterImpl(executor);
			}
		}
		return propertySet;
	}
}

class ChangeableSetExecutor extends SetExecutor {

	public ChangeableSetExecutor(Log log, Class clazz, String property) {
		this.log = log;
		this.property = property;
		discover(clazz);
	}

	protected void discover(Class clazz) {
		if (!Changeable.class.isAssignableFrom(clazz)) {
			// the ChangeableVelocityUberspectImpl already checks
			// whether the obj is changeable, so this case should never happen.
			log.error(
					"An attempt was made to instantiate a SetExecutor for" + " " + clazz.getName() + " although it isn't"
					+ " changeable - it was probably referenced in a" + " velocity set statement: #set(obj.property = 'xyz')");
			return;
		}
		try {
			if (property != null) {
				setMethod((Changeable.class).getMethod("setProperty", new Class[] {
					java.lang.String.class, java.lang.Object.class}));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			String msg = "Exception while looking for put('" + property + "') method";

			log.error(msg, e);
			throw new VelocityException(msg, e);
		}
	}

	public Object execute(Object o, Object arg) {
		Changeable c = (Changeable) o;
		Object oldVal = c.get(property);

		try {
			c.setProperty(property, arg);
		} catch (InsufficientPrivilegesException e) {
			log.error("Error while setting property '" + property + "'", e);
		}
		return oldVal;
	}

	private final String property;
}
