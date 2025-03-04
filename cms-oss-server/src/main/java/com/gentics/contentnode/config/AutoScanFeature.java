package com.gentics.contentnode.config;

import com.gentics.lib.log.NodeLogger;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.DuplicatePostProcessor;

import java.io.IOException;

/**
 * AutoScanFeature adds all classes that are in the inhabitant file that are
 * all classes annotated with @Service, @Component, etc.
 * Those classes are thereafter available via the ServiceLocator.
 */
public class AutoScanFeature implements Feature {

	@Inject
	ServiceLocator serviceLocator;

	@Override
	public boolean configure(FeatureContext context) {
		DynamicConfigurationService dynamicConfigurationService =
				serviceLocator.getService(DynamicConfigurationService.class);

		try {
			// Populator - populate HK2 service locators from inhabitants files
			// ClasspathDescriptorFileFinder - find files from META-INF/hk2-locator/default
			dynamicConfigurationService.getPopulator().populate(
					new ClasspathDescriptorFileFinder(this.getClass().getClassLoader()),
					new DuplicatePostProcessor()
			);

		} catch (IOException | MultiException ex) {
			NodeLogger log = NodeLogger.getNodeLogger(AutoScanFeature.class);
			log.error(String.format("Dependency injection failed. %s", ex));
		}
		return true;
	}
}