package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.contentnode.rest.model.ContentLanguage;
import com.gentics.contentnode.rest.model.response.LanguageList;

/**
 * Unit tests for {@link ListResponses}
 */
public class ListResponsesTest {
	@Test
	public void testItems() {
		ContentLanguage language = new ContentLanguage();
		LanguageList list = new LanguageList();
		list.setItems(List.of(language));

		assertThat(ListResponses.items(list)).containsExactly(language);
	}

	@Test
	public void testNullIsEmpty() {
		assertThat(ListResponses.items(new LanguageList())).isEmpty();
		assertThat(ListResponses.<ContentLanguage>items(null)).isEmpty();
	}
}
