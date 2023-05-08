/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ContentRenderer.java,v 1.18 2010-04-20 12:52:11 floriangutmann Exp $
 */
package com.gentics.contentnode.parser;

import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * This is the main renderer for any content.node content. This renderer uses other
 * templaterenderer to render the different language features, like tags, velocity,..
 * TODO refactor this to DefaultRenderer
 */
public class ContentRenderer implements TemplateRenderer {

	/**
	 * The key under which the tag-renderer is registered.
	 */
	public static final String RENDERER_TAG = "tagnode";

	/**
	 * Key under which the aloha renderer is registered
	 */
	public static final String RENDERER_ALOHA = "aloha";

	/**
	 * The key under which this renderer is registered.
	 */
	public static final String RENDERER_CONTENT = "content";

	/**
	 * defines the configuration key where the renderer configurations are stored:
	 * RENDERER_CONFKEY.name = name which is used to register the Renderer
	 * RENDERER_CONFKEY.class = class of the Renderer
	 * RENDERER_CONFKEY.default = set to true if this renderer should be used as the default renderer
	 */
	public static final String RENDERER_CONFKEY = "jp_renderers";
    
	private static final NodeLogger logger = NodeLogger.getNodeLogger(ContentRenderer.class);
    
	public ContentRenderer() {}

	/**
	 * Register all content.node renderers to the {@link RendererFactory}.
	 */
	public static void registerRenderer() {
		// TODO is the following line required?
		NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();

		// TODO read existent renderers from configuration
		RendererFactory.registerRenderer(RENDERER_CONTENT, new ContentRenderer());
	}

	/**
	 * Render the code, using templaterenderers for the different language features.
	 *
	 * @param renderResult the current renderresult.
	 * @param template the template to render.
	 * @return the rendered code.
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		NodePreferences prefs = NodeConfigRuntimeConfiguration.getPreferences();
		Collection<Map<String, String>> renderers = prefs.getPropertyObject(RENDERER_CONFKEY);

		for (Map<String, String> renderer : renderers) {
			String name = renderer.get("name");
			if (renderType.useRenderer(name)) {
				// it is important to push a new RenderInfo onto the stack, before changing the default renderer
				// otherwise the default renderer would be changed permanently
				renderType.push();
				if (ObjectTransformer.getBoolean(renderer.get("default"), false)) {
					renderType.setDefaultRenderer(name);
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Rendering string with {" + name + "} - and stack: {" + renderType.getStack().getReadableStack() + "} template: {" + template
							+ "}");
				}

				try {
					TemplateRenderer tRenderer = RendererFactory.getRenderer(name);

					try {
						RuntimeProfiler.beginMark(JavaParserConstants.RENDERER_RENDER, name);
						template = tRenderer.render(renderResult, template);
					} finally {
						RuntimeProfiler.endMark(JavaParserConstants.RENDERER_RENDER, name);
					}
				} finally {
					renderType.pop();
				}
			}
		}

		return template;
	}
}
