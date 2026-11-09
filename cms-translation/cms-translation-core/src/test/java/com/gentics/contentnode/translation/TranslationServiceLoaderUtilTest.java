package com.gentics.contentnode.translation;

import static com.gentics.contentnode.translation.ConfigurationTestUtils.mockTranslationConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.gentics.contentnode.etc.TranslationServiceLoaderUtil;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class TranslationServiceLoaderUtilTest {

	private final static String TRANSLATION_IMPLEMENTATION = "com.gentics.contentnode.translation.DeepLTranslationService";
	private static final String INTERFACE_FQDN = "com.gentics.contentnode.api.translation.TranslationService";
	MockedStatic<NodeConfigRuntimeConfiguration> mockedNodeRuntimeConfig = Mockito.mockStatic(
			NodeConfigRuntimeConfiguration.class);

	@BeforeEach
	void setUp() {
		mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
				.thenReturn(mockTranslationConfig(TRANSLATION_IMPLEMENTATION, "<api-token>"));
	}

	@AfterEach
	void tearDown() throws IOException {
		var serviceFileLocation = TranslationServiceLoaderUtil.getClasspath().get();
		Files.deleteIfExists(
				Paths.get(String.format("%s/META-INF/services/%s", serviceFileLocation, INTERFACE_FQDN)));

		Mockito.framework().clearInlineMocks();
	}

	@Test
	void givenNoServiceDefinitionFileExists_expectToCreateServiceFileAndLoadService()
			throws IOException {
		var isCreated = TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		var isLoaded = TranslationServiceLoaderUtil.reloadService();

		assertThat(isCreated).isTrue();
		assertThat(isLoaded).isTrue();
	}

	@Test
	void givenImplementationIsUpdated_expectToUpdateServiceFileAndReloadService()
			throws IOException {
		var isCreated = TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		var isLoaded = TranslationServiceLoaderUtil.reloadService();

		assertThat(isCreated).isTrue();

		mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
				.thenReturn(mockTranslationConfig(
						"com.gentics.contentnode.api.translation.BetterDeepLTranslationService",
						"<api-token>"));

		var isCreatedWithNewImplementation = TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		assertThat(isCreatedWithNewImplementation).isTrue();
	}

	@Test
	void givenCreatingServiceFileWithInvalidImplementation_expectToFailLoadingService()
			throws IOException {
		mockedNodeRuntimeConfig.when(NodeConfigRuntimeConfiguration::getPreferences)
				.thenReturn(mockTranslationConfig(
						"com.gentics.contentnode.api.translation.BetterDeepLTranslationService",
						"<api-token>"));

		var isCreated = TranslationServiceLoaderUtil.createOrUpdateServiceDefinitionFile();
		var isLoaded = TranslationServiceLoaderUtil.reloadService();

		assertThat(isCreated).isTrue();
		assertThat(isLoaded).isFalse();
	}

}