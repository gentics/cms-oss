package com.gentics.contentnode.object;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.contentnode.factory.ChannelTrx;

/**
 * Channel Transaction Invocation Handler
 */
public class ChannelTrxInvocationHandler implements InvocationHandler {
	/**
	 * Map of interfaces for classes
	 */
	private final static Map<Class<?>, Class<?>[]> INTERFACES = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Channel ID
	 */
	private final int channelId;

	/**
	 * Wrapped object
	 */
	private final Object object;

	/**
	 * Create an instance
	 * @param channelId channel ID
	 * @param object object
	 */
	protected ChannelTrxInvocationHandler(int channelId, Object object) {
		this.channelId = channelId;
		this.object = object;
	}

	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try (ChannelTrx cTrx = new ChannelTrx(channelId)) {
			// special behaviour for .equals method: localizable node objects are equal, if they are of the same class, share the same ID and the same node ID
			if ("equals".equals(method.getName()) && args.length == 1 && object instanceof Disinheritable<?> && args[0] instanceof Disinheritable<?>) {
				Disinheritable<?> dObj = (Disinheritable<?>)object;
				Disinheritable<?> other = (Disinheritable<?>)args[0];
				return dObj.equals(other) && dObj.getNode().getId().equals(other.getNode().getId());
			} else {
				return wrap(method.invoke(object, args));
			}
		}
	}

	/**
	 * Wrap the given object, if it is a {@link NodeObject}
	 * @param o object to wrap
	 * @return either proxy or the original object
	 */
	protected Object wrap(Object o) {
		if (o instanceof NodeObject) {
			return wrap(channelId, (NodeObject)o);
		} else if (o instanceof Collection<?>) {
			return wrap(channelId, (Collection<?>)o);
		} else if (o instanceof Object[]) {
			Object[] oArray = (Object[])o;
			Object[] wrapper = oArray.clone();
			for (int i = 0; i < oArray.length; i++) {
				wrapper[i] = wrap(oArray[i]);
			}
			return wrapper;
		} else {
			return o;
		}
	}

	/**
	 * Get the interfaces that are implemented by the given class (or any of its superclasses)
	 * @param objectClass object class
	 * @return array of implemented interfaces
	 */
	protected static Class<?>[] getInterfaces(Class<?> objectClass) {
		return INTERFACES.computeIfAbsent(objectClass, clazz -> {
			Set<Class<?>> interfaces = new HashSet<>();

			while (clazz != null) {
				interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
				clazz = clazz.getSuperclass();
			}

			return (Class<?>[]) interfaces.toArray(new Class<?>[interfaces.size()]);
		});
	}

	/**
	 * Wrap the given collection with an invocation handler that changes the channel scope
	 * @param channelId channel ID
	 * @param collection collection to wrap
	 * @return proxy
	 */
	public static Collection<?> wrap(int channelId, Collection<?> collection) {
		Class<?>[] interfaceArray = getInterfaces(collection.getClass());
		return (Collection<?>) Proxy.newProxyInstance(ChannelTrxInvocationHandler.class.getClassLoader(), interfaceArray,
				new ChannelTrxInvocationHandler(channelId, collection));
	}

	/**
	 * Wrap the given object with an invocation handler that changes the channel scope
	 * @param channelId channel ID
	 * @param nodeObject node object to wrap
	 * @return proxy
	 */
	public static NodeObject wrap(int channelId, NodeObject nodeObject) {
		Class<?>[] interfaceArray = getInterfaces(nodeObject.getClass());
		return (NodeObject) Proxy.newProxyInstance(ChannelTrxInvocationHandler.class.getClassLoader(), interfaceArray,
				new ChannelTrxInvocationHandler(channelId, nodeObject));
	}
}
