/*
 * @author Stefan Hepp
 * @date 25.12.2005
 * @version $Id: StaticUrlFactory.java,v 1.14 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.factory.url;

import java.util.Map;
import java.util.Objects;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.lib.log.NodeLogger;

/**
 * The static url factory creates url which can be written to a static
 * filesystem or a contentmap.
 */
public class StaticUrlFactory extends AbstractRenderUrlFactory {
	private int linkWay;
	private int fileLinkWay;
	private String fileLinkPrefix;

	/**
	 * When this flag is set, the implementation may automatically detect the "best" linkway
	 * If the flag is not set, the given linkway will be used in any case.
	 */
	private boolean allowAutoDetection = true;

	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Get the publish path for the file. This will not contain protocol and hostname, but only the publish directory and optionally filename.
	 * This method will consider special treatment of the publish directory of the node (which might be ignored in some cases)
	 * @param file file
	 * @param appendFileName true to append the filename
	 * @return publish path
	 * @throws NodeException
	 */
	public static String getPublishPath(File file, boolean appendFileName) throws NodeException {
		Folder folder = file.getFolder();
		Node node = folder.getNode();
		ContentRepository cr = node.getContentRepository();

		if (appendFileName) {
			return String.format("%s%s", file.getFullPublishPath(true, !ignoreNodePublishDir(cr)), file.getFilename());
		} else {
			return file.getFullPublishPath(false, !ignoreNodePublishDir(cr));
		}
	}

	/**
	 * Get the publish path for the page. This will not contain protocol and hostname, but only the publish directory and optionally filename.
	 * This method will consider special treatment of the publish directory of the node (which might be ignored in some cases)
	 * @param page page
	 * @param appendFileName true to append the filename
	 * @return publish path
	 * @throws NodeException
	 */
	public static String getPublishPath(Page page, boolean appendFileName) throws NodeException {
		Folder folder = page.getFolder();
		Node node = folder.getNode();
		ContentRepository cr = node.getContentRepository();

		if (appendFileName) {
			return String.format("%s%s", page.getFullPublishPath(true, !ignoreNodePublishDir(cr)), page.getFilename());
		} else {
			return page.getFullPublishPath(false, !ignoreNodePublishDir(cr));
		}
	}

	/**
	 * Check whether the separate binary publish directory should be ignored when publishing into the given ContentRepository (which may be null)
	 * @param cr ContentRepository (may be null)
	 * @return true to ignore the separate binary publish directory
	 */
	public static boolean ignoreSeparateBinaryPublishDir(ContentRepository cr) {
		return cr != null && cr.getCrType() == Type.mesh;
	}

	/**
	 * Tests if the node publish dir can be ignored for rendering the URL.
	 * This is only the case for a mesh CR and when the project per node feature is deactivated.
	 * @param cr
	 * @return
	 */
	public static boolean ignoreNodePublishDir(ContentRepository cr) {
		return cr != null && cr.getCrType() == Type.mesh && !cr.isProjectPerNode();
	}

	/**
	 * Set the flag to allow auto-detection of better linkway
	 * @param allowAutoDetection true for auto detection
	 */
	public void setAllowAutoDetection(boolean allowAutoDetection) {
		this.allowAutoDetection = allowAutoDetection;
	}

	/**
	 * Check whether auto-detection of better linkway is allowed
	 * @return true for auto detection
	 */
	public boolean isAllowAutoDetection() {
		return allowAutoDetection;
	}

	/**
	 * this is the factory implementation of the renderurl interface.
	 *
	 * For details, see make_link.php ;)
	 */
	private class MyUrl extends AbstractContentRenderUrl {

		public MyUrl(Class<? extends NodeObject> targetClass, Integer targetId) {
			super(targetClass, targetId);
		}

		protected String renderFolder(Folder folder) throws NodeException {
			return generateFolderUrl(folder, linkWay);
		}

		/**
		 * Returns either http or https.
		 *
		 * @param https Wether to use secure http (true)
		 *        or not (false).
		 */
		protected String getUrlProtocol(boolean https) {
			if (https) {
				return "https://";
			} else {
				return "http://";
			}
		}

		protected String renderFile(File file) throws NodeException {
			if (logger.isDebugEnabled()) {
				logger.debug("Render url to {" + file + "}");
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			boolean handleDeps = renderType.doHandleDependencies();

			Node node = file.getFolder().getNode();

			if (logger.isDebugEnabled()) {
				logger.debug("Node of {" + file + "} is {" + node + "}");
			}

			NodePreferences prefs = renderType.getPreferences();

			// use filelinkway if publishing is done to cnmap only and filelinkway is set, else use linkway
			int myLinkWay;
			if (allowAutoDetection) {
				// If the node defines a custom url rendering, use that
				myLinkWay = node.getLinkwayFiles();
				if (myLinkWay <= 0) {
					// Otherwise, use the old fashioned way
					myLinkWay = fileLinkWay > 0 && node.doPublishContentmap()
								&& !node.doPublishFilesystem() && prefs.getFeature("contentmap")
							? fileLinkWay
							: linkWay;
				}
			} else {
				myLinkWay = fileLinkWay > 0 ? fileLinkWay : linkWay;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("filelinkway is {" + RenderType.renderLinkWay(myLinkWay) + "}");
				if (myLinkWay != linkWay) {
					logger.debug(" (Configured linkway is {" + RenderType.renderLinkWay(linkWay) + "})");
					logger.debug(" (Configured filelinkway is {" + RenderType.renderLinkWay(fileLinkWay) + "})");
					logger.debug(" (node.publishContentmap: {" + node.doPublishContentmap() + "})");
					logger.debug(" (node.publishFilesystem: {" + node.doPublishFilesystem() + "})");
					logger.debug(" (feature.contentmap: {" + prefs.getFeature("contentmap") + "})");
				}
			}

			// when the target object is published into a MeshCR, we will only render Mesh links, if the source object is published into the
			// same MeshCR into the same project. Otherwise we will use static links with hostname
			ContentRepository cr = node.getContentRepository();
			if ((myLinkWay & LINK_PORTAL) > 0) {
				if (cr != null && cr.getCrType() == Type.mesh) {
					Node sourceNode = t.getObject(Node.class, getSourceNodeId());
					if (sourceNode != null && !Objects.equals(sourceNode.getMaster(), node.getMaster())
							&& !Objects.equals(sourceNode.getContentRepository(), cr)) {
						myLinkWay = LINK_HOST;
					} else {
					return MeshPublisher.LINKRENDERER.apply(file, MeshPublisher.getBranchName(node, cr.getVersion()));
				}
				}
			}

			if ((myLinkWay & LINK_PORTAL) > 0) {
				// for portal-urls use either plink or fileLinkPrefix, if set
				if (fileLinkPrefix != null && !"".equals(fileLinkPrefix)) {

					StringBuffer url = new StringBuffer(fileLinkPrefix);

					url.append(t.getTType(File.class));
					url.append(".");
					url.append(file.getId());
					url.append("&amp;");
					url.append(file.getName());

					return url.toString();

				} else {
					return generatePortalUrl(file);
				}
			}

			StringBuffer url = new StringBuffer();

			boolean includeHost = (myLinkWay & LINK_HOST) > 0;

			if ((myLinkWay & LINK_AUTO) > 0) {
				includeHost = !node.getId().equals(getSourceNodeId());
				if (logger.isDebugEnabled()) {
					logger.debug(
							"linkway is {" + RenderType.renderLinkWay(myLinkWay) + "}: comparing nodes: {" + node.getId() + "} vs. {" + getSourceNodeId() + "}");
				}
			}

			if (includeHost) {
				if (logger.isDebugEnabled()) {
					logger.debug("Including hostname");
				}
				url.append(getUrlProtocol(node.isHttps()));
				url.append(node.getHostname());
			} else {
				logger.debug("Not including hostname");
			}

			url.append(getPublishPath(file, true));

			// handle dependencies
			if (handleDeps) {
				if (includeHost) {
					// depend on the hostname
					renderType.addDependency(node, "host");
				}
				// depend on the pub_dir's, the file's name, the folder and the node
				renderType.addDependency(node, "pub_dir_bin");
				renderType.addDependency(file.getFolder(), "pub_dir");
				renderType.addDependency(file.getFolder(), "node");
				renderType.addDependency(file, "folder");
				renderType.addDependency(file, "name");
			}

			return url.toString();
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.factory.url.AbstractContentRenderUrl#renderPage(com.gentics.contentnode.object.Page)
		 */
		protected String renderPage(Page page) throws NodeException {
			if (logger.isDebugEnabled()) {
				logger.debug("Render url to {" + page + "}");
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			boolean handleDeps = renderType.doHandleDependencies();

			Folder folder = page.getFolder();

			if (folder == null) {
				throw new NodeException("Error while rendering URL to " + page + ": Folder of page does not exist");
			}

			Node node = folder.getNode();

			if (node == null) {
				throw new NodeException("Error while rendering URL to " + page + ": Node of page does not exist");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Node of {" + page + "} is {" + node + "}, published node is {" + t.getPublishedNodeId() + "}");
			}

			NodePreferences prefs = renderType.getPreferences();

			int myLinkWay;
			if (allowAutoDetection) {
				// If the node defines a custom url rendering, use that
				myLinkWay = node.getLinkwayPages();
				if (myLinkWay <= 0) {
					// Otherwise, use the old fashioned way
					myLinkWay = getLinkWay(node, prefs);
				}
			} else {
				myLinkWay = linkWay;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Linkway is {" + RenderType.renderLinkWay(myLinkWay) + "}");
				if (myLinkWay != linkWay) {
					logger.debug(" (Configured linkway is {" + RenderType.renderLinkWay(linkWay) + "})");
					logger.debug(" (node.publishContentmap: {" + node.doPublishContentmap() + "})");
					logger.debug(" (node.publishFilesystem: {" + node.doPublishFilesystem() + "})");
					logger.debug(" (feature.contentmap: {" + prefs.getFeature("contentmap") + "})");
				}
			}

			// when the target object is published into a MeshCR, we will only render Mesh links, if the source object is published into the
			// same MeshCR into the same project. Otherwise we will use static links with hostname
			ContentRepository cr = node.getContentRepository();
			if ((myLinkWay & LINK_PORTAL) > 0) {
				if (cr != null && cr.getCrType() == Type.mesh) {
					Node sourceNode = t.getObject(Node.class, getSourceNodeId());
					if (sourceNode != null && !Objects.equals(sourceNode.getMaster(), node.getMaster())
							&& !Objects.equals(sourceNode.getContentRepository(), cr)) {
						myLinkWay = LINK_HOST;
					} else {
					return MeshPublisher.LINKRENDERER.apply(page, MeshPublisher.getBranchName(node, cr.getVersion()));
				}
				}
			}

			if ((myLinkWay & LINK_PORTAL) > 0) {
				StringBuffer url = new StringBuffer("<plink id=\"");

				url.append(Page.TYPE_PAGE);
				url.append(".");
				url.append(page.getId());
				url.append("\"");

				if (!node.getId().equals(getSourceNodeId()) && node.isChannel()) {
					url.append(" channelid=\"").append(node.getId()).append("\"");
				}

				if (prefs.getFeature("plink_attributes")) {
					// get list of plink-attributes from config, parse them
					Object props = prefs.getPropertyObject("plink_attributes");

					if (props instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<Object, Object> propsMap = (Map<Object, Object>) props;

						for (Map.Entry<Object, Object> entry : propsMap.entrySet()) {
							if (entry.getValue() instanceof Map) {
								String name = ObjectTransformer.getString(entry.getKey(), null);
								Map<Object, Object> values = (Map<Object, Object>) entry.getValue();
								String tagName = ObjectTransformer.getString(values.get("tagname"), null);
								String objType = ObjectTransformer.getString(values.get("tagobject"), null);
								int tType = ObjectTransformer.getInt(values.get("t_type"), 0);
								NodeObject object = null;

								if ("content".equals(objType) || "template".equals(objType) || "obj".equals(objType)) {
									if ("obj".equals(objType)) {
										if (tType == Page.TYPE_PAGE) {
											object = page;
										} else if (tType == Template.TYPE_TEMPLATE) {
											object = page.getTemplate();
										} else if (tType == Folder.TYPE_FOLDER) {
											object = page.getFolder();
										}
									} else {// this is a template or content tag
										// TODO what to do here
									}
								}

								try {
									if (object != null) {
										renderType.push((StackResolvable) object);
									}

									String value = null;
									// resolve the tagname
									Object resolvedValue = renderType.getStack().resolve(tagName);

									if (resolvedValue instanceof GCNRenderable) {
										RenderResult result = new RenderResult();

										value = ((GCNRenderable) resolvedValue).render(result);
									} else if (resolvedValue != null) {
										value = resolvedValue.toString();
									}
									if (value != null) {
										url.append(" ").append(name).append("=\"").append(value).append("\"");
									}
								} finally {
									if (object != null) {
										renderType.pop();
									}
								}
							}
						}
					}
				}

				url.append(" />");

				return url.toString();
			}

			boolean includeHost = (myLinkWay & LINK_HOST) > 0;

			if ((myLinkWay & LINK_AUTO) > 0) {
				includeHost = !node.getId().equals(getSourceNodeId());
				if (logger.isDebugEnabled()) {
					logger.debug(
							"linkway is {" + RenderType.renderLinkWay(myLinkWay) + "}: comparing nodes: {" + node.getId() + "} vs. {" + getSourceNodeId() + "}");
				}
			}

			StringBuffer url = new StringBuffer();

			if (includeHost) {
				if (logger.isDebugEnabled()) {
					logger.debug("Including hostname");
				}
				url.append(getUrlProtocol(node.isHttps()));
				url.append(node.getHostname());
			} else if (logger.isDebugEnabled()) {
				logger.debug("Not including hostname");
			}

			url.append(page.getFullPublishPath(true)).append(page.getFilename());

			if (handleDeps) {
				addDependencies(page, renderType);
			}

			return url.toString();
		}

		private int getLinkWay(Node node, NodePreferences prefs) {
			// force portal-linkway if publishing into cnmap
			int myLinkWay = node.doPublishContentmap() && !node.doPublishFilesystem() && prefs.getFeature("contentmap") ? LINK_PORTAL : linkWay;

			return myLinkWay;
		}

		public void addDependencies(Page page, RenderType renderType) throws NodeException {
			Folder folder = page.getFolder();
			Node node = folder.getNode();
			int linkWay = getLinkWay(node, renderType.getPreferences());
			boolean includeHost = (linkWay & LINK_HOST) > 0;

			if (includeHost) {
				// we depend on the node's hostname
				renderType.addDependency(node, "host");
			}
			// we depend on the node's pub_dir
			renderType.addDependency(node, "pub_dir");
			// the folder's pub_dir
			renderType.addDependency(folder, "pub_dir");
			// the folder's node_id
			renderType.addDependency(folder, "node");
			// the page's folder_id
			renderType.addDependency(page, "folder");
			// the page's filename
			renderType.addDependency(page, "filename");
			// the page's status
			renderType.addDependency(page, "online");
		}

		protected String renderContentTag(ContentTag contentTag) throws NodeException {
			return null;
		}

		protected String renderTemplateTag(TemplateTag templateTag) throws NodeException {
			return null;
		}

		protected String renderOfflinePage(Page page) throws NodeException {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			if (renderType.doHandleDependencies()) {
				addDependencies(page, renderType);
			}
			return "#";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.render.RenderUrl#getLinkManagement()
		 */
		public LinkManagement getLinkManagement() {
			return StaticUrlFactory.this.getLinkManagement();
		}
	}

	/**
	 * create a new factory with the given url settings.
	 * @param linkWay the linkway to use for urls.
	 * @param fileLinkWay the linkway for files.
	 * @param fileLinkPrefix the linkway for portal-fileurl.
	 */
	public StaticUrlFactory(int linkWay, int fileLinkWay, String fileLinkPrefix) {
		this.linkWay = linkWay;
		this.fileLinkWay = fileLinkWay;
		this.fileLinkPrefix = fileLinkPrefix;
	}

	public RenderUrl createRenderUrl(Class targetObjClass, Integer targetObjId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Generating RenderUrl for {" + targetObjClass + ", " + targetObjId + "}");
		}
		return new MyUrl(targetObjClass, targetObjId);
	}
}
