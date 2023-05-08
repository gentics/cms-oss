package com.gentics.contentnode.rest.model.response.admin;

import java.util.Map;

/**
 * Model of a custom tool
 */
public class CustomTool {
	protected int id;

	protected String key;

	protected Map<String, String> name;

	protected String toolUrl;

	protected String iconUrl;

	protected boolean newtab;

	/**
	 * Internal ID
	 * @return ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the internal ID
	 * @param id ID
	 * @return fluent API
	 */
	public CustomTool setId(int id) {
		this.id = id;
		return this;
	}

	/**
	 * Unique key of the tool
	 * @return unique key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key
	 * @param key key
	 * @return fluent API
	 */
	public CustomTool setKey(String key) {
		this.key = key;
		return this;
	}

	/**
	 * Name of the Tool in english and german
	 * @return name as map
	 */
	public Map<String, String> getName() {
		return name;
	}

	/**
	 * Set the name in english and german
	 * @param name name map
	 * @return fluent API
	 */
	public CustomTool setName(Map<String, String> name) {
		this.name = name;
		return this;
	}

	/**
	 * Tool URL
	 * @return tool URL
	 */
	public String getToolUrl() {
		return toolUrl;
	}

	/**
	 * Set Tool URL
	 * @param toolUrl tool URL
	 * @return fluent API
	 */
	public CustomTool setToolUrl(String toolUrl) {
		this.toolUrl = toolUrl;
		return this;
	}

	/**
	 * Optional icon URL
	 * @return icon URL
	 */
	public String getIconUrl() {
		return iconUrl;
	}

	/**
	 * Set icon URL
	 * @param iconUrl icon URL
	 * @return fluent API
	 */
	public CustomTool setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
		return this;
	}

	/**
	 * True if the tool shall be opened in a new tab
	 * @return true for new tab
	 */
	public boolean isNewtab() {
		return newtab;
	}

	/**
	 * Set newtab flag
	 * @param newtab true for new tab
	 * @return fluent API
	 */
	public CustomTool setNewtab(boolean newtab) {
		this.newtab = newtab;
		return this;
	}

	@Override
	public String toString() {
		return key;
	}
}
