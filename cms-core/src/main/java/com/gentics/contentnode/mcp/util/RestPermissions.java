package com.gentics.contentnode.mcp.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.gentics.contentnode.rest.filters.AuthorizationRequestFilter;
import com.gentics.contentnode.rest.filters.RequiredPerm;

/**
 * Enforces the {@link RequiredPerm} annotations of REST resource implementations for MCP tools.
 * The REST API checks them in the {@link AuthorizationRequestFilter}, which tools calling the
 * implementation directly bypass.
 */
public final class RestPermissions {
	private RestPermissions() {
	}

	/**
	 * Wrap a REST resource implementation, so that every call first checks the permissions annotated at the
	 * implementing method and the implementation class, as the REST API does
	 * @param <T> resource interface type
	 * @param resource resource interface
	 * @param impl resource implementation
	 * @return guarded resource
	 */
	public static <T> T guard(Class<T> resource, T impl) {
		Class<?> implClass = impl.getClass();
		return resource.cast(Proxy.newProxyInstance(resource.getClassLoader(), new Class<?>[] { resource },
				(proxy, method, args) -> {
					Method implMethod = implClass.getMethod(method.getName(), method.getParameterTypes());
					AuthorizationRequestFilter.check(implMethod, implClass);
					try {
						return implMethod.invoke(impl, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				}));
	}
}
