package com.gentics.contentnode.aloha;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Service that adds the gcnlinkchecker plugin, when the feature is activated
 */
public class LinkCheckerAlohaPluginService implements AlohaPluginService {

	@Override
	public void addPluginConfiguration(ObjectNode settings, Node node, Page page, RenderResult renderResult,
			RenderType renderType) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER, node)) {
			ObjectNode gcnLinkCheckerPlugin = settings.objectNode();

			gcnLinkCheckerPlugin.put("defaultProtocol", LinkChecker.getDefaultProtocol(page));
			gcnLinkCheckerPlugin.put("absoluteBase", LinkChecker.getAbsoluteBase(page));
			gcnLinkCheckerPlugin.put("relativeBase", LinkChecker.getRelativeBase(page));

			JsonNode plugins = settings.get("plugins");
			if (plugins instanceof ObjectNode) {
				((ObjectNode) plugins).set("gcnlinkchecker", gcnLinkCheckerPlugin);
			}
		}
	}

	@Override
	public Set<String> getPlugins(Node node) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.LINK_CHECKER, node)) {
			return Collections.singleton("gcn/gcnlinkchecker");
		} else {
			return Collections.emptySet();
		}
	}
}
