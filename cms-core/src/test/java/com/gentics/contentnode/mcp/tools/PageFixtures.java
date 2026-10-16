package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Template;

/**
 * REST page fixtures for the read tool tests
 */
final class PageFixtures {
	private PageFixtures() {
	}

	/**
	 * Page 4711 in folder 57 of node 3, with template 9 and the tags "teaser" (construct 8, text) and "content1"
	 * (construct 5, richtext)
	 * @return page
	 */
	static Page page() {
		Folder folder = new Folder();
		folder.setId(57);
		folder.setName("News");
		folder.setNodeId(3);
		Template template = new Template();
		template.setId(9);
		template.setName("Article");
		Map<String, Tag> tags = new LinkedHashMap<>();
		tags.put("teaser", tag("teaser", 8, Property.Type.STRING, "Short"));
		tags.put("content1", tag("content1", 5, Property.Type.RICHTEXT, "<p>Long</p>"));
		Page page = new Page();
		page.setId(4711);
		page.setName("Home");
		page.setFolder(folder);
		page.setFolderId(57);
		page.setTemplate(template);
		page.setTemplateId(9);
		page.setTags(tags);
		return page;
	}

	/**
	 * Create a content tag with one property "text"
	 * @param name tag name
	 * @param constructId construct ID
	 * @param type property type
	 * @param value string value
	 * @return tag
	 */
	static Tag tag(String name, int constructId, Property.Type type, String value) {
		Property property = new Property();
		property.setType(type);
		property.setStringValue(value);
		Tag tag = new Tag();
		tag.setName(name);
		tag.setConstructId(constructId);
		tag.setType(Tag.Type.CONTENTTAG);
		tag.setActive(true);
		tag.setProperties(new LinkedHashMap<>(Map.of("text", property)));
		return tag;
	}
}
