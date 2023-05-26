package com.gentics.contentnode.server;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.Source;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for services, that modify the {@link ServletContextHandler} (e.g. add routes)
 */
public interface ServletContextHandlerService {
	/**
	 * Initialize the context
	 * @param context context
	 */
	void init(ServletContextHandler context);

	/**
	 * Callback, which is called when the configuration is reloaded
	 */
	void onReloadConfiguration();

	/**
	 * Add the filter to the servlet handler. If the filter is already added (identified by the given name), it is reconfigured and restarted
	 * @param servletHandler servlet handler
	 * @param name filter name
	 * @param filterClass filter class
	 * @param pathSpec path spec for the filter mapping
	 * @param dispatches dispatching types
	 * @param init optional initializer
	 * @throws NodeException
	 */
	default void addFilter(ServletHandler servletHandler, String name, Class<? extends Filter> filterClass, String pathSpec, EnumSet<DispatcherType> dispatches, Consumer<FilterHolder> init) throws NodeException {
		// get existing filter holder
		FilterHolder filterHolder = servletHandler.getFilter(name);

		// if the filter holder is not found, create a new one
		if (filterHolder == null) {
			filterHolder = servletHandler.newFilterHolder(Source.EMBEDDED);
			filterHolder.setName(name);
			filterHolder.setHeldClass(filterClass);
		}

		// optional intialization
		if (init != null) {
			init.accept(filterHolder);
		}

		// add the filter holder
		servletHandler.addFilter(filterHolder);
		try {
			// if the filter was already started, we restart and initialize it
			if (filterHolder.isStarted()) {
				filterHolder.stop();
				filterHolder.start();
				filterHolder.initialize();
			}
		} catch (Exception e) {
			throw new NodeException("Error while initializing filter " + filterClass.getName(), e);
		}

		// check whether a mapping for the filter already exists
		Optional<FilterMapping> optMapping = Stream
				.of(Optional.ofNullable(servletHandler.getFilterMappings()).orElse(new FilterMapping[0]))
				.filter(mapping -> StringUtils.equals(name, mapping.getFilterName())).findFirst();

		// no mapping exists, so add it
		if (!optMapping.isPresent()) {
			servletHandler.addFilterWithMapping(filterHolder, pathSpec, dispatches);
		}
	}
}
