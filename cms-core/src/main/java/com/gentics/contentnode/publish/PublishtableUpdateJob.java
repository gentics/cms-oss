package com.gentics.contentnode.publish;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.AsynchronousJob;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.MulticonnectionTransaction;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.log.NodeLogger;

/**
 * Asynchronous Job that updates the publish table for a page in a node
 */
public class PublishtableUpdateJob implements AsynchronousJob {

	/**
	 * Page
	 */
	private Page page;

	/**
	 * Rendered page source
	 */
	private String source;

	/**
	 * Publish table path
	 */
	private String path;

	/**
	 * Channel ID
	 */
	private int channelId;

	/**
	 * Node ID
	 */
	private int nodeId;

	/**
	 * Flag to mark whether successful writing into the publish table shall be reported back to the publish queue
	 */
	private boolean reportToPublishQueue = false;

	/**
	 * Transaction
	 */
	private MulticonnectionTransaction transaction;

	/**
	 * Logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(PublishtableUpdateJob.class);

	/**
	 * Update alternate URLs for the page and node in table publish_alt_url
	 * @param page page
	 * @param node node
	 * @throws NodeException
	 */
	public static void updateAlternateUrls(Page page, Node node) throws NodeException {
		// get all alternate URLs of the page, prefixed with the node's hostname
		String hostname = node.getHostname();
		Set<String> alternateUrls = new HashSet<>(page.getAlternateUrls().stream()
			.map(url -> FilePublisher.getPath(false, false, hostname, url))
			.collect(Collectors.toSet()));
		int publishId = DBUtils.select(
			"SELECT id FROM publish WHERE page_id = ? AND node_id = ?",
			ps -> {
				ps.setInt(1, page.getId());
				ps.setInt(2, node.getId());
			},
			DBUtils.firstInt("id"),
			// We need to force using the write transaction, because a read transaction would not see the changes in
			// the publish table.
			Transaction.UPDATE_STATEMENT);

		DBUtils.selectAndUpdate("SELECT * FROM publish_alt_url WHERE publish_id = ?", ps -> ps.setInt(1, publishId),
				rs -> {
					while (rs.next()) {
						String url = rs.getString("url");
						if (!alternateUrls.contains(url)) {
							// stored URL is not set for the page any more, so delete entry
							rs.deleteRow();
						} else {
							// stored URL is still set for the page, remove from set
							alternateUrls.remove(url);
						}
					}

					// insert all missing alternate URLs
					for (String missing : alternateUrls) {
						rs.moveToInsertRow();
						rs.updateInt("publish_id", publishId);
						rs.updateString("url", missing);
						rs.insertRow();
					}
				});
	}

	/**
	 * Create an instance
	 * @param page page
	 * @param source source
	 * @param path publish table path
	 * @param t transaction
	 * @param channelId channelid (may be 0)
	 * @param nodeId node id (must not be 0)
	 * @param reportToPublishQueue true when success shall be reported to the publish queue
	 */
	public PublishtableUpdateJob(Page page, String source, String path, MulticonnectionTransaction t, int channelId, int nodeId, boolean reportToPublishQueue) {
		this.page = page;
		this.source = source;
		this.path = path;
		this.transaction = t;
		this.channelId = channelId;
		this.nodeId = nodeId;
		this.reportToPublishQueue = reportToPublishQueue;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.AsynchronousJob#process()
	 */
	public int process(RenderResult renderResult) throws Exception {
		Folder folder = null;

		TransactionManager.setCurrentTransaction(transaction);
		boolean niceUrlsFeature = NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS);

		try {
			// set the channel id into the transaction
			if (channelId > 0) {
				transaction.setChannelId(channelId);
			}

			folder = page.getFolder();
			final Node node = folder.getNode();

			Map<String, Object> idMap = new HashMap<String, Object>();
			idMap.put("page_id", page.getId());
			idMap.put("node_id", node.getId());

			Map<String, Object> data = new HashMap<String, Object>();
			if (source != null) {
				data.put("source", source);
			}
			data.put("active", 1);
			data.put("path", path);
			data.put("filename", page.getFilename());
			data.put("pdate", transaction.getUnixTimestamp());
			data.put("folder_id", folder.getMaster().getId());
			data.put("updateimagestore", node.doPublishFilesystem() ? 1 : 0);

			if (niceUrlsFeature && !ObjectTransformer.isEmpty(page.getNiceUrl())) {
				data.put("nice_url", new StringBuffer(node.getHostname()).append(page.getNiceUrl()).toString());
			}

			// also remove all entries for other channelset IDs of this page from the node (only one channelset variant of the page may be published into a node/channel)
			for (Object id : page.getChannelSet().values()) {
				DBUtils.executeUpdate("UPDATE publish SET active = 0 WHERE page_id = ? AND node_id = ?", new Object[] {id, node.getId()});
			}

			DBUtils.updateOrInsert("publish", idMap, data);

			if (niceUrlsFeature) {
				updateAlternateUrls(page, node);
			}

			// commit the transaction so that rendering into the publish table will take immediate effect
			transaction.commit(false);

			if (reportToPublishQueue) {
				PublishQueue.reportPublishActionDone(Page.TYPE_PAGE, ObjectTransformer.getInt(page.getId(), 0), nodeId, PublishAction.UPDATE_PUBLISH_TABLE);
			}

			logger.debug("Updated publish table for page {" + page.getId() + "}");
		} catch (Exception e) {
			throw new NodeException("Error while writing " + page + " to publish table", e);
		} finally {
			if (channelId > 0) {
				transaction.resetChannel();
			}
		}
		return 1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.AsynchronousJob#getDescription()
	 */
	public String getDescription() {
		StringBuilder str = new StringBuilder("Update publish table for ");
		str.append(page.toString());
		return str.toString();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.AsynchronousJob#isLogged()
	 */
	public boolean isLogged() {
		return true;
	}
}
