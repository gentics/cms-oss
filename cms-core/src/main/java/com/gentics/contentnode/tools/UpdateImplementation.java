package com.gentics.contentnode.tools;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.rest.client.ObjectMapperProvider;
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.JerseyRestClientImpl;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.ContentMaintenanceAction;
import com.gentics.contentnode.rest.model.ContentMaintenanceType;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.devtools.PackageListResponse;
import com.gentics.contentnode.rest.model.devtools.TemplateInPackage;
import com.gentics.contentnode.rest.model.request.ContentMaintenanceActionRequest;
import com.gentics.contentnode.rest.model.request.MaintenanceModeRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TagStatusResponse;
import com.gentics.contentnode.rest.model.response.devtools.PagedTemplateInPackageListResponse;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerStatus;
import com.gentics.contentnode.rest.model.response.scheduler.SchedulerStatusResponse;
import com.gentics.contentnode.tools.update.Config;
import com.gentics.contentnode.tools.update.Logger;

import jakarta.ws.rs.client.Entity;

/**
 * CmdLine Tool for updating implementations in the CMS
 */
public class UpdateImplementation implements AutoCloseable {
	public final static int SCHEDULER_API_DO = 875;

	protected Config config;

	protected RestClient client;

	protected boolean schedulerWasRunning = false;

	/**
	 * Main function
	 * @param args cmdline arguments
	 */
	public static void main(String[] args) {
		// init CommandLineParser and CommandLine
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		Options options = createOptions();

		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace(System.err);
			printHelpAndExit(options, 1);
		}

		if (line == null || line.hasOption(Config.HELP_LONG_PARAM)) {
			printHelpAndExit(options, 0);
		}

		try (UpdateImplementation tool = new UpdateImplementation(line)) {
			tool.setMaintenanceMode();

			tool.suspendScheduler();

			tool.reloadConfig();

			tool.syncPackages();

			tool.triggerSyncPages();

			tool.repairCr();

			tool.republishObjects();

			tool.resumeScheduler();

			tool.resetMaintenanceMode();
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(1);
		}

		System.exit(0);
	}

	/**
	 * Create the command line options
	 * 
	 * @return command line options
	 */
	@SuppressWarnings("static-access")
	private static Options createOptions() {
		Options options = new Options();
		options.addOption(OptionBuilder.hasArg().withDescription("Gentics CMS base URL").withLongOpt(Config.BASE_LONG_PARAM).create(Config.BASE_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("username").withLongOpt(Config.USER_LONG_PARAM).create(Config.USER_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("password").withLongOpt(Config.PASSWORD_LONG_PARAM).create(Config.PASSWORD_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArgs().withDescription("package name(s)").withLongOpt(Config.PACKAGES_LONG_PARAM).create(Config.PACKAGES_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArgs().withDescription("cr id(s)").withLongOpt(Config.CR_LONG_PARAM).create(Config.CR_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("timeout in ms").withLongOpt(Config.TIMEOUT_LONG_PARAM).create(Config.TIMEOUT_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("set maintenance mode").withLongOpt(Config.MAINTENANCE_MODE_LONG_PARAM).create(Config.MAINTENANCE_MODE_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("maintenance message").withLongOpt(Config.MAINTENANCE_MESSAGE_LONG_PARAM).create(Config.MAINTENANCE_MESSAGE_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("reload configuration").withLongOpt(Config.RELOAD_CONFIG_LONG_PARAM).create(Config.RELOAD_CONFIG_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("republish pages").withLongOpt(Config.REPUBLISH_PAGES_LONG_PARAM).create(Config.REPUBLISH_PAGES_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("republish files").withLongOpt(Config.REPUBLISH_FILES_LONG_PARAM).create(Config.REPUBLISH_FILES_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("republish folders").withLongOpt(Config.REPUBLISH_FOLDERS_LONG_PARAM).create(Config.REPUBLISH_FOLDERS_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("resume scheduler").withLongOpt(Config.RESUME_SCHEDULER_LONG_PARAM).create(Config.RESUME_SCHEDULER_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("trigger page sync for synchronized templates").withLongOpt(Config.TRIGGER_SYNC_PAGES_LONG_PARAM).create(Config.TRIGGER_SYNC_PAGES_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("await page sync for synchronized templates").withLongOpt(Config.AWAIT_SYNC_PAGES_LONG_PARAM).create(Config.AWAIT_SYNC_PAGES_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("await page sync timeout in ms").withLongOpt(Config.AWAIT_SYNC_PAGES_TIMEOUT_LONG_PARAM).create(Config.AWAIT_SYNC_PAGES_TIMEOUT_SHORT_PARAM));
		options.addOption(OptionBuilder.hasArg().withDescription("configuration file").withLongOpt(Config.FILE_LONG_PARAM).create(Config.FILE_SHORT_PARAM));
		options.addOption(OptionBuilder.withDescription("print usage help").withLongOpt(Config.HELP_LONG_PARAM).create());
		return options;
	}

	/**
	 * Private helper method to print the help screen and exit
	 * 
	 * @param options
	 *            command line options
	 * @param exitStatus exit status
	 */
	private static void printHelpAndExit(Options options, int exitStatus) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("UpdateImplementation", options);
		System.exit(exitStatus);
	}

	/**
	 * Create an instance of the Tool
	 * @param line command line
	 * @throws Exception
	 */
	protected UpdateImplementation(CommandLine line) throws Exception {
		if (line.hasOption("f")) {
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setDefaultMergeable(true);
			try (InputStream in = new FileInputStream(line.getOptionValue("f"))) {
				config = mapper.readValue(in, Config.class);
			}
		} else {
			config = new Config();
		}

		config.update(line);
		config.validate();

		System.out.println(String.format("Updating implementation on CMS with base URL %s as user %s", config.getBase(),
				config.getUser()));

		client = new JerseyRestClientImpl(() -> {
			ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider())
					.property(ClientProperties.CONNECT_TIMEOUT, config.getTimeout())
					.property(ClientProperties.READ_TIMEOUT, config.getTimeout());
			return JerseyClientBuilder.createClient(clientConfig).register(ObjectMapperProvider.class).register(JacksonFeature.class)
					.register(MultiPartFeature.class);
		}, config.getBase() + "/rest");

		client.login(config.getUser(), config.getPassword());

		// check configured packages and CRs for existence
		checkPackages();
		checkCrs();
	}

	/**
	 * Set maintenance mode and/or message
	 * @throws RestException
	 */
	protected void setMaintenanceMode() throws RestException {
		if (config.isMaintenanceMode() || !StringUtils.isBlank(config.getMaintenanceMessage())) {
			try (Logger log = new Logger("Setting maintenance mode/message")) {
				MaintenanceModeRequest request = new MaintenanceModeRequest();
				if (config.isMaintenanceMode()) {
					request.setMaintenance(true);
				}
				if (!StringUtils.isBlank(config.getMaintenanceMessage())) {
					request.setMessage(config.getMaintenanceMessage());
					request.setBanner(true);
				}
				MaintenanceResponse response = client.base().path("admin").path("maintenance").request().post(Entity.json(request),
						MaintenanceResponse.class);
				client.assertResponse(response);
			}
		}
	}

	/**
	 * Suspend the scheduler and wait until it is suspended
	 * @throws Exception
	 */
	protected void suspendScheduler() throws Exception {
		SchedulerStatus status = getSchedulerStatus();
		if (status == SchedulerStatus.suspended) {
			System.out.println("Scheduler already suspended");
			return;
		} else if (status == SchedulerStatus.running) {
			schedulerWasRunning = true;
		}

		try (Logger log = new Logger("Suspending scheduler")) {
			SchedulerStatusResponse response = client.base().path("scheduler").path("suspend").request().put(Entity.json(""), SchedulerStatusResponse.class);
			client.assertResponse(response);
			status = response.getStatus();

			long startWait = System.currentTimeMillis();
			while (status != SchedulerStatus.suspended && ((System.currentTimeMillis() - startWait) < config.getTimeout())) {
				Thread.sleep(1000);
				log.dot();
				status = getSchedulerStatus();
			}

			if (status != SchedulerStatus.suspended) {
				throw new Exception(String.format("Failed to suspend scheduler in %d milliseconds", config.getTimeout()));
			}
		}
	}

	/**
	 * Reload configuration
	 * @throws RestException
	 */
	protected void reloadConfig() throws RestException {
		if (config.isReloadConfig()) {
			try (Logger log = new Logger("Reloading configuration")) {
				GenericResponse response = client.base().path("admin").path("config").path("reload").request()
						.put(Entity.json(""), GenericResponse.class);
				client.assertResponse(response);
			}
		}
	}

	/**
	 * Check the devtools packages
	 * @throws Exception
	 */
	protected void checkPackages() throws Exception {
		if (config.getPackages() != null) {
			for (String pack : config.getPackages()) {
				try (Logger log = new Logger(String.format("Checking existence of package %s", pack))) {
					client.base().path("devtools").path("packages").path(pack).request().get(com.gentics.contentnode.rest.model.devtools.Package.class);
				}
			}
		}
	}

	/**
	 * Sync the devtools packages
	 * @throws Exception
	 */
	protected void syncPackages() throws Exception {
		if (config.getPackages() != null) {
			for (String pack : config.getPackages()) {
				try (Logger log = new Logger(String.format("Synchronizing package %s from fs to cms", pack))) {
					GenericResponse response = client.base().path("devtools").path("packages").path(pack).path("fs2cms").request().put(Entity.json(""), GenericResponse.class);
					client.assertResponse(response);
				}
			}
		}
	}

	/**
	 * Trigger page sync (and optionally wait, until done)
	 * @throws Exception
	 */
	protected void triggerSyncPages() throws Exception {
		if (config.isTriggerSyncPages()) {
			TemplateSaveRequest syncPages = new TemplateSaveRequest();
			syncPages.setUnlock(true);
			syncPages.setSyncPages(true);

			// first collect all templates, which are contained in the packages
			Set<TemplateInPackage> templates = new LinkedHashSet<>();

			for (String pack : config.getPackages()) {
				PagedTemplateInPackageListResponse response = client.base().path("devtools").path("packages").path(pack)
						.path("templates").request().get(PagedTemplateInPackageListResponse.class);
				client.assertResponse(response);
				for (TemplateInPackage templateInPackage : response.getItems()) {
					if (templateInPackage.getId() != null) {
						templates.add(templateInPackage);
					}
				}
			}

			// now iterate over all templates and check the tag status
			if (!CollectionUtils.isEmpty(templates)) {
				System.out.println(String.format("Need to check %d template(s) for page sync", templates.size()));

				int num = 0;
				for (TemplateInPackage template : templates) {
					num++;
					System.out.println(String.format("%d/%d: Checking template %s (%d)", num, templates.size(),
							template.getName(), template.getId()));
					if (requiresSyncPages(template)) {
						try (Logger log = new Logger(String.format("%d/%d: Trigger page sync for template %s (%d)", num,
								templates.size(), template.getName(), template.getId()))) {
							GenericResponse syncResponse = client.base().path("template").path(template.getGlobalId())
									.request().post(Entity.json(syncPages), GenericResponse.class);
							client.assertResponse(syncResponse);
							Optional.ofNullable(syncResponse.getResponseInfo()).map(ResponseInfo::getResponseMessage)
									.ifPresent(msg -> System.out.print(msg));
						}

						if (config.isAwaitSyncPages()) {
							try (Logger log = new Logger(String.format("%d/%d: Await page sync for template %s (%d)",
									num, templates.size(), template.getName(), template.getId()))) {
								long startWait = System.currentTimeMillis();
								while (requiresSyncPages(template)
										&& ((System.currentTimeMillis() - startWait) < config.getAwaitSyncPagesTimeout())) {
									log.dot();
									Thread.sleep(1000);
								}
								
								int tagsToSync = getTagsToSync(template);
								if (tagsToSync > 0) {
									throw new Exception(String.format(
											"Failed to synchronize all pages with template %s (%d) in %d milliseconds",
											template.getName(), template.getId(), config.getAwaitSyncPagesTimeout()));
								}
							}
						}
					} else {
						System.out.println(String.format("%d/%d: Template %s (%d) does not require page sync", num,
								templates.size(), template.getName(), template.getId()));
					}
				}
			}
		}
	}

	/**
	 * Get the tag status of the given template and count the total number of tags to be synchronized (excluding the incompatible tags)
	 * @param template template
	 * @return number of tags to be synchronized
	 * @throws RestException
	 */
	protected int getTagsToSync(TemplateInPackage template) throws RestException {
		TagStatusResponse tagStatusResponse = client.base().path("template").path(template.getGlobalId()).path("tagstatus").request()
				.get(TagStatusResponse.class);
		client.assertResponse(tagStatusResponse);

		AtomicInteger count = new AtomicInteger();
		tagStatusResponse.getItems().forEach(tagStatus -> {
			count.addAndGet(tagStatus.getMissing());
			count.addAndGet(tagStatus.getOutOfSync() - tagStatus.getIncompatible());
		});

		return count.get();
	}

	/**
	 * Check whether the pages of the template need to be synchronized
	 * @param template template
	 * @return true iff pages need to be synchronized
	 * @throws RestException
	 */
	protected boolean requiresSyncPages(TemplateInPackage template) throws RestException {
		return getTagsToSync(template) > 0;
	}

	/**
	 * Check the CRs
	 * @throws Exception
	 */
	protected void checkCrs() throws Exception {
		if (config.getCrs() != null) {
			for (String cr : config.getCrs()) {
				try (Logger log = new Logger(String.format("Checking CR %s", cr))) {
					ContentRepositoryResponse response = client.base().path("contentrepositories").path(cr).request()
							.get(ContentRepositoryResponse.class);
					client.assertResponse(response);
				}
			}
		}
	}

	/**
	 * Repair the CRs
	 * @throws Exception
	 */
	protected void repairCr() throws Exception {
		if (config.getCrs() != null) {
			for (String cr : config.getCrs()) {
				ContentRepositoryResponse response = client.base().path("contentrepositories").path(cr).request()
						.get(ContentRepositoryResponse.class);
				client.assertResponse(response);
				String crName = response.getContentRepository().getName();
				try (Logger log = new Logger(String.format("Repairing CR %s", crName))) {
					response = client.base().path("contentrepositories").path(cr).path("structure").path("repair")
							.queryParam("wait", 1000).request().put(Entity.json(""), ContentRepositoryResponse.class);
					client.assertResponse(response);

					Status status = response.getContentRepository().getCheckStatus();

					long startWait = System.currentTimeMillis();
					while ((status == Status.queued || status == Status.running) && ((System.currentTimeMillis() - startWait) < config.getTimeout())) {
						Thread.sleep(1000);
						log.dot();
						response = client.base().path("contentrepositories").path(cr).request()
								.get(ContentRepositoryResponse.class);
						client.assertResponse(response);
						status = response.getContentRepository().getCheckStatus();
					}

					if (status == Status.queued || status == Status.running) {
						throw new Exception(String.format("Still waiting for CR %s to be repaired after %d milliseconds", crName, config.getTimeout()));
					} else if (status != Status.ok) {
						throw new Exception(String.format("Repairing CR %s caused status %s", crName, status));
					}
				}
			}
		}
	}

	/**
	 * Republish objects
	 * @throws RestException
	 */
	protected void republishObjects() throws RestException {
		if (config.isRepublishFiles() || config.isRepublishFolders() || config.isRepublishPages()) {
			Set<ContentMaintenanceType> types = new HashSet<>();
			if (config.isRepublishFiles()) {
				types.add(ContentMaintenanceType.file);
			}
			if (config.isRepublishFolders()) {
				types.add(ContentMaintenanceType.folder);
			}
			if (config.isRepublishPages()) {
				types.add(ContentMaintenanceType.page);
			}
			try (Logger log = new Logger("Republishing " + types.toString())) {
				boolean haveCrs = !ObjectTransformer.isEmpty(config.getCrs());
				boolean havePackages = !ObjectTransformer.isEmpty(config.getPackages());
				if (haveCrs || havePackages) {
					Set<Node> touchedNodes = new HashSet<>();
					NodeList nodes = client.base().path("node").request().get(NodeList.class);
					client.assertResponse(nodes);
					log.dot();
					for (Node node : nodes.getItems()) {
						if (haveCrs && node.getContentRepositoryId() != null
								&& config.getCrs().contains(Integer.toString(node.getContentRepositoryId()))) {
							touchedNodes.add(node);
						} else if (node.getType() == ItemType.node && havePackages) {
							PackageListResponse nodePackages = client.base().path("devtools").path("nodes").path(Integer.toString(node.getId()))
									.path("packages").request().get(PackageListResponse.class);
							client.assertResponse(nodePackages);
							log.dot();
							if (nodePackages.getItems().stream()
									.filter(pack -> config.getPackages().contains(pack.getName())).findFirst()
									.isPresent()) {
								touchedNodes.add(node);
							}
						}
					}
					if (!touchedNodes.isEmpty()) {
						Set<Integer> ids = touchedNodes.stream().map(Node::getId).collect(Collectors.toSet());
						ContentMaintenanceActionRequest request = new ContentMaintenanceActionRequest()
								.setAction(ContentMaintenanceAction.dirt).setTypes(types).setNodes(ids);
						GenericResponse response = client.base().path("admin").path("content").path("publishqueue")
								.request().post(Entity.json(request), GenericResponse.class);
						client.assertResponse(response);
					}
				}
			}
		}
	}

	/**
	 * Resume the scheduler and wait until it is resumed
	 * @throws Exception
	 */
	protected void resumeScheduler() throws Exception {
		if (config.getResumeScheduler() == Config.ResumeScheduler.never) {
			return;
		}
		if (config.getResumeScheduler() == Config.ResumeScheduler.running && !schedulerWasRunning) {
			return;
		}

		SchedulerStatus status = getSchedulerStatus();
		if (status == SchedulerStatus.running) {
			System.out.println("Scheduler already running");
			return;
		}

		try (Logger log = new Logger("Resuming scheduler")) {
			SchedulerStatusResponse response = client.base().path("scheduler").path("resume").request().put(Entity.json(""), SchedulerStatusResponse.class);
			client.assertResponse(response);
			status = response.getStatus();

			long startWait = System.currentTimeMillis();
			while (status != SchedulerStatus.running && ((System.currentTimeMillis() - startWait) < config.getTimeout())) {
				log.dot();
				Thread.sleep(1000);
				status = getSchedulerStatus();
			}

			if (status != SchedulerStatus.running) {
				throw new Exception(String.format("Failed to resume scheduler in %d milliseconds", config.getTimeout()));
			}
		}
	}

	/**
	 * Reset maintenance mode and/or message
	 * @throws RestException
	 */
	protected void resetMaintenanceMode() throws RestException {
		if (config.isMaintenanceMode() || !StringUtils.isBlank(config.getMaintenanceMessage())) {
			try (Logger log = new Logger("Resetting maintenance mode/message")) {
				MaintenanceModeRequest request = new MaintenanceModeRequest();
				if (config.isMaintenanceMode()) {
					request.setMaintenance(false);
				}
				if (!StringUtils.isBlank(config.getMaintenanceMessage())) {
					request.setMessage("");
				}
				MaintenanceResponse response = client.base().path("admin").path("maintenance").request().post(Entity.json(request),
						MaintenanceResponse.class);
				client.assertResponse(response);
			}
		}
	}

	/**
	 * Get the scheduler status
	 * @return status
	 * @throws RestException
	 */
	protected SchedulerStatus getSchedulerStatus() throws RestException {
		SchedulerStatusResponse response = client.base().path("scheduler").path("status").request().get(SchedulerStatusResponse.class);
		client.assertResponse(response);
		return response.getStatus();
	}

	@Override
	public void close() {
		if (client != null) {
			try {
				client.logout();
			} catch (RestException e) {
				e.printStackTrace();
			}
		}
	}
}
