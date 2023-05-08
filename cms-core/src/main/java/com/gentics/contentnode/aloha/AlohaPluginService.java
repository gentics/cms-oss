package com.gentics.contentnode.aloha;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * Interface for services that add plugins and plugin configuration to the rendered pages
 */
public interface AlohaPluginService {
	/**
	 * Add plugin configuration to the settings
	 * @param settings settings
	 * @param node node for which the page is rendered
	 * @param page rendered page
	 * @param renderResult render result
	 * @param renderType render type
	 * @throws NodeException
	 */
	void addPluginConfiguration(ObjectNode settings, Node node, Page page, RenderResult renderResult, RenderType renderType) throws NodeException;

	/**
	 * Return plugins to be rendered
	 * @param node node for which the page is rendered
	 * @return set of plugin names
	 * @throws NodeException
	 */
	Set<String> getPlugins(Node node) throws NodeException;
}
