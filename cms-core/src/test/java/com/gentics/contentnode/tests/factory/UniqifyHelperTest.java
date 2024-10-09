package com.gentics.contentnode.tests.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;

/**
 * Test cases for the {@link UniquifyHelper}
 */
public class UniqifyHelperTest {
	public static ContentLanguage mockLanguage(String code, int id) {
		ContentLanguage mocked = mock(ContentLanguage.class);
		when(mocked.getId()).thenReturn(id);
		when(mocked.getCode()).thenReturn(code);
		return mocked;
	}

	public static ContentLanguage german = mockLanguage("de", 1);

	public static ContentLanguage english = mockLanguage("en", 2);

	public static ContentLanguage french = mockLanguage("fr", 15);

	public static ContentLanguage italian = mockLanguage("it", 17);

	public static List<ContentLanguage> languages = Arrays.asList(german, french, english, italian);

	@Test
	public void testNoChanges() throws NodeException {
		I18nMap map = new I18nMap();
		map.put(english.getId(), "english");
		map.put(german.getId(), "german");
		map.put(italian.getId(), "italian");
		map.put(french.getId(), "french");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "default", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(english.getId(), "english"),
				entry(german.getId(), "german"), entry(french.getId(), "french"), entry(italian.getId(), "italian"));
	}

	@Test
	public void testMakeUniqueInOrder() throws NodeException {
		I18nMap map = new I18nMap();
		map.put(english.getId(), "path");
		map.put(german.getId(), "path");
		map.put(italian.getId(), "path");
		map.put(french.getId(), "path");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "default", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(english.getId(), "path_2"),
				entry(german.getId(), "path"), entry(french.getId(), "path_3"), entry(italian.getId(), "path_1"));
	}

	@Test
	public void testMakeUniqueInOrderTwice() throws NodeException {
		I18nMap map = new I18nMap();
		map.put(english.getId(), "path");
		map.put(german.getId(), "path");
		map.put(italian.getId(), "path");
		map.put(french.getId(), "path");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "default", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(english.getId(), "path_2"),
				entry(german.getId(), "path"), entry(french.getId(), "path_3"), entry(italian.getId(), "path_1"));

		uniqueMap = UniquifyHelper.makeUnique(languages, uniqueMap, "default", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(english.getId(), "path_2"),
				entry(german.getId(), "path"), entry(french.getId(), "path_3"), entry(italian.getId(), "path_1"));
	}

	@Test
	public void testMakeUniqueWithDefault1() throws NodeException {
		I18nMap map = new I18nMap();
		map.put(english.getId(), "path");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "path", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(english.getId(), "path"));
	}

	@Test
	public void testMakeUniqueWithDefault2() throws NodeException {
		I18nMap map = new I18nMap();
		map.put(german.getId(), "path");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "path", Optional.empty(),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(german.getId(), "path_1"));
	}

	@Test
	public void testMakeUniqueChangedFirst() throws NodeException {
		I18nMap original = new I18nMap();
		original.put(english.getId(), "english");
		original.put(german.getId(), "german");
		original.put(italian.getId(), "italian");
		original.put(french.getId(), "french");

		I18nMap map = new I18nMap();
		map.put(english.getId(), "english");
		map.put(german.getId(), "italian");
		map.put(italian.getId(), "italian");
		map.put(french.getId(), "french");

		I18nMap uniqueMap = UniquifyHelper.makeUnique(languages, map, "default", Optional.of(original),
				SeparatorType.underscore, Folder.MAX_PUB_DIR_LENGTH);

		assertThat(uniqueMap).as("Unique Map").containsOnly(entry(german.getId(), "italian_1"),
				entry(english.getId(), "english"), entry(italian.getId(), "italian"), entry(french.getId(), "french"));
	}
}
