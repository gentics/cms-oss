/*
 * @author Stefan Hepp
 * @date 30.12.2005
 * @version $Id: RendererFactory.java,v 1.13 2010-08-31 15:37:10 clemens Exp $
 */
package com.gentics.contentnode.render;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.render.renderer.EscapeFormatterRenderer;
import com.gentics.contentnode.render.renderer.MetaEditableRenderer;
import com.gentics.contentnode.render.renderer.NBSPFormatterRenderer;
import com.gentics.contentnode.render.renderer.OldEscapeFormatterRenderer;
import com.gentics.lib.log.NodeLogger;

/**
 * The RendererFactory is a static factory which holds different
 * TemplateRenderer implementations, which are stored by key.
 * Own renderer implementations can be added using the {@link #registerRenderer} method.
 */
public class RendererFactory {

	/**
	 * Key for the default escape renderer.
	 */
	public static final String RENDERER_ESCAPE = "escape";

	public static final String RENDERER_OLDESCAPE = "oldescape";

	/**
	 * Key for the default nbsp renderer.
	 */
	public static final String RENDERER_NBSP = "nbsp";
    
	/**
	 * Editable Renderer
	 */
	public static final String RENDERER_METAEDITABLE = "metaeditable";

	private static Map<String, TemplateRenderer> renderer = new HashMap<String, TemplateRenderer>();
	private static boolean renderersInitialized = false;

	static {
		renderer.put(RENDERER_ESCAPE, new EscapeFormatterRenderer());
		renderer.put(RENDERER_NBSP, new NBSPFormatterRenderer());
		renderer.put(RENDERER_OLDESCAPE, new OldEscapeFormatterRenderer());
		renderer.put(RENDERER_METAEDITABLE, new MetaEditableRenderer());
	}
    
	/**
	 * Register a new renderer under a given key. The renderer may be reused in more than one
	 * thread, so it must be stateless and threadsave.
	 * @param type the key under which the renderer will be stored.
	 * @param renderer the renderer to register.
	 */
	public static void registerRenderer(String type, TemplateRenderer renderer) {
		RendererFactory.renderer.put(type, renderer);
	}

	/**
	 * Get a list of all currently registered renderer-keys.
	 * @return list of all current renderer-keys as String.
	 */
	public static Set<String> getRendererKeys() {
		return Collections.unmodifiableSet(renderer.keySet());
	}

	/**
	 * Get a renderer by key. The renderer is not duplicated, so any modification on the
	 * renderer will modify all used renderers of this key.
	 *
	 * @param type the key of the renderer to get.
	 * @return the renderer, or null if the key is unknown.
	 */
	public static TemplateRenderer getRenderer(String type) {
		return (TemplateRenderer) RendererFactory.renderer.get(type);
	}

	/**
	 * initialize renderes from properties
	 * @param nodePreferences node preferences
	 */
	public static synchronized void initRenderers(NodePreferences nodePreferences) {
		if (!renderersInitialized) {
			Collection<Map<String, String>> renderers = nodePreferences.getPropertyObject(ContentRenderer.RENDERER_CONFKEY);

			try {
				for (Map<String, String> renderer : renderers) {
					String name = renderer.get("name");
					String clazz = renderer.get("class");

					if (clazz != null) {
						// check whether the class has a constructor with NodePreferences as parameter
						Class<?> rendererClass = Class.forName(clazz);

						try {
							Constructor<?> constructor = rendererClass.getConstructor(new Class[] { NodePreferences.class});

							registerRenderer(name, (TemplateRenderer) constructor.newInstance(new Object[] { nodePreferences}));
						} catch (NoSuchMethodException e) {
							// try the default constructor
							registerRenderer(name, (TemplateRenderer) rendererClass.newInstance());
						}
					}
				}
			} catch (Exception e) {
				NodeLogger.getLogger(RendererFactory.class).error("problem when configuring renderers", e);
			}
			renderersInitialized = true;
		}
	}
}
