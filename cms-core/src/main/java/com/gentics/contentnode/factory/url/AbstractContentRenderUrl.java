/*
 * @author Stefan Hepp
 * @date 29.12.2005
 * @version $Id: AbstractContentRenderUrl.java,v 1.16 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.factory.url;

import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * The AbstractContentRenderUrl provides methods to create urls and a basic url creation implementation.
 */
public abstract class AbstractContentRenderUrl implements RenderUrl {

	private int mode;
	private Class<? extends NodeObject> targetClass;
	private Integer targetId;
	private NodeObject source;
	private Integer sourceNodeId;

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Initialize a new renderurl.
	 * @param targetClass the target object class.
	 * @param targetId the target object id.
	 */
	protected AbstractContentRenderUrl(Class<? extends NodeObject> targetClass, Integer targetId) {
		this.targetClass = targetClass;
		this.targetId = targetId;
		this.source = null;
		mode = MODE_LINK;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public int getMode() {
		return mode;
	}

	public Class<? extends NodeObject> getTargetClass() {
		return targetClass;
	}

	public Object getTargetId() {
		return targetId;
	}

	public void setSourceObject(NodeObject source) throws NodeException {
		this.source = source;

		if (logger.isDebugEnabled()) {
			logger.debug("Setting source object to {" + this.source + "}");
		}

		Integer publishedNodeId = TransactionManager.getCurrentTransaction().getPublishedNodeId();

		if (publishedNodeId != null) {
			sourceNodeId = publishedNodeId;
		} else if (source != null) {
			if (source instanceof Page) {
				Page page = (Page) source;

				sourceNodeId = page.getFolder().getNode().getId();
			}
			if (source instanceof Folder) {
				Folder folder = (Folder) source;

				sourceNodeId = folder.getNode().getId();
			}
			if (source instanceof Node) {
				Node node = (Node) source;

				sourceNodeId = node.getId();
			}
			if (source instanceof File) {
				File file = (File) source;

				sourceNodeId = file.getFolder().getNode().getId();
			}
		}

		if (sourceNodeId == null) {
			sourceNodeId = new Integer(0);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("sourceNodeId is now {" + sourceNodeId + "}");
		}
	}

	public NodeObject getSourceObject() {
		return source;
	}

	public String toString() {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			NodeObject obj = t.getObject(targetClass, targetId);

			// target object does not exist
			if (obj == null) {
				if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN) && renderType.doHandleDependencies()) {
					// check whether target object is in wastebin and may be restored
					// for this case, we need to add a dependency
					try (WastebinFilter filter = Wastebin.INCLUDE.set(); PublishCacheTrx pCacheTrx = new PublishCacheTrx(false)) {
						obj = t.getObject(targetClass, targetId);
						if (obj != null && obj.isDeleted()) {
							renderType.addDependency(obj, "online");
						}
					}
				}

				return "#";
			}

			// TODO: get user prefs?
			NodePreferences prefs = t.getRenderType().getPreferences();
			StackResolvable rootObject = t.getRenderType().getRenderedRootObject();

			if (rootObject instanceof NodeObject) {
				setSourceObject((NodeObject) rootObject);
			} else {
				// TODO is this possible to happen?
				setSourceObject(null);
			}

			boolean manageLinkUrls = true;

			switch (getLinkManagement()) {
			case ON:
				manageLinkUrls = true;
				break;

			case OFF:
				manageLinkUrls = false;
				break;

			case FEATURE:
				manageLinkUrls = prefs.getFeature("managelinkurl");
				break;
			}

			if (obj instanceof Page) {
				Page page = (Page) obj;
    
				// check for Feature managelinkurl and dsfallback/contentset_id
				int editMode = TransactionManager.getCurrentTransaction().getRenderType().getEditMode();

				if (manageLinkUrls && (editMode == RenderType.EM_PUBLISH || !prefs.getFeature("managelinkurl_onlyforpublish"))) {
					// check whether page is published
					if (!ObjectTransformer.getBoolean(page.get("online"), false)) {
						// page not published -> check for languagefallback
						if (prefs.getFeature("dsfallback")) {
							// get fallback page
							Page page2 = getFallbackPage(page);

							// no fallback page found -> empty url
							if (page2 == null) {
								return renderOfflinePage(page);
							}
						} else {
							// no fallback -> empty url
							return renderOfflinePage(page);
						}
					}
				}
    
				return renderPage(page);
    
			} else if (obj instanceof File) {
				File file = (File) obj;
    
				// check feature managelinkurl
				if (manageLinkUrls) {
					if ("".equals(file.getName())) {
						return "#";
					}
				}
    
				return renderFile(file);
    
			} else if (obj instanceof Folder) {
				Folder folder = (Folder) obj;
    
				return renderFolder(folder);
			} else if (obj instanceof ContentTag) {
				ContentTag tag = (ContentTag) obj;

				return renderContentTag(tag);
			} else if (obj instanceof TemplateTag) {
				TemplateTag tag = (TemplateTag) obj;

				return renderTemplateTag(tag);
			}
		} catch (NodeException e) {
			// TODO error handling
			logger.error("Error while rendering url to {" + targetClass + "/" + targetId + "}", e);
		}

		return "#";
	}

	/**
	 * Get the source node ID
	 * @return source node ID
	 */
	protected Integer getSourceNodeId() {
		return sourceNodeId;
	}

	/**
	 * helper function to create a new static url to a folder.
	 * @param folder the target folder.
	 * @param linkWay the folder linkway.
	 * @return the url to the folder.
	 */
	protected String generateFolderUrl(Folder folder, int linkWay) throws NodeException {
		StringBuffer url = new StringBuffer();

		if (logger.isDebugEnabled()) {
			logger.debug("Render url to {" + folder + "}");
		}

		Node node = folder.getNode();

		if (logger.isDebugEnabled()) {
			logger.debug("Node of {" + folder + "} is {" + node + "}");
		}

		boolean includeHost = (linkWay & LINK_HOST) > 0;

		if ((linkWay & LINK_AUTO) > 0) {
			includeHost = !(node.getId().equals(getSourceNodeId()));
			if (logger.isDebugEnabled()) {
				logger.debug("linkway is {" + RenderType.renderLinkWay(linkWay) + "}: comparing nodes: {" + node.getId() + "} vs. {" + getSourceNodeId() + "}");
			}
		}

		if (includeHost) {
			if (logger.isDebugEnabled()) {
				logger.debug("Including hostname");
			}

			if (node.isHttps()) {
				url.append("https://");
			} else {
				url.append("http://");
			}

			url.append(node.getHostname());
		} else {
			logger.debug("Not including hostname");
		}

		url.append(node.getPublishDir());
		url.append(folder.getPublishDir());

		return url.toString();
	}

	/**
	 * helper function to create a new plink url.
	 * @param obj the target object.
	 * @return the plink code.
	 */
	protected String generatePortalUrl(NodeObject obj) throws NodeException {

		int tType = TransactionManager.getCurrentTransaction().getTType(obj.getObjectInfo().getObjectClass());

		// we have to reset the ttype here as images will be published to the CR
		// using 10008 as objecttype
		if (tType == ContentFile.TYPE_IMAGE) {
			tType = ContentFile.TYPE_FILE;
		}

		if (obj instanceof AbstractContentObject) {
			((AbstractContentObject) obj).addDependency("id", obj.getId());
		}

		StringBuffer url = new StringBuffer("<plink id=\"");

		url.append(tType);
		url.append(".");
		url.append(obj.getId());
		url.append("\"");

		Node node;

		if (obj instanceof Folder) {
			node = ((Folder) obj).getNode();
		} else {
			node = ((Folder) obj.getParentObject()).getNode();
		}

		if (!node.getId().equals(getSourceNodeId()) && node.isChannel()) {
			url.append(" channelid=\"").append(node.getId()).append("\"");
		}

		return url.append(" />").toString();
	}

	/**
	 * Do the language fallback cancan for a page.
	 * @param page the page for which the fallback variant should be searched.
	 * @return the page if published, or a published variant, or null if no available published variants are found.
	 */
	private Page getFallbackPage(Page page) throws NodeException {
		if (page == null) {
			return null;
		}

		if (ObjectTransformer.getBoolean(page.get("online"), false)) {
			return page;
		}

		List<Page> pages = page.getLanguageVariants(true);

		if (pages.size() > 0) {
			for (Iterator<Page> iterator = pages.iterator(); iterator.hasNext();) {
				Page fallback = (Page) iterator.next();

				if (fallback.isOnline()) {
					return fallback;
				}
			}
		}

		return null;
	}

	/**
	 * render the link to a folder.
	 * @param folder the target folder.
	 * @return the complete url.
	 * @throws NodeException 
	 */
	protected abstract String renderFolder(Folder folder) throws NodeException;

	/**
	 * render the link to a file.
	 * @param file the target file.
	 * @return the complete url.
	 * @throws NodeException 
	 */
	protected abstract String renderFile(File file) throws NodeException;

	/**
	 * render the link to a page.
	 * @param page the target page.
	 * @return the complete url.
	 * @throws NodeException 
	 */
	protected abstract String renderPage(Page page) throws NodeException;
    
	/**
	 * Render an empty link to a page that is currently not available (offline). The result depends on display device - e.g. a "#" for  HTML.
	 * @return A rendered reference to a currently unavailable page
	 * @throws NodeException
	 */
	protected abstract String renderOfflinePage(Page page) throws NodeException;

	/**
	 * Render the link to edit a contenttag
	 * @param contentTag content tag
	 * @return the complete url
	 * @throws NodeException
	 */
	protected abstract String renderContentTag(ContentTag contentTag) throws NodeException;
    
	protected abstract String renderTemplateTag(TemplateTag templateTag) throws NodeException;
}
