/*
 * @author norbert
 * @date 10.08.2005
 * @version $Id: ClassHelper.java,v 1.6 2007-11-13 10:03:47 norbert Exp $
 */
package com.gentics.lib.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * helper class for class-specific methods (e.g. invoking setters, ...)
 * @author norbert
 */
public class ClassHelper {

	/**
	 * Invoke the expected getter method for the given parameter on the given
	 * object.
	 * @param object object for which the getter method shall be invoked
	 * @param parameterName name of the parameter
	 * @return value of the parameter
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public final static Object invokeGetter(Object object, String parameterName) throws SecurityException, NoSuchMethodException, IllegalArgumentException,
				IllegalAccessException, InvocationTargetException {
		Class clazz = object.getClass();
		String getterName = getGetterName(parameterName);

		Method getter = null;

		// look for the getter method
		getter = clazz.getMethod(getterName, new Class[] {});
		// TODO if "get" getter does not exist, try "is" getter
		return getter.invoke(object, new Object[] {});
	}

	/**
	 * invoke the expected setter method for the given parameter
	 * @param object object for which the setter shall be invoked
	 * @param parameterName name of the parameter
	 * @param parameterValue value to set
	 * @return true when the setter was invoked, false if not
	 */
	public final static boolean invokeSetter(Object object, String parameterName,
			Object parameterValue) throws NoSuchMethodException, IllegalAccessException,
				InvocationTargetException {
		// TODO: make this method much more flexible. When the setter for the
		// exact class of the parameterValue was not found, try with
		// superclasses/interfaces. When this also fails, search for a method
		// with the name but any other paranmeter class and try to convert the
		// given object
		Class clazz = object.getClass();
		String setterName = getSetterName(parameterName);
		Method setter = null;

		// handle null values
		if (parameterValue == null) {
			// find all possible setters
			Method[] methods = clazz.getMethods();

			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(setterName)) {
					if (setter != null) {
						// found another possible setter, cannot set the null
						// value
						throw new NoSuchMethodException(
								"Unable to set null-value for parameter {" + parameterName + "} for object of class {" + clazz.getName() + "}: Setter method {"
								+ setterName + "} is ambigous.");
					} else {
						setter = methods[i];
					}
				}
			}
		} else {
			// value is non-null
			Class valueClass = parameterValue.getClass();
			Class alternativeClass = null;

			// if the valueClass is some implementation of org.w3c.dom.Node,
			// "downgrade" it to org.w3c.dom.Node
			if (org.w3c.dom.Node.class.isAssignableFrom(valueClass)) {
				valueClass = org.w3c.dom.Node.class;
			}

			// when the class has a primitive type, we prepare the alterative class
			// TODO put this into a helper class (maybe usefull somewhere else)
			if (valueClass.equals(Boolean.class)) {
				alternativeClass = Boolean.TYPE;
			} else if (valueClass.equals(Character.class)) {
				alternativeClass = Character.TYPE;
			} else if (valueClass.equals(Byte.class)) {
				alternativeClass = Byte.TYPE;
			} else if (valueClass.equals(Short.class)) {
				alternativeClass = Short.TYPE;
			} else if (valueClass.equals(Integer.class)) {
				alternativeClass = Integer.TYPE;
			} else if (valueClass.equals(Long.class)) {
				alternativeClass = Long.TYPE;
			} else if (valueClass.equals(Float.class)) {
				alternativeClass = Float.TYPE;
			} else if (valueClass.equals(Double.class)) {
				alternativeClass = Double.TYPE;
			} else if (valueClass.equals(Void.class)) {
				alternativeClass = Void.TYPE;
			}

			// look for the setter method
			try {
				try {
					setter = clazz.getMethod(setterName, new Class[] { valueClass});
				} catch (NoSuchMethodException ex) {
					// when the method was not found, but we have an alternative
					// class
					// for the parameter, we try again
					if (alternativeClass != null) {
						setter = clazz.getMethod(setterName, new Class[] { alternativeClass});
					} else {
						throw ex;
					}
				}
			} catch (NoSuchMethodException ex) {
				// last possibility
				setter = clazz.getMethod(setterName, new Class[] { Object.class});
			}
		}

		setter.invoke(object, new Object[] { parameterValue});
		return true;
	}

	/**
	 * transform the given parameter name to the expected name of the setter
	 * method (according to the javabeans spec.)
	 * @param parameterName name of the parameter
	 * @return expected name of the setter method
	 */
	protected final static String getSetterName(String parameterName) {
		StringBuffer setterName = new StringBuffer();

		setterName.append("set").append(parameterName.substring(0, 1).toUpperCase()).append(parameterName.substring(1));

		return setterName.toString();
	}

	/**
	 * transform the given parameter name to the expected name of the getter
	 * method (according to the javabeans spec.)
	 * @param parameterName name of the parameter
	 * @return expected name of the getter method
	 */
	protected final static String getGetterName(String parameterName) {
		StringBuffer getterName = new StringBuffer();

		getterName.append("get").append(parameterName.substring(0, 1).toUpperCase()).append(parameterName.substring(1));

		return getterName.toString();
	}
}
