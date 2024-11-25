package com.gentics.contentnode.servlet.queue;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.dbcopy.CopyController;
import com.gentics.contentnode.dbcopy.DBCopyController;
import com.gentics.contentnode.dbcopy.DBObject;
import com.gentics.contentnode.dbcopy.StructureCopy;
import com.gentics.contentnode.dbcopy.Table;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.rest.model.request.NodeCopyRequest;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * InvokerQueueEntry for the nodecopy process.
 */
public class NodeCopyQueueEntry extends AbstractInvokerQueueEntry {

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(NodeCopyQueueEntry.class);

	/**
	 * Copy a node
	 * @param node node to copy
	 * @param request request containing copy data
	 * @return response message
	 * @throws NodeException
	 */
	public static String copy(Node node, NodeCopyRequest request) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences pref = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		Properties copyProps = new Properties();

		copyProps.setProperty("node", Integer.toString(node.getId()));
		copyProps.setProperty("copypage", request.isPages() ? "yes" : "no");
		copyProps.setProperty("copyperm", "yes");
		copyProps.setProperty("copytemplate", request.isTemplates() ? "yes" : "no");
		copyProps.setProperty("copyfile", request.isFiles() ? "yes" : "no");
		copyProps.setProperty("copyworkflow", request.isWorkflows() ? "yes" : "no");
		copyProps.setProperty("dbFileContentInDB", pref.getFeature("contentfile_data_to_db") ? "true" : "false");
		copyProps.setProperty("filepath", new File(ConfigurationValue.DBFILES_PATH.get()).getAbsolutePath());

		StructureCopy copy = null;
		List<Integer> newNodeIds = new Vector<Integer>();

		try {
			PermissionStore permissionStore = PermissionStore.getInstance();

			for (int i = 0; i < request.getCopies(); i++) {
				logger.info("Starting copy node {" + node.getId() + "} (copy #" + i + ")");
				CopyController copyController = new DBCopyController();

				try (InputStream in = NodeConfigRuntimeConfiguration.class.getResourceAsStream("copy_configuration.xml")) {
					copy = new StructureCopy(in, copyController, t, copyProps);
				}
				copy.startCopy();
				Map<StructureCopy.ObjectKey, DBObject> objectStructure = copy.getObjectStructure(false);

				copy.copyStructure(objectStructure, false);
				copy.finishCopy();

				// finally get the id's of the new objects from the
				// roottable
				Table rootTable = copy.getTables().getTable(copy.getTables().getRoottable());

				for (Iterator<DBObject> iter = objectStructure.values().iterator(); iter.hasNext();) {
					DBObject element = iter.next();

					if (element.getSourceTable().equals(rootTable)) {
						Integer newNodeId = ObjectTransformer.getInteger(element.getNewId(), null);

						if (newNodeId != null) {
							newNodeIds.add(newNodeId);
						}
					}

					Table sourceTable = element.getSourceTable();
					// Refresh the permission cache for folders
					if (sourceTable.getName().equals("folder")) {
						permissionStore.refreshObject(Folder.TYPE_FOLDER, ObjectTransformer.getInt(element.getId(), -1));

						if (sourceTable.getId().equals("foldernode")) {
							// If the current folder is a root folder (of a node), refresh the permissions for that node also.
							permissionStore.refreshObject(Node.TYPE_NODE, ObjectTransformer.getInt(element.getId(), -1));
						}
					}
				}

				logger.info("Done copy of node {" + node.getId() + "}");
			}

			// add names of the copies, separated by \n
			List<Node> newNodes = t.getObjects(Node.class, newNodeIds);
			StringBuffer newNodeNames = new StringBuffer();

			for (Node newNode : newNodes) {
				if (newNodeNames.length() > 0) {
					newNodeNames.append("\n");
				}
				newNodeNames.append(newNode.getFolder().getName());
			}

			return I18NHelper.get("nodecopy_done", node.getFolder().getName(), Integer.toString(request.getCopies()), newNodeNames.toString());
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		} finally {
			copy = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.contentnode.servlet.queue.InvokerQueueEntry#getType()
	 */
	public String getType() {
		return "nodecopy";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.contentnode.servlet.queue.InvokerQueueEntry#invoke()
	 */
	public void invoke() {
		try {
			Transaction transaction = TransactionManager.getCurrentTransaction();

			// make sure the backend language is set
			ContentNodeHelper.setLanguageId(ObjectTransformer.getInt(getParameter("language"), -1));

			// read the parameters
			Integer nodeId = ObjectTransformer.getInteger(getIdParameter(), null);
			Node node = transaction.getObject(Node.class, nodeId);
			int numCopies = ObjectTransformer.getInt(getParameter("num"), 1);
			boolean copyPages = ObjectTransformer.getBoolean(getParameter("copyPages"), false);
			boolean copyTemplates = ObjectTransformer.getBoolean(getParameter("copyTemplates"), false);
			boolean copyFiles = ObjectTransformer.getBoolean(getParameter("copyFiles"), false);
			boolean copyWorkflows = ObjectTransformer.getBoolean(getParameter("copyWorkflows"), false);
			int userId = ObjectTransformer.getInt(getParameter("userid"), -1);

			if (node == null) {
				logger.warn("Not starting nodecopy: could not find");

				return;
			}

			logger.info(
					"\nStarting NodeCopy process for node {" + nodeId + "}." + "\nNumber of copies: " + numCopies + "\nCopy Pages: " + copyPages
					+ "\nCopy Templates: " + copyTemplates + "\nCopy Files: " + copyFiles + "\nCopy Workflows: " + copyWorkflows);

			try {
				String message = copy(node,
						new NodeCopyRequest().setCopies(numCopies).setPages(copyPages).setTemplates(copyTemplates).setFiles(copyFiles).setWorkflows(copyWorkflows));

				// send success message to user
				if (userId > 0) {
					MessageSender messageSender = new MessageSender();

					transaction.addTransactional(messageSender);

					messageSender.sendMessage(new Message(1, userId, message, 1));
				}
			} catch (Throwable t) {
				logger.error("Error during {" + getType() + "} of node {" + nodeId + "}", t);
			}
		} catch (NodeException e) {
			logger.error("Error while doing nodecopy:", e);
		}
    }
}
