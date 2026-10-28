package com.gentics.contentnode.rest.resource.impl.internal.wastebin;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.internal.InternalResource;
import com.gentics.contentnode.rest.resource.impl.internal.JobController;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.log.NodeLogger;

/**
 * Resource to manage the purge wastebin job
 */
@Path("/internal/wastebin")
public class WastebinResource extends InternalResource {
	/**
	 * Logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger("wastebin");

	/**
	 * Job Controller for the purge wastebin job
	 */
	protected static JobController controller = new JobController("Purge Wastebin", "wastebin", () -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		t.setInstantPublishingEnabled(false);
		IntegerColumnRetriever ex = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM node", ex);

		List<Node> nodes = t.getObjects(Node.class, ex.getValues());
		List<String> failedNodes = new ArrayList<>();
		for (Node node : nodes) {
			// purge every node in its own transaction, so that an error while purging a node (which might also close the transaction)
			// does not prevent purging the other nodes
			try (Trx nodeTrx = new Trx()) {
				nodeTrx.getTransaction().setInstantPublishingEnabled(false);
				nodeTrx.getTransaction().getObject(node).purgeWastebin();
				nodeTrx.success();
			} catch (NodeException e) {
				failedNodes.add(I18NHelper.getName(node));
				logger.error(String.format("Error while purging wastebin for node %s", I18NHelper.getName(node)), e);
			}
		}

		if (!failedNodes.isEmpty()) {
			throw new NodeException(String.format("Error while purging wastebin for node(s) %s", String.join(", ", failedNodes)));
		}
	});

	/**
	 * Get the job status
	 * @return job status
	 */
	@GET
	@Path("/purge/status")
	public JobStatus getStatus() {
		return controller.getJobStatus();
	}

	/**
	 * Start the job
	 * @return job status
	 */
	@POST
	@Path("/purge/start")
	public JobStatus startPurge() {
		return controller.start();
	}

	/**
	 * Stop the job
	 * @return generic response
	 */
	@POST
	@Path("/purge/stop")
	public GenericResponse stopPurge() {
		return controller.stop();
	}
}
