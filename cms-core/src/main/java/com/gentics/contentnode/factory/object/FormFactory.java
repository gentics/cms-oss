package com.gentics.contentnode.factory.object;

import static com.gentics.api.lib.etc.ObjectTransformer.getInteger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.datasource.VersioningDatasource.Version;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.db.TableVersion;
import com.gentics.lib.etc.StringUtils;

import io.reactivex.Flowable;

/**
 * Object Factory for creating {@link Form} objects
 */
@DBTables({ @DBTable(clazz = Form.class, name = "form") })
public class FormFactory extends AbstractFactory {
	/**
	 * SQL Statement to select a single form
	 */
	protected final static String SELECT_FORM_SQL = createSelectStatement("form");

	/**
	 * SQL Statement for batchloading forms
	 */
	protected final static String BATCHLOAD_FORM_SQL = createBatchLoadStatement("form");

	/**
	 * Postfix of form properties, that can be translated
	 */
	protected final static String I18N_POSTFIX = "_i18n";

	/**
	 * Postfix of form properties, that reference pages, which should be rendered to get the property value
	 */
	public final static String PAGEID_POSTFIX = "_pageid";

	/**
	 * Indexed status attributes
	 */
	protected final static String[] INDEXED_STATUS_ATTRIBUTES = { "modified", "online", "published", "publisherId", "planned", "publishAt", "offlineAt" };

	/**
	 * Indexed mc attributes
	 */
	protected final static String[] INDEXED_MC_ATTRIBUTES = { "online", "nodeId" };

	/**
	 * Loader for {@link FormService}s
	 */
	protected final static ServiceLoaderUtil<FormService> formFactoryServiceLoader = ServiceLoaderUtil
			.load(FormService.class);

	static {
		// register the factory class
		try {
			registerFactoryClass("form", Form.TYPE_FORM, true, FactoryForm.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	private static class FactoryForm extends AbstractContentObject implements Form, ExtensiblePublishableObject<Form> {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 8321562011986964532L;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("description")
		@Updateable
		protected String description;

		@DataField("folder_id")
		@Updateable
		@Unversioned
		protected int folderId;

		@DataField("success_page_id")
		@Updateable
		protected int successPageId;

		@DataField("success_node_id")
		@Updateable
		protected int successNodeId;

		@DataField("languages")
		@Updateable
		protected List<String> languages;

		@DataField("data")
		@Updateable
		protected JsonNode data;

		@DataField("cdate")
		@Unversioned
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("creator")
		@Unversioned
		protected int creatorId = 0;

		@DataField("edate")
		@Updateable
		@Unversioned
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		@DataField("editor")
		@Updateable
		@Unversioned
		protected int editorId = 0;

		@DataField("pdate")
		@Updateable
		@Unversioned
		protected ContentNodeDate pDate = new ContentNodeDate(0);

		@DataField("publisher")
		@Updateable
		@Unversioned
		protected int publisherId = 0;

		@DataField("time_pub")
		@Unversioned
		protected ContentNodeDate timePub = new ContentNodeDate(0);

		@DataField("time_pub_version")
		@Unversioned
		protected Integer timePubVersion;

		@DataField("time_off")
		@Unversioned
		protected ContentNodeDate timeOff = new ContentNodeDate(0);

		/**
		 * Timestamp of deletion, if the object was deleted (and pub into the wastebin), 0 if object is not deleted
		 */
		@DataField("deleted")
		@Unversioned
		protected int deleted;

		/**
		 * ID of the user who put the object into the wastebin, 0 if not deleted
		 */
		@DataField("deletedby")
		@Unversioned
		protected int deletedBy;

		/**
		 * modified flag
		 */
		@DataField("modified")
		@Updateable
		@Unversioned
		protected boolean formModified;

		/**
		 * Online flag
		 */
		@DataField("online")
		@Updateable
		@Unversioned
		protected boolean online;

		@DataField("locked")
		@Updateable
		@Unversioned
		protected int locked;

		@DataField("lockedby")
		@Updateable
		@Unversioned
		protected int lockedBy;

		/**
		 * Versions
		 */
		protected NodeObjectVersion[] versions;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryForm(NodeObjectInfo info) {
			super(null, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info object info
		 */
		protected FactoryForm(Integer id, NodeObjectInfo info) {
			super(id, info);
		}

		/**
		 * Create an instance
		 * @param id id
		 * @param info info
		 * @param dataMap data map
		 * @param udate udate
		 * @param globalId global id
		 * @throws NodeException
		 */
		public FactoryForm(Integer id, NodeObjectInfo info,
				Map<String, Object> dataMap, int udate, GlobalId globalId) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
			this.udate = udate;
			this.globalId = globalId;
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public NodeObject copy() throws NodeException {
			return new EditableFactoryForm(this, getFactory().getFactoryHandle(Form.class).createObjectInfo(Form.class, true), true);
		}

		@Override
		public String[] getStackKeywords() {
			return new String[] { "form" };
		}

		@Override
		public Resolvable getKeywordResolvable(String keyword) throws NodeException {
			return this;
		}

		@Override
		public Resolvable getShortcutResolvable() throws NodeException {
			return null;
		}

		@Override
		public String getStackHashKey() {
			return "form:" + getHashKey();
		}

		@Override
		public boolean isOnline() throws NodeException {
			return online;
		}

		@Override
		public boolean isModified() throws NodeException {
			return formModified;
		}

		@Override
		public ContentNodeDate getPDate() {
			return pDate;
		}

		@Override
		public SystemUser getPublisher() throws NodeException {
			SystemUser publisher = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, publisherId);
			assertNodeObjectNotNull(publisher, publisherId, "Publisher", true);
			return publisher;
		}

		@Override
		public void publish(int at, boolean keepTimePubVersion) throws ReadOnlyException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser user = t.getObject(SystemUser.class, t.getUserId());
			int pdate = t.getUnixTimestamp();

			// remember the old online status
			boolean onlineBefore = isOnline();

			Map<String, Object> id = new HashMap<>();
			id.put("id", getId());

			if (at > 0) {
				NodeObjectVersion version = null;
				if (keepTimePubVersion) {
					version = getTimePubVersion();
				}
				if (version == null) {
					createFormVersion(this, true, false, null, t.getUnixTimestamp());
					version = getLastVersion();
				}

				Map<String, Object> data = new HashMap<>();
				data.put("time_pub", at);
				data.put("time_pub_version", version != null ? version.getId() : null);
				if (user != null) {
					data.put("publisher", user.getId());
					publisherId = getInteger(user.getId(), publisherId);
				}

				DBUtils.updateOrInsert("form", id, data);

				this.timePub = new ContentNodeDate(at);
				this.timePubVersion = version != null ? version.getId() : null;

				ActionLogger.logCmd(ActionLogger.PAGETIME, TYPE_FORM, getId(), getFolderId(), String.format("Form scheduled for publishing @%d", at));

				// we need to sent the NOTIFY event for the form in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			} else {
				Map<String, Object> data = new HashMap<>();
				data.put("online", 1);
				data.put("pdate", pdate);
				data.put("time_pub", 0);
				data.put("time_pub_version", null);
				this.timePub = new ContentNodeDate(0);
				this.timePubVersion = null;
				this.pDate = new ContentNodeDate(pdate);
				if (user != null) {
					data.put("publisher", user.getId());
					publisherId = getInteger(user.getId(), publisherId);
				}
				online = true;

				DBUtils.updateOrInsert("form", id, data);
				createFormVersion(this, true, true, t.getUserId(), pdate);
				ActionLogger.logCmd(ActionLogger.PAGEPUB, TYPE_FORM, getId(), getFolderId(), "Form published");

				// trigger event
				int eventMask = Events.EVENT_CN_PAGESTATUS;
				String[] props = null;
				if (!onlineBefore) {
					// form was offline before, so its "online" status changed (from off to on)
					props = new String[] { "online"};
					eventMask |= Events.UPDATE;
				}
				t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), props, eventMask));

				onPublish(this, onlineBefore, t.getUserId());
			}

			recalculateModifiedFlag();

			// handle the time management
			handleTimemanagement();

			unlock();

			// dirt the form cache
			t.dirtObjectCache(Form.class, getId());
		}

		@Override
		public void takeOffline(int at) throws ReadOnlyException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean onlineBefore = isOnline();

			Map<String, Object> id = new HashMap<>();
			id.put("id", getId());

			if (at > 0 && at >= t.getUnixTimestamp()) {
				Map<String, Object> data = new HashMap<>();
				data.put("time_off", at);
				timeOff = new ContentNodeDate(at);

				DBUtils.updateOrInsert("form", id, data);

				ActionLogger.logCmd(ActionLogger.PAGETIME, TYPE_FORM, getId(), getFolderId(), String.format("Form scheduled for taking offline @%d", at));
			} else {
				Map<String, Object> data = new HashMap<>();
				data.put("online", 0);
				online = false;
				DBUtils.updateOrInsert("form", id, data);

				ActionLogger.logCmd(ActionLogger.PAGEOFFLINE, TYPE_FORM, getId(), getFolderId(), "Form offline");

				String[] props = null;
				int eventMask = Events.EVENT_CN_PAGESTATUS;
				if (onlineBefore) {
					// form was online before, so its "online" status changed (from on to off)
					props = new String[] { "online"};
					eventMask |= Events.UPDATE;
				}
				if (DependencyManager.isDependencyTriggering()) {
					triggerEvent(new DependencyObject(this), props, eventMask, 0, 0);
				} else {
					t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), props, eventMask));
				}

				onTakeOffline(this, onlineBefore, t.getUserId());
			}

			// dirt the form cache
			t.dirtObjectCache(Form.class, getId());

			// we need to sent the NOTIFY event for the form in order to allow indexing (for feature ELASTICSEARCH)
			t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
		}

		@Override
		public boolean handleTimemanagement() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean handled = false;

			int transactionTime = t.getUnixTimestamp();
			int publishAtTime = getTimePub().getIntTimestamp();
			NodeObjectVersion timePubVersion = getTimePubVersion();
			int offlineAtTime = getTimeOff().getIntTimestamp();

			boolean publishAtDue = publishAtTime > 0 && publishAtTime <= transactionTime && timePubVersion != null;
			boolean offlineAtDue = offlineAtTime > 0 && offlineAtTime <= transactionTime;

			if (publishAtDue && offlineAtDue) {
				// determine whether to publish at or to take offline (whichever comes later)
				if (publishAtTime > offlineAtTime) {
					publishVersion(timePubVersion);
				} else {
					takeOffline();
				}
				internalClearTimePub();
				internalClearTimeOff();
				handled = true;
			} else if (publishAtDue) {
				publishVersion(timePubVersion);
				internalClearTimePub();
				handled = true;
			} else if (offlineAtDue) {
				takeOffline();
				internalClearTimeOff();
				handled = true;
			}

			if (handled) {
				// we need to sent the NOTIFY event for the form in order to allow indexing (for feature ELASTICSEARCH)
				t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			}

			return handled;
		}

		/**
		 * Set the given form version as published
		 * @param version version to publish
		 * @throws NodeException
		 */
		protected void publishVersion(NodeObjectVersion version) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			boolean wasOnline = isOnline();
			int newPdate = t.getUnixTimestamp();

			DBUtils.update("UPDATE nodeversion SET published = ? WHERE id = ?", 1, version.getId());
			DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND id != ?",
					new Object[] { Form.TYPE_FORM, getId(), version.getId() });

			DBUtils.update("UPDATE form SET online = ?, pdate = ?, publisher = ? WHERE id = ?", 1, newPdate,
					version.getEditor().getId(), getId());
			this.pDate = new ContentNodeDate(newPdate);
			this.publisherId = version.getEditor().getId();
			this.online = true;

			recalculateModifiedFlag();

			// trigger events
			String[] props = null;
			int eventMask = Events.EVENT_CN_PAGESTATUS;

			if (!wasOnline) {
				// form was offline before, so its "online" status changed (from off to on)
				props = new String[] { "online"};
				eventMask |= Events.UPDATE;
			}

			// trigger an event for the changed status
			if (DependencyManager.isDependencyTriggering()) {
				triggerEvent(new DependencyObject(this), props, eventMask, 0, 0);
			} else {
				t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), props, eventMask));
			}

			// dirt the form cache
			t.dirtObjectCache(Form.class, getId());

			onPublish(this, wasOnline, t.getUserId());
		}

		@Override
		public void clearTimePub() throws NodeException {
			internalClearTimePub();

			ActionLogger.logCmd(ActionLogger.PAGETIME, TYPE_FORM, getId(), getFolderId(),
					"Cleard scheduled publish at time");
			TransactionManager.getCurrentTransaction().addTransactional(
					new TransactionalTriggerEvent(Form.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
		}

		/**
		 * Clear the set time pub without logging in logcmd or triggering reindex
		 * @throws NodeException
		 */
		protected void internalClearTimePub() throws NodeException {
			timePub = new ContentNodeDate(0);
			timePubVersion = null;
			DBUtils.update("UPDATE form SET time_pub = ?, time_pub_version = ? WHERE id = ?", 0, null, getId());

			recalculateModifiedFlag();

			TransactionManager.getCurrentTransaction().dirtObjectCache(Form.class, getId());
		}

		@Override
		public void clearTimeOff() throws NodeException {
			internalClearTimeOff();

			ActionLogger.logCmd(ActionLogger.PAGETIME, TYPE_FORM, getId(), getFolderId(),
					"Cleard scheduled offline at time");
			TransactionManager.getCurrentTransaction().addTransactional(
					new TransactionalTriggerEvent(Form.class, getId(), INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
		}

		/**
		 * Clear the set time off without logging in logcmd or triggering reindex
		 * @throws NodeException
		 */
		protected void internalClearTimeOff() throws NodeException {
			timeOff = new ContentNodeDate(0);
			DBUtils.update("UPDATE form SET time_off = ? WHERE id = ?", 0, getId());
			TransactionManager.getCurrentTransaction().dirtObjectCache(Form.class, getId());
		}

		@Override
		public NodeObjectVersion[] getVersions() throws NodeException {
			if (versions == null) {
				versions = loadVersions();
			}
			return versions;
		}

		@Override
		public void restoreVersion(NodeObjectVersion toRestore) throws NodeException {
			NodeObjectVersion currentVersion = getVersion();

			Transaction t = TransactionManager.getCurrentTransaction();
			TableVersion formTableVersion = getFormTableVersion();
			formTableVersion.restoreVersion(Integer.toString(getId()), toRestore.getDate().getIntTimestamp());

			createFormVersion(this, false, false, t.getUserId(), t.getUnixTimestamp());
			recalculateModifiedFlag();

			t.dirtObjectCache(Form.class, getId());

			// get the version of the page
			NodeObjectVersion newVersion = getVersion();

			if (currentVersion == null || !currentVersion.getNumber().equals(newVersion.getNumber())) {
				ActionLogger.logCmd(ActionLogger.RESTORE, Form.TYPE_FORM, getId(), toRestore.getDate().getTimestamp(),
						"restored version " + toRestore.getNumber());
			}
		}

		@Override
		public void purgeOlderVersions(NodeObjectVersion oldestKeptVersion) throws NodeException {
			if (oldestKeptVersion == null) {
				throw new NodeException("Could not purge older versions, since no oldest version was given");
			}

			Long formId = ObjectTransformer.getLong(getId(), null);
			int versionTimestamp = oldestKeptVersion.getDate().getIntTimestamp();

			if (formId == null) {
				throw new NodeException("Error while purging versions for {" + this + "}: Could not get id");
			}

			// get the table version instances
			TableVersion formTableVersion = getFormTableVersion();

			// purge the versions
			formTableVersion.purgeVersions(formId, versionTimestamp);

			// remove the nodeversion entries
			DBUtils.executeUpdate("DELETE FROM nodeversion WHERE o_type = ? AND o_id = ? AND timestamp < ?",
					new Object[] { Form.TYPE_FORM, formId, versionTimestamp });

			clearVersions();
		}

		@Override
		public void clearVersions() {
			versions = null;
		}

		@Override
		public Folder getFolder() throws NodeException {
			Folder folder = TransactionManager.getCurrentTransaction().getObject(Folder.class, folderId);
			assertNodeObjectNotNull(folder, folderId, "Folder");
			return folder;
		}

		@Override
		public Integer getFolderId() throws NodeException {
			return folderId;
		}

		@Override
		public int getSuccessPageId() throws NodeException {
			return successPageId;
		}

		@Override
		public int getSuccessNodeId() throws NodeException {
			return successNodeId;
		}

		@Override
		public NodeObject getParentObject() throws NodeException {
			return getFolder();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public List<String> getLanguages() {
			return languages;
		}

		@Override
		public JsonNode getData() {
			return data;
		}

		@Override
		public ObjectNode getData(String language) throws NodeException {
			Objects.requireNonNull(language, "Form language must not be null");

			ObjectNode copy = data.deepCopy();
			convertI18n(copy, getI18nCodes(language));

			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			boolean handleDependencies = renderType != null ? renderType.doHandleDependencies() : false;
			boolean storeDependencies = renderType != null ? renderType.isStoreDependencies() : false;

			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, this, handleDependencies, false, false)) {
				// for the StaticUrlFactory, forbid auto detection of the linkway
				RenderUrlFactory renderUrlFactory = rTrx.get().getRenderUrlFactory();
				if (renderUrlFactory instanceof StaticUrlFactory) {
					((StaticUrlFactory) renderUrlFactory).setAllowAutoDetection(false);
				}

				renderReferencedPages(copy, language);

				// take over the collected dependencies
				if (handleDependencies && storeDependencies) {
					for (Dependency dep : rTrx.get().getDependencies()) {
						renderType.addDependency(dep.getSource(), dep.getSourceProperty());
					}
				}
			}

			// if an (existing) success page is set, render the URL (also doing language fallback)
			if (successPageId > 0) {
				try (ChannelTrx cTrx = new ChannelTrx(successNodeId)) {
					Page successPage = t.getObject(Page.class, successPageId);
					if (successPage != null) {
						successPage = PageLanguageFallbackList.doFallback(successPage, LanguageFactory.get(language),
								successPage.getOwningNode());
					}

					if (successPage != null) {
						// generate the url factory with a static host link
						StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_HOST, RenderUrl.LINK_HOST, null);

						// we don't want the linkway to be changed
						urlFactory.setAllowAutoDetection(false);

						// disable link management
						urlFactory.setLinkManagement(LinkManagement.OFF);

						try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, successPage, false, false, false)) {
							// Render the URL and set it as live URL
							RenderUrl renderUrl = urlFactory.createRenderUrl(Page.class, successPage.getId());

							copy.put("successurl", renderUrl.toString());
						}
					}
				}
			}
			copy.put("language", language);
			copy.put("name", getName());
			copy.put("cms_id", getId());
			return copy;
		}

		/**
		 * Get the i18n codes for translation in the fallback order
		 * @param language first language
		 * @return codes list
		 * @throws NodeException
		 */
		protected List<String> getI18nCodes(String language) throws NodeException {
			List<ContentLanguage> langs = getFolder().getOwningNode().getLanguages();

			List<String> codes = new ArrayList<>();
			codes.add(language);
			for (ContentLanguage lang : langs) {
				if (!codes.contains(lang.getCode())) {
					codes.add(lang.getCode());
				}
			}

			return codes;
		}

		/**
		 * Recursively convert all _i18n fields into the translation
		 * @param node current node
		 * @param codes language codes with correct fallback order
		 * @throws NodeException
		 */
		protected void convertI18n(JsonNode node, List<String> codes) throws NodeException {
			if (node.isObject()) {
				ObjectNode objectNode = (ObjectNode) node;
				Map<String, JsonNode> replace = new HashMap<>();
				for (Iterator<String> i = objectNode.fieldNames(); i.hasNext(); ) {
					String name = i.next();
					if (name.endsWith(I18N_POSTFIX)) {
						// get translation (with possible fallback)
						replace.put(name, getTranslation(objectNode.path(name), codes));
					} else {
						// recursion
						convertI18n(objectNode.get(name), codes);
					}
				}

				for (Map.Entry<String, JsonNode> entry : replace.entrySet()) {
					String oldName = entry.getKey();
					JsonNode translation = entry.getValue();
					String newName = oldName.substring(0, oldName.length() - I18N_POSTFIX.length());

					objectNode.remove(oldName);
					objectNode.set(newName, translation);
				}
			} else if (node.isArray()) {
				for (JsonNode sub : node) {
					convertI18n(sub, codes);
				}
			}
		}

		/**
		 * Recursively transform all _pageid fields into the rendered pages
		 * @param node current node
		 * @param language language
		 * @throws NodeException
		 */
		protected void renderReferencedPages(JsonNode node, String language) throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (node.isObject()) {
				ObjectNode objectNode = (ObjectNode) node;
				Map<String, String> replace = new HashMap<>();
				for (Iterator<String> i = objectNode.fieldNames(); i.hasNext();) {
					String name = i.next();
					if (name.endsWith(PAGEID_POSTFIX)) {
						int pageId = objectNode.path(name).asInt();
						try (PropertyTrx linkWayTrx = new PropertyTrx("contentnode.linkway", "host");
								PropertyTrx fileLinkWayTrx = new PropertyTrx("contentnode.linkway_file", "host");
								PublishCacheTrx pcTrx = new PublishCacheTrx(false)) {
							Page page = t.getObject(Page.class, pageId);
							if (page != null) {
								page = PageLanguageFallbackList.doFallback(page, LanguageFactory.get(language),
										page.getOwningNode());
							}

							if (page != null) {
								// render the page and put into replace map
								String renderedPage = page.render();
								replace.put(name, renderedPage);
							} else {
								replace.put(name, "");
							}
						}

					} else {
						// recursion
						renderReferencedPages(objectNode.get(name), language);
					}
				}

				// replace
				for (Map.Entry<String, String> entry : replace.entrySet()) {
					String oldName = entry.getKey();
					String renderedPage = entry.getValue();
					String newName = oldName.substring(0, oldName.length() - PAGEID_POSTFIX.length());

					objectNode.remove(oldName);
					objectNode.put(newName, renderedPage);
				}
			} else if (node.isArray()) {
				for (JsonNode sub : node) {
					renderReferencedPages(sub, language);
				}
			}
		}

		@Override
		public List<String> getIndexableContent(String language) throws NodeException {
			Objects.requireNonNull(language, "Form language must not be null");
			return collectI18n(data, getI18nCodes(language), new ArrayList<>());
		}

		/**
		 * Recursively collect translated content for indexing
		 * @param node current node
		 * @param codes list of language codes in fallback order
		 * @param i18n list of translated content (will be modified)
		 * @return i18n list
		 * @throws NodeException
		 */
		protected List<String> collectI18n(JsonNode node, List<String> codes, List<String> i18n) throws NodeException {
			if (node.isObject()) {
				ObjectNode objectNode = (ObjectNode) node;
				for (Iterator<String> i = objectNode.fieldNames(); i.hasNext();) {
					String name = i.next();
					if (name.endsWith(I18N_POSTFIX)) {
						// get translation (with possible fallback)
						JsonNode translationNode = getTranslation(objectNode.path(name), codes);
						if (translationNode != null && translationNode.isTextual()) {
							String translation = translationNode.asText();
							if (!org.apache.commons.lang3.StringUtils.isBlank(translation)) {
								i18n.add(translation);
							}
						}
					} else {
						// recursion
						collectI18n(objectNode.get(name), codes, i18n);
					}
				}
			} else if (node.isArray()) {
				for (JsonNode sub : node) {
					collectI18n(sub, codes, i18n);
				}
			}

			return i18n;
		}

		/**
		 * Get the translation
		 * @param i18n current i18n node
		 * @param codes language codes with fallback order
		 * @return translation (may be null)
		 * @throws NodeException
		 */
		protected JsonNode getTranslation(JsonNode i18n, List<String> codes) throws NodeException {
			if (i18n.isTextual()) {
				return i18n;
			}

			for (String code : codes) {
				JsonNode langNode = i18n.path(code);
				if (!langNode.isMissingNode()) {
					return langNode;
				}
			}

			return null;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);
			assertNodeObjectNotNull(creator, creatorId, "Creator");
			return creator;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);
			assertNodeObjectNotNull(editor, editorId, "Editor");
			return editor;
		}

		@Override
		public ContentNodeDate getTimePub() {
			return timePub;
		}

		@Override
		public NodeObjectVersion getTimePubVersion() throws NodeException {
			if (timePubVersion != null) {
				return getVersionById(timePubVersion);
			} else {
				return null;
			}
		}

		@Override
		public ContentNodeDate getTimeOff() {
			return timeOff;
		}

		@Override
		public boolean isDeleted() {
			return deleted > 0;
		}

		@Override
		public int getDeleted() {
			return deleted;
		}

		@Override
		public SystemUser getDeletedBy() throws NodeException {
			return TransactionManager.getCurrentTransaction().getObject(SystemUser.class, deletedBy);
		}

		@Override
		public boolean isLocked() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			int lockTimeout = ObjectTransformer.getInt(
					NodeConfigRuntimeConfiguration.getPreferences().getProperty("lock_time"),
					600);

			return (locked != 0 && (t.getUnixTimestamp() - locked) < lockTimeout);
		}

		@Override
		public ContentNodeDate getLockedSince() throws NodeException {
			if (isLocked()) {
				return new ContentNodeDate(locked);
			} else {
				return null;
			}
		}

		@Override
		public SystemUser getLockedBy() throws NodeException {
			if (isLocked()) {
				Transaction t = TransactionManager.getCurrentTransaction();

				return t.getObject(SystemUser.class, lockedBy);
			} else {
				return null;
			}
		}

		@Override
		public void unlock() throws NodeException {
			// nothing to do, if not locked
			if (!isLocked()) {
				return;
			}

			Transaction t = TransactionManager.getCurrentTransaction();
			if (lockedBy != t.getUserId()) {
				throw new ReadOnlyException(
						"Could not unlock {" + this + "} from user {" + t.getUserId()
								+ "}, since it is locked for user {" + lockedBy + "} since {" + locked + "}",
						"form.readonly.locked", Integer.toString(getId()));
			}

			locked = 0;
			lockedBy = 0;

			DBUtils.update("UPDATE form SET locked = ?, lockedby = ? WHERE id = ?", 0, 0, getId());

			t.dirtObjectCache(Form.class, getId());

			ActionLogger.logCmd(ActionLogger.UNLOCK, Form.TYPE_FORM, getId(), 0, "Form.unlock()");
		}

		@Override
		public Object get(String key) {
			try {
				switch (key) {
				case "name":
					return getName();
				case "description":
					return getDescription();
				case "cdate":
					return getCDate().getIntTimestamp();
				case "edate":
					return getEDate().getIntTimestamp();
				case "pdate":
					return getPDate().getIntTimestamp();
				case "creator":
					return getCreator();
				case "editor":
					return getEditor();
				case "publisher":
					return getPublisher();
				case "folder":
					return getFolder();
				default:
					return super.get(key);
				}
			} catch (NodeException e) {
				return null;
			}
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			// put page into wastebin
			if (!force && NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN, getFolder().getOwningNode())) {
				putIntoWastebin();

				onDelete(this, true, t.getUserId());

				return;
			}

			// delete by adding to the delete list
			Collection<Form> deleteList = ((FormFactory)t.getObjectFactory(Form.class)).getDeleteList(Form.class);
			deleteList.add(this);

			onDelete(this, false, t.getUserId());
		}

		@Override
		public void restore() throws NodeException {
			if (!isDeleted()) {
				return;
			}

			// we need to restore the folder of this form as well (if in wastebin)
			getFolder().restore();

			// restore the object
			Transaction t = TransactionManager.getCurrentTransaction();
			DBUtils.update("UPDATE form SET deleted = ?, deletedby = ? WHERE id = ?", 0, 0, getId());
			deleted = 0;
			deletedBy = 0;
			ActionLogger.logCmd(ActionLogger.WASTEBINRESTORE, Form.TYPE_FORM, getId(), null, "Form.restore()");
			t.dirtObjectCache(Form.class, getId(), true);
			t.addTransactional(new TransactionalTriggerEvent(this, null, Events.CREATE));

			// make the name unique
			Form editableForm = t.getObject(this, true);
			editableForm.save();
			editableForm.unlock();
		}

		@Override
		public String toString() {
			return String.format("Form {name: %s, id: %d}", getName(), getId());
		}

		/**
		 * Put form into wastebin
		 * @throws NodeException
		 */
		protected void putIntoWastebin() throws NodeException {
			if (isDeleted()) {
				return;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			// take form offline and mark as deleted
			DBUtils.update("UPDATE form SET online = ?, deleted = ?, deletedby = ? WHERE id = ?", 0, t.getUnixTimestamp(), t.getUserId(), getId());
			deleted = t.getUnixTimestamp();
			deletedBy = t.getUserId();

			ActionLogger.logCmd(ActionLogger.WASTEBIN, Form.TYPE_FORM, getId(), null, "Form.delete()");
			t.dirtObjectCache(Form.class, getId(), true);
			t.addTransactional(TransactionalTriggerEvent.deleteIntoWastebin(getFolder().getNode(), this));
		}

		/**
		 * Recalculate the modified flag, depending on whether the last version is the published version (or the version planned to be published)
		 * and whether there are unversioned changes
		 * @return true iff the modified flag was changed
		 * @throws NodeException
		 */
		protected boolean recalculateModifiedFlag() throws NodeException {
			this.versions = null;
			boolean newModified = formModified;
//			NodeObjectVersion timePubVersion = getTimePubVersion();
			NodeObjectVersion lastVersion = getLastVersion();
			NodeObjectVersion publishedPageVersion = getPublishedVersion();

			if (publishedPageVersion == null /* && timePubVersion == null */) {
				// when there is no published version at all (i.e. page is "new"), the page is never considered modified
				newModified = false;
			} else {
				if (lastVersion == null) {
					// when no last version exists, the page is not modified
					newModified = false;
				} else {
					// if the last version is not the published one, the page is considered modified
					newModified = !lastVersion.isPublished() /* && !lastVersion.equals(timePubVersion) */;
				}

				// if not yet considered modified, we check whether there is a diff between the last version and current version
				if (!newModified && lastVersion != null) {
					TableVersion formTableVersion = getFormTableVersion();

					if (!formTableVersion.getDiff(new Object[] {getId()}, lastVersion.getDate().getIntTimestamp(), -1).isEmpty()) {
						newModified = true;
					}
				}
			}

			if (formModified != newModified) {
				DBUtils.update("UPDATE form SET modified = ? WHERE id = ?", newModified, getId());
				formModified = newModified;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public void folderInheritanceChanged() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), FormFactory.INDEXED_MC_ATTRIBUTES, Events.NOTIFY));
		}

		@Override
		public NodeObject getPublishedObject() throws NodeException {
			// get the published version of the form
			Transaction t = TransactionManager.getCurrentTransaction();
			NodeObjectVersion publishedVersion = getPublishedVersion();

			// if no published version could be detected, return the current object
			if (publishedVersion == null) {
				return this;
			}

			// check whether this is the published version
			if (publishedVersion.getDate().getIntTimestamp() == getObjectInfo().getVersionTimestamp()) {
				return this;
			}

			return t.getObject(Form.class, getId(), publishedVersion.getDate().getIntTimestamp(), false);
		}

		@Override
		public List<ExtensiblePublishableObjectService<Form>> getServices() {
			return StreamSupport.stream(formFactoryServiceLoader.spliterator(), false).collect(Collectors.toList());
		}
	}

	private static class EditableFactoryForm extends FactoryForm {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 2830933005917334659L;

		/**
		 * Flag to mark whether the object has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Create an empty instance of an editable value
		 * @param info
		 */
		public EditableFactoryForm(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor to create a copy of the given form
		 * @param form form to copy
		 * @param info info about the copy
		 * @param asNew true when the content language shall be a new object, false for just the editable version of the object
		 */
		public EditableFactoryForm(Form form, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : form.getId(), info, getDataMap(form), asNew ? -1 : form.getUdate(), asNew ? null : form.getGlobalId());
			if (asNew) {
				this.modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.modified = true;
				this.name = name;
			}
		}

		@Override
		public void setDescription(String description) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.description, description)) {
				this.modified = true;
				this.description = description;
			}
		}

		@Override
		public void setFolderId(Integer folderId) throws NodeException, ReadOnlyException {
			// always set the folder id of the master folder
			Transaction t = TransactionManager.getCurrentTransaction();
			try (NoMcTrx nmt = new NoMcTrx()){
				Folder folder = t.getObject(Folder.class, folderId);
				folderId = folder.getMaster().getId();
			}

			if (ObjectTransformer.getInt(this.getFolderId(), 0) != ObjectTransformer.getInt(folderId, 0)) {
				this.folderId = folderId;
				modified = true;
			}
		}

		@Override
		public void setSuccessPageId(int successPageId) throws NodeException, ReadOnlyException {
			if (successPageId > 0) {
				// always set the page id of the master folder
				Transaction t = TransactionManager.getCurrentTransaction();
				try (NoMcTrx nmt = new NoMcTrx()){
					Page page = t.getObject(Page.class, successPageId);
					if (page != null) {
						successPageId = page.getMaster().getId();
					} else {
						successPageId = 0;
					}
				}
			}

			if (this.successPageId != successPageId) {
				this.successPageId = successPageId;
				modified = true;
			}
		}

		@Override
		public void setSuccessNodeId(int successNodeId) throws NodeException, ReadOnlyException {
			if (this.successNodeId != successNodeId) {
				this.successNodeId = successNodeId;
				modified = true;
			}
		}

		@Override
		public void setLanguages(List<String> languages) throws ReadOnlyException {
			if (!Objects.equals(this.languages, languages)) {
				this.modified = true;
				this.languages = languages;
			}
		}

		@Override
		public void setData(JsonNode data) throws ReadOnlyException {
			if (!Objects.equals(this.data, data)) {
				this.modified = true;
				this.data = data;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = false;
			boolean isNew = isEmptyId(getId());
			Form origForm = null;
			int origFolderId = -1;

			if (!isNew) {
				origForm = t.getObject(Form.class, getId());
				origFolderId = origForm.getFolderId();
			}

			name = ObjectTransformer.getString(name, "").trim();
			description = ObjectTransformer.getString(description, "");
			if (languages == null) {
				languages = new ArrayList<>();
			}
			if (data == null) {
				data = mapper.createObjectNode();
			}

			// make the name unique
			setName(UniquifyHelper.makeUnique(name, Form.MAX_NAME_LENGTH,
					"SELECT name FROM form WHERE id != ? AND folder_id = ? AND deleted = ? AND name = ?",
					SeparatorType.blank, ObjectTransformer.getInt(getId(), -1), folderId, 0));

			if (modified) {
				// object is modified, so update it
				isModified = true;

				// set creator data for new objects
				if (isNew) {
					creatorId = t.getUserId();
					cDate = new ContentNodeDate(t.getUnixTimestamp());

					// new forms will start locked
					locked = t.getUnixTimestamp();
					lockedBy = t.getUserId();
				}

				// set the editor data
				editorId = t.getUserId();
				eDate = new ContentNodeDate(t.getUnixTimestamp());

				saveFactoryObject(this);
				modified = false;
			}

			// logcmd, versions and trigger event
			if (isModified) {
				createFormVersion(this, false, false, t.getUserId(), t.getUnixTimestamp());
				recalculateModifiedFlag();
				List<String> modifiedData = new ArrayList<>();

				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, Form.TYPE_FORM, getId(), 0, "Form.create");
					t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), null, Events.CREATE));
				} else {
					String[] mod = getModifiedData(origForm, this);
					modifiedData.addAll(Arrays.asList(mod));

					ActionLogger.logCmd(ActionLogger.EDIT, Form.TYPE_FORM, getId(), 0, "Form.update");
					t.addTransactional(new TransactionalTriggerEvent(Form.class, getId(), mod, Events.UPDATE));

				}

				t.dirtObjectCache(Form.class, getId());
				if (isNew) {
					t.dirtObjectCache(Folder.class, getFolderId());
				} else if (getFolderId() != origFolderId) {
					// form was moved to another folder
					t.dirtObjectCache(Folder.class, origFolderId);
					t.dirtObjectCache(Folder.class, getFolderId());
				}

				onSave(this, isNew, true, t.getUserId());
			}

			return isModified;
		}
	}

	/**
	 * Create a version of the form (after it was saved)
	 * @param form form for which a version shall be created
	 * @param majorVersion true if the version shall be a major version, false if not
	 * @param publishedVersion true if the last version shall be marked as published version, false if not
	 * @param userId ID of the user for which the versions shall be created. If this is null, the transaction user will be used. If this also is null or zero, the last editor of the form will be used
	 * @param versionTimestamp timestamp for the version
	 * @return true iff a form version was created
	 * @throws NodeException
	 */
	public static boolean createFormVersion(Form form, boolean majorVersion, boolean publishedVersion, Integer userId, int versionTimestamp) throws NodeException {
		// only major version can be published versions
		if (!majorVersion) {
			publishedVersion = false;
		}
		Long formId = ObjectTransformer.getLong(form.getId(), null);

		if (formId == null) {
			throw new NodeException("Error while creating versions for {" + form + "}: Could not get id");
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		String user = ObjectTransformer.getString(t.getUserId(), "");

		if (userId != null) {
			user = ObjectTransformer.getString(userId, user);
		}

		if ("0".equals(user) || StringUtils.isEmpty(user)) {
			user = ObjectTransformer.getString(form.getEditor().getId(), user);
		}

		NodeObjectVersion[] formVersions = form.getVersions();

		// get an instance of the form version number generator
		PageVersionNumberGenerator gen = new DefaultPageVersionNumberGenerator();
		NodeObjectVersion lastVersion = null;

		if (!ObjectTransformer.isEmpty(formVersions)) {
			lastVersion = formVersions[0];
			// ensure that the new version does not have an older timestamp than the last version
			versionTimestamp = Math.max(versionTimestamp, lastVersion.getDate().getIntTimestamp());
		}

		// get the table version instance
		TableVersion formTableVersion = getFormTableVersion();

		// create the version
		boolean versionCreated = formTableVersion.createVersion2(formId, versionTimestamp, user);

		// when we did not create a new version, we check, whether a new version already exists
		if (!versionCreated && lastVersion != null) {
			int lastVersionTimestamp = 0;
			Version[] versions = formTableVersion.getVersions(formId);

			if (!ObjectTransformer.isEmpty(versions)) {
				lastVersionTimestamp = Math.max(lastVersionTimestamp, versions[versions.length - 1].getTimestamp());
			}

			// when we found newer entries in the versioned data, we also need to create a new nodeversion
			if (lastVersion.getDate().getIntTimestamp() < lastVersionTimestamp) {
				versionCreated = true;
			}
		}

		// when a version was created, also store the information in the
		// nodeversion table
		if (versionCreated && (lastVersion == null || lastVersion.getDate().getIntTimestamp() != versionTimestamp)) {
			String nextVersionNumber = lastVersion != null
					? gen.getNextVersionNumber(lastVersion.getNumber(), majorVersion)
					: gen.getFirstVersionNumber(majorVersion);
			DBUtils.update(
					"INSERT INTO nodeversion (timestamp, user_id, o_type, o_id, published, nodeversion) VALUES (?, ?, ?, ?, ?, ?)",
					versionTimestamp, Integer.parseInt(user), Form.TYPE_FORM, formId, publishedVersion,
					nextVersionNumber);

			ActionLogger.logCmd(majorVersion ? ActionLogger.MAJORVERSION : ActionLogger.VERSION, Form.TYPE_FORM, form.getId(), 0,
					"created version " + nextVersionNumber);

			// if the created nodeversion shall be the published one, mark all others as "not published"
			if (publishedVersion) {
				DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND timestamp != ?",
						new Object[] { Form.TYPE_FORM, formId, versionTimestamp });
			}
		} else if (majorVersion) {
			if (lastVersion != null && !lastVersion.isMajor()) {
				// generate next major version
				String nextVersionNumber = gen.makePublishedVersion(lastVersion.getNumber());

				DBUtils.updateWithPK("nodeversion", "id", "nodeversion = ?", new Object[] { nextVersionNumber },
						"o_type = ? AND o_id = ? AND timestamp = ?", new Object[] { Form.TYPE_FORM, formId, lastVersion.getDate().getIntTimestamp() });

				ActionLogger.logCmd(ActionLogger.MAJORVERSION, Form.TYPE_FORM, form.getId(), 0,
						"updated version " + lastVersion.getNumber() + " to " + nextVersionNumber);

				versionCreated = true;
			}
			if (publishedVersion && lastVersion != null && !lastVersion.isPublished()) {
				// mark version as published
				DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 1 },
						"o_type = ? AND o_id = ? AND timestamp = ?", new Object[] { Form.TYPE_FORM, formId, lastVersion.getDate().getIntTimestamp() });

				// mark all other versions to not be the published one
				DBUtils.updateWithPK("nodeversion", "id", "published = ?", new Object[] { 0 }, "o_type = ? AND o_id = ? AND timestamp != ?",
						new Object[] { Form.TYPE_FORM, formId, lastVersion.getDate().getIntTimestamp() });

				versionCreated = true;
			}
		}

		form.clearVersions();

		return versionCreated;
	}

	/**
	 * Handle time management for forms
	 * @throws NodeException
	 */
	public static void doTimeManagement() throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		final int time = t.getUnixTimestamp();
		final Set<Integer> formIds = new HashSet<>();

		// get all forms that are due to publish at a certain timestamp
		formIds.addAll(DBUtils.select("SELECT id FROM form WHERE time_pub > ? AND time_pub <= ? AND time_pub_version IS NOT NULL AND deleted = ?", ps -> {
			ps.setInt(1, 0);
			ps.setInt(2, time);
			ps.setInt(3, 0);
		}, DBUtils.IDS));

		// get all forms that are due to take offline at a certain timestamp
		formIds.addAll(DBUtils.select("SELECT id FROM form WHERE time_off > ? AND time_off <= ? AND deleted = ?", ps -> {
			ps.setInt(1, 0);
			ps.setInt(2, time);
			ps.setInt(3, 0);
		}, DBUtils.IDS));

		try {
			List<Form> forms = t.getObjects(Form.class, formIds);
			DependencyManager.initDependencyTriggering();
			for (Form form : forms) {
				form.handleTimemanagement();
			}
			PublishQueue.finishFastDependencyDirting();
		} finally {
			try {
				t.commit(false);
			} catch (TransactionException e) {
				logger.error("Couldn't Transaction.commit(false)", e);
			}
		}
	}

	/**
	 * Get the table version object for forms
	 * @return table version object for forms
	 * @throws NodeException
	 */
	private static TableVersion getFormTableVersion() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		TableVersion formVersion = new TableVersion(false);

		formVersion.setAutoIncrement(true);
		formVersion.setHandle(t.getDBHandle());
		formVersion.setTable("form");
		formVersion.setWherePart("gentics_main.id = ?");
		return formVersion;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		if (Form.class.equals(clazz)) {
			return (T) new EditableFactoryForm(handle.createObjectInfo(Form.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs,
			List<Integer>[] idLists) throws SQLException, NodeException {
		return (T) new FactoryForm(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info)
			throws NodeException, ReadOnlyException {
		if (object instanceof Form) {
			Transaction t = TransactionManager.getCurrentTransaction();
			int lockTimeout = ObjectTransformer.getInt(
					NodeConfigRuntimeConfiguration.getPreferences().getProperty("lock_time"), 600);
			int now = (int)(System.currentTimeMillis()/1000);

			// check whether the form is locked
			Pair<Integer, Integer> lockInfo = DBUtils.select("SELECT locked, lockedby FROM form WHERE id = ? FOR UPDATE", st -> st.setInt(1, object.getId()), rs -> {
				if (rs.next()) {
					return Pair.of(rs.getInt("locked"), rs.getInt("lockedby"));
				}
				return Pair.of(0, 0);
			});

			int lockTime = lockInfo.getLeft();
			int lockedBy = lockInfo.getRight();
			boolean locked = false;
			boolean alreadyLocked = false;

			if (lockTime != 0 && lockedBy > 0 && (now - lockTime) < lockTimeout
					&& (t.getUserId() != lockedBy)) {
				locked = true;
			} else if (lockedBy == t.getUserId() && lockTime != 0 && (now - lockTime) < lockTimeout) {
				alreadyLocked = true;
			}

			// if not, lock
			if (!locked) {
				// not locked, lock now
				DBUtils.update("UPDATE form SET locked = ?, lockedby = ? WHERE id = ?", now, t.getUserId(), object.getId());

				t.dirtObjectCache(Form.class, object.getId());
				if (!alreadyLocked) {
					ActionLogger.logCmd(ActionLogger.LOCK, Form.TYPE_FORM, object.getId(), 0, "Form.lock()");
				}
			} else {
				throw new ReadOnlyException(
						"Could not lock {" + object + "} for user {" + t.getUserId()
								+ "}, since it is locked for user {" + lockedBy + "} since {" + lockTime + "}",
						"form.readonly.locked", Integer.toString(object.getId()));
			}

			EditableFactoryForm editableCopy = new EditableFactoryForm(t.getObject((Form) object), info, false);
			return (T) editableCopy;
		} else {
			return null;
		}
	}

	@Override
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, Form.class)) {
			Collection<Form> deleted = getDeleteList(Form.class);

			for (Form form : deleted) {
				// add logcmd
				ActionLogger.logCmd(ActionLogger.DEL, Form.TYPE_FORM, form.getId(), 0, "Form.delete");
				Events.trigger(form, new String[] { ObjectTransformer.getString(form.getOwningNode().getId(), ""),
						MeshPublisher.getMeshUuid(form), MeshPublisher.getMeshLanguage(form) }, Events.DELETE);
			}

			// delete the forms with their versions
			List<Integer> ids = Flowable.fromIterable(deleted).map(Form::getId).toList().blockingGet();
			flushDelete("DELETE FROM form WHERE id IN", Form.class);
			DBUtils.executeMassStatement("DELETE FROM nodeversion WHERE o_type = ? AND o_id IN", ids, 2, new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, Form.TYPE_FORM);
				}
			});
			DBUtils.executeMassStatement("DELETE FROM form_nodeversion WHERE id IN", ids, 1, null);
		}
	}
}
