package com.gentics.contentnode.translation;

import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.etc.TranslationFeatureService;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class, which acts as bridge to the optional translation module
 */
public class TranslationUtil {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			TranslationFeatureService.class);
	private static final String INTERFACE_FQDN = "com.gentics.contentnode.api.translation.TranslationService";
	private static final String REFERENCE_IMPLEMENTATION = "com.gentics.contentnode.translation.DeepLTranslationService";
	/**
	 * {@link TranslationService} service
	 */
	private static ServiceLoaderUtil<TranslationService> loader = ServiceLoaderUtil.load(
			TranslationService.class);

	/**
	 * This method retrieves the next TranslationService from the loader.
	 *
	 * @return An Optional containing the next TranslationService if one exists.
	 * @throws NoSuchElementException If no more TranslationServices are available.
	 */
	public static Optional<TranslationService> getTranslationService() {
		if (loader.iterator().hasNext()) {
			return Optional.of(loader.iterator().next());
		} else {
			logger.warn("No TranslationService implementation is available.");
			return Optional.empty();
		}
	}

	/**
	 * Creates a service definition file if needed.
	 *
	 * <p>This method checks if a translation service is present. If it is, the method returns true.
	 * If not, it attempts to create a service definition file in the classpath. If the file is
	 * successfully created, the method returns true. If the file cannot be created, the method
	 * returns false.
	 *
	 * @return true if the service definition file is present or was successfully created, false
	 * otherwise
	 */
	public static boolean createServiceDefinitionFileIfNeeded() {
		final var translationService = TranslationUtil.getTranslationService();
		if (translationService.isPresent()) {
			logger.info(String.format("Service implementation '%s' is already present",
					translationService.get().getClass().getSimpleName()));
			return true;
		}

		final var classpath = getClasspath();
		logger.info(
				String.format(
						"Service definition file does not exist. Creating one in the classpath: '%s'.",
						classpath));
		final var serviceDefinitionFilePath = Paths.get(
				String.format("%s/META-INF/services/%s", classpath, INTERFACE_FQDN));
		final var implementationClass = getImplementationClass();

		try {
			Files.createDirectories(serviceDefinitionFilePath.getParent());
			Files.write(serviceDefinitionFilePath, implementationClass.getBytes(),
					StandardOpenOption.CREATE);
			logger.info(String.format("Service definition file for class '%s' successfully created.",
					implementationClass));

			return true;
		} catch (Exception ex) {
			logger.error(String.format(
					"Failed to create the service definition file. Encountered error: '%s'",
					ex.getMessage()));
			return false;
		}
	}

	/**
	 * Get the first path of the classpath if multiple paths are set
	 *
	 * @return the classpath
	 */
	private static String getClasspath() {
		String systemClasspath = System.getProperty("java.class.path");

		return Arrays.stream(systemClasspath.split(":"))
				.findFirst()
				.orElse(systemClasspath);
	}

	/**
	 * This method retrieves the implementation class name from the configuration.
	 *
	 * @return A String representing the implementation class name or the reference implementation.
	 */
	private static String getImplementationClass() {
		var className = ConfigurationValue.TRANSLATION_SERVICE_IMPLEMENTATION.get();
		if (!StringUtils.isBlank(className)) {
			return className;
		}
		return REFERENCE_IMPLEMENTATION;
	}

	public static void reloadService() {
		try{
			loader = ServiceLoaderUtil.load(TranslationService.class);
		}
		catch (ServiceConfigurationError e) {
			logger.error(String.format("Could not load service. The reason is: '%s'",e.getMessage()));
		}
	}

	static public String translate(String textToTranslate, String sourceLanguage,
			String targetLanguage) throws Exception {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.AUTOMATIC_TRANSLATION)) {
			return null;
		}
		return loader.execForFirst(service -> {
			try {
				return service.translate(textToTranslate, sourceLanguage, targetLanguage);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).orElse(StringUtils.EMPTY);
	}
}
