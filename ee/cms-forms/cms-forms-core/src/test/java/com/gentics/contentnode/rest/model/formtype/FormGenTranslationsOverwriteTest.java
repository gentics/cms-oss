package com.gentics.contentnode.rest.model.formtype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link FormGenTranslations#overwrite(FormGenTranslations)}
 */
public class FormGenTranslationsOverwriteTest {
	/**
	 * Test adding new keys
	 */
	@Test
	public void addKeys() {
		FormGenTranslations general = new FormGenTranslations();
		general.put("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en"));
		general.put("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en"));

		FormGenTranslations custom = new FormGenTranslations();
		custom.put("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en"));
		custom.put("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en"));

		FormGenTranslations overwritten = general.overwrite(custom);

		// general must be unchanged
		assertThat(general).containsOnly(
				entry("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en")));

		// custom must be unchanged
		assertThat(custom).containsOnly(
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));

		// overwritten must contain all
		assertThat(overwritten).containsOnly(
				entry("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en")),
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));
	}

	/**
	 * Test adding languages to already existing keys
	 */
	@Test
	public void addLanguages() {
		FormGenTranslations general = new FormGenTranslations();
		general.put("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en"));
		general.put("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en"));
		general.put("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en"));
		general.put("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en"));

		FormGenTranslations custom = new FormGenTranslations();
		custom.put("translation.key.one", Map.of("it", "translation.one.it", "fr", "translation.one.fr"));
		custom.put("translation.key.three", Map.of("es", "translation.three.es", "pt", "translation.three.pt"));

		FormGenTranslations overwritten = general.overwrite(custom);

		// general must be unchanged
		assertThat(general).containsOnly(
				entry("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en")),
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));

		// custom must be unchanged
		assertThat(custom).containsOnly(
				entry("translation.key.one", Map.of("it", "translation.one.it", "fr", "translation.one.fr")),
				entry("translation.key.three", Map.of("es", "translation.three.es", "pt", "translation.three.pt")));

		// overwritten must contain all
		assertThat(overwritten).containsOnly(
				entry("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en", "it", "translation.one.it", "fr", "translation.one.fr")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en")),
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en", "es", "translation.three.es", "pt", "translation.three.pt")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));
	}

	/**
	 * Test overwriting translations
	 */
	@Test
	public void overwriteTranslations() {
		FormGenTranslations general = new FormGenTranslations();
		general.put("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en"));
		general.put("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en"));
		general.put("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en"));
		general.put("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en"));

		FormGenTranslations custom = new FormGenTranslations();
		custom.put("translation.key.one", Map.of("de", "custom.translation.one.de"));
		custom.put("translation.key.two", Map.of("en", "custom.translation.two.en"));

		FormGenTranslations overwritten = general.overwrite(custom);

		// general must be unchanged
		assertThat(general).containsOnly(
				entry("translation.key.one", Map.of("de", "translation.one.de", "en", "translation.one.en")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "translation.two.en")),
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));

		// custom must be unchanged
		assertThat(custom).containsOnly(
				entry("translation.key.one", Map.of("de", "custom.translation.one.de")),
				entry("translation.key.two", Map.of("en", "custom.translation.two.en")));

		// overwritten must contain all
		assertThat(overwritten).containsOnly(
				entry("translation.key.one", Map.of("de", "custom.translation.one.de", "en", "translation.one.en")),
				entry("translation.key.two", Map.of("de", "translation.two.de", "en", "custom.translation.two.en")),
				entry("translation.key.three", Map.of("de", "translation.three.de", "en", "translation.three.en")),
				entry("translation.key.four", Map.of("de", "translation.four.de", "en", "translation.four.en")));
	}
}
