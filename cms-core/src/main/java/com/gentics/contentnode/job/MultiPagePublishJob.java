package com.gentics.contentnode.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.Level;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.messaging.MessageSender;
import com.gentics.contentnode.msg.DefaultNodeMessage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.MultiPagePublishRequest;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.lib.i18n.CNI18nString;

/**
 * This job will offer a static method that allows you to publish a single page.
 * It's implementatio of {@link #processAction()} will background publishing
 * multiple pages at once
 *
 * @author clemens
 */
public class MultiPagePublishJob extends AbstractUserActionJob {

	/**
	 * Parameter that specifies the objects to localize
	 */
	public static final String PARAM_IDS = "ids";

	/**
	 * publish all languages
	 */
	public static final String PARAM_ISALLLANG = "isAlllang";

	/**
	 * publish at timestamp
	 */
	public static final String PARAM_AT = "at";

	/**
	 * publish message for workflows
	 */
	public static final String PARAM_MESSAGE = "message";

	/**
	 * keep the page's internal "publish at" time
	 */
	public static final String PARAM_KEEPPUBLISHAT = "keepPublishAt";

	/**
	 * keep the current "publishAt" version
	 */
	public static final String PARAM_KEEP_VERSION = "keepVersion";

	/**
	 * publish pages in a node/channel
	 */
	public static final String PARAM_NODEID = "nodeId";

	/**
	 * collection of page ids to be published
	 */
	protected Collection<String> ids;

	/**
	 * whether to publish all languages will default to false
	 */
	protected boolean isAlllang;

	/**
	 * publish at timestamp defaults to 0 for immediate publish
	 */
	protected int at;

	/**
	 * publish message for workflows an optional publish message if using
	 * multilevel workflows
	 */
	protected String message;

	/**
	 * if true the pages internal "publish at" timestamp will be hounoured, if
	 * false the page will just be published right away
	 */
	protected boolean keepPublishAt;

	/**
	 * True if the current publishAt version shall be kept
	 */
	protected boolean keepVersion;

	/**
	 * Node ID
	 */
	protected Integer nodeId;

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	@Override
	public String getJobDescription() {
		return new CNI18nString("multipagepublishjob").toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobParameters(org.quartz.JobDataMap)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		ids = ObjectTransformer.getCollection(map.get(PARAM_IDS), null);
		isAlllang = ObjectTransformer.getBoolean(map.get(PARAM_ISALLLANG), false);
		at = ObjectTransformer.getInt(map.get(PARAM_AT), 0);
		message = ObjectTransformer.getString(map.get(PARAM_MESSAGE), null);
		keepPublishAt = map.getBoolean(PARAM_KEEPPUBLISHAT);
		keepVersion = map.getBoolean(PARAM_KEEP_VERSION);
		nodeId = ObjectTransformer.getInteger(map.get(PARAM_NODEID), null);

		// the only thing that really matters to us is having a list of ids
		return ids != null;
	}

	// TODO test if everything works fine from the queue
	// NOPE, it doesnt! yay!
	// 1. add a parameter to the rest api that tells the api to check for a
	// publish date itself, rather than
	// use the provided timestamp
	// 2. modify page.cmd.php#cmd_page_timepub to be able to use this parameter

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	@Override
	protected void processAction() throws InsufficientPrivilegesException,
				NodeException,
				JobExecutionException {
		if (ObjectTransformer.isEmpty(ids)) {
			return;
		}

		t.setInstantPublishingEnabled(false);

		// set the node id
		if (nodeId != null) {
			t.setChannelId(nodeId);
		}

		try {
			int readOnlyCount = 0;
			int inheritedCount = 0;
			List<String> feedback = new ArrayList<String>();
			List<Page> skipped = new ArrayList<Page>();
			
			for (String id : ids) {
				try {
					PublishSuccessState state = publishPage(id, isAlllang, at, message, keepPublishAt, keepVersion, feedback);
					
					if (state == PublishSuccessState.SKIPPED) {
						skipped.add(PageResourceImpl.getPage(id, ObjectPermission.view));
					} else if (state == PublishSuccessState.INHERITED) {
						inheritedCount++;
					}
				} catch (ReadOnlyException e) {
					readOnlyCount++;
				} finally {
					// commit (but don't close) the transaction after every page
					// otherwise, when publishing page variants, this could lead to DB locks, since the same content
					// should be locked twice
					t.commit(false);
				}
			}
			
			String skippedMsg = null;
			
			if (skipped.size() == 1) {
				skippedMsg = feedback.get(0);
			} else if (skipped.size() > 1) {
				skippedMsg = generateIncorrectMandatoryTagsError(skipped, new ArrayList<Tag>(0));
			}
			
			if (null != skippedMsg) {
				addMessage(new DefaultNodeMessage(Level.WARN, getClass(), skippedMsg));
			}
			
			if (readOnlyCount > 0) {
				CNI18nString message = new CNI18nString("multipagepublishjob.lockedpages");
				
				message.addParameter(Integer.toString(readOnlyCount));
				addMessage(new DefaultNodeMessage(Level.INFO, getClass(), message.toString()));
			}

			if (inheritedCount > 0) {
				CNI18nString message = new CNI18nString("multipagepublishjob.inheritedpages");

				message.addParameter(Integer.toString(inheritedCount));
				addMessage(new DefaultNodeMessage(Level.INFO, getClass(), message.toString()));
			}
		} finally {
			if (nodeId != null) {
				t.resetChannel();
			}
		}
	}

	/**
	 * various success states of publishing a page as a page may just be
	 * published or put into a workflow
	 */
	public enum PublishSuccessState {

		/**
		 * a page has been published successfully
		 */
		PUBLISHED,

		/**
		 * Publish At was set for a page
		 */
		PUBLISHAT,

		/**
		 * a page was successfully put into a publish workflow
		 */
		WORKFLOW,
		/**
		 * a page was successfully pushed a step further in the publish workflow
		 */
		WORKFLOW_STEP,
		/**
		 * a page was skipped from being published on account of it not having
		 * all its mandatory tags correctly populated.
		 */
		SKIPPED,
		/**
		 * a page was skipped, because it is inherited (must be published for the master node)
		 */
		INHERITED
	}

	/**
	 * Returns a list of the mandatory (content) tags that belong to the given
	 * page.
	 *
	 * @param page
	 *            The page whose tags are to be retrieved
	 * @return The given page's mandatory tags
	 * @throws NodeException
	 */
	private static List<Tag> getMandatoryTags(Page page) throws NodeException {
		List<TemplateTag> templateTags = new ArrayList<TemplateTag>(page.getTemplate().getTemplateTags().values());

		List<ObjectTag> objectTags = new ArrayList<ObjectTag>(page.getObjectTags().values());

		List<Tag> mandatory = new ArrayList<Tag>();

		for (TemplateTag templateTag : templateTags) {
			if (templateTag.getMandatory() == true) {
				mandatory.add(page.getTag(templateTag.getName()));
			}
		}

		for (ObjectTag objectTag : objectTags) {
			if (objectTag.isRequired() == true) {
				mandatory.add(objectTag);
			}
		}

		return mandatory;
	}

	/**
	 * Returns a list of all mandatory content tags of the given page which are
	 * not populated correctly values.
	 *
	 * @param page
	 *            The page whose tags are to be retrieved
	 * @param regexConfig
	 *            A map of globally configured regular expressions; used to test
	 *            the validity of various tag parts
	 * @return The given page's invalid mandatory tags
	 * @throws NodeException
	 */
	private static List<Tag> getInvalidMandatoryTags(Page page,
			Map<?, ?> regexConfig) throws NodeException {
		List<Tag> tags = getMandatoryTags(page);
		List<Tag> invalids = new Vector<Tag>(tags.size());

		for (Tag tag : tags) {
			List<Part> parts = tag.getConstruct().getParts();
			ValueList values = tag.getValues();

			for (Part part : parts) {
				if (part.isRequired() == true) {
					Value value = values.getByPartId(part.getId());

					if (value == null || value.getValueText().trim().length() == 0) {
						invalids.add(tag);
						break;
					}

					PartType partType = value.getPartType();
					if (partType == null) {
						throw new NodeException("PartType of part {" + value.getPartId() + "} of value {" + value.getId() + "} doesn't exist");
					}

					if (!partType.validate(regexConfig)) {
						invalids.add(tag);
					}
				}
			}
		}

		return invalids;
	}

	/**
	 * Generates an error message about which pages were not published and which
	 * incomplete mandatory tags were at fault.
	 *
	 * @param pages
	 *            A list of pages that were skipped during publishing.
	 * @param tags
	 *            A list of invalid mandatory tags that were detected during a
	 *            publish attempt.
	 * @return Human readable error message detailing the reasons for skipping
	 *         one or more pages from being published.
	 * @throws NodeException
	 */
	public static String generateIncorrectMandatoryTagsError(
			List<Page> pages, List<Tag> tags) throws NodeException {
		CNI18nString i18n;
		StringBuilder names = new StringBuilder();
		int count = 0;
		int size = pages.size() == 1 ? tags.size() : pages.size();

		// Because the message should name up to 5 failed items.
		int sentinal = Math.min(5, size);
		int more = size - sentinal;

		// Because naming 6 item is better than naming 5 and then having
		// "... & 1 more"
		if (1 == more) {
			sentinal += 1;
			more = 0;
		}

		if (pages.size() == 1) {
			for (Tag tag : tags) {
				names.append(tag.getName());
				if (++count == sentinal) {
					break;
				}
				if (sentinal > 2) {
					names.append(", ");
				}
				if (0 == more && count == sentinal - 1) {
					names.append(" & ");
				}
			}

			// i18n: "The page {0} was not published because the following Tags
			// are not filled correctly: {1}"
			i18n = new CNI18nString("page.publish.skippedpage");
			i18n.addParameter('"' + pages.get(0).getName() + '"');
		} else {
			for (Page page : pages) {
				names.append('"').append(page.getName()).append("\"");
				if (++count == sentinal) {
					break;
				}
				if (sentinal > 2) {
					names.append(", ");
				}
				if (0 == more && count == sentinal - 1) {
					names.append(" & ");
				}
			}

			// i18n: "The following pages were not filled because they contain
			// Tags that were not filled correctly: {0}"
			i18n = new CNI18nString("page.publish.skippedpages");
		}

		if (more > 0) {
			names.append(", & ").append(more).append(" ").append((new CNI18nString("page.publish.more")).toString());
		}

		i18n.addParameter(names.toString());

		return i18n.toString();
	}

	/**
	 * publishes a single page
	 *
	 * If any of the mandatory tags of the given page are not populated with the
	 * required values, the page will be skipped from being published. The same
	 * is true for any language versions of this page that would have been
	 * published in this operation.
	 *
	 * @param id
	 *            the id of the page to be published
	 * @param isAlllang
	 *            {@link PagePublishRequest#isAlllang()}
	 * @param at
	 *            {@link PagePublishRequest#getAt()}
	 * @param message
	 *            {@link PagePublishRequest#getMessage()}
	 * @param keepPublishAt
	 *            set to true if you want to keep the pages internal publishAt
	 *            timestamp effective and dont want to publish it right away if
	 *            a timestamp is set. If set this will also override the "at"
	 *            parameter. {@link MultiPagePublishRequest#isKeepPublishAt()}
	 * @param keepVersion
	 *            true to keep the current publishAt version
	 * @return publish success state, see {@link PublishSuccessState}
	 * @throws EntityNotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public static PublishSuccessState publishPage(String id,
			boolean isAlllang,
			int at,
			String message,
			boolean keepPublishAt,
			boolean keepVersion) throws EntityNotFoundException,
				InsufficientPrivilegesException,
				ReadOnlyException,
				NodeException {
		return publishPage(id, isAlllang, at, message, keepPublishAt, keepVersion, null);
	}

	/**
	 * @see com.gentics.contentnode.job.MultiPagePublishJob#publishPage()
	 *
	 * @param id
	 *            the id of the page to be published
	 * @param isAlllang
	 *            {@link PagePublishRequest#isAlllang()}
	 * @param at
	 *            {@link PagePublishRequest#getAt()}
	 * @param message
	 *            {@link PagePublishRequest#getMessage()}
	 * @param keepPublishAt
	 *            set to true if you want to keep the pages internal publishAt
	 *            timestamp effective and dont want to publish it right away if
	 *            a timestamp is set. If set this will also override the "at"
	 *            parameter. {@link MultiPagePublishRequest#isKeepPublishAt()}
	 * @param keepVersion
	 *            true to keep the publishAt version
	 * @param feedback
	 *            A list in which to pass back feedback messages.  Works like an
	 *            out-parameter.
	 * @return publish success state, see {@link PublishSuccessState}
	 * @throws EntityNotFoundException
	 * @throws InsufficientPrivilegesException
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public static PublishSuccessState publishPage(String id,
			boolean isAlllang,
			int at,
			String message,
			boolean keepPublishAt,
			boolean keepVersion, List<String> feedback) throws EntityNotFoundException,
				InsufficientPrivilegesException,
				ReadOnlyException,
				NodeException {
		// Load the Page from GCN
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = null;
		List<Page> pages = new Vector<Page>();

		// get the page (readonly)
		page = PageResourceImpl.getPage(id);

		Node nodeForPermission = page.getChannel();
		if (nodeForPermission == null) {
			nodeForPermission = page.getOwningNode();
		}

		try (ChannelTrx trx = new ChannelTrx(nodeForPermission)) {
			page = PageResourceImpl.getPage(id, ObjectPermission.view);

			// check whether it is inherited
			if (page.isInherited()) {
				return PublishSuccessState.INHERITED;
			}

			// load the page (and lock it)
			page = PageResourceImpl.getLockedPage(id, ObjectPermission.view);
			if (isAlllang) {
				pages.addAll(PageResourceImpl.getLockedLanguageVariants(id, ObjectPermission.view));
			} else {
				pages.add(page);
			}

			try {
				Map<?, ?> multilevelWorkFlows = t.getNodeConfig().getDefaultPreferences().getPropertyMap("multilevel_pub_workflow");

				if (multilevelWorkFlows == null) {
					multilevelWorkFlows = new HashMap<>();
				}

				Folder folder = page.getFolder();
				// determine the node, for which to check the permission
				// if the page is bound to a channel, we check permission in that channel,
				// otherwise we check permission in the page's node

				Node node = folder.getNode();
				boolean multilevelWorkflow = ObjectTransformer.getBoolean(multilevelWorkFlows.get(ObjectTransformer.getString(node.getId(), null)), false);
				SystemUser user = t.getObject(SystemUser.class, t.getUserId());

				List<Tag> invalidTags = new ArrayList<Tag>();
				List<Page> skippedPages = new ArrayList<Page>();
				Map<?, ?> regexConfig = t.getNodeConfig().getDefaultPreferences().getPropertyMap("regex");

				// check whether all pages that need to be published have all mandatory tags filled
				for (Page p : pages) {
					List<Tag> invalidMandatoryTags = getInvalidMandatoryTags(p, regexConfig);

					if (!invalidMandatoryTags.isEmpty()) {
						invalidTags.addAll(invalidMandatoryTags);
						skippedPages.add(p);
					}
				}

				// Check permission for publishing the page
				if (PermHandler.ObjectPermission.publish.checkObject(page)) {
					// prepare the message sender
					MessageSender messageSender = new MessageSender();

					t.addTransactional(messageSender);

					// publish the page(s)
					for (Page p : pages) {
						if (skippedPages.contains(p)) {
							continue;
						}

						// check whether the page was in queue
						SystemUser pubQueueUser = p.getPubQueueUser();
						NodeObjectVersion publishAtVersion = null;

						// keep the current publishAt version, if requested
						if (keepVersion && p.getTimePubVersion() != null) {
							publishAtVersion = p.getTimePubVersion();
						}

						// if the page was in queue, we need to inform the user
						// who put the page into queue, that it is published now
						// but we only do this, when multilevelworkflow is NOT used
						// (otherwise, the message would be sent twice)
						if (!multilevelWorkflow && pubQueueUser != null) {
							CNI18nString messageTextI18n = null;

							if (keepPublishAt) {
								at = p.getTimePubQueue().getIntTimestamp();
								publishAtVersion = p.getTimePubVersionQueue();
							}

							if (at > 0) {
								messageTextI18n = new CNI18nString("publishing of page <pageid {0}> at {1} has been approved.");
								messageTextI18n.setParameter("0", ObjectTransformer.getString(p.getId(), null));
								messageTextI18n.setParameter("1", new ContentNodeDate(at).getFullFormat());
							} else {
								messageTextI18n = new CNI18nString("the page <pageid {0}> has been published.");
								messageTextI18n.setParameter("0", ObjectTransformer.getString(p.getId(), null));
							}

							try (LangTrx lTrx = new LangTrx(pubQueueUser)) {
								messageSender.sendMessage(
										new com.gentics.contentnode.messaging.Message(t.getUserId(), pubQueueUser.getId(), messageTextI18n.toString()));
							}
						}

						p.publish(at, publishAtVersion);
					}

					if (!skippedPages.isEmpty()) {
						if (null != feedback) {
							feedback.add(generateIncorrectMandatoryTagsError(skippedPages, invalidTags));
						}
						return PublishSuccessState.SKIPPED;
					}

					return at > 0 ? PublishSuccessState.PUBLISHAT : PublishSuccessState.PUBLISHED;

				} else if (!multilevelWorkflow) {
					// get users with publish permission
					Map<SystemUser, PermHandler> parentUsers = PageFactory.getPublishers(page);

					MessageSender messageSender = new MessageSender();

					t.addTransactional(messageSender);
					for (Page p : pages) {
						if (skippedPages.contains(p)) {
							continue;
						}

						// queue publishing the page
						NodeObjectVersion version = null;
						if (at > 0 && keepVersion && p.getTimePubVersion() != null) {
							version = p.getTimePubVersion();
						}
						p.queuePublish(t.getObject(SystemUser.class, t.getUserId()), at, version);

						// inform the group members
						CNI18nString messageTextI18n = null;

						if (at > 0) {
							messageTextI18n = new CNI18nString("message.publishat.queue");
							messageTextI18n.setParameter("0", user.getFirstname() + " " + user.getLastname());
							messageTextI18n.setParameter("1", ObjectTransformer.getString(p.getId(), null));
							messageTextI18n.setParameter("2", new ContentNodeDate(at).getFullFormat());
						} else {
							messageTextI18n = new CNI18nString("message.publish.queue");
							messageTextI18n.setParameter("0", user.getFirstname() + " " + user.getLastname());
							messageTextI18n.setParameter("1", ObjectTransformer.getString(p.getId(), null));
						}

						for (Map.Entry<SystemUser, PermHandler> entry : parentUsers.entrySet()) {
							SystemUser parentUser = entry.getKey();
							PermHandler permHandler = entry.getValue();
							// we need to check the publish permission for every user, since this will
							// consider node restrictions
							if (permHandler.canPublish(p)) {
								try (LangTrx lTrx = new LangTrx(parentUser)) {
									messageSender.sendMessage(new com.gentics.contentnode.messaging.Message(t.getUserId(), ObjectTransformer.getInt(
											parentUser.getId(), -1), messageTextI18n.toString()));
								}
							}
						}
					}

					if (!skippedPages.isEmpty()) {
						if (null != feedback) {
							feedback.add(generateIncorrectMandatoryTagsError(skippedPages, invalidTags));
						}
						return PublishSuccessState.SKIPPED;
					}

					return PublishSuccessState.WORKFLOW;
				} else {
					// check whether the page is already in a workflow
					PublishWorkflow workflow = page.getWorkflow();

					if (workflow != null) {
						// get the current step and check whether the user is member
						// of any of the assigned groups
						PublishWorkflowStep currentStep = workflow.getCurrentStep();
						List<UserGroup> stepGroups = currentStep.getUserGroups();

						boolean isMember = false;

						for (UserGroup group : stepGroups) {
							if (group.isMember(user)) {
								isMember = true;
								break;
							}
						}

						// if the user is not directly a member of any group to
						// which the page is currently assigned, we check, whether
						// the user is member of a super group
						if (!isMember) {
							// get all groups of the user
							List<UserGroup> userGroups = user.getUserGroups();

							// check for super groups of step groups
							for (UserGroup userGroup : userGroups) {
								for (UserGroup stepGroup : stepGroups) {
									// check whether the usergroup is parent of the
									// step group
									if (stepGroup.getParents().contains(userGroup)) {
										// check whether the usergroups gives
										// permission to view the page
										PermHandler groupPermHandler = t.getGroupPermHandler(ObjectTransformer.getInt(userGroup.getId(), 0));

										if (groupPermHandler.canView(page)) {
											isMember = true;
											break;
										}
									}
								}

								if (isMember) {
									break;
								}
							}
						}

						if (isMember) {
							workflow.addStep(message);
							workflow.save();
							return PublishSuccessState.WORKFLOW_STEP;
						} else {
							throw new InsufficientPrivilegesException(I18NHelper.get("page.nopermission", id), page, PermType.publishpages);
						}
					} else {
						// push the page into the workflow
						workflow = (PublishWorkflow) t.createObject(PublishWorkflow.class);
						workflow.setPageId(page.getId());
						workflow.addStep(message);
						workflow.save();
						return PublishSuccessState.WORKFLOW;
					}
				}
			} finally {
				for (Page p : pages) {
					p.unlock();
				}
			}
		}
	}
}
