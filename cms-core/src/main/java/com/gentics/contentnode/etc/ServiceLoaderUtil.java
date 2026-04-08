package com.gentics.contentnode.etc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;

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

	/**
	 * Execute the given function for the first found service implementation, which matches
	 * @param <R> type of the result
	 * @param function function to be executed
	 * @return optional result (empty optional, if not service implementation is found)
	 * @throws NodeException
	 */
	public <R> Optional<R> execForFirstMatching(Predicate<? super S> predicate, Function<S, R> function) throws NodeException {
		Optional<S> optionalMatch = services.stream().filter(predicate).findFirst();
		if (optionalMatch.isPresent()) {
			return Optional.ofNullable(function.apply(optionalMatch.get()));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Call the given consumer for all service implementations
	 * @param consumer consumer
	 * @throws NodeException
	 */
	public void call(Consumer<S> consumer) throws NodeException {
		for (S service : services) {
			consumer.accept(service);
		}
	}

	/**
	 * Call the given consumer for the first found service implementation, which matches
	 * @param <R> type of the result
	 * @param consumer consumer to be called
	 * @throws NodeException
	 */
	public void callForFirstMatching(Predicate<? super S> predicate, Consumer<S> consumer) throws NodeException {
		Optional<S> optionalMatch = services.stream().filter(predicate).findFirst();
		if (optionalMatch.isPresent()) {
			consumer.accept(optionalMatch.get());
		}
	}

	/**
	 * Get the first service implementation with the specified class.
	 * @return An empty {@link Optional} if no service implementation with the specified class was registered, and
	 * 		an {@code Optional} with the specified service implementation otherwise.
	 */
	@SuppressWarnings("unchecked")
	public <T extends S> Optional<T> get(Class<T> svc) {
		for (var service: services) {
			if (svc.isInstance(service)) {
				return Optional.of((T) service);
			}
		}

		return Optional.empty();
	}

	@Override
	public Iterator<S> iterator() {
		return services.iterator();
	}
}
