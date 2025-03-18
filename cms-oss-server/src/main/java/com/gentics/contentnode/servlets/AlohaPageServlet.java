package com.gentics.contentnode.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.CnMapPublisher;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;

/**
 * Servlet that returns a page rendered in Aloha mode.
 * @deprecated use {@link PageResourceImpl#render(String, Integer, String, boolean, String, com.gentics.contentnode.rest.PageResourceImpl.LinksType, boolean)} instead
 */
@Deprecated
public class AlohaPageServlet extends ContentNodeUserServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Renders the page and returns the rendered and Aloha prepared result to
	 * the client.<br />
	 * <br />
	 * Parameter:
	 * <ul>
	 * <li>sid: Session id of the user</li>
	 * <li>realid: Id of the page to render</li>
	 * <li>nodeid: Id of the node for which this page shall be rendered (for multichannelling)</li>
	 * <li>language: Id of the language in which aloha is loaded (1 = German, 2 =
	 * English)</li>
	 * <li>real: String that indicates if the page should be rendered in view
	 * mode or in edit mode. Use "view" for read only and "edit" for edit mode.
	 * The default value is "edit".</li>
	 * <li>type: "html" for getting the page as html (with the script includes
	 * added to the head) or "json" for getting the content and includes
	 * separated in a json response. The default value is "html"</li>
	 * <li>proxyprefix: optional prefix for all URLs</li>
	 * <li>links: "backend" (default) when the generated URLs to other pages or files shall lead to the backend, "frontend" for URLs that link to the frontend </li>
	 * <li>translation_master: id of the page from which this page is translated (if it is translated)</li>
	 * <li>translation_version: version timestamp of the page version from which this page is translated (empty if translated from the current version)</li>
	 * </ul>
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response,
			ContentNodeFactory factory, Transaction t) throws ServletException, IOException {
		boolean channelSet = false;

		// get the node preferences
		NodePreferences nodePreferences = t.getNodeConfig().getDefaultPreferences();

		// set the prefix, if one was passed in a parameter
		boolean prefixSet = false;
		String prefix = ObjectTransformer.getString(request.getParameter("proxyprefix"), null);

		if (!StringUtils.isEmpty(prefix)) {
			prefixSet = true;
			nodePreferences.setProperty(DynamicUrlFactory.STAG_PREFIX_PARAM,
					addPathPrefix(prefix, nodePreferences.getProperty(DynamicUrlFactory.STAG_PREFIX_PARAM)));
			nodePreferences.setProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM,
					addPathPrefix(prefix, nodePreferences.getProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM)));
			nodePreferences.setProperty(DynamicUrlFactory.PROXY_PREFIX_PARAM, prefix);
		}

		// determine which type the URLs shall have
		boolean backendUrls = true;

		if ("frontend".equals(ObjectTransformer.getString(request.getParameter("links"), null))) {
			backendUrls = false;
		}

		try {
			PrintWriter writer = response.getWriter();

			// get and check the page id to render
			Integer pageId = ObjectTransformer.getInteger(request.getParameter("realid"), null);

			if (pageId == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// type of response
			String type = request.getParameter("type");

			if (!"json".equals(type)) {
				type = "html";
			}

			// read only
			String real = request.getParameter("real");
			int renderMode = RenderType.EM_ALOHA_READONLY;

			if (!Arrays.asList("view", "newview").contains(real)) {
				renderMode = RenderType.EM_ALOHA;
			}

			// this flag will be set to true, if the user requested edit mode, but had no permission
			boolean readOnlyBecauseNoPerm = false;

			// this flag will be set to true, if the user requested edit mode, but the page was locked
			boolean readOnlyBecauseLocked = false;

			// load the page
			Page page = null;
			Folder folder = null;
			Wastebin wastebin = Wastebin.EXCLUDE;

			try {
				boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
				// get the nodeid of the scope (may be empty)
				Integer nodeId = ObjectTransformer.getInteger(request.getParameter("nodeid"), null);

				// if the feature "multichannelling" is turned on, we might have to render the page in a different scope
				if (multiChannelling) {
					if (nodeId != null) {
						t.setChannelId(nodeId);
						channelSet = true;
					}
				}

				// first get the page readonly (this will check for existance)
				page = t.getObject(Page.class, pageId);
				if (page == null) {
					// maybe the page is deleted
					try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
						page = t.getObject(Page.class, pageId);

						if (page != null) {
							Node node = page.getOwningNode();
							if (!nodePreferences.isFeature(Feature.WASTEBIN, node) || !t.canWastebin(node)) {
								page = null;
							} else {
								if (renderMode == RenderType.EM_ALOHA) {
									renderMode = RenderType.EM_ALOHA_READONLY;
								}
								wastebin = Wastebin.INCLUDE;
							}
						}
					}

					if (page == null) {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
				}

				try (WastebinFilter filter = wastebin.set()) {
					// if multichannelling is on, no nodeId was specified, but the page belongs to a channel,
					// we set this channel now. Otherwise, rendering of the page would fail
					if (multiChannelling && nodeId == null && page.getChannel() != null) {
						t.setChannelId(page.getChannel().getId());
						channelSet = true;
					}

					// now check view permissions
					if (!PermHandler.ObjectPermission.view.checkObject(page)) {
						halt("You don't have the permission to view this page.", response);
						return;
					}

					// if we also have edit permission, we try to lock the page
					if (renderMode == RenderType.EM_ALOHA && PermHandler.ObjectPermission.edit.checkObject(page)) {
						try {
							page = t.getObject(Page.class, pageId, true);
						} catch (ReadOnlyException e) {
							// in case we cannot lock the page, we will use the readonly variant and change the rendermode
							renderMode = RenderType.EM_ALOHA_READONLY;
							readOnlyBecauseLocked = true;
						}
					} else {
						if (renderMode == RenderType.EM_ALOHA) {
							// when we don't have edit permission, we load the page readonly
							readOnlyBecauseNoPerm = true;
						}
						renderMode = RenderType.EM_ALOHA_READONLY;
					}
					folder = page.getFolder();
					// set the response type always to text/html, since we wrap the page's content in HTML
					response.setContentType("text/html");
				}
			} catch (NodeException e) {
				halt("Could not load the page, probably you have provided an invalid pageId.", response);
				logger.error("Could not load Page with id { " + pageId + " }", e);
				return;
			}

			// render the page and return the result
			String renderedPage;
			Map<TagmapEntryRenderer, Object> renderedProperties = null;

			RenderResult renderResult = new RenderResult();

			// set the RR for the current transaction, so that parameters are not resetted
			t.setRenderResult(renderResult);

			try (WastebinFilter filter = wastebin.set()) {
				NodePreferences preferences = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
				RenderType renderType = RenderType.getDefaultRenderType(preferences, renderMode, t.getSessionId(), 0);

				if (backendUrls) {
					renderType.setRenderUrlFactory(new DynamicUrlFactory(t.getSessionId()));
					renderType.setParameter(AlohaRenderer.LINKS_TYPE, "backend");
				} else {
					// TODO linkways must be fetched from the configuration
					renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINKWAY_PORTAL, RenderUrl.LINKWAY_PORTAL, null));
					renderType.setParameter(AlohaRenderer.LINKS_TYPE, "frontend");
					renderType.setFrontEnd(true);
				}
				renderType.addRenderer("aloha");
				// when the return type is html, the script includes shall be
				// added
				// to the content
				renderType.setParameter(AlohaRenderer.ADD_SCRIPT_INCLUDES, Boolean.valueOf("html".equals(type)));
				renderType.setParameter(AlohaRenderer.LAST_ACTION, request.getParameter("lastaction"));

				// set parameters if the user requested edit mode, but got readonly
				if (readOnlyBecauseNoPerm) {
					renderType.setParameter(AlohaRenderer.READONLY_PERM, true);
				}
				if (readOnlyBecauseLocked) {
					renderType.setParameter(AlohaRenderer.READONLY_LOCKED, true);
				}
				if (page.isDeleted()) {
					renderType.setParameter(AlohaRenderer.DELETED, true);
				}

				t.setRenderType(renderType);

				// when the page shall be rendered for json, we also render the contentmap entries
				if ("json".equals(type)) {
					renderedProperties = new HashMap<>();
					Node node = page.getFolder().getNode();
					ContentMap contentMap = node.getContentMap();

					if (contentMap != null) {
						List<TagmapEntryRenderer> tagmapEntries = contentMap.getTagmapEntries(Page.TYPE_PAGE);

						for (Iterator<TagmapEntryRenderer> iterator = tagmapEntries.iterator(); iterator.hasNext();) {
							TagmapEntryRenderer entry = iterator.next();

							// only add the "real" tagmap entries (which have a tagname)
							if (!StringUtils.isEmpty(entry.getTagname())) {
								renderedProperties.put(entry, null);
							}
						}
					}
				}

				// if the page shall be opened for translation, we tell the renderer
				renderType.setParameter(AlohaRenderer.TRANSLATION_MASTER,
						ObjectTransformer.getInteger(request.getParameter(AlohaRenderer.TRANSLATION_MASTER), null));
				renderType.setParameter(AlohaRenderer.TRANSLATION_VERSION,
						ObjectTransformer.getInteger(request.getParameter(AlohaRenderer.TRANSLATION_VERSION), null));

				String template = RenderUtils.getPreviewTemplate(page, renderMode);
				renderedPage = page.render(template, renderResult, renderedProperties, null, CnMapPublisher.LINKTRANSFORMER, null);
			} catch (NodeException e) {
				halt("Error while rendering page", response);
				logger.error("Error while rendering page", e);
				return;
			}

			if ("html".equals(type)) {
				writer.write(renderedPage);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				StringWriter sw = new StringWriter();

				JsonGenerator jg = new JsonFactory().createJsonGenerator(sw);

				jg.useDefaultPrettyPrinter();
				ObjectNode master = mapper.createObjectNode();

				master.put("content", renderedPage);
				Collection headColl = ObjectTransformer.getCollection(renderResult.getParameters().get(AlohaRenderer.SCRIPT_INCLUDES), Collections.EMPTY_LIST);

				if (!headColl.isEmpty()) {
					master.put("head", ObjectTransformer.getString(headColl.iterator().next(), null));
				}

				if (renderedProperties != null) {
					ObjectNode properties = mapper.createObjectNode();

					for (Iterator<Map.Entry<TagmapEntryRenderer, Object>> iterator = renderedProperties.entrySet().iterator(); iterator.hasNext();) {
						Map.Entry<TagmapEntryRenderer, Object> entry = iterator.next();
						TagmapEntryRenderer tagmapEntry = entry.getKey();
						Object value = entry.getValue();

						if (value instanceof Collection) {
							Collection valueColl = (Collection) value;
							ArrayNode array = properties.putArray(tagmapEntry.getMapname());

							for (Iterator iterator2 = valueColl.iterator(); iterator2.hasNext();) {
								String v = ObjectTransformer.getString(iterator2.next(), null);

								array.add(v);
							}
						} else {
							properties.put(tagmapEntry.getMapname(), ObjectTransformer.getString(value, null));
						}
					}

					master.put("properties", properties);
				}

				mapper.writeValue(jg, master);
				writer.write(sw.toString());
			}
			writer.close();
		} finally {
			if (prefixSet) {
				nodePreferences.unsetProperty(DynamicUrlFactory.STAG_PREFIX_PARAM);
				nodePreferences.unsetProperty(DynamicUrlFactory.PORTLETAPP_PREFIX_PARAM);
			}
			if (channelSet) {
				t.resetChannel();
			}
		}
	}

	/**
	 * Prefix the given page with the given prefix. Make sure that no double slashes are created.
	 * @param prefix Prefix
	 * @param path Path
	 * @return prefixed path
	 */
	protected String addPathPrefix(String prefix, String path) {
		if (prefix == null) {
			return path;
		} else if (path == null) {
			return prefix;
		}
		StringBuffer prefixedPath = new StringBuffer(prefix.length() + path.length());

		prefixedPath.append(prefix);
		if (prefix.endsWith("/") && path.startsWith("/")) {
			prefixedPath.append(path.substring(1));
		} else {
			prefixedPath.append(path);
		}

		return prefixedPath.toString();
	}
}
