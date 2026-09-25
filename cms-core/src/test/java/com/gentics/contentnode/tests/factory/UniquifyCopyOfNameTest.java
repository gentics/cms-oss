package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.UniquifyHelper.makeCopyOfNameUnique;
import static org.apache.commons.collections4.SetUtils.hashSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.i18n.StaticLanguageProvider;
import com.gentics.lib.i18n.LanguageProvider;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.i18n.LanguageProviderWrapper;

/**
 * Tests for {@link UniquifyHelper#makeCopyOfNameUnique(String, Set)}
 */
@RunWith(value = Parameterized.class)
public class UniquifyCopyOfNameTest {
	public final static Locale LOCALE = new Locale("en", "EN");

	@Parameters(name = "{index}: start {0}, expected {1}, obstructors {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"image", "Copy of image", hashSet("image")});
		data.add(new Object[] {"image", "Copy 1 of image", hashSet("image", "Copy of image")});
		data.add(new Object[] {"image", "Copy 2 of image", hashSet("image", "Copy of image", "Copy 1 of image")});
		data.add(new Object[] {"Copy of image", "Copy of Copy of image", hashSet("Copy of image")});
		data.add(new Object[] {"IMAGE", "Copy 1 of IMAGE", hashSet("Copy of image")});
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		ResourceBundle resourceBundle = ResourceBundle.getBundle("contentnode", LOCALE);
		LanguageProvider languageProvider = new StaticLanguageProvider(new Language("1", LOCALE, new Properties() {
			@Override
			public String getProperty(String key) {
				return resourceBundle.getString(key);
			}
		}));

		LanguageProviderFactory.getInstance().registerProviderWrapper(new LanguageProviderWrapper() {
			@Override
			public LanguageProvider getCurrentProvider() {
				return languageProvider;
			}

			@Override
			public String getCurrentLanguageCode() {
				return "en";
			}
		});
	}

	@Parameter(0)
	public String start;

	@Parameter(1)
	public String expected;

	@Parameter(2)
	public Set<String> obstructors;

	@Test
	public void testMakeUnique() throws NodeException {
		String result = makeCopyOfNameUnique(start, obstructors);
		assertThat(result).as("Unique value").isEqualTo(expected);
	}
}
