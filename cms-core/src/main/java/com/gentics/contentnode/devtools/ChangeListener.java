package com.gentics.contentnode.devtools;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.media.sse.EventOutput;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;

/**
 * Change Listener, containing an EventOutput instance and the dependencies for which events
 * shall be sent to the EventOutput
 */
public class ChangeListener {
	/**
	 * Node ID for rendering the page
	 */
	protected String nodeId;

	/**
	 * Page ID to be rendered
	 */
	protected String pageId;

	/**
	 * EventOutput that will get the events
	 */
	protected EventOutput eventOutput = new EventOutput();

	/**
	 * Registered dependencies
	 */
	protected Map<Integer, Set<Integer>> dependencies = new HashMap<>();

	/**
	 * Create an instance for listening to changes in the dependencies of the given page (rendered in the given node)
	 * @param nodeId node ID
	 * @param pageId page ID
	 */
	public ChangeListener(String nodeId, String pageId) {
		this.nodeId = nodeId;
		this.pageId = pageId;
	}

	/**
	 * Render the page and store the recalculated dependencies
	 * @return rendered content
	 * @throws NodeException
	 */
	public String render() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			Node node = null;
			String content = null;
			Map<Integer, Set<Integer>> newDependencies = new HashMap<>();

			if (!ObjectTransformer.isEmpty(nodeId)) {
				node = t.getObject(Node.class, nodeId);
				if (node == null) {
					throw new EntityNotFoundException(I18NHelper.get("rest.node.notfound", nodeId));
				}
			}
			try (ChannelTrx cTrx = new ChannelTrx(node)) {
				Page page = t.getObject(Page.class, pageId);
				if (page == null) {
					throw new EntityNotFoundException("Could not find page", "page.notfound", pageId);
				}

				String template = RenderUtils.getPreviewTemplate(page, RenderType.EM_LIVEPREVIEW);

				// render the page, handle dependencies
				RenderType renderType = RenderType.getDefaultRenderType(t.getNodeConfig().getDefaultPreferences(), RenderType.EM_LIVEPREVIEW, t.getSessionId(), 0);
				renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
				renderType.setParameter(AlohaRenderer.LINKS_TYPE, "backend");
				renderType.addRenderer("aloha");
				renderType.setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, false);
				renderType.setHandleDependencies(true);
				renderType.setStoreDependencies(false);
				t.setRenderType(renderType);
				content = page.render(template, t.getRenderResult(), null, null, null, null);

				for (Dependency dep : renderType.getDependencies()) {
					DependencyObject source = dep.getSource();
					newDependencies.computeIfAbsent(t.getTType(source.getObjectClass()), key -> new HashSet<>()).add(source.getObjectId());

					if (source.getElement() != null) {
						newDependencies.computeIfAbsent(t.getTType(source.getElementClass()), key -> new HashSet<>()).add(source.getElement().getId());
					}

					// add listener on implicit construct dependency
					if (source.getElement() instanceof Tag) {
						Tag tag = (Tag)source.getElement();
						newDependencies.computeIfAbsent(Construct.TYPE_CONSTRUCT, key -> new HashSet<>()).add(tag.getConstructId());
					}
				}
			}

			dependencies = newDependencies;
			return content;
		}
	}

	/**
	 * If the listener has a dependency on the given object, return the event output object, otherwise return null
	 * @param objectType object type
	 * @param objectId object ID
	 * @return event output or null
	 */
	public EventOutput listen(int objectType, int objectId) {
		return dependencies.getOrDefault(objectType, Collections.emptySet()).contains(objectId) ? eventOutput : null;
	}

	/**
	 * Get the event output
	 * @return event output
	 */
	public EventOutput getEventOutput() {
		return eventOutput;
	}
}
