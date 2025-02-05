package com.gentics.contentnode.rest.resource.impl.internal.wastebin;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.internal.InternalResource;
import com.gentics.contentnode.rest.resource.impl.internal.JobController;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.lib.db.IntegerColumnRetriever;

/**
 * Resource to manage the purge wastebin job
 */
@Path("/internal/wastebin")
public class WastebinResource extends InternalResource {
	/**
	 * Job Controller for the purge wastebin job
	 */
	protected static JobController controller = new JobController("Purge Wastebin", "wastebin", () -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		t.setInstantPublishingEnabled(false);
		IntegerColumnRetriever ex = new IntegerColumnRetriever("id");
		DBUtils.executeStatement("SELECT id FROM node", ex);

		List<Node> nodes = t.getObjects(Node.class, ex.getValues());
		for (Node node : nodes) {
			node.purgeWastebin();
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
