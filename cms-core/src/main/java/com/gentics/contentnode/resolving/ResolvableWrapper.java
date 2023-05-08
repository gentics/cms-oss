package com.gentics.contentnode.resolving;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Resolvable wrapper for objects
 *
 * @param <T> type of the wrapped object
 */
public class ResolvableWrapper <T> implements Resolvable {
	/**
	 * Wrapped object
	 */
	protected T wrapped;

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
	protected final static Object invokeGetter(Object object, String parameterName) throws SecurityException, NoSuchMethodException, IllegalArgumentException,
				IllegalAccessException, InvocationTargetException {
		Class<? extends Object> clazz = object.getClass();

		for (String prefix : Arrays.asList("get", "is")) {
			String getterName = getGetterName(prefix, parameterName);
			try {
				Method getter = clazz.getMethod(getterName);
				return getter.invoke(object);
			} catch (NoSuchMethodException ignored) {
			}
		}

		return null;
	}

	/**
	 * transform the given parameter name to the expected name of the getter
	 * method (according to the javabeans spec.)
	 * @param prefix method name prefix
	 * @param parameterName name of the parameter
	 * @return expected name of the getter method
	 */
	protected final static String getGetterName(String prefix, String parameterName) {
		return String.format("%s%s%s", prefix, parameterName.substring(0, 1).toUpperCase(), parameterName.substring(1));
	}

	/**
	 * Create an instance
	 * @param wrapped wrapped object
	 */
	public ResolvableWrapper(T wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		// simply call the getter on the object
		try {
			return invokeGetter(wrapped, key);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean canResolve() {
		return true;
	}

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	public T unwrap() {
		return wrapped;
	}
}
