package com.gentics.contentnode.etc;

import com.gentics.contentnode.api.translation.TranslationService;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class, which acts as bridge to the optional translation module
 */
public class TranslationServiceLoaderUtil {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(
			TranslationFeatureService.class);
	private static final String INTERFACE_FQDN = "com.gentics.contentnode.api.translation.TranslationService";
	/**
	 * {@link TranslationService} service
	 */
	private static ServiceLoaderUtil<TranslationService> loader;

	/**
	 * This method retrieves the next TranslationService from the loader.
	 *
	 * @return An Optional containing the next TranslationService if one exists.
	 * @throws NoSuchElementException If no more TranslationServices are available.
	 */
	public static Optional<TranslationService> getTranslationService() {
		if (loader != null && loader.iterator().hasNext()) {
			return Optional.of(loader.iterator().next());
		}
		return Optional.empty();
	}

	/**
	 * Creates or updates a service definition file if needed.
	 *
	 * @return true if the service definition file is created or updated, false otherwise
	 */
	public static boolean createOrUpdateServiceDefinitionFile() {
		final var classpathOpt = getClasspath();
		if (classpathOpt.isEmpty()) {
			logger.error("No directory is present in the classpath");
			return false;
		}
		final var classpath = classpathOpt.get();

		final var implementationClassOpt = getImplementationClass();
		if (implementationClassOpt.isEmpty()) {
			logger.error("No implementation for the 'TranslationService' interface is configured.");
			return false;
		}
		final var implementationClassFromConfig = implementationClassOpt.get();

		final var serviceDefinitionFilePath = Paths.get(
				String.format("%s/META-INF/services/%s", classpath, INTERFACE_FQDN));

		try {
			if (serviceDefinitionRequireUpdate(implementationClassFromConfig,
					serviceDefinitionFilePath)) {
				Files.createDirectories(serviceDefinitionFilePath.getParent());
				Files.write(serviceDefinitionFilePath, implementationClassFromConfig.getBytes(),
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				logger.info(String.format("Service definition file for class '%s' successfully created.",
						implementationClassFromConfig));
			}

			return true;
		} catch (Exception e) {
			logger.error(String.format(
							"Failed to create the service definition file. Encountered error: '%s'", e.getMessage()), e);
			return false;
		}
	}

	/**
	 * Checks if the TranslationService implementation is already provided.
	 *
	 * @return true if the implementation is provided and matches the configured class name, false otherwise
	 */
	public static boolean isAlreadyProvided() {
		final var implementationClassOpt = getImplementationClass();
		if (implementationClassOpt.isEmpty()) {
			logger.error("No implementation for the 'TranslationService' interface is configured.");
			return false;
		}
		final var implementationClassFromConfig = implementationClassOpt.get();

		var currentService = getTranslationService();
		return currentService.map(translationService -> translationService.getClass().getName()
						.equals(implementationClassFromConfig))
				.orElse(false);
	}

	/**
	 * Checks if the service definition file requires an update.
	 *
	 * @param implementationClass       The class name of the implementation.
	 * @param serviceDefinitionFilePath The file path of the service definition.
	 * @return Returns true if the service definition file does not exist or if the implementation
	 * class from the definition file does not match the provided implementation class. Otherwise,
	 * returns false.
	 * @throws IOException If an I/O error occurs reading from the file or a malformed or unmappable
	 *                     byte sequence is read.
	 */
	private static boolean serviceDefinitionRequireUpdate(String implementationClass,
			Path serviceDefinitionFilePath)
			throws IOException {
		if (!Files.exists(serviceDefinitionFilePath)) {
			return true;
		}

		var implementationClassFromDefinitionFile = Files.readString(serviceDefinitionFilePath);
		return !implementationClassFromDefinitionFile.equals(implementationClass);
	}

	/**
	 * Get the first path of the classpath if multiple paths are set
	 *
	 * @return the classpath
	 */
	public static Optional<String> getClasspath() {
		String systemClasspath = System.getProperty("java.class.path");

		return Arrays.stream(systemClasspath.split(Pattern.quote(File.pathSeparator)))
				.filter(path -> Files.isDirectory(Paths.get(path)))
				.findFirst();
	}

	/**
	 * This method retrieves the implementation class name from the configuration.
	 *
	 * @return An optional String representing the implementation class name or the reference
	 * implementation.
	 */
	private static Optional<String> getImplementationClass() {
		var className = ConfigurationValue.TRANSLATION_SERVICE_IMPLEMENTATION.get();
		if (!StringUtils.isBlank(className)) {
			return Optional.of(className);
		}
		return Optional.empty();
	}

	/**
	 * Reloads the service.
	 *
	 * @return true if reload was successfully
	 */
	public static boolean reloadService() {
		try {
			loader = ServiceLoaderUtil.load(TranslationService.class);
			return true;
		} catch (ServiceConfigurationError e) {
			logger.error(String.format("Could not load service. The reason is: '%s'", e.getMessage()), e);
			return false;
		}
	}

}
