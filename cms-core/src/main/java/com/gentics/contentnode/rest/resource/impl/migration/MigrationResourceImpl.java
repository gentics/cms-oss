package com.gentics.contentnode.rest.resource.impl.migration;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.FeatureNotLicensedException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.distributed.TrxCallable;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.migration.MigrationHelper;
import com.gentics.contentnode.migration.MigrationPartMapper;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.migration.jobs.TemplateMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.utility.ConstructComparator;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.request.ConstructSortAttribute;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.migration.MigrationReinvokeRequest;
import com.gentics.contentnode.rest.model.request.migration.MigrationTagsRequest;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.migration.MigrationGetLogResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationGetLogsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobItemsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationPartsResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationStatusResponse;
import com.gentics.contentnode.rest.model.response.migration.MigrationTagsResponse;
import com.gentics.contentnode.rest.model.response.migration.PossiblePartMappingsResponse;
import com.gentics.contentnode.rest.resource.migration.MigrationResource;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.RestCallable;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * Resource used for performing Tag Type and Template Migrations
 *
 * @author Taylor
 */
@Path("/migration")
@Authenticated
public class MigrationResourceImpl implements MigrationResource {

	@Override
	@GET
	@Path("/cancelMigration")
	public GenericResponse cancelMigration() throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			return DistributionUtil.call(new CancelMigration().setLanguageId().setSession(trx.getTransaction().getSession()));
		}
	}

	@Override
	@GET
	@Path("/getMigrationStatus")
	public MigrationStatusResponse getMigrationStatus() throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			return DistributionUtil.call(new GetMigrationStatus().setLanguageId().setSession(trx.getTransaction().getSession()));
		}
	}

	@Override
	@GET
	@Path("/getMigrationJobItems/{jobId}")
	public MigrationJobItemsResponse getMigrationJobItems(@PathParam("jobId") int jobId) throws NodeException {
		MigrationJobItemsResponse response;

		try (Trx trx = ContentNodeHelper.trx()) {
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);

			response = new MigrationJobItemsResponse(null, new ResponseInfo(ResponseCode.OK, "The migration job items have been fetched."));
			response.setJobId(jobId);
			response.setJobItems(dbLogger.getMigrationJobItemEntries(jobId));
			return response;
		}
	}

	@Override
	@GET
	@Path("/getMigrationLog/{jobId}")
	public MigrationGetLogResponse getMigrationLog(@PathParam("jobId") int jobId) throws NodeException {

		try (Trx trx = ContentNodeHelper.trx()) {
			MigrationGetLogResponse response = new MigrationGetLogResponse(null, new ResponseInfo(ResponseCode.OK, "Log sucessfully loaded."));
			File logFile = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER).getLogFileForJob(jobId);

			// TODO add a threshold. We don't want to return 10MB to the client. Add a flag that indicates that the threshold was reached. Only return xx kB from the
			// bottom of the log..
			String contents = FileUtils.readFileToString(logFile);

			// Replace unixstyle linebreaks to fix display of the log for IE
			contents = contents.replaceAll("\n", "\r\n");
			response.setLogContents(contents);
			return response;
		} catch (IOException e) {
			throw new NodeException(e);
		}
	}

	@Override
	@GET
	@Path("/getMigrationLogs")
	public MigrationGetLogsResponse getMigrationLogs() throws NodeException {

		Map<String, String> logs = new HashMap<String, String>();
		DateFormat dateFormat = new SimpleDateFormat(MigrationHelper.getTtmLogDateFormat());

		try (Trx trx = ContentNodeHelper.trx()) {
			File folder = MigrationHelper.getLogDir();

			MigrationGetLogsResponse response = new MigrationGetLogsResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched migration logs."));

			if (!folder.exists()) {
				return response;
			}
			// Add all files in the migration log folder to the response object
			for (File fileEntry : folder.listFiles()) {
				String dateString = fileEntry.getName().replace(MigrationHelper.getTtmLogPrefix(), "");
				SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String strDate = sdfDate.format(dateFormat.parse(dateString));

				logs.put(strDate, fileEntry.getName());
			}
			response.setLogFilenames(logs);

			// Load the job entries from the database and add them to the response
			MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);

			response.setJobEntries(dbLogger.getMigrationJobEntries());

			return response;
		} catch (ParseException e) {
			throw new NodeException(e);
		}
	}

	@Override
	@POST
	@Path("/reinvokeMigration")
	public MigrationResponse reinvokeTagTypeMigration(MigrationReinvokeRequest request) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			return DistributionUtil.call(new ReinvokeTTM().setRequest(request).setLanguageId().setSession(trx.getTransaction().getSession()));
		}
	}

	@Override
	@POST
	@Path("/performTemplateMigration")
	public MigrationResponse performTemplateMigration(TemplateMigrationRequest request) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			return DistributionUtil.call(new PerformTemplateMigration().setRequest(request).setLanguageId().setSession(trx.getTransaction().getSession()));
		}
	}

	@Override
	@POST
	@Path("/performMigration")
	public MigrationResponse performTagTypeMigration(TagTypeMigrationRequest request) throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			return DistributionUtil.call(new PerformTagTypeMigration().setRequest(request).setLanguageId().setSession(trx.getTransaction().getSession()));
		}
	}

	@Override
	@GET
	@Path("/getPartsForTagType/{id}")
	public MigrationPartsResponse getPartsForTagType(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			List<Part> nodeParts = MigrationHelper.fetchPartsForTagtype(id, t);

			if (nodeParts == null || nodeParts.isEmpty()) {
				throw new NodeException("No parts were found");
			}

			List<com.gentics.contentnode.rest.model.Part> restParts = new ArrayList<com.gentics.contentnode.rest.model.Part>();

			// Convert each tag to its corresponding REST model
			for (Part part : nodeParts) {
				restParts.add(ModelBuilder.getPart(part));
			}

			MigrationPartsResponse response = new MigrationPartsResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched parts"));

			response.setParts(restParts);
			return response;
		}
	}

	@Override
	@POST
	@Path("/getMigrationTagTypes")
	public MigrationTagsResponse getMigrationTagTypes(MigrationTagsRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<com.gentics.contentnode.rest.model.Construct> restList = getMigrationConstructList(request);

			// Create response object with tag list
			MigrationTagsResponse response = new MigrationTagsResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched tags"));

			// Convert each tag to its corresponding REST model and add it to the response object
			for (com.gentics.contentnode.rest.model.Construct construct : restList) {
				response.addTagType(construct.getId(), construct);
			}

			return response;
		}
	}

	@Override
	@POST
	@Path("/getMigrationConstructs")
	public ConstructListResponse getMigrationConstructs(MigrationTagsRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<com.gentics.contentnode.rest.model.Construct> restList = getMigrationConstructList(request);

			// Create response object with tag list
			ConstructListResponse response = new ConstructListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched tags"));
			response.setConstructs(restList);

			return response;
		}
	}

	/**
	 * Get the list of constructs (rest models) sorted by name
	 * @param request request
	 * @return sorted list of constructs
	 * @throws NodeException
	 */
	protected List<com.gentics.contentnode.rest.model.Construct> getMigrationConstructList(MigrationTagsRequest request) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> ids = request.getIds();
		List<Construct> tagTypes = null;

		if (request.getType() == null) {
			throw new NodeException("Request to retrieve migration tag types was missing object type.");
		}

		if (ids == null) {
			throw new NodeException("Request to retrieve migration tag types was missing object list.");
		}

		// Get tag types based on type
		if (request.getType().equalsIgnoreCase("page")) {
			tagTypes = MigrationHelper.fetchAllTagTypesForPages(ids);
		} else if (request.getType().equalsIgnoreCase("template")) {
			tagTypes = MigrationHelper.fetchAllTagTypesForTemplates(ids, t);
		} else if (request.getType().equalsIgnoreCase("objtagdef")) {
			tagTypes = MigrationHelper.fetchAllTagTypesForOEDef(ids, t);
		} else if (request.getType().equalsIgnoreCase("global")) {
			tagTypes = MigrationHelper.fetchAllTagTypes(t);
		} else {
			throw new NodeException("Unrecognized object type encountered during tag type migration: " + request.getType());
		}

		// sort the list of constructs by name
		Collections.sort(tagTypes, new ConstructComparator(ConstructSortAttribute.name, SortOrder.asc));

		List<com.gentics.contentnode.rest.model.Construct> restConstructs = new ArrayList<>(tagTypes.size());

		// Convert each tag to its corresponding REST model and add it to the response object
		for (Construct tagType : tagTypes) {
			restConstructs.add(ModelBuilder.getConstruct(tagType));
		}

		return restConstructs;
	}

	@Override
	@GET
	@Path("/getPossiblePartMappings")
	public PossiblePartMappingsResponse getPossiblePartMappings(@QueryParam("fromTagTypeId") int fromTagTypeId,
			@QueryParam("toTagTypeId") int toTagTypeId) throws NodeException {

		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Construct fromTagType = t.getObject(Construct.class, fromTagTypeId);
			Construct toTagType = t.getObject(Construct.class, toTagTypeId);

			if (fromTagType == null || toTagType == null) {
				throw new NodeException("One of the constructs that should be compared could not be found");
			}
			Map<Part, List<Part>> possibleMappings = MigrationPartMapper.getPossiblePartTypeMappings(fromTagType, toTagType);

			// Transform the nested map node objects into rest model objects
			Map<Integer, List<com.gentics.contentnode.rest.model.Part>> possibleMappingsRestModel = new HashMap<Integer, List<com.gentics.contentnode.rest.model.Part>>();

			for (Part fromPart : possibleMappings.keySet()) {
				com.gentics.contentnode.rest.model.Part fromPartRestModel = ModelBuilder.getPart(fromPart);
				List<Part> possibleParts = possibleMappings.get(fromPart);
				List<com.gentics.contentnode.rest.model.Part> possiblePartsRestModel = new ArrayList<com.gentics.contentnode.rest.model.Part>();

				for (Part possiblePart : possibleParts) {
					com.gentics.contentnode.rest.model.Part possiblePartRestModel = ModelBuilder.getPart(possiblePart);

					possiblePartsRestModel.add(possiblePartRestModel);
				}
				possibleMappingsRestModel.put(fromPartRestModel.getId(), possiblePartsRestModel);
			}

			PossiblePartMappingsResponse response = new PossiblePartMappingsResponse(null,
					new ResponseInfo(ResponseCode.OK, "Successfully determined possible part mappings."));

			response.setPossibleMapping(possibleMappingsRestModel);
			return response;
		}
	}

	/**
	 * Task to cancel a migration
	 */
	public static class CancelMigration extends TrxCallable<GenericResponse> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2366523174823502468L;

		@Override
		protected GenericResponse callWithTrx() throws NodeException {
			// check if a migration is currently in progress
			if (MigrationHelper.isTagTypeMigrationExecuting()) {
				try {
					AbstractMigrationJob job = Operator.getCurrentlyRunningJobs().stream().map(RestCallable::getWrapped).filter(callable -> {
						return callable instanceof AbstractMigrationJob;
					}).map(callable -> AbstractMigrationJob.class.cast(callable)).findAny().orElseThrow();
					job.interrupt();

					GenericResponse response = new GenericResponse();

					response.addMessage(new Message(Type.SUCCESS, "Successfully cancelled migration process"));
					return response;
				} catch (Exception e) {
					NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while cancelling migration process", e);
					I18nString message = new CNI18nString("rest.general.error");

					return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.FAILURE, "Error while cancelling migration process. See server logs for details."));
				}

			}
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "No migration is currently in process."));
		}
	}

	/**
	 * Task to get the migration status
	 */
	public static class GetMigrationStatus extends TrxCallable<MigrationStatusResponse> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -2424668581063847148L;

		@Override
		protected MigrationStatusResponse callWithTrx() throws NodeException {
			ResponseInfo defaultInfo = new ResponseInfo(ResponseCode.OK, "No migration is currently in process.");
			MigrationStatusResponse response = new MigrationStatusResponse(null, defaultInfo);

			try {
				// Get the status of the last job
				MigrationDBLogger migrationDBlogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);

				List<MigrationJobEntry> jobs = migrationDBlogger.getMigrationJobEntries();
				MigrationJobEntry latestJob = null;

				for (MigrationJobEntry job : jobs) {
					if (latestJob == null || latestJob.getJobId() < job.getJobId()) {
						latestJob = job;
					}
				}
				response.setLatestJob(latestJob);

				AbstractMigrationJob job = Operator.getCurrentlyRunningJobs().stream().map(RestCallable::getWrapped)
						.filter(callable -> {
							return callable instanceof AbstractMigrationJob;
						}).map(callable -> AbstractMigrationJob.class.cast(callable)).findAny().orElse(null);

				if (job != null) {
					response.setStatus(AbstractMigrationJob.STATUS_IN_PROGRESS);
					response.setPercentComplete(job.getPercentCompleted());
					int jobId = job.getMigrationJobId();

					response.setJobId(jobId);
					return response;
				}
				response.setStatus(AbstractMigrationJob.STATUS_PENDING);
				return response;
			} catch (Exception e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while retrieving migration status", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationStatusResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while retrieving migration status. See server logs for details."));
			}
		}
	}

	/**
	 * Task to reinvoke a TTM
	 */
	public static class ReinvokeTTM extends TrxCallable<MigrationResponse> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 7147150866961073853L;

		/**
		 * Request
		 */
		protected MigrationReinvokeRequest request;

		/**
		 * Set the request
		 * @param request request
		 * @return fluent API
		 */
		public ReinvokeTTM setRequest(MigrationReinvokeRequest request) {
			this.request = request;
			return this;
		}

		@Override
		protected MigrationResponse callWithTrx() throws NodeException {
			try {
				// Check if a migration is already in progress
				if (!MigrationHelper.isTagTypeMigrationExecuting()) {

					// retrieve the mapping config from the db and check whether there is a db entry for the jobid
					MigrationDBLogger dbLogger = new MigrationDBLogger(MigrationDBLogger.DEFAULT_LOGGER);
					MigrationJobEntry jobLogEntry = dbLogger.getMigrationJobEntry(request.getJobId());

					if (jobLogEntry == null) {
						throw new NodeException("The migration job with id {" + request.getJobId() + "} could not be found.");
					}
					TagTypeMigrationRequest originalMigrationRequest = null;

					ObjectMapper mapper = new ObjectMapper();

					try {
						originalMigrationRequest = mapper.readValue(jobLogEntry.getConfig(), new TypeReference<TagTypeMigrationRequest>() {});
					} catch (Exception e) {
						throw new NodeException("Error while unmarshalling json to java object.", e);
					}

					// Build the list which contains only the selected object
					ArrayList<Integer> objects = new ArrayList<Integer>();

					objects.add(request.getObjectId());

					// Create and execute job
					TagTypeMigrationJob job = new TagTypeMigrationJob()
							.setRequest(originalMigrationRequest)
							.setType(request.getType())
							.setObjectIds(objects);

					GenericResponse executeJobResponse = job.execute(10, TimeUnit.SECONDS);

					// Create and return response
					MigrationResponse response = new MigrationResponse();

					response.setMessages(executeJobResponse.getMessages());
					response.setResponseInfo(executeJobResponse.getResponseInfo());
					response.setJobId(job.getMigrationJobId());
					return response;
				}

				return new MigrationResponse(
						new ResponseInfo(ResponseCode.FAILURE, "Unable to perform tag type migration because another migration is already in progress."));
			} catch (NodeException e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while reinvoking job {" + request.getJobId() + "} for object {" + request.getJobId() + "}", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while executing tag type migration job. See server logs for details."));
			}
		}
	}

	/**
	 * Task to perform a template migration
	 */
	public static class PerformTemplateMigration extends TrxCallable<MigrationResponse> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 1413911874447010941L;

		/**
		 * Request
		 */
		protected TemplateMigrationRequest request;

		/**
		 * Set the request
		 * @param request request
		 * @return fluent API
		 */
		public PerformTemplateMigration setRequest(TemplateMigrationRequest request) {
			this.request = request;
			return this;
		}

		@Override
		protected MigrationResponse callWithTrx() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			try {
				// TODO: Is this check reversed?
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.TAGTYPEMIGRATION)) {
					throw new FeatureNotLicensedException("The TTM feature is not licensed!");
				}

				// Check if a migration is already in progress
				if (!MigrationHelper.isTagTypeMigrationExecuting()) {

					// validate mappings
					// if (!TagTypeMigrationPartMapper.isMappingValid(request.getMappings(), t)) {
					// throw new NodeException("Invalid mapping was received.");
					// }

					// Create and execute job
					TemplateMigrationJob job = new TemplateMigrationJob()
							.setRequest(request);

					GenericResponse executeJobResponse = job.execute(10, TimeUnit.SECONDS);

					// Create and return response
					MigrationResponse response = new MigrationResponse();

					response.setMessages(executeJobResponse.getMessages());
					response.setResponseInfo(executeJobResponse.getResponseInfo());
					return response;
				}

				return new MigrationResponse(
						new ResponseInfo(ResponseCode.FAILURE, "Unable to perform template migration because another migration is already in progress."));

			} catch (FeatureNotLicensedException e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while executing tag type migration job", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTLICENSED, "Error while executing migration job. See server logs for details."));
			} catch (NodeException e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while executing tag type migration job", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while executing migration job. See server logs for details."));
			}
		}
	}

	/**
	 * Task to perform a tagtype migration
	 */
	public static class PerformTagTypeMigration extends TrxCallable<MigrationResponse> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 5836642752204455121L;

		/**
		 * Request
		 */
		protected TagTypeMigrationRequest request;

		/**
		 * Set the request
		 * @param request request
		 * @return fluent API
		 */
		public PerformTagTypeMigration setRequest(TagTypeMigrationRequest request) {
			this.request = request;
			return this;
		}

		@Override
		protected MigrationResponse callWithTrx() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			try {
				if (NodeConfigRuntimeConfiguration.isFeature(Feature.TAGTYPEMIGRATION)) {
					throw new FeatureNotLicensedException("The TTM feature is not licensed!");
				}

				// Check if a migration is already in progress
				if (!MigrationHelper.isTagTypeMigrationExecuting()) {

					// validate mappings
					if (!MigrationPartMapper.isMappingValid(request.getMappings(), t)) {
						throw new NodeException("Invalid mapping was received.");
					}

					// Check whether the handlePagesByTemplate setting is valid
					if (request.isHandlePagesByTemplate() && (request.getObjectIds().size() != 1 || !request.getType().equalsIgnoreCase("page"))) {
						request.setHandlePagesByTemplate(false);
					}

					// Create and execute job
					TagTypeMigrationJob job = new TagTypeMigrationJob()
							.setRequest(request)
							.setType(request.getType())
							.setObjectIds(request.getObjectIds())
							.setHandlePagesByTemplate(request.isHandlePagesByTemplate())
							.setHandleAllNodes(request.isHandleAllNodes())
							.setPreventTriggerEvent(request.isPreventTriggerEvent());

					GenericResponse executeJobResponse = job.execute(1, TimeUnit.SECONDS);

					// Create and return response
					MigrationResponse response = new MigrationResponse();

					response.setMessages(executeJobResponse.getMessages());
					response.setResponseInfo(executeJobResponse.getResponseInfo());
					return response;
				}

				return new MigrationResponse(
						new ResponseInfo(ResponseCode.FAILURE, "Unable to perform tag type migration because another migration is already in progress."));

			} catch (FeatureNotLicensedException e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while executing tag type migration job", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTLICENSED, "Error while executing tag type migration job. See server logs for details."));
			} catch (NodeException e) {
				NodeLogger.getNodeLogger(MigrationResourceImpl.class).error("Error while executing tag type migration job", e);
				I18nString message = new CNI18nString("rest.general.error");

				return new MigrationResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while executing tag type migration job. See server logs for details."));
			}
		}
	}
}
