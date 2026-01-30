/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: LanguageFactory.java,v 1.12.2.1 2011-01-18 13:21:53 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.UniquifyHelper.SeparatorType;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.AbstractContentLanguage;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * An objectfactory to create {@link ContentLanguage} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = ContentLanguage.class, name = "contentgroup") })
public class LanguageFactory extends AbstractFactory {
	/**
	 * SQL Statement to select a single contentgroup
	 */
	protected final static String SELECT_CONTENTGROUP_SQL = createSelectStatement("contentgroup");

	/**
	 * SQL Statement for batchloading contentgroups
	 */
	protected final static String BATCHLOAD_CONTENTGROUP_SQL = createBatchLoadStatement("contentgroup");

	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.CONTENT_LANGUAGE, ContentLanguage.TYPE_CONTENTLANGUAGE, true, FactoryContentLanguage.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Get a map of all existing languages (keys are the language codes)
	 * @return language map
	 * @throws NodeException
	 */
	public static Map<String, ContentLanguage> languagesPerCode() throws NodeException {
		// TODO caching
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObjects(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS)).stream()
				.collect(Collectors.toMap(ContentLanguage::getCode, Function.identity()));
	}

	/**
	 * Get the language with the given code
	 * @param code language code
	 * @return language or null, if not found
	 * @throws NodeException
	 */
	public static ContentLanguage get(String code) throws NodeException {
		return languagesPerCode().get(code);
	}

	/**
	 * Factory Implementation of ContentLanguage
	 */
	private static class FactoryContentLanguage extends AbstractContentLanguage {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -3957289497913366856L;

		@DataField("name")
		@Updateable
		protected String name;

		@DataField("code")
		@Updateable
		protected String code;

		private List<Integer> nodeIds;

		/**
		 * Create an empty instance
		 * @param info info
		 */
		protected FactoryContentLanguage(NodeObjectInfo info) {
			super(null, info);
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
		public FactoryContentLanguage(Integer id, NodeObjectInfo info,
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

		public String getName() {
			return name;
		}

		public String getCode() {
			return code;
		}

		public String toString() {
			return "ContentLanguage {" + getId() + "}";
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.render.Renderable#render()
		 */
		public String render() throws NodeException {
			return render(TransactionManager.getCurrentTransaction().getRenderResult());
		}

		public String render(RenderResult renderResult) throws NodeException {
			return getName();
		}

		protected synchronized List<Integer> loadNodeIds() throws NodeException {
			if (nodeIds == null) {
				if (isEmptyId(getId())) {
					nodeIds = new ArrayList<>();
				} else {
					nodeIds = DBUtils.select("SELECT node_id id FROM node_contentgroup WHERE contentgroup_id = ?",
							ps -> ps.setInt(1, getId()), DBUtils.IDLIST);
				}
			}
			return nodeIds;
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObjects(Node.class, loadNodeIds());
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryContentLanguage(this, getFactory().getFactoryHandle(ContentLanguage.class).createObjectInfo(ContentLanguage.class, true),
					true);
		}

		@Override
		public List<Page> getPages() throws NodeException {
			final List<Integer> pageIds = new Vector<Integer>();

			DBUtils.executeStatement("SELECT id FROM page WHERE contentgroup_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setObject(1, getId());
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						pageIds.add(rs.getInt("id"));
					}
				}
			});
			return TransactionManager.getCurrentTransaction().getObjects(Page.class, pageIds);
		}

		@Override
		public void delete(boolean force) throws InsufficientPrivilegesException,
					NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();
			LanguageFactory langFactory = (LanguageFactory) t.getObjectFactory(ContentLanguage.class);

			langFactory.deleteLanguage(this);
		}
	}

	/**
	 * Implementation of Editable Content Language
	 */
	private static class EditableFactoryContentLanguage extends FactoryContentLanguage {

		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = 3883369594659675030L;

		/**
		 * Flag to mark whether the object has been modified (contains changes which need to be persisted by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * List of nodes
		 */
		protected List<Node> nodes;

		/**
		 * Create an empty instance of an editable value
		 * @param info
		 */
		public EditableFactoryContentLanguage(NodeObjectInfo info) {
			super(info);
			modified = true;
		}

		/**
		 * Constructor to create a copy of the given content language
		 * @param lang content language to copy
		 * @param info info about the copy
		 * @param asNew true when the content language shall be a new object, false for just the editable version of the object
		 */
		public EditableFactoryContentLanguage(FactoryContentLanguage lang, NodeObjectInfo info, boolean asNew) throws NodeException {
			super(asNew ? null : lang.getId(), info, getDataMap(lang), asNew ? -1 : lang.getUdate(), asNew ? null : lang.getGlobalId());
			if (asNew) {
				this.modified = true;
			}
		}

		@Override
		public void setCode(String code) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.code, code)) {
				this.code = code;
				this.modified = true;
			}
		}

		@Override
		public void setName(String name) throws ReadOnlyException {
			if (!StringUtils.isEqual(this.name, name)) {
				this.name = name;
				this.modified = true;
			}
		}

		@Override
		public List<Node> getNodes() throws NodeException {
			if (nodes == null) {
				nodes = new Vector<Node>(super.getNodes());
			}
			return nodes;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#save()
		 */
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			assertEditable();
			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = false;
			boolean isNew = isEmptyId(getId());
			ContentLanguage origLang = null;

			if (!isNew) {
				origLang = t.getObject(ContentLanguage.class, getId());
			}

			// make the name unique
			setName(UniquifyHelper.makeUnique(name, ContentLanguage.MAX_NAME_LENGTH, "SELECT name FROM contentgroup WHERE id != ? AND name = ?",
					SeparatorType.blank, ObjectTransformer.getInt(getId(), -1)));
			// make the code unique
			setCode(UniquifyHelper.makeUnique(code, ContentLanguage.MAX_CODE_LENGTH, "SELECT code FROM contentgroup WHERE id != ? AND code = ?",
					ObjectTransformer.getInt(getId(), -1)));

			if (modified) {
				// object is modified, so update it
				isModified = true;
				saveFactoryObject(this);
				modified = false;
			}

			// change node assignment
			List<Integer> currentAssignedNodes = loadNodeIds();
			List<Node> newAssignedNodes = getNodes();
			List<Integer> toAssign = new Vector<Integer>();
			List<Integer> toUnassign = new Vector<Integer>(currentAssignedNodes);
			List<Integer> toDirt = new Vector<Integer>();

			for (Node node : newAssignedNodes) {
				if (!currentAssignedNodes.contains(node.getId())) {
					// node must be assigned, but is not
					Integer nodeId = ObjectTransformer.getInteger(node.getId(), null);

					toAssign.add(nodeId);
					toDirt.add(nodeId);
				}
				// node must not be unassigned
				toUnassign.remove(ObjectTransformer.getInteger(node.getId(), null));
			}

			if (!toUnassign.isEmpty()) {
				toDirt.addAll(toUnassign);
				DBUtils.executeMassStatement("DELETE FROM node_contentgroup WHERE contentgroup_id = ? AND node_id IN", toUnassign, 2, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, ObjectTransformer.getInt(getId(), -1));
					}
				});
			}
			if (!toAssign.isEmpty()) {
				for (Integer id : toAssign) {
					DBUtils.executeInsert("INSERT INTO node_contentgroup (contentgroup_id, node_id) VALUES (?, ?)", new Object[] { getId(), id });
				}
			}

			// logcmd and trigger event
			if (isModified) {
				if (isNew) {
					ActionLogger.logCmd(ActionLogger.CREATE, TYPE_CONTENTLANGUAGE, getId(), 0, "ContentLanguage.create");
					t.addTransactional(new TransactionalTriggerEvent(ContentLanguage.class, getId(), null, Events.CREATE));
				} else {
					ActionLogger.logCmd(ActionLogger.EDIT, TYPE_CONTENTLANGUAGE, getId(), 0, "ContentLanguage.update");
					t.addTransactional(new TransactionalTriggerEvent(ContentLanguage.class, getId(), getModifiedData(origLang, this), Events.UPDATE));
				}
			}

			if (!toDirt.isEmpty()) {
				for (Integer nId : toDirt) {
					t.dirtObjectCache(Node.class, nId);
				}
			}

			if (isModified) {
				t.dirtObjectCache(ContentLanguage.class, getId());
			}

			return isModified;
		}
	}

	public LanguageFactory() {
		super();
	}

	/**
	 * Delete the given language
	 * @param lang language to delete
	 */
	public void deleteLanguage(FactoryContentLanguage lang) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Collection deleteList = getDeleteList(t, ContentLanguage.class);

		deleteList.add(lang);
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, SELECT_CONTENTGROUP_SQL, null, null);
	}

	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_CONTENTGROUP_SQL);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.ObjectFactory#createObject(com.gentics.lib.base.factory.FactoryHandle, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		if (ContentLanguage.class.equals(clazz)) {
			return (T) new EditableFactoryContentLanguage(handle.createObjectInfo(ContentLanguage.class, true));
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id,
			NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws NodeException {
		return (T) new FactoryContentLanguage(id, info, rs.getValues(), getUdate(rs), getGlobalId(rs, "contentgroup"));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#flush()
	 */
	public void flush() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!isEmptyDeleteList(t, ContentLanguage.class)) {
			// get the deleted pages
			AbstractFactory pageFactory = (AbstractFactory) t.getObjectFactory(Page.class);
			Collection<Page> deletedPages = pageFactory.getDeleteList(t, Page.class);

			// check whether a language is still used by a page, that is not deleted
			Collection<ContentLanguage> deleteLangs = getDeleteList(t, ContentLanguage.class);

			for (ContentLanguage lang : deleteLangs) {
				List<Page> pages = lang.getPages();

				for (Page page : pages) {
					if (!deletedPages.contains(page)) {
						throw new NodeException("Cannot delete language " + lang + ", it is still used at least by the page " + page + " which is not deleted");
					}
				}

				ActionLogger.logCmd(ActionLogger.DEL, ContentLanguage.TYPE_CONTENTLANGUAGE, lang.getId(), 0, "ContentLanguage.delete");
			}

			// delete the assignments to nodes and the languages
			flushDelete("DELETE FROM node_contentgroup WHERE contentgroup_id IN", ContentLanguage.class);
			flushDelete("DELETE FROM contentgroup WHERE id IN", ContentLanguage.class);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}

		if (object instanceof FactoryContentLanguage) {
			return (T) new EditableFactoryContentLanguage((FactoryContentLanguage) object, info, false);
		} else {
			throw new NodeException("LanguageFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}
}
