package com.gentics.contentnode.tests.utils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Static helper for mocking NodeObjects
 */
public class ContentNodeMockUtils {
	/**
	 * Mock a page
	 * @param id page ID
	 * @param content content
	 * @return mocked page
	 * @throws NodeException
	 */
	public static Page page(int id, Content content) throws NodeException {
		Page page = mock(Page.class);
		when(page.getStackKeywords()).thenReturn(Page.RENDER_KEYS);
		when(page.getId()).thenReturn(id);

		when(page.getContent()).thenReturn(content);
		when(content.getPages()).thenReturn(Arrays.asList(page));

		return page;
	}

	/**
	 * Mock content
	 * @param id content ID
	 * @return mocked content
	 * @throws NodeException
	 */
	public static Content content(int id) throws NodeException {
		Content content = mock(Content.class);
		when(content.getId()).thenReturn(id);

		Map<String, ContentTag> tags = new HashMap<>();
		when(content.getContentTags()).thenReturn(tags);
		doReturn(tags).when(content).getTags();

		return content;
	}

	/**
	 * Mock content tag
	 * @param id tag ID
	 * @param construct construct
	 * @param name tag name
	 * @param content content
	 * @return mocked tag
	 * @throws NodeException
	 */
	public static ContentTag contentTag(int id, Construct construct, String name, Content content) throws NodeException {
		ContentTag tag = mock(ContentTag.class);
		when(tag.getId()).thenReturn(id);
		when(tag.getName()).thenReturn(name);
		when(tag.getContainer()).thenReturn(content);
		when(tag.getConstruct()).thenReturn(construct);
		content.getContentTags().put(name, tag);

		return tag;
	}

	/**
	 * Mock construct
	 * @param id construct ID
	 * @param keyword construct keyword
	 * @param name construct name
	 * @return mocked construct
	 */
	public static Construct construct(int id, String keyword, String name) {
		Construct construct = mock(Construct.class);
		when(construct.getId()).thenReturn(id);
		when(construct.getKeyword()).thenReturn(keyword);
		when(construct.getName()).thenReturn(new CNI18nString(name));
		return construct;
	}
}
