package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import com.gentics.api.lib.exception.NodeException;

/**
 * Utility class for {@link ServiceLoader}, which will preload all service implementations
 * and store them in a synchronized list
 *
 * @param <S> type of the services
 */
public class ServiceLoaderUtil<S> implements Iterable<S> {
	/**
	 * List of service implementations
	 */
	protected List<S> services = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Load the service implementations and return an instance for it
	 * @param <S> type of the service implementations
	 * @param svc class of the service implementations
	 * @return instance
	 */
	public static <S> ServiceLoaderUtil<S> load(Class<S> svc) {
		return new ServiceLoaderUtil<S>(ServiceLoader.load(svc));
	}

	/**
	 * Create an instance
	 * @param loader service loader
	 */
	protected ServiceLoaderUtil(ServiceLoader<S> loader) {
		for (S service : loader) {
			services.add(service);
		}
	}

	/**
	 * Execute the given function for the first found service implementation
	 * @param <R> type of the result
	 * @param function function to be executed
	 * @return optional result (empty optional, if not service implementation is found)
	 * @throws NodeException
	 */
	public <R> Optional<R> execForFirst(Function<S, R> function) throws NodeException {
		if (services.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(function.apply(services.get(0)));
		}
	}

	@Override
	public Iterator<S> iterator() {
		return services.iterator();
	}
}
