package com.gentics.contentnode.publish;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentMap.ContentMapTrx;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;
import com.gentics.contentnode.publish.cr.TagmapEntryRenderer;
import com.gentics.contentnode.publish.mesh.MeshPublishController;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher.Scheduled;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.lib.log.NodeLogger;

/**
 * Static implementation of instant publishing
 */
public class InstantPublisher {
	/**
	 * Log
	 */
	private final static NodeLogger log = NodeLogger.getNodeLogger(InstantPublisher.class);

	/**
	 * Private constructor
	 */
	private InstantPublisher() {
	}

	/**
	 * Handle instant publishing for the event on the given object. If the
	 * object shall be published into a content repository, and the content
	 * repository has instant publishing activated, the object is immediately
	 * written into the content repository (or removed from there)
	 * @param object object to handle
	 * @param eventMask event mask
	 * @param node TODO
	 * @param property property attached to the event (null if not applicable)
	 * @throws NodeException
	 */
	public static void handleInstantPublishing(NodeObject object, int eventMask, Node node, String[] property) throws NodeException {
		// TODO handle multichannelling here

		// no object, no instant publishing
		if (object == null) {
			return;
		}

		// no instant publishing on notify events
		if (Events.isEvent(eventMask, Events.NOTIFY)) {
			return;
		}

		// get the current transaction
		Transaction t = TransactionManager.getCurrentTransaction();

		// check whether the feature instant publishing is activated
		if (!t.getNodeConfig().getDefaultPreferences().getFeature("instant_cr_publishing")) {
			return;
		}

		// check whether instant publishing was disabled for the transaction
		if (!t.isInstantPublishingEnabled()) {
			return;
		}

		// when an object tag was modified, we switch to the container object
		if (object instanceof ObjectTag && (Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.CREATE))) {
			object = ((ObjectTag) object).getNodeObject();
			eventMask = Events.UPDATE;
			if (object == null) {
				return;
			}
		}

		// check whether the object was already published within this transaction
		Map<String, Object> attrsMap = t.getAttributes();
		Collection<NodeObject> publishedObjects = null;

		if (node == null) {
			publishedObjects = (Collection<NodeObject>) attrsMap.get(Transaction.TRX_ATTR_PUBLISHED);
			if (publishedObjects == null) {
				publishedObjects = new Vector<NodeObject>();
				attrsMap.put(Transaction.TRX_ATTR_PUBLISHED, publishedObjects);
			}
		} else {
			Map<Object, Collection<NodeObject>> perNodeMap = (Map<Object, Collection<NodeObject>>) attrsMap.get(Transaction.TRX_ATTR_PUBLISHED_PERNODE);

			if (perNodeMap == null) {
				perNodeMap = new HashMap<Object, Collection<NodeObject>>();
				attrsMap.put(Transaction.TRX_ATTR_PUBLISHED_PERNODE, perNodeMap);
			}
			publishedObjects = perNodeMap.get(node.getId());
			if (publishedObjects == null) {
				publishedObjects = new Vector<NodeObject>();
				perNodeMap.put(node.getId(), publishedObjects);
			}
		}

		if (publishedObjects.contains(object)) {
			// already published the object, don't do it again
			return;
		}
		publishedObjects.add(object);

		int objType = ObjectTransformer.getInt(object.getTType(), -1);

		// determine the node in which the object is published
		Page page = null;
		Folder folder = null;
		ContentFile file = null;
		Form form = null;

		switch (objType) {
		case Page.TYPE_PAGE:
			if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS) || Events.isEvent(eventMask, Events.MOVE) || Events.isEvent(eventMask, Events.DELETE)) {
				page = (Page) object;
				// check whether the template's markup language is excluded from publishing
				if (!Events.isEvent(eventMask, Events.DELETE)
						&& page.getTemplate().getMarkupLanguage().isExcludeFromPublishing()) {
					return;
				}
				if (node == null) {
					node = page.getChannel();
				}
				if (node == null) {
					node = page.getFolder().getNode();
				}
			} else {
				return;
			}
			break;

		case Folder.TYPE_FOLDER:
			if (Events.isEvent(eventMask, Events.CREATE) || Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.DELETE)
					|| Events.isEvent(eventMask, Events.MOVE)) {
				folder = (Folder) object;
				if (node == null) {
					node = folder.getChannel();
				}
				if (node == null) {
					node = folder.getNode();
				}
			} else {
				return;
			}
			break;

		case ImageFile.TYPE_FILE:
		case ImageFile.TYPE_IMAGE:
			if (Events.isEvent(eventMask, Events.CREATE) || Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.DELETE)
					|| Events.isEvent(eventMask, Events.MOVE)) {
				file = (ContentFile) object;
				if (node == null) {
					node = file.getChannel();
				}
				if (node == null) {
					node = file.getFolder().getNode();
				}
			} else {
				return;
			}
			break;
		case Form.TYPE_FORM:
			if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS) || Events.isEvent(eventMask, Events.MOVE) || Events.isEvent(eventMask, Events.DELETE)) {
				form = (Form) object;
				if (node == null) {
					node = form.getOwningNode();
				}
			} else {
				return;
			}
			break;

		default:
			// this is not an object, which is published into the contentrepository (as object)
			return;
		}

		// the node does not publish into the contentmap or has publishing disabled, so do nothing
		if (!node.doPublishContentmap() || node.isPublishDisabled()) {
			return;
		}
		switch (objType) {
		case Page.TYPE_PAGE:
			if (!node.doPublishContentMapPages()) {
				return;
			}
			break;
		case Folder.TYPE_FOLDER:
			if (!node.doPublishContentMapFolders()) {
				return;
			}
			break;
		case ImageFile.TYPE_FILE:
		case ImageFile.TYPE_IMAGE:
			if (!node.doPublishContentMapFiles()) {
				return;
			}
			break;
		case Form.TYPE_FORM:
			break;
		default:
			return;
		}

		ContentRepository cr = node.getContentRepository();

		// no cr found or the cr has instant publishing disabled
		if (cr == null || !cr.isInstantPublishing() || InstantCRPublishing.isTemporarilyDisabled(cr)) {
			return;
		}

		ContentMap contentMap = cr.getContentMap();
		boolean meshCr = cr.getCrType() == Type.mesh;
		boolean mccr = meshCr ? cr.isProjectPerNode() : contentMap.isMultichannelling();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		final NodeObject finalObject = object;
		final Node finalNode = node;

		final List<Dependency> dependencies = meshCr ? new ArrayList<>() : null;

		// when the published object is a file, check whether it is offline
		if (file != null && !Events.isEvent(eventMask, Events.DELETE)) {
			if (!FileOnlineStatus.isOnline(file, node)) {
				// if the file is currently offline, but the feature "contentfile_auto_offline" is not activated for the node, or the files is forced online, we set the
				// file online now
				if (file.isForceOnline() || !prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE, node)) {
					// when setting a file online in instant publishing, it is ok to do this in the same transaction, since
					// the transaction is short lived. longer running transactions must not do instant publishing
					FileOnlineStatus.setOnlineStatus(file.getMaster().getId(), node.getId(), true);
				} else {
					// the file is offline, so don't do instant publishing
					if (log.isDebugEnabled()) {
						log.debug("Omit instant publishing of " + file + ". File is currently offline");
					}
					publishedObjects.remove(file);
					return;
				}
			}
		}

		File dbFilesDir = new File(ConfigurationValue.DBFILES_PATH.get());

		// set the rendertype
		RenderType myRenderType = RenderType.getDefaultRenderType(prefs, RenderType.EM_PUBLISH, null, -1);

		myRenderType.setRenderUrlFactory(
				new StaticUrlFactory(RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway")),
				RenderType.parseLinkWay(prefs.getProperty("contentnode.linkway_file")), prefs.getProperty("contentnode.linkway_file_path")));
		if (prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE)) {
			// when the feature "autooffline" is generally on, we need to collect dependencies (but not store them).
			// when publishing a page or folder into the contentrepository that has a dependency on a file, which is
			// currently offline, we will need to set the file online and also publish it into the contentrepository.
			myRenderType.setHandleDependencies(true);
			myRenderType.setStoreDependencies(false);
		} else {
			myRenderType.setHandleDependencies(false);
		}
		RenderType oldRenderType = t.getRenderType();
		t.setRenderType(myRenderType);

		try {
			boolean needToRevealInherited = false;
			List<Operator> operations = new ArrayList<>();
			List<Consumer<MeshPublisher>> meshOperations = new ArrayList<>();
			int nodeId = node.getId();

			switch (objType) {
			case Page.TYPE_PAGE:
			{
				boolean delete = false;
				boolean offline = false;
				boolean render = false;

				if (Events.isEvent(eventMask, Events.DELETE)) {
					delete = true;
				} else if (Events.isEvent(eventMask, Events.MOVE)) {
					if (page.isOnline()) {
						render = true;
					}
				} else {
					if (page.isOnline()) {
						render = true;
					} else {
						offline = true;
					}
				}
				if ((delete || offline) && !node.getFeatures().contains(Feature.DISABLE_INSTANT_DELETE)) {
					// immediately remove the object from the contentmap
					if (meshCr) {
						meshOperations.add(mp -> {
							mp.remove(mp.getProject(finalNode), finalNode, objType, MeshPublisher.getMeshUuid(finalObject), MeshPublisher.getMeshLanguage(finalObject));
						});
					} else {
						operations.add(CnMapPublisher.removeObjectFromCR(object, contentMap, node));
					}
					if (delete) {
						needToRevealInherited = true;
					}
				}
				if (render) {
					// render the page with all tagmap entries

					if (node.isChannel()) {
						t.setChannelId(node.getId());
					}
					try {
						if (meshCr) {
							meshOperations.add(mp -> {
								mp.processQueue(Collections.singleton(Scheduled.from(nodeId, new NodeObjectWithAttributes<>(finalObject))), finalNode, null, dependencies);
							});
						} else {
							// prepare a map which will hold all rendered tagmap entries
							Map<TagmapEntryRenderer, Object> attributes = new HashMap<>();
							List<TagmapEntryRenderer> tagmapEntries = contentMap.getTagmapEntries(Page.TYPE_PAGE);

							for (TagmapEntryRenderer entry : tagmapEntries) {
								attributes.put(entry, null);
							}

							RenderResult renderResult = t.getRenderResult();
							PageRenderResult pageRenderResult = new PageRenderResult(renderResult);

							t.setRenderResult(pageRenderResult);
							pageRenderResult.setAllowRepublish(false);
							String source = page.render(pageRenderResult, attributes, CnMapPublisher.LINKTRANSFORMER);

							// immediately write the page into the contentmap
							operations.add(CnMapPublisher.writePageIntoCR(page, contentMap, attributes, source, null, false));
						}
					} finally {
						if (node.isChannel()) {
							t.resetChannel();
						}
					}

					// when publishing for a channel, we get all hidden pages and remove them from the CR
					if (node.isChannel() && !cr.isMultichannelling()) {
						Collection<Integer> hiddenPageIds = page.getHiddenPageIds().values();

						for (Integer hiddenPageId : hiddenPageIds) {
							if (meshCr) {
								meshOperations.add(mp -> {
									Page toRemove = t.getObject(Page.class, hiddenPageId, false, false, true);
									mp.remove(mp.getProject(finalNode), finalNode, Page.TYPE_PAGE, MeshPublisher.getMeshUuid(toRemove), MeshPublisher.getMeshLanguage(toRemove));
								});
							} else {
								operations.add(CnMapPublisher.removeObjectFromCR(t.getObject(Page.class, hiddenPageId, false, false, true), contentMap, node));
							}
						}
					}
				}
				break;
			}

			case Folder.TYPE_FOLDER:
			case ImageFile.TYPE_FILE:
			case ImageFile.TYPE_IMAGE:
				if ((Events.isEvent(eventMask, Events.CREATE) || Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.MOVE))
						&& !Events.isEvent(eventMask, Events.CHILD)) {
					// TODO determine whether the binary content for files needs
					// to be written

					if (node.isChannel()) {
						t.setChannelId(node.getId());
					}
					try {
						if (Events.isEvent(eventMask, Events.MOVE)) {
							if (meshCr) {
								// TODO
							} else {
								// Previous node recorded in property[0]
								operations.add(CnMapPublisher.removeObjectFromCR(object, contentMap, t.getObject(Node.class, Integer.valueOf(property[0]))));
							}
						}
						if (meshCr) {
							meshOperations.add(mp -> {
								mp.processQueue(Collections.singleton(Scheduled.from(nodeId, new NodeObjectWithAttributes(finalObject))), finalNode, null, dependencies);
							});
						} else {
							// immediately write the object into the contentmap
							operations.add(CnMapPublisher.writePreparedObject(node.getId(), CnMapPublisher.prepareObjectForWriting(object, null, contentMap, dbFilesDir, true, false),
									contentMap, object.toString(), true));
						}
					} finally {
						if (node.isChannel()) {
							t.resetChannel();
						}
					}

					// when publishing for a channel, we remove hidden objects now
					if (node.isChannel() && object instanceof LocalizableNodeObject<?> && !mccr) {
						LocalizableNodeObject<NodeObject> locObj = (LocalizableNodeObject<NodeObject>) object;
						Map<Integer, Integer> channelSet = locObj.getChannelSet();

						for (Integer id : channelSet.values()) {
							if (ObjectTransformer.getInt(id, 0) == ObjectTransformer.getInt(object.getId(), 0)) {
								continue;
							}
							if (meshCr) {
								meshOperations.add(mp -> {
									mp.remove(mp.getProject(finalNode), finalNode, objType, MeshPublisher.getMeshUuid(finalObject), MeshPublisher.getMeshLanguage(finalObject));
								});
							} else {
								operations.add(CnMapPublisher.removeObjectFromCR(t.getObject(object.getObjectInfo().getObjectClass(), id, false, false, true),
										contentMap, node));
							}
						}
					}

				} else if (Events.isEvent(eventMask, Events.DELETE) && !node.getFeatures().contains(Feature.DISABLE_INSTANT_DELETE) ) {
					// immediately remove the object from the contentmap
					if (meshCr) {
						meshOperations.add(mp -> {
							mp.remove(mp.getProject(finalNode), finalNode, objType, MeshPublisher.getMeshUuid(finalObject), MeshPublisher.getMeshLanguage(finalObject));
						});
					} else {
						operations.add(CnMapPublisher.removeObjectFromCR(object, contentMap, node));
					}
					needToRevealInherited = true;
				}
				break;

			case Form.TYPE_FORM:
				if (meshCr) {
					boolean delete = false;
					boolean offline = false;
					boolean render = false;
					Form finalForm = form;

					if (Events.isEvent(eventMask, Events.DELETE)) {
						delete = true;
					} else if (Events.isEvent(eventMask, Events.MOVE)) {
						if (form.isOnline()) {
							render = true;
						}
					} else {
						if (form.isOnline()) {
							render = true;
						} else {
							offline = true;
						}
					}
					if (delete && !node.getFeatures().contains(Feature.DISABLE_INSTANT_DELETE)) {
						meshOperations.add(mp -> {
							mp.remove(mp.getProject(finalNode), finalNode, objType, MeshPublisher.getMeshUuid(finalObject), null);
						});
					} else if (offline && !node.getFeatures().contains(Feature.DISABLE_INSTANT_DELETE)) {
						meshOperations.add(mp -> {
							for (String language : finalForm.getLanguages()) {
								mp.offline(mp.getProject(finalNode), null, objType, MeshPublisher.getMeshUuid(finalObject), language);
							}
						});
					}
					if (render) {
						meshOperations.add(mp -> {
							mp.processQueue(Collections.singleton(Scheduled.from(nodeId, new NodeObjectWithAttributes<>(finalObject))), finalNode, null, dependencies);
						});
					}
				}
				break;
			default:
				break;
			}

			if (!operations.isEmpty()) {
				try (ContentMapTrx trx = contentMap.startInstantPublishingTrx()) {
					for (Operator op : operations) {
						op.operate();
					}

					// We use wallclock time so that instant publishing doesn't break
					// cache consistency during a publish run
					int timestamp = (int) (System.currentTimeMillis() / 1000);
					contentMap.setLastMapUpdate(timestamp, Collections.singleton(node), false);
					PublishController.instantPublished(object);
					InstantCRPublishing.resetErrorCount(contentMap);
					trx.setSuccess();
				} catch (SQLException e) {
					InstantCRPublishing.increaseErrorCount(contentMap);
					throw new NodeException("Error while committing transaction to write {" + object + "} into {" + contentMap + "}", e);
				} catch (NodeException e) {
					InstantCRPublishing.increaseErrorCount(contentMap);
					throw e;
				} catch (RuntimeException e) {
					InstantCRPublishing.increaseErrorCount(contentMap);
					throw e;
				}
			}
			if (!meshOperations.isEmpty()) {
				try (MeshPublishController meshPublishController = MeshPublishController.get(cr)) {
					for (MeshPublisher mp : meshPublishController.get()) {
						if (mp.checkStatus() && mp.checkSchemasAndProjects(false, false)) {
							for (Consumer consumer : meshOperations) {
								consumer.accept(mp);
							}
						} else {
							log.error(String.format("Could not instantly publish into %s, which is not valid", cr));
						}
					}
					PublishController.instantPublished(object);
					meshPublishController.success();
				}
			}

			if (page != null && objType == Page.TYPE_PAGE) {
				InstantPublisher.handleStartPageOfParentFolder(node, page, contentMap);
			}

			// now we probably need to check for offline files that were needed
			if ((objType == Page.TYPE_PAGE || objType == Folder.TYPE_FOLDER) && prefs.isFeature(Feature.CONTENTFILE_AUTO_OFFLINE)) {
				List<NodeObject> offlineObjects = null;
				if (dependencies != null) {
					offlineObjects = InstantPublisher.handleOfflineDependencies(cr, node, dependencies);
				} else {
					offlineObjects = InstantPublisher.handleOfflineDependencies(cr, node, myRenderType.getDependencies());
				}

				for (NodeObject offline : offlineObjects) {
					if (node.isChannel()) {
						handleInstantPublishing(offline, Events.UPDATE, node, property);
					} else {
						handleInstantPublishing(offline, Events.UPDATE, null, property);
					}
				}
			}
			myRenderType.resetDependencies();

			// when we deleted an object from a channel, we need to make the inherited object visible again
			if (needToRevealInherited && node.isChannel() && object instanceof LocalizableNodeObject<?>) {
				LocalizableNodeObject<NodeObject> locObj = (LocalizableNodeObject<NodeObject>) object;

				if (!locObj.isMaster()) {
					InstantPublisher.unhideObjectForInstantPublishing(locObj, node);
				}
			}
		} finally {
			t.setRenderType(oldRenderType);
		}
	}

	/**
	 * Write the startpage of the trigger pages startfolder into the cr
	 * 
	 * @param node
	 *            The parent node of the trigger page
	 * @param triggerPage
	 *            The page which should be examined to determine the startpage of the parent folder
	 * @param contentMap
	 *            The contentmap that should be used for writing the page into
	 * @throws NodeException
	 */
	protected static void handleStartPageOfParentFolder(Node node, Page triggerPage, ContentMap contentMap) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// Check whether the startpage of the parent folder should be published as well
		if (prefs.isFeature(Feature.PUBLISH_FOLDER_STARTPAGE, node)) {
			Folder parentFolder = triggerPage.getFolder();
			Page startPageOfParentFolder = parentFolder.getStartpage();

			if (startPageOfParentFolder == null) {
				return;
			}

			// Get all language variants of this page and publish them
			// Since this page is one of the language variants, we don't have to extra-publish it.
			for (Page currentLanguageVariant : startPageOfParentFolder.getLanguageVariants(false)) {
				if (currentLanguageVariant != null) {
					handleInstantPublishing(currentLanguageVariant, Events.EVENT_CN_PAGESTATUS, node, null);
				}
			}
		}
	}

	/**
	 * Handle dependencies on offline objects, which should also be published into the contentrepository
	 * @param cr contentrepository
	 * @param node for which the dependencies were calculated
	 * @param dependencies list of dependencies
	 * @return list of objects, that were offline, but need to be set online, because the current object depends on it
	 * @throws NodeException
	 */
	protected static List<NodeObject> handleOfflineDependencies(ContentRepository cr, Node node, List<Dependency> dependencies) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<NodeObject> objects = new Vector<NodeObject>();
		List<NodeObject> checked = new Vector<NodeObject>();
		List<Node> crNodes = cr.getNodes();

		for (Dependency dependency : dependencies) {
			NodeObject sourceObject = dependency.getSource().getObject();

			if (sourceObject instanceof com.gentics.contentnode.object.File) {
				com.gentics.contentnode.object.File file = (com.gentics.contentnode.object.File) sourceObject;

				// omit files, that should not be published into the contentrepository
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					if (!crNodes.contains(file.getNode())) {
						continue;
					}
				}

				Node channel = null;

				if (node.isChannel()) {
					file = file.getChannelVariant(node);
					channel = node;
				}

				if (!checked.contains(file)) {
					if (channel != null) {
						if (!FileOnlineStatus.isOnline(file, channel)) {
							FileOnlineStatus.setOnline(file, channel, true);
							objects.add(file);
						}
					} else {
						if (!FileOnlineStatus.isOnline(file)) {
							FileOnlineStatus.setOnline(file, true);
							objects.add(file);
						}
					}
					checked.add(file);
				}
			}
		}

		return objects;
	}

	/**
	 * Reveal the object, that was hidden by a localized copy
	 * @param object localized copy, that has been removed now
	 * @param node node
	 * @throws NodeException
	 */
	protected static void unhideObjectForInstantPublishing(LocalizableNodeObject<NodeObject> object, Node node) throws NodeException {
		LocalizableNodeObject<NodeObject> nextHigherObject = object.getNextHigherObject();

		handleInstantPublishing(nextHigherObject,
				ObjectTransformer.getInt(object.getObject().getTType(), 0) == Page.TYPE_PAGE ? Events.EVENT_CN_PAGESTATUS : Events.UPDATE, node, null);
	}
}
