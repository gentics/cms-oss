package com.gentics.contentnode.events;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.ContentNodeFactory.WithTransaction;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.TransactionManager.Executable;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.log.ActionLogger.Log;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueueMigration;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.DirtQueueEntry;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Class representing a queue entry
 */
public class QueueEntry {
	/**
	 * Log
	 */
	private final static NodeLogger log = NodeLogger.getNodeLogger(QueueEntry.class);

	/**
	 * Transform the queue entry into its REST Model
	 */
	public final static Function<QueueEntry, DirtQueueEntry> TRANSFORM2REST = entry -> {
		return new DirtQueueEntry()
			.setId(entry.id)
			.setObjType(entry.objType)
			.setObjId(entry.objId)
			.setTimestamp(entry.timestamp)
			.setLabel(entry.getType().getLabel())
			.setFailed(entry.failed)
			.setFailReason(entry.failReason);
	};

	/**
	 * constant for events not triggered for simulation mode
	 */
	public static final int SIMULATIONMODE_OFF = 0;

	/**
	 * constant for events triggered in simulation mode (not the last event)
	 */
	public static final int SIMULATIONMODE_INTERMEDIATE = 1;

	/**
	 * constant for the last event triggered in simulation mode
	 */
	public static final int SIMULATIONMODE_FINAL = 2;

	/**
	 * Maximum number of error details printed for contentrepository data checks
	 */
	private static final int NUM_ERROR_DETAILS = 100;

	protected int id;

	protected int timestamp;

	protected int objType;

	protected int objId;

	protected int eventMask;

	protected String[] property;

	protected int simulation = SIMULATIONMODE_OFF;

	protected String sid;

	protected boolean failed;

	protected String failReason;

	/**
	 * Get the oldest queue entry
	 * @return oldest queue entry or null if no queue entry present
	 * @throws NodeException
	 */
	public static QueueEntry getOldestQueueEntry() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT * FROM dirtqueue WHERE failed = ? ORDER BY id ASC");
			pst.setInt(1, 0);

			res = pst.executeQuery();

			if (res.next()) {
				// we found the oldest record, check it
				return new QueueEntry(res);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new NodeException("Error while getting oldest queue entry", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Get number queue entries older than the given entry
	 * @param entry queue entry
	 * @return number of older queue entries
	 * @throws NodeException
	 */
	public static int getNumberOfOlderQueueEntries(QueueEntry entry) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT count(*) c FROM dirtqueue WHERE id < ? AND failed = ?");
			pst.setInt(1, entry.id);
			pst.setInt(2, 0);

			res = pst.executeQuery();

			if (res.next()) {
				return res.getInt("c");
			} else {
				return 0;
			}
		} catch (SQLException e) {
			throw new NodeException("Error while counting queue entries", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Create an instance of the queue entry based on data given in the
	 * resultset
	 * @param res result set
	 * @throws SQLException
	 */
	public QueueEntry(ResultSet res) throws SQLException {
		id = res.getInt("id");
		timestamp = res.getInt("timestamp");
		objType = res.getInt("obj_type");
		if (res.wasNull()) {
			objType = -1;
		}
		objId = res.getInt("obj_id");
		if (res.wasNull()) {
			objId = -1;
		}
		eventMask = res.getInt("events");
		if (res.wasNull()) {
			eventMask = -1;
		}

		String propertyList = res.getString("property");

		if (!StringUtils.isEmpty(propertyList)) {
			property = propertyList.split(",");
		} else {
			property = null;
		}

		/* Updating the Page status changes the property online
		 * therefore the mask must contain Events.UPDATE. */
		if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS) && "online".equals(propertyList)) {
			eventMask |= Events.UPDATE;
		}

		simulation = res.getInt("simulation");
		sid = res.getString("sid");

		failed = res.getBoolean("failed");
		failReason = res.getString("failreason");
	}

	/**
	 * Create new publish entry
	 * @param timestamp timestamp
	 */
	public QueueEntry(int timestamp) {
		id = -1;
		objId = -1;
		objType = -1;
		eventMask = -1;
		property = new String[] { "publish"};
		this.timestamp = timestamp;
	}

	/**
	 * Create new queue entry
	 * @param timestamp timestamp
	 * @param objId obj_id
	 * @param objType obj_type
	 * @param eventMask eventmask
	 * @param property properties
	 * @param simulation true when the event is a simulation, false for real events
	 * @param sid of the user who triggered this event
	 */
	public QueueEntry(int timestamp, int objId, int objType, int eventMask,
			String[] property, int simulation, String sid) {
		this.id = -1;
		this.timestamp = timestamp;
		this.objId = objId;
		this.objType = objType;
		this.eventMask = eventMask;
		this.property = property;
		this.simulation = simulation;
		this.sid = sid;
	}

	/**
	 * Create a new queue entry
	 * @param timestamp timestamp
	 * @param object nodeobject
	 * @param eventMask eventmask
	 * @param property properties
	 * @param simulation true for simulated events, false for "real" events
	 * @param sid of the user who triggered this event
	 */
	public QueueEntry(int timestamp, NodeObject object, int eventMask, String[] property, int simulation, String sid) {
		this(timestamp, ObjectTransformer.getInt(object.getId(), -1), object.getFactory().getTType(object.getObjectInfo().getObjectClass()), eventMask,
				property, simulation, sid);
	}

	/**
	 * Check whether the queue entry is the publish process
	 * @return true for the publish process, false for anything else
	 */
	public boolean isPublish() {
		return objType == -1 && objId == -1 && property != null && property.length == 1 && "publish".equals(property[0]);
	}

	/**
	 * Check whether the queue entry is a blocker
	 * @return true for a blocker, false for anything else
	 */
	public boolean isBlocker() {
		return objType == -1 && objId == -1 && property != null && property.length == 1 && "blocker".equals(property[0]);
	}

	/**
	 * Check whether the queue entry is a maintenance action (republish
	 * pages, files/images, folders)
	 * @return true for a maintenance action, false for another queue entry
	 */
	public boolean isMaintenanceAction() {
		return Events.isEvent(eventMask, Events.MAINTENANCE_PUBLISH) || Events.isEvent(eventMask, Events.MAINTENANCE_DELAY)
				|| Events.isEvent(eventMask, Events.MAINTENANCE_REPUBLISH) || Events.isEvent(eventMask, Events.MAINTENANCE_MARKPUBLISHED)
				|| Events.isEvent(eventMask, Events.MAINTENANCE_MIGRATE2PUBLISHQUEUE);
	}

	/**
	 * Check whether the queue entry is a content repository action (clean)
	 * @return true for a contentrepository action, false for another queue entry
	 */
	public boolean isContentRepositoryAction() {
		return Events.isEvent(eventMask, Events.DATACHECK_CR);
	}

	/**
	 * Check whether the queue entry shall directly be written into the publish queue
	 * @return true if the queue entry is directly written into the publish queue, false if not
	 */
	public boolean isHideOrReveal() {
		return Events.isEvent(eventMask, Events.HIDE) || Events.isEvent(eventMask, Events.REVEAL);
	}

	/**
	 * Check whether the queue entry is a logging event
	 * @return true for logging event
	 */
	public boolean isLoggingEvent() {
		return Events.isEvent(eventMask, Events.LOGGING_START) || Events.isEvent(eventMask, Events.LOGGING_END);
	}

	/**
	 * Handle a maintenance action
	 * @throws NodeException
	 */
	protected void handleMaintenanceAction() throws NodeException {
		// this will be the node ids (or null for all nodes)
		int[] nodeId = null;
		String rangeProperty = null;
		int rangeStart = -1;
		int rangeEnd = -1;
		String[] attributes = null;
		PublishQueue.Action dirtAction = PublishQueue.Action.DEPENDENCY;

		// read and interpret the given properties
		if (property != null) {
			// first property would be the node ids (or "all" for all nodes)
			if (property.length >= 1) {
				if (property[0].indexOf('|') > 0) {
					String[] nodes = property[0].split("\\|");

					nodeId = new int[nodes.length];
					for (int i = 0; i < nodes.length; i++) {
						nodeId[i] = ObjectTransformer.getInt(nodes[i], -1);
						if (nodeId[i] == -1) {
							// when -1 was found, no node restriction will be used
							nodeId = null;
							break;
						}
					}
					if (nodeId != null && nodeId.length == 0) {
						nodeId = null;
					}
				} else {
					nodeId = new int[] { ObjectTransformer.getInt(property[0], -1)};
					if (nodeId[0] == -1) {
						// when -1 was found, no node restriction will be used
						nodeId = null;
					}
				}
			}
			// second property would be the range property
			if (property.length >= 2) {
				rangeProperty = property[1];
				if ("-".equals(rangeProperty)) {
					rangeProperty = null;
				}
			}
			// third property would be the rangestart timestamp
			if (property.length >= 3) {
				rangeStart = ObjectTransformer.getInt(property[2], -1);
			}
			// fourth property would be the rangeend timestamp
			if (property.length >= 4) {
				rangeEnd = ObjectTransformer.getInt(property[3], -1);
			}

			// fifth property would be the dirt mode
			if (property.length >= 5) {
				if ("modify".equals(property[4])) {
					dirtAction = PublishQueue.Action.MODIFY;
				}
			}

			// sixth property would be the attributes to dirt
			if (property.length >= 6) {
				attributes = StringUtils.splitString(property[5], '|');
			}
		}

		if (log.isDebugEnabled()) {
			String rangeRestriction = null;

			if (StringUtils.isEmpty(rangeProperty)) {
				rangeRestriction = "without range restriction";
			} else {
				rangeRestriction = "range restricted by property {" + rangeProperty + "} to {" + rangeStart + "}-{" + rangeEnd + "}";
			}
			String action = null;

			if (Events.isEvent(eventMask, Events.MAINTENANCE_PUBLISH)) {
				action = "publish";
			} else if (Events.isEvent(eventMask, Events.MAINTENANCE_DELAY)) {
				action = "delay";
			} else if (Events.isEvent(eventMask, Events.MAINTENANCE_REPUBLISH)) {
				action = "republish delayed";
			} else if (Events.isEvent(eventMask, Events.MAINTENANCE_MARKPUBLISHED)) {
				action = "mark as published";
			} else if (Events.isEvent(eventMask, Events.MAINTENANCE_MIGRATE2PUBLISHQUEUE)) {
				action = "migrate dirted objects to publishqueue";
			}
			log.debug(
					"About to " + action + " objects of type {" + objType + "} for " + (nodeId != null ? "nodes {" + nodeId + "} " : "all nodes ")
					+ rangeRestriction);
		}

		if (Events.isEvent(eventMask, Events.MAINTENANCE_PUBLISH)) {
			// publish all pages/files/folders
			switch (objType) {
			case Page.TYPE_PAGE:
				PublishQueue.dirtPublishedPages(nodeId, rangeProperty, rangeStart, rangeEnd, dirtAction, attributes);
				break;

			case ContentFile.TYPE_FILE:
			case ContentFile.TYPE_IMAGE:
				PublishQueue.dirtImagesAndFiles(nodeId, rangeProperty, rangeStart, rangeEnd, dirtAction, attributes);
				break;

			case Folder.TYPE_FOLDER:
				PublishQueue.dirtFolders(nodeId, rangeProperty, rangeStart, rangeEnd, dirtAction, attributes);
				break;

			case Form.TYPE_FORM:
				PublishQueue.dirtForms(nodeId, rangeProperty, rangeStart, rangeEnd, dirtAction, attributes);
				break;

			default:
				break;
			}
		} else if (Events.isEvent(eventMask, Events.MAINTENANCE_DELAY)) {
			if (objType == ContentFile.TYPE_IMAGE) {
				objType = ContentFile.TYPE_FILE;
			}
			PublishQueue.delayDirtedObjects(nodeId, true, objType, rangeProperty, rangeStart, rangeEnd);
		} else if (Events.isEvent(eventMask, Events.MAINTENANCE_REPUBLISH)) {
			if (objType == ContentFile.TYPE_IMAGE) {
				objType = ContentFile.TYPE_FILE;
			}
			PublishQueue.delayDirtedObjects(nodeId, false, objType, rangeProperty, rangeStart, rangeEnd);
		} else if (Events.isEvent(eventMask, Events.MAINTENANCE_MARKPUBLISHED)) {
			if (objType == ContentFile.TYPE_IMAGE) {
				objType = ContentFile.TYPE_FILE;
			}
			PublishQueue.undirtObjects(nodeId, objType, rangeProperty, rangeStart, rangeEnd);
		} else if (Events.isEvent(eventMask, Events.MAINTENANCE_MIGRATE2PUBLISHQUEUE)) {
			long start = System.currentTimeMillis();

			PublishQueueMigration.migrateDirtedObjects();
			long duration = System.currentTimeMillis() - start;

			ActionLogger.logCmd(ActionLogger.MAINTENANCE, 0, 0, 0, "Migrated dirted objects (" + duration + " + ms)");
		}
	}

	/**
	 * Handle the contentrepository action
	 * @throws NodeException
	 */
	protected void handleContentRepositoryAction() throws NodeException {
		if (Events.isEvent(eventMask, Events.DATACHECK_CR)) {
			// contentrepository id
			int contentRepositoryId = -1;
			boolean doClean = false;

			if (property != null && property.length >= 1) {
				contentRepositoryId = ObjectTransformer.getInt(property[0], contentRepositoryId);
			}
			if (property != null && property.length >= 2) {
				doClean = ObjectTransformer.getBoolean(property[1], doClean);
			}

			boolean crIsClean = true;
			StringBuffer cleanResult = new StringBuffer();

			if (contentRepositoryId > 0) {
				// read all objects in the contentrepository, check whether they are still published into this contentrepository and if not, delete them
				Transaction t = TransactionManager.getCurrentTransaction();
				ContentRepository cr = t.getObject(ContentRepository.class, contentRepositoryId);
				if (cr != null) {
					try {
						switch (cr.getCrType()) {
						case cr:
						case mccr:
						ContentMap toClean = cr.getContentMap();

						if (toClean != null) {
							cr.getNodes().forEach(n -> toClean.addNode(n));

							crIsClean = cleanCR(toClean, doClean, cleanResult);

							// finally store the results in the DB
							DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ?, datacheckresult = ? WHERE id = ?",
									new Object[] { crIsClean ? ContentRepository.DATACHECK_STATUS_OK : ContentRepository.DATACHECK_STATUS_ERROR, cleanResult.toString(), contentRepositoryId });
						} else {
							DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ?, datacheckresult = ? WHERE id = ?",
									new Object[] { ContentRepository.DATACHECK_STATUS_OK, "nothing to check", contentRepositoryId });
						}
							break;
						case mesh:
							try (MeshPublisher mp = new MeshPublisher(cr)) {
								StringBuilder result = new StringBuilder();
								crIsClean = mp.checkDataConsistency(doClean, result);
								DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ?, datacheckresult = ? WHERE id = ?",
										new Object[] { crIsClean ? ContentRepository.DATACHECK_STATUS_OK : ContentRepository.DATACHECK_STATUS_ERROR, result.toString(), contentRepositoryId });
							}
							break;
						}
					} catch (Exception e) {
						log.error("Error while checking/cleaning data in contentrepository " + contentRepositoryId, e);
						StringWriter writer = new StringWriter();

						e.printStackTrace(new PrintWriter(writer));
						DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ?, datacheckresult = ? WHERE id = ?",
								new Object[] { ContentRepository.DATACHECK_STATUS_ERROR, writer.toString(), contentRepositoryId });
					} finally {
						t.dirtObjectCache(ContentRepository.class, contentRepositoryId);
					}
				} else {
					throw new NodeException(String.format("Cannot check data of unknown contentrepository %d", contentRepositoryId));
				}
			}
		}
	}

	/**
	 * Directly write an entry to the publish queue
	 * @param obj object to trigger the dependency on
	 * @throws NodeException
	 */
	protected void handleHideOrReveal(NodeObject obj) throws NodeException {
		int channelId = 0;
		if (!ObjectTransformer.isEmpty(property)) {
			channelId = ObjectTransformer.getInt(property[0], channelId);
		}

		DependencyObject depObj = new DependencyObject(obj);
		obj.triggerEvent(depObj, null, eventMask, 0, channelId);
	}

	/**
	 * Check the CR for objects, that should not be published into it
	 * If the CR is a MCCR, this method will iterate through all channels
	 * @param toClean contentmap to clean
	 * @param doClean true for also removing the objects, false for only checking
	 * @param cleanResult stringbuffer that will get the clean result
	 * @return true if the cr is clean, false if not
	 * @throws NodeException
	 */
	protected boolean cleanCR(final ContentMap toClean, boolean doClean, StringBuffer cleanResult) throws NodeException {
		// set the status to "Running" (in a new transaction so that this is immediately visible)
		TransactionManager.execute(new Executable() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
			 */
			public void execute() throws NodeException {
				DBUtils.executeUpdate("UPDATE contentrepository SET datastatus = ? WHERE id = ?", new Object[] {ContentRepository.DATACHECK_STATUS_RUNNING, toClean.getId()});
				TransactionManager.getCurrentTransaction().dirtObjectCache(ContentRepository.class, toClean.getId());
			}
		});

		printNodeSummary(toClean, cleanResult);
		if (toClean.isMultichannelling()) {
			WritableMCCRDatasource mccrDatasource = toClean.getMCCRDatasource();
			ChannelTree tree = mccrDatasource.getChannelStructure();

			return cleanCR(toClean, doClean, cleanResult, tree.getChildren());
		} else {
			return doCleanCR(toClean, null, doClean, cleanResult);
		}
	}

	/**
	 * Recursively check the CR for objects, that should not be published into it for the given list of channels
	 * @param toClean contentmap to clean
	 * @param doClean true for also removing the objects, false for only checking
	 * @param cleanResult stringbuffer that will get the clean result
	 * @return true if the cr is clean, false if not
	 * @throws NodeException
	 */
	protected boolean cleanCR(ContentMap toClean, boolean doClean, StringBuffer cleanResult, List<ChannelTreeNode> channels) throws NodeException {
		boolean isCrClean = true;

		for (ChannelTreeNode channel : channels) {
			// print the header for the channel
			cleanResult.append(StringUtils.repeat("=", 40)).append("\nChecking channel ").append(channel.getChannel().getName()).append("\n").append(StringUtils.repeat("=", 40)).append(
					"\n");

			// first check whether the channel still exists as node and is still assigned to the CR
			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = t.getObject(Node.class, channel.getChannel().getId());

			if (node == null) {
				cleanResult.append("Channel does not exist any more. Next publish process will remove it from this CR!\n");
			} else if (!toClean.getNodes().contains(node)) {
				cleanResult.append("Channel is no longer assigned to this CR. Next publish process will remove it from this CR!\n");
			} else {
				toClean.getMCCRDatasource().setChannel(channel.getChannel().getId());
				isCrClean = doCleanCR(toClean, node, doClean, cleanResult) && isCrClean;

				// do the recursion
				isCrClean = cleanCR(toClean, doClean, cleanResult, channel.getChildren()) && isCrClean;
			}

		}
		return isCrClean;
	}

	/**
	 * Print the node summary to the stringbuffer
	 * @param contentMap contentmap
	 * @param cleanResult string buffer
	 * @throws NodeException
	 */
	protected void printNodeSummary(ContentMap contentMap, StringBuffer cleanResult) throws NodeException {
		List<Node> publishedNodes = contentMap.getNodes();

		cleanResult.append(StringUtils.repeat("=", 40)).append("\n");
		cleanResult.append("Contained Nodes\n");
		if (publishedNodes.isEmpty()) {
			cleanResult.append("-- none --\n");
		} else {
			for (Node node : publishedNodes) {
				int lastPublishTS = node.getLastPublishTimestamp();
				int lastMapUpdate = contentMap.getLastMapUpdate(node);
				cleanResult.append(node.getFolder().getName()).append("\t");
				cleanResult.append("published: ");
				if (lastPublishTS > 0) {
					cleanResult.append(new ContentNodeDate(lastPublishTS).getFullFormat());
				} else {
					cleanResult.append("n/a");
				}
				cleanResult.append("\t");
				cleanResult.append("cr: ");
				if (lastMapUpdate > 0) {
					cleanResult.append(new ContentNodeDate(lastMapUpdate).getFullFormat());
				} else {
					cleanResult.append("n/a");
				}
				cleanResult.append("\n");
			}
		}
		cleanResult.append(StringUtils.repeat("=", 40)).append("\n");
	}

	/**
	 * Check the CR for objects, that should not be published into it
	 * If the CR is a MCCR, a channel should already have been selected
	 * @param toClean contentmap to clean
	 * @param node optional node to check for
	 * @param doClean true for also removing the objects, false for only checking
	 * @param cleanResult stringbuffer that will get the clean result
	 * @return true if the cr is clean, false if not
	 * @throws NodeException
	 */
	protected boolean doCleanCR(ContentMap toClean, Node node, boolean doClean, StringBuffer cleanResult) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		WriteableDatasource ds = toClean.getWritableDatasource();
		boolean crIsClean = true;
		int errorDetails = 0;
		List<Node> publishedNodes = toClean.getNodes();

		// check folders
		cleanResult.append("Checking folders\n");
		DatasourceFilter folderFilter = null;
		if (node == null) {
			folderFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10002"));
		} else {
			folderFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10002 AND object.channel_id == node.id"));
			folderFilter.addBaseResolvable("node", node);
		}
		Collection<Resolvable> folders = ds.getResult(folderFilter, null);
		Collection<Resolvable> deletedFolders = new Vector<Resolvable>();

		for (Resolvable folder : folders) {
			Folder cmsFolder = t.getObject(Folder.class, ObjectTransformer.getInteger(folder.get("obj_id"), null));
			if (node == null) {
				if (!toClean.isPublishedIntoCR(cmsFolder)) {
					deletedFolders.add(folder);
				}
			} else {
				if (!toClean.isPublishedIntoCR(cmsFolder, node)) {
					deletedFolders.add(folder);
				}
			}
		}
		// now handle the found folders
		if (!deletedFolders.isEmpty()) {
			if (doClean) {
				ds.delete(deletedFolders);
				cleanResult.append("Deleted ").append(deletedFolders.size()).append(" folders\n");
			} else {
				cleanResult.append("Found ").append(deletedFolders.size()).append(" incorrect folders:\n");
				for (Resolvable resolvable : deletedFolders) {
					cleanResult.append(resolvable.get("obj_id"));
					if (errorDetails < NUM_ERROR_DETAILS) {
						cleanResult.append("\t");
						Folder folder = t.getObject(Folder.class, ObjectTransformer.getInteger(resolvable.get("obj_id"), null));
						if (folder == null) {
							cleanResult.append("deleted");
						} else {
							cleanResult.append("belongs to node ").append(folder.getOwningNode().getFolder().getName());
				}
						errorDetails++;
					}
					cleanResult.append("\n");
				}
				crIsClean = false;
			}
		} else {
			cleanResult.append("No incorrect folders found\n");
		}

		// check pages
		cleanResult.append(StringUtils.repeat("-", 40)).append("\nChecking pages\n");
		DatasourceFilter pageFilter = null;
		if (node == null) {
			pageFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10007"));
		} else {
			pageFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10007 AND object.channel_id == node.id"));
			pageFilter.addBaseResolvable("node", node);
		}
		Collection<Resolvable> pages = ds.getResult(pageFilter, null);
		Collection<Resolvable> deletedPages = new Vector<Resolvable>();

		for (Resolvable page : pages) {
			Page cmsPage = t.getObject(Page.class, ObjectTransformer.getInteger(page.get("obj_id"), null));
			if (node == null) {
				if (!toClean.isPublishedIntoCR(cmsPage)) {
					deletedPages.add(page);
				}
			} else {
				if (!toClean.isPublishedIntoCR(cmsPage, node)) {
					deletedPages.add(page);
				}
			}
		}
		// now handle the found pages
		if (!deletedPages.isEmpty()) {
			if (doClean) {
				ds.delete(deletedPages);
				cleanResult.append("Deleted ").append(deletedPages.size()).append(" pages\n");
			} else {
				cleanResult.append("Found ").append(deletedPages.size()).append(" incorrect pages:\n");
				for (Resolvable resolvable : deletedPages) {
					cleanResult.append(resolvable.get("obj_id"));
					if (errorDetails < NUM_ERROR_DETAILS) {
						cleanResult.append("\t");
						Page page = t.getObject(Page.class, ObjectTransformer.getInteger(resolvable.get("obj_id"), null));
						if (page == null) {
							cleanResult.append("deleted");
						} else if (!page.isOnline()) {
							cleanResult.append("offline");
						} else {
							cleanResult.append("belongs to node ").append(page.getOwningNode().getFolder().getName());
				}
						errorDetails++;
					}
					cleanResult.append("\n");
				}
				crIsClean = false;
			}
		} else {
			cleanResult.append("No incorrect pages found\n");
		}

		// check files
		cleanResult.append(StringUtils.repeat("-", 40)).append("\nChecking files\n");
		DatasourceFilter fileFilter = null;
		if (node == null) {
			fileFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10008"));
		} else {
			fileFilter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == 10008 AND object.channel_id == node.id"));
			fileFilter.addBaseResolvable("node", node);
		}
		Collection<Resolvable> files = ds.getResult(fileFilter, null);
		Collection<Resolvable> deletedFiles = new Vector<Resolvable>();

		for (Resolvable file : files) {
			com.gentics.contentnode.object.File cmsFile = t.getObject(com.gentics.contentnode.object.File.class,
					ObjectTransformer.getInteger(file.get("obj_id"), null));
			if (node == null) {
				if (!toClean.isPublishedIntoCR(cmsFile)) {
					deletedFiles.add(file);
				}
			} else {
				if (!toClean.isPublishedIntoCR(cmsFile, node)) {
					deletedFiles.add(file);
				}
			}
		}
		// now handle the found files
		if (!deletedFiles.isEmpty()) {
			if (doClean) {
				ds.delete(deletedFiles);
				cleanResult.append("Deleted ").append(deletedFiles.size()).append(" files\n");
			} else {
				cleanResult.append("Found ").append(deletedFiles.size()).append(" incorrect files:\n");
				for (Resolvable resolvable : deletedFiles) {
					cleanResult.append(resolvable.get("obj_id"));
					if (errorDetails < NUM_ERROR_DETAILS) {
						cleanResult.append("\t");
						ContentFile file = t.getObject(ContentFile.class, ObjectTransformer.getInteger(resolvable.get("obj_id"), null));
						if (file == null) {
							cleanResult.append("deleted");
						} else {
							boolean isVisible = false;
							boolean isOnline = false;

							for (Node pNode : publishedNodes) {
								// check whether the object is visible in the node
								if (MultichannellingFactory.isVisibleInNode(pNode, file)) {
									isVisible = true;
								}
								// check whether the file is marked online in the node
								if (FileOnlineStatus.isOnline(file, pNode)) {
									isOnline = true;
								}
							}

							if (!isVisible) {
								cleanResult.append("belongs to node ").append(file.getOwningNode().getFolder().getName());
							} else if (!isOnline) {
								cleanResult.append("offline");
							}
						}
						errorDetails++;
					}
					cleanResult.append("\n");
				}
				crIsClean = false;
			}
		} else {
			cleanResult.append("No incorrect files found\n");
		}

		return crIsClean;
	}

	/**
	 * Trigger the event
	 * @return true when the event was triggered, false if not
	 * @throws NodeException
	 */
	public boolean triggerEvent() throws NodeException {
		if (isPublish()) {
			return false;
		} else if (isBlocker()) {
			return false;
		} else if (isMaintenanceAction()) {
			handleMaintenanceAction();
			return true;
		} else if (isContentRepositoryAction()) {
			handleContentRepositoryAction();
			return true;
		} else {
			Transaction t = TransactionManager.getCurrentTransaction();
			NodeLogger depLogger = DependencyManager.getLogger();

			if (depLogger.isInfoEnabled()) {
				depLogger.info("Start dirting for event " + this);
			}

			DependencyManager.initDependencyTriggering(isSimulated());

			// when the event shall be logged, store the result in a temporary file
			PrintWriter out = null;
			// check whether event logging shall be done
			EventLogging eventLogging = EventLogging.getEventLogging();

			try {

				if (eventLogging.isLogEventAnalysis()) {
					try {
						File tmpFile = new File(System.getProperty("java.io.tmpdir"), "tmp_analysis_" + sid + ".xml");

						// when the event is the logging start, we do not append to the tmp file, but start a new one
						out = new PrintWriter(new FileOutputStream(tmpFile, Events.isEvent(eventMask, Events.LOGGING_START) ? false : true));
						DependencyManager.setLoggingWriter(out);

						// when the event is a "LOGGING_START", we write out the action node
						if (Events.isEvent(eventMask, Events.LOGGING_START)) {
							if (objId > 0) {
								Log log = ActionLogger.getLogCmd(objId);

								if (log != null) {
									String obj_cnt = "";

									if (this.property != null) {
										if (this.property.length > 0) {
											int length = ObjectTransformer.getInt(this.property[0], 0);

											if (length > 0) {
												obj_cnt = " obj_cnt=\"" + length + "\" ";
											}
										}
									}
									out.println(
											"<action obj_type=\"" + log.getOType() + "\" obj_id=\"" + log.getOId() + "\" name=\"" + log.getCmdDesc() + "\"" 
											+ obj_cnt + ">");
									eventLogging.startNewAnalysis(sid, log.getOType(), log.getOId(), log.getCmdDesc(), timestamp);
								} else {
									out.println("<action obj_type=\"" + "?" + "\" obj_id=\"" + "?" + "\" name=\"" + "?" + "\">");
									eventLogging.startNewAnalysis(sid, -1, -1, "?", timestamp);
								}
							} else if (property != null && property.length >= 3) {
								// the command information is supposed to be
								// given in properties
								int objType = ObjectTransformer.getInt(property[0], -1);
								int objId = ObjectTransformer.getInt(property[1], -1);
								int cmdDescId = ObjectTransformer.getInt(property[2], -1);
								String objCount = (property.length > 3 ? " obj_cnt=\"" + property[3] + "\" " : "");

								out.println(
										"<action obj_type=\"" + (objType != -1 ? objType + "" : "?") + "\" obj_id=\"" + (objId != -1 ? objId + "" : "?")
										+ "\" name=\"" + ActionLogger.getReadableCmd(cmdDescId) + "\"" + objCount + ">");
								eventLogging.startNewAnalysis(sid, objType, objId, ActionLogger.getReadableCmd(cmdDescId), timestamp);
							}
						}
					} catch (FileNotFoundException e) {
						log.error("", e);
					}
				}

				if (objType > 0) {
					// transform the objecttype into an object class
					Class<? extends NodeObject> objClass = t.getClass(objType);

					if (objId != -1 && objClass != null) {
						Integer objIdInt = new Integer(objId);

						// clear the cache for the dirted object
						t.dirtObjectCache(objClass, objIdInt, false);

						NodeObject obj = t.getObject(objClass, objIdInt);

						if (obj != null) {

							if (isHideOrReveal()) {
								handleHideOrReveal(obj);
							} else {
							DependencyObject depObj = new DependencyObject(obj, (NodeObject) null);

							// delete events don't have any properties (the stored "property" actually is the node ID)
							if (Events.isEvent(eventMask, Events.DELETE)) {
								property = null;
							}

							if (ObjectTransformer.isEmpty(property)) {
								obj.triggerEvent(depObj, null, eventMask, 0, 0);
							} else {
								obj.triggerEvent(depObj, property, eventMask, 0, 0);
							}
							}
						} else {
							// the object was not found (it was deleted from the
							// db)
							// create a dummy object and trigger it (we might
							// want to dirt after an object has been removed)
							obj = new DummyObject(objIdInt, t.createObjectInfo(objClass));

							// delete events should have the node ID as property
							if (Events.isEvent(eventMask, Events.DELETE) && property != null && property.length >= 1) {
								((DummyObject) obj).setNodeId(ObjectTransformer.getInt(property[0], 0));
								property = null;
							}
							DependencyObject depObj = new DependencyObject(obj, (NodeObject) null);

							if (ObjectTransformer.isEmpty(property)) {
								obj.triggerEvent(depObj, null, eventMask, 0, 0);
							} else {
								obj.triggerEvent(depObj, property, eventMask, 0, 0);
							}
						}
					}

					PublishQueue.finishFastDependencyDirting();

					int numDirted = DependencyManager.getDirtCounter().getCount();

					if (depLogger.isInfoEnabled()) {
						depLogger.info("dirted " + numDirted + " objects");
					}

					// add a dirtlog
					if (numDirted > 0) {
						if (!isSimulated()) {
							ActionLogger.log(ActionLogger.DIRT, null, null, new Integer(numDirted), "dirted " + numDirted + " objects");
						}
						// add the number of dirted objects to the analysis
						if (eventLogging.isLogEventAnalysis()) {
							eventLogging.addDirtedObjects(sid, numDirted, timestamp);
						}
					}
				}

			} finally {
				DependencyManager.resetDependencyTriggering(Events.isEvent(eventMask, Events.LOGGING_END));
				if (out != null) {
					out.close();
					// when this was the last event of a simulation, move
					// the file to it's real name
					if (eventLogging.isLogEventAnalysis() && Events.isEvent(eventMask, Events.LOGGING_END)) {
						eventLogging.finalizeAnalysis(sid, timestamp);
					}
				}
			}
			return true;
		}
	}

	/**
	 * Inserts thos QueueEntry as a new row into the dirtqueue table.
	 * 
	 * <p>
	 * Internal function that doesn't perform any synchronization.
	 */
	private void insertIntoDb(ContentNodeFactory factory) throws NodeException {
		factory.withTransaction(
				new WithTransaction() {
			public void withTransaction(Transaction t) throws NodeException {
				PreparedStatement pst = null;
				ResultSet keys = null;

				try {
					pst = t.prepareInsertStatement(
							"INSERT INTO dirtqueue (timestamp, obj_type, obj_id, events, property, simulation, sid) VALUES (?, ?, ?, ?, ?, ?, ?)");
					pst.setInt(1, timestamp);
					if (objType >= 0) {
						pst.setInt(2, objType);
					} else {
						pst.setNull(2, Types.INTEGER);
					}
					if (objId >= 0) {
						pst.setInt(3, objId);
					} else {
						pst.setNull(3, Types.INTEGER);
					}
					if (eventMask >= 0) {
						pst.setInt(4, eventMask);
					} else {
						pst.setNull(4, Types.INTEGER);
					}
					if (property != null && property.length > 0) {
						pst.setString(5, StringUtils.merge(property, ","));
					} else {
						pst.setNull(5, Types.VARCHAR);
					}
					pst.setInt(6, simulation);
					pst.setString(7, sid);
					if (pst.executeUpdate() > 0) {
						keys = pst.getGeneratedKeys();
						if (keys.next()) {
							id = keys.getInt(1);
						}
					}
				} catch (SQLException e) {
					throw new NodeException("Error while inserting queued dirt event", e);
				} finally {
					t.closeResultSet(keys);
					t.closeStatement(pst);
				}
			}
		}, true, false);
	}
    
	/**
	 * Store the queue entry into the database
	 * 
	 * <p>
	 * Only stores this entry into the dirtqueue table if it has not already been stored.
	 * 
	 * <p>
	 * Starts a new transaction and commits the transaction when finished to ensure the
	 * whole transaction is synchronized statically.
	 * 
	 * <p>
	 * The transaction is synchronized statically to avoid the following szenario:
	 * 
	 * <ol>
	 * <li>thread 1: insert of dirtevent is performed
	 * <li>publish thread: publishrun inserts and commits its own entry into the dirtqueue
	 *     and immediately starts the publishrun because it thinks the publishrun QueueEntry
	 *     is the only one in the queue because thread 1 has not yet committed.
	 * <li>thread 1: commit of dirtevent is performed
	 * <li>dirtqueue worker thread: finds the dirtevent inserted by thread 1 and begins
	 *     processing the dirtevent while a publish process is running.
	 * <li>Deadlock occurs because publish thread and dirtqueue worker thread both access the dependencymap2
	 * </ol>
	 * 
	 * @param factory
	 * 		  A ContentNodeFactory that will be used to allocate a new transaction.
	 * @return fluent API
	 * 
	 * @throws NodeException
	 *		  If an Exception occurs when inserting the new row into the DB.
	 */
	public QueueEntry store(ContentNodeFactory factory) throws NodeException {
		// if id is positive this entry has already been stored
		if (id < 0) {
			synchronized (QueueEntry.class) {
				insertIntoDb(factory);
			}
		}
		return this;
	}

	/**
	 * Delete this dirtqueue entry
	 * @throws NodeException
	 */
	public void delete() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;

		try {
			pst = t.prepareDeleteStatement("DELETE FROM dirtqueue WHERE id = ?");
			pst.setInt(1, id);
			pst.execute();
		} catch (SQLException e) {
			throw new NodeException("Error while deleting dirtqueue entry {" + id + "}", e);
		} finally {
			t.closeStatement(pst);
		}
	}

	/**
	 * Set the dirtqueue entry to be failed (with the stacktrace as reason)
	 * @param e exception that was thrown when handling the dirtqueue entry failed
	 * @return fluent API
	 * @throws NodeException
	 */
	public QueueEntry setFailed(Exception e) throws NodeException {
		StringWriter writer = new StringWriter();

		e.printStackTrace(new PrintWriter(writer));
		DBUtils.executeUpdate("UPDATE dirtqueue SET failed = ?, failreason = ? WHERE id = ?", new Object[] { 1, writer.toString(), id});
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (isPublish()) {
			return "publishevent @" + timestamp;
		} else if (isBlocker()) {
			return "blocker @" + timestamp;
		} else {
			return objType + "." + objId + " mask: " + Events.toString(eventMask)
					+ (ObjectTransformer.isEmpty(property) ? "" : " property: " + StringUtils.merge(property, ",")) + " @" + timestamp
					+ (isSimulated() ? " (simulated)" : "");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof QueueEntry) {
			return ((QueueEntry) obj).id == id;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return id;
	}

	/**
	 * get the timestamp from the original backend event
	 * @return timestmap in ms
	 */
	public long getTimestamp() {
		return timestamp * 1000L;
	}

	/**
	 * Check whether this event is simulated
	 * @return true for a simulated event, false for a real event
	 */
	public boolean isSimulated() {
		return simulation == SIMULATIONMODE_FINAL || simulation == SIMULATIONMODE_INTERMEDIATE;
	}

	/**
	 * Check whether the queue entry is failed
	 * @return true for failed
	 */
	public boolean isFailed() {
		return failed;
	}

	/**
	 * Get the entries id
	 * @return id of the entry
	 */
	public int getId() {
		return id;
	}

	/**
	 * Get the queue entry type
	 * @return type
	 */
	public QueueEntryType getType() {
		if (isLoggingEvent()) {
			return QueueEntryType.log;
		} else if (isMaintenanceAction()) {
			return QueueEntryType.maintenance;
		} else if (isContentRepositoryAction()) {
			return QueueEntryType.cr;
		} else if (isPublish()) {
			return QueueEntryType.publish;
		} else {
			return QueueEntryType.dirt;
		}
	}
}