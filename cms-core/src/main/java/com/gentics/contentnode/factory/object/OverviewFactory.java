/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: OverviewFactory.java,v 1.17.2.1 2011-01-18 13:21:53 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * An objectfactory which can create {@link Overview} and {@link OverviewEntry} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = Overview.class, name = "ds"),
	@DBTable(clazz = OverviewEntry.class, name = "ds_obj") })
public class OverviewFactory extends AbstractFactory {

	/**
	 * SQL Statement to select ds data
	 */
	protected final static String SELECT_DS_SQL = createSelectStatement("ds");

	/**
	 * SQL Statement to select versioned ds data
	 */
	protected final static String SELECT_VERSIONED_DS_SQL = "SELECT * FROM ds_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM ds_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Statement for batchloading ds
	 */
	protected final static String BATCHLOAD_DS_SQL = createBatchLoadStatement("ds");

	/**
	 * SQL Params for the selection of versioned ds data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_DS_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * First part of the SQL Statement for inserting a ds
	 */
	protected final static String INSERT_DS_WO_PARAMS_SQL = "INSERT INTO ds (templatetag_id, contenttag_id, objtag_id, o_type, is_folder, orderkind, orderway, max_obj, recursion, uuid) VALUES ";

	/**
	 * Second part of the SQL Statement for inserting a ds
	 */
	protected final static String INSERT_DS_PARAMS_SQL = "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement for inserting a ds
	 */
	protected final static String INSERT_DS_SQL = "%s%s".formatted(INSERT_DS_WO_PARAMS_SQL, INSERT_DS_PARAMS_SQL);

	/**
	 * SQL Statement for updating a ds
	 */
	protected final static String UPDATE_DS_SQL = "UPDATE ds SET templatetag_id = ?, contenttag_id = ?, objtag_id = ?, o_type = ?, is_folder = ?, orderkind = ?, orderway = ?, max_obj = ?, recursion = ? WHERE id = ?";

	/**
	 * SQL Statement to select ds_obj data
	 */
	protected final static String SELECT_DS_OBJ_SQL = createSelectStatement("ds_obj");

	/**
	 * SQL Statement to select versioned ds_obj data
	 */
	protected final static String SELECT_VERSIONED_DS_OBJ_SQL = "SELECT * FROM ds_obj_nodeversion WHERE id = ? AND nodeversiontimestamp = "
			+ "(SELECT MAX(nodeversiontimestamp) FROM ds_obj_nodeversion "
			+ "WHERE (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? AND id = ? GROUP BY id)";

	/**
	 * SQL Statement for batchloading ds_obj
	 */
	protected final static String BATCHLOAD_DS_OBJ_SQL = createBatchLoadStatement("ds_obj");

	/**
	 * SQL Params for the selection of versioned ds_obj data
	 */
	protected final static VersionedSQLParam[] SELECT_VERSIONED_DS_OBJ_PARAMS = {
		VersionedSQLParam.ID, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.VERSIONTIMESTAMP, VersionedSQLParam.ID};

	/**
	 * First part of the SQL Statement for inserting a ds entry
	 */
	protected final static String INSERT_DS_OBJ_WO_PARAMS_SQL = "INSERT INTO ds_obj (templatetag_id, contenttag_id, objtag_id, ds_id, o_id, node_id, obj_order, auser, adate) VALUES ";

	/**
	 * Second part of the SQL Statement for inserting a ds entry
	 */
	protected final static String INSERT_DS_OBJ_PARAMS_SQL = "(?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * SQL Statement for inserting a ds entry
	 */
	protected final static String INSERT_DS_OBJ_SQL = "%s%s".formatted(INSERT_DS_OBJ_WO_PARAMS_SQL, INSERT_DS_OBJ_PARAMS_SQL);

	/**
	 * SQL Statement for updating a ds entry
	 */
	protected final static String UPDATE_DS_OBJ_SQL = "UPDATE ds_obj SET templatetag_id = ?, contenttag_id = ?, objtag_id = ?, ds_id = ?, o_id = ?, node_id = ?, obj_order = ?, auser = ?, adate = ? WHERE id = ?";

	private static class FactoryOverview extends Overview {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = 8856241259694658719L;

		protected int selectionType;
		protected Class objClass;
		protected int orderKind;
		protected int orderWay;
		protected int maxObjects;
		protected boolean recursion;
		protected Class containerClass;
		protected Integer containerId;

		protected List<Integer> entryIds;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		public FactoryOverview(NodeObjectInfo info) {
			super(null, info);
		}

		public FactoryOverview(Integer id, NodeObjectInfo info, int selectionType, Class objClass,
				int orderKind, int orderWay, int maxObjects, boolean recursion,
				Class containerClass, Integer containerId, int udate, GlobalId globalId) {
			super(id, info);
			this.selectionType = selectionType;
			this.objClass = objClass;
			this.orderKind = orderKind;
			this.orderWay = orderWay;
			this.maxObjects = maxObjects;
			this.recursion = recursion;
			this.containerClass = containerClass;
			this.containerId = containerId;
			this.udate = udate;
			this.globalId = globalId;
		}

		public int getSelectionType() {
			return selectionType;
		}

		public Class getObjectClass() {
			return objClass;
		}

		@Override
		public int getObjectType() throws NodeException {
			if (objClass == null) {
				return 0;
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getTType(objClass);
		}

		public int getOrderKind() {
			return orderKind;
		}

		public int getOrderWay() {
			return orderWay;
		}

		public int getMaxObjects() {
			return maxObjects;
		}

		public boolean doRecursion() {
			return recursion;
		}

		public Tag getContainer() throws NodeException {
			if (containerId != null && Tag.class.isAssignableFrom(containerClass)) {
				return (Tag) TransactionManager.getCurrentTransaction().getObject(containerClass, containerId);
			} else {
				return null;
			}
		}

		public Value getValue() throws NodeException {
			// this is quite a hack, but that's the way IT works ..
			Tag container = getContainer();

			if (null == container) {
				return null;
			}
			ValueList values = container.getValues();

			for (Value value : values) {
				if (value.getPart().getPartTypeId() == OverviewPartType.TYPE_ID) {
					// unlinked; this.value = value;
					// break;
					return value;
				}
			}

			return null;
		}

		public List<OverviewEntry> getOverviewEntries() throws NodeException {
			return loadDsObjects();
		}

		/**
		 * Check whether object must be sorted after fetching them from the database
		 * @return true if object must be sorted manually, false if not
		 */
		private boolean isPostSort() {
			return !getObjectInfo().isCurrentVersion() && ContentTag.class.equals(containerClass);
		}

		/**
		 * Load the Ids of the entry objects.
		 * @throws NodeException
		 */
		private synchronized void loadDsObjectIds() throws NodeException {
			if (entryIds == null) {
				if (containerId == null) {
					// if the container is new, we don't have entries yet
					entryIds = new ArrayList<>();
				} else {
					// get the order way
					String orderWay = getOrderWay() == ORDERWAY_DESC ? " DESC" : " ASC";

					String type;

					if (ContentTag.class.equals(containerClass)) {
						type = "contenttag";
					} else if (TemplateTag.class.equals(containerClass)) {
						type = "templatetag";
					} else if (ObjectTag.class.equals(containerClass)) {
						type = "objtag";
					} else {
						entryIds = Collections.emptyList();
						return;
					}

					// TODO check fallback when ds is defined in parent and not editable.

					if (getObjectInfo().isCurrentVersion() || !"contenttag".equals(type)) {
						// for some mysterious reasons, ds_id seems not to be the id of the ds-object
						entryIds = DBUtils.select("SELECT id FROM ds_obj WHERE " + type + "_id = ? ORDER BY obj_order " + orderWay, ps -> ps.setInt(1, containerId), DBUtils.IDLIST);
					} else {
						// for contenttags, the entries may be versioned
						int versionTimestamp = getObjectInfo().getVersionTimestamp();

						entryIds = DBUtils.select("SELECT id FROM ds_obj_nodeversion WHERE " + type
								+ "_id = ? AND (nodeversionremoved > ? OR nodeversionremoved = 0) AND nodeversiontimestamp <= ? GROUP BY id", ps -> {
									ps.setInt(1, containerId);
									ps.setInt(2, versionTimestamp);
									ps.setInt(3, versionTimestamp);
								}, DBUtils.IDLIST);
					}
				}
			}
		}

		private List<OverviewEntry> loadDsObjects() throws NodeException {
			// load the entry ids (if not done before)
			loadDsObjectIds();

			Transaction t = TransactionManager.getCurrentTransaction();

			try {
				List<OverviewEntry> objects = null;

				objects = getObjectInfo().isEditable()
						? t.getObjects(OverviewEntry.class, entryIds, getObjectInfo().isEditable())
						: t.getObjects(OverviewEntry.class, entryIds, getObjectInfo().getVersionTimestamp());
				if (objects.size() > 1 && isPostSort()) {
					Collections.sort(objects, new Comparator<OverviewEntry>() {
						public int compare(OverviewEntry e1, OverviewEntry e2) {
							if (getOrderWay() == ORDERWAY_DESC) {
								return e2.getObjectOrder() - e1.getObjectOrder();
							} else {
								return e1.getObjectOrder() - e2.getObjectOrder();
							}
						}
					});
				}

				// now fix the ds_id value in the overview entries (because in the database, the ds_id does not point to the correct ds entry)
				for (OverviewEntry entry : objects) {
					FactoryOverviewEntry fEntry = (FactoryOverviewEntry) entry;

					fEntry.dsId = getId();
				}

				return objects;
			} catch (TransactionException e) {
				throw new NodeException("Error while loading overview objects", e);
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Overview {" + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryOverview(this, getFactory().getFactoryHandle(Overview.class).createObjectInfo(Overview.class, true), true);
		}

		// when dirting an overview, its entries have to be cleared from the cache as well to allow sort-order to be updated
		public void dirtCache() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			loadDsObjectIds();
			// get the values now, this will make sure that it is not necessary to load the values in single sql statements
			t.getObjects(OverviewEntry.class, entryIds);

			for (Integer entryId : entryIds) {
				t.dirtObjectCache(OverviewEntry.class, entryId, false);
			}
		}
	}

	/**
	 * Editable instance of a factory overview
	 */
	private static class EditableFactoryOverview extends FactoryOverview {

		/**
		 * Flag to mark whether the page has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * editable copies of the overview entries in this overview
		 */
		private List<OverviewEntry> overviewEntries = null;

		/**
		 * Create an empty, editable instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactoryOverview(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
			maxObjects = 0;
			orderWay = ORDERWAY_UNDEFINED;
			orderKind = ORDER_UNDEFINED;
			selectionType = SELECTIONTYPE_UNDEFINED;
			objClass = null;
		}

		/**
		 * Create an editable copy of the given overview
		 * @param overview overview
		 * @param info object info
		 * @param asNew true if the overview shall be a real copy (new object), false for an editable copy of the given overview
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactoryOverview(FactoryOverview overview, NodeObjectInfo info, boolean asNew) throws ReadOnlyException, NodeException {
			super(asNew ? null : overview.getId(), info, overview.getSelectionType(), overview.getObjectClass(), overview.getOrderKind(), overview.getOrderWay(),
					overview.getMaxObjects(), overview.doRecursion(), asNew ? null : overview.containerClass, asNew ? null : overview.containerId,
					asNew ? -1 : overview.getUdate(), asNew ? null : overview.getGlobalId());
			if (asNew) {
				// TODO copy the overview entries
				modified = true;
			}
		}

		@Override
		public List<OverviewEntry> getOverviewEntries() throws NodeException {
			if (overviewEntries == null) {
				overviewEntries = new Vector<OverviewEntry>(super.getOverviewEntries());
			}
			return overviewEntries;
		}

		@Override
		public void setContainer(Tag container) throws ReadOnlyException {
			assertEditable();
			Integer newContainerId = null;
			Class<? extends NodeObject> newContainerClass = null;

			if (container != null) {
				newContainerId = container.getId();
				newContainerClass = container.getObjectInfo().getObjectClass();
			}

			if (containerClass != newContainerClass || ObjectTransformer.getInt(containerId, 0) != ObjectTransformer.getInt(newContainerId, 0)) {
				// set the new container
				containerClass = newContainerClass;
				containerId = newContainerId;
				modified = true;
			}
		}

		@Override
		public void setMaxObjects(int maxObjects) throws ReadOnlyException {
			assertEditable();
			if (this.maxObjects != maxObjects) {
				this.maxObjects = maxObjects;
				modified = true;
			}
		}

		@Override
		public void setObjectType(int ttype) throws ReadOnlyException,
					NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			setObjectClass(t.getClass(ttype));
		}

		@Override
		public void setObjectClass(Class<? extends NodeObject> clazz) throws ReadOnlyException {
			assertEditable();
			if (this.objClass != clazz) {
				this.objClass = clazz;
				modified = true;
			}
		}

		@Override
		public void setOrderKind(int orderKind) throws ReadOnlyException {
			assertEditable();
			if (this.orderKind != orderKind) {
				this.orderKind = orderKind;
				modified = true;
			}
		}

		@Override
		public void setOrderWay(int orderWay) throws ReadOnlyException {
			assertEditable();
			if (this.orderWay != orderWay) {
				this.orderWay = orderWay;
				modified = true;
			}
		}

		@Override
		public void setRecursion(boolean recursion) throws ReadOnlyException {
			assertEditable();
			if (this.recursion != recursion) {
				this.recursion = recursion;
				modified = true;
			}
		}

		@Override
		public void setSelectionType(int selectionType) throws ReadOnlyException {
			assertEditable();
			if (this.selectionType != selectionType) {
				this.selectionType = selectionType;
				modified = true;
			}
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			return saveBatch(null, null, null);
		}

		@Override
		public boolean saveBatch(BatchUpdater batchUpdater, Operator before, Operator after) throws InsufficientPrivilegesException,
					NodeException {
			// first check whether the overview is editable
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = this.modified;

			// save the overview, if necessary
			if (isModified) {
				saveOverviewObject(this, batchUpdater, before, after);
				this.modified = false;
			}

			// save all overview entries and remove those that no longer exist
			List<OverviewEntry> entries = getOverviewEntries();

			List<Object> entryIdsToRemove = new Vector<Object>();

			if (entryIds != null) {
				entryIdsToRemove.addAll(entryIds);
			}
			for (OverviewEntry entry : entries) {
				// save the entry
				isModified |= entry.saveBatch(batchUpdater, () -> entry.setOverview(this), null);

				// do not remove the entry, which was saved
				entryIdsToRemove.remove(entry.getId());
			}

			if (!entryIdsToRemove.isEmpty()) {
				StringBuffer sql = new StringBuffer("DELETE FROM ds_obj WHERE id IN (");

				sql.append(StringUtils.repeat("?", entryIdsToRemove.size(), ","));
				sql.append(")");
				DBUtils.executeUpdate(sql.toString(), (Object[]) entryIdsToRemove.toArray(new Object[entryIdsToRemove.size()]));

				isModified = true;
			}

			// delete all other overviews with the same container
			final int containerIdInt = ObjectTransformer.getInt(containerId, 0);
			final boolean isContentTag = ContentTag.class.isAssignableFrom(containerClass);
			final boolean isTemplateTag = TemplateTag.class.isAssignableFrom(containerClass);
			final boolean isObjectTag = ObjectTag.class.isAssignableFrom(containerClass);

			if (!xor(isContentTag, isTemplateTag, isObjectTag)) {
				throw new NodeException("The type of the overview " + getId() + " can't be determined distinctly");
			}

			if (containerIdInt > 0) {
				final List<Integer> idsToDelete = new ArrayList<Integer>();
				DBUtils.executeStatement("SELECT id FROM ds WHERE contenttag_id = ? and templatetag_id = ? and objtag_id = ?", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, isContentTag ? containerIdInt : 0);
						stmt.setInt(2, isTemplateTag ? containerIdInt : 0);
						stmt.setInt(3, isObjectTag ? containerIdInt : 0);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							idsToDelete.add(rs.getInt("id"));
						}
					}
				});
				idsToDelete.remove(getId());
				if (!idsToDelete.isEmpty()) {
					DBUtils.selectAndDelete("ds", "SELECT id FROM ds WHERE id IN", idsToDelete);
					DBUtils.selectAndDelete("ds_obj", "SELECT id FROM ds_obj WHERE ds_id IN", idsToDelete);
					for (int id : idsToDelete) {
						t.dirtObjectCache(Overview.class, id);
					}
				}
			}

			if (isModified) {
				t.dirtObjectCache(Overview.class, getId());
			}

			return isModified;
		}

		/**
		 * Returns true if and only if one of the parameters is true
		 * @param a
		 * @param b
		 * @param c
		 * @return boolean true if and only if on of the parameters is true
		 */
		public static boolean xor(boolean a, boolean b, boolean c) {
			return (a ^ b ^ c) ^ (a && b && c);
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			// The original overview is not set. There is nothing we can copy.
			if (original == null) {
				return;
			}
			super.copyFrom(original);
			Overview oOverview = (Overview) original;

			// copy the meta data
			setMaxObjects(oOverview.getMaxObjects());
			setObjectClass(oOverview.getObjectClass());
			setOrderKind(oOverview.getOrderKind());
			setOrderWay(oOverview.getOrderWay());
			setRecursion(oOverview.doRecursion());
			setSelectionType(oOverview.getSelectionType());

			// copy the overview entries
			List<OverviewEntry> origEntries = oOverview.getOverviewEntries();
			List<OverviewEntry> thisEntries = getOverviewEntries();

			// copy the original entries over this entries
			for (int i = 0; i < origEntries.size(); ++i) {
				OverviewEntry entry = origEntries.get(i);

				if (thisEntries.size() <= i) {
					// this overview has less entries, so add a copy
					thisEntries.add((OverviewEntry) entry.copy());
				} else {
					// this overview has at least so many entries, so copy the i-th overview entry
					thisEntries.get(i).copyFrom(entry);
				}
			}

			// remove any surplus entries
			while (thisEntries.size() > origEntries.size()) {
				thisEntries.remove(thisEntries.size() - 1);
			}
		}
	}

	private static class FactoryOverviewEntry extends OverviewEntry {

		/**
		 * Serial version uid
		 */
		private static final long serialVersionUID = -4923884135491673365L;

		protected Integer objectId;
		protected int objectOrder;
		protected Integer dsId;
		protected Integer aUserId;

		protected ContentNodeDate aDate;

		protected Class containerClass;
		protected Integer containerId;

		protected int nodeId;

		/**
		 * Create an empty instance
		 * @param info object info
		 */
		public FactoryOverviewEntry(NodeObjectInfo info) {
			super(null, info);
			aUserId = 0;
			aDate = new ContentNodeDate(0);
		}

		public FactoryOverviewEntry(Integer id, NodeObjectInfo info, Integer objectId, int objectOrder, Class containerClass, Integer containerId,
				Integer dsId, Integer aUserId, ContentNodeDate aDate, int nodeId, int udate, GlobalId globalId) {
			super(id, info);
			this.objectId = objectId;
			this.objectOrder = objectOrder;
			this.containerClass = containerClass;
			this.containerId = containerId;
			this.dsId = dsId;
			this.aUserId = aUserId;
			this.aDate = aDate;
			this.nodeId = nodeId;
			this.udate = udate;
			this.globalId = globalId;
		}

		public Integer getObjectId() {
			return objectId;
		}

		@Override
		public NodeObject getObject() throws NodeException {
			// the overview will tell us, which objects are selected
			Overview overview = getOverview();
			Class<? extends NodeObject> objectClass = null;

			if (overview.getSelectionType() == Overview.SELECTIONTYPE_SINGLE) {
				objectClass = overview.getObjectClass();
			} else {
				objectClass = Folder.class;
			}

			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(objectClass, objectId);
		}

		public int getObjectOrder() {
			return objectOrder;
		}

		public Integer getAuthorizeUserId() {
			return aUserId;
		}

		public Integer getDatasourceId() {
			return dsId;
		}

		@Override
		public Overview getOverview() throws NodeException {
			Transaction t = TransactionManager.getCurrentTransaction();

			return t.getObject(Overview.class, dsId);
		}

		public Tag getContainer() throws NodeException {
			if (containerId != null && Tag.class.isAssignableFrom(containerClass)) {
				return (Tag) TransactionManager.getCurrentTransaction().getObject(containerClass, containerId);
			} else {
				return null;
			}
		}

		@Override
		public int getNodeId() {
			return nodeId;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "OverviewEntry {" + getId() + "}";
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.object.NodeObject#copy()
		 */
		public NodeObject copy() throws NodeException {
			return new EditableFactoryOverviewEntry(this, getFactory().getFactoryHandle(OverviewEntry.class).createObjectInfo(OverviewEntry.class, true), true);
		}
	}

	/**
	 * Editable instances of overview entries
	 */
	private static class EditableFactoryOverviewEntry extends FactoryOverviewEntry {

		/**
		 * Flag to mark whether the page has been modified (contains changes which need to be persistet by calling {@link #save()}).
		 */
		private boolean modified = false;

		/**
		 * Create an empty, editable instance
		 * @param info object info
		 * @throws NodeException
		 */
		protected EditableFactoryOverviewEntry(NodeObjectInfo info) throws NodeException {
			super(info);
			modified = true;
		}

		/**
		 * Create an editable copy of the given overview entry
		 * @param entry overview entry
		 * @param info object info
		 * @param asNew true if the overview entry shall be a real copy (new object), false for an editable copy of the given overview entry
		 * @throws ReadOnlyException
		 * @throws NodeException
		 */
		protected EditableFactoryOverviewEntry(FactoryOverviewEntry entry, NodeObjectInfo info, boolean asNew) throws ReadOnlyException, NodeException {
			super(asNew ? null : entry.getId(), info, entry.getObjectId(), entry.getObjectOrder(), asNew ? null : entry.containerClass,
					asNew ? null : entry.containerId, asNew ? null : entry.getDatasourceId(), asNew ? 0 : entry.aUserId, asNew ? new ContentNodeDate(0) : entry.aDate,
					entry.getNodeId(), asNew ? -1 : entry.getUdate(), asNew ? null : entry.getGlobalId());
			if (asNew) {
				modified = true;
			}
		}

		@Override
		public void setAuthorizeUserId(Integer aUserId) throws ReadOnlyException {
			assertEditable();
			if (this.aUserId != aUserId) {
				this.aUserId = aUserId;
				modified = true;
			}
		}

		@Override
		public void setContainer(Tag container) throws ReadOnlyException {
			assertEditable();
			Integer newContainerId = null;
			Class<? extends NodeObject> newContainerClass = null;

			if (container != null) {
				newContainerId = container.getId();
				newContainerClass = container.getObjectInfo().getObjectClass();
			}

			if (containerClass != newContainerClass || ObjectTransformer.getInt(containerId, 0) != ObjectTransformer.getInt(newContainerId, 0)) {
				// set the new container
				containerClass = newContainerClass;
				containerId = newContainerId;
				modified = true;
			}
		}

		@Override
		public void setObjectId(Integer objectId) throws ReadOnlyException {
			assertEditable();
			if (this.objectId != objectId) {
				this.objectId = objectId;
				modified = true;
			}
		}

		@Override
		public void setObjectOrder(int objectOrder) throws ReadOnlyException {
			assertEditable();
			if (this.objectOrder != objectOrder) {
				this.objectOrder = objectOrder;
				modified = true;
			}
		}

		@Override
		public void setOverview(Overview overview) throws ReadOnlyException, NodeException {
			if (overview != null) {
				if (ObjectTransformer.getInt(this.dsId, 0) != ObjectTransformer.getInt(overview.getId(), 0)) {
					this.dsId = overview.getId();
					modified = true;
				}

				setContainer(overview.getContainer());
			}
		}

		@Override
		public void setNodeId(int nodeId) throws ReadOnlyException {
			assertEditable();
			if (this.nodeId != nodeId) {
				this.nodeId = nodeId;
				modified = true;
			}
		}

		/**
		 * Set the id after saving the object
		 * @param id id of the object
		 */
		protected void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public boolean save() throws InsufficientPrivilegesException, NodeException {
			return saveBatch(null, null, null);
		}

		@Override
		public boolean saveBatch(BatchUpdater batchUpdater, Operator before, Operator after) throws InsufficientPrivilegesException,
					NodeException {
			// first check whether the overview entry is editable
			assertEditable();

			Transaction t = TransactionManager.getCurrentTransaction();

			boolean isModified = this.modified;

			// save the overview entry, if necessary
			if (isModified) {
				// when this is an overview over folders and the feature "TODO"
				// is on, check whether the user has permission to add the
				// specified folder to the overview
				if (ObjectTransformer.getInt(aUserId, 0) == 0) {
					aUserId = t.getUserId();
				}
				if (aDate.getIntTimestamp() == 0) {
					aDate = new ContentNodeDate(t.getUnixTimestamp());
				}

				saveOverviewEntryObject(this, batchUpdater, before, after);
				this.modified = false;
			}

			if (isModified) {
				t.dirtObjectCache(OverviewEntry.class, getId());
			}

			return isModified;
		}

		/* (non-Javadoc)
		 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
			super.copyFrom(original);
			OverviewEntry oEntry = (OverviewEntry) original;

			setObjectId(oEntry.getObjectId());
			setObjectOrder(oEntry.getObjectOrder());
			setAuthorizeUserId(oEntry.getAuthorizeUserId());
		}
	}

	public OverviewFactory() {
		super();
	}

	public <T extends NodeObject> T createObject(FactoryHandle handle,
			Class<T> clazz) throws NodeException {
		if (Overview.class.equals(clazz)) {
			return (T) new EditableFactoryOverview(handle.createObjectInfo(Overview.class, true));
		} else if (OverviewEntry.class.equals(clazz)) {
			return (T) new EditableFactoryOverviewEntry(handle.createObjectInfo(OverviewEntry.class, true));
		} else {
			return null;
		}
	}

	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		if (Overview.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_DS_SQL, SELECT_VERSIONED_DS_SQL, SELECT_VERSIONED_DS_PARAMS);
		} else if (OverviewEntry.class.equals(clazz)) {
			return loadDbObject(clazz, id, info, SELECT_DS_OBJ_SQL, SELECT_VERSIONED_DS_OBJ_SQL, SELECT_VERSIONED_DS_OBJ_PARAMS);
		}
		return null;
	}
    
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		if (Overview.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_DS_SQL + buildIdSql(ids));
		} else if (OverviewEntry.class.equals(clazz)) {
			return batchLoadDbObjects(clazz, ids, info, BATCHLOAD_DS_OBJ_SQL + buildIdSql(ids));
		}
		return null;
	}

	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException, NodeException {
		if (Overview.class.equals(clazz)) {
			return (T) loadOverview(clazz, id, info, rs);
		} else if (OverviewEntry.class.equals(clazz)) {
			return (T) loadOverviewEntry(clazz, id, info, rs);
		}
		return null;
	}

	private Overview loadOverview(Class<? extends NodeObject> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs) throws SQLException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		int selectionType = rs.getInt("is_folder");
		Class<? extends NodeObject> objClass = t.getClass(rs.getInt("o_type"));
		int orderKind = rs.getInt("orderkind");
		int orderWay = rs.getInt("orderway");
		int maxObjects = rs.getInt("max_obj");
		boolean recursion = rs.getInt("recursion") > 0;

		Class<? extends NodeObject> containerClass = null;
		int containerId = 0;

		if ((containerId = rs.getInt("contenttag_id")) > 0) {
			containerClass = ContentTag.class;
		} else if ((containerId = rs.getInt("templatetag_id")) > 0) {
			containerClass = TemplateTag.class;
		} else if ((containerId = rs.getInt("objtag_id")) > 0) {
			containerClass = ObjectTag.class;
		}

		return new FactoryOverview(id, info, selectionType, objClass, orderKind, orderWay, maxObjects, recursion, containerClass, new Integer(containerId),
				getUdate(rs), getGlobalId(rs, "ds"));
	}

	private OverviewEntry loadOverviewEntry(Class<? extends NodeObject> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs) throws SQLException {

		Integer objectId = new Integer(rs.getInt("o_id"));
		int objectOrder = rs.getInt("obj_order");
		Integer dsId = new Integer(rs.getInt("ds_id"));
		Integer aUserId = new Integer(rs.getInt("auser"));
		ContentNodeDate aDate = new ContentNodeDate(rs.getInt("adate"));

		Class<? extends NodeObject> containerClass = null;
		int containerId = 0;

		if ((containerId = rs.getInt("contenttag_id")) > 0) {
			containerClass = ContentTag.class;
		} else if ((containerId = rs.getInt("templatetag_id")) > 0) {
			containerClass = TemplateTag.class;
		} else if ((containerId = rs.getInt("objtag_id")) > 0) {
			containerClass = ObjectTag.class;
		}

		return new FactoryOverviewEntry(id, info, objectId, objectOrder, containerClass, containerId, dsId, aUserId, aDate, rs.getInt("node_id"), getUdate(rs),
				getGlobalId(rs, "ds_obj"));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.factory.object.AbstractFactory#getEditableCopy(com.gentics.lib.base.object.NodeObject, com.gentics.lib.base.object.NodeObjectInfo)
	 */
	public <T extends NodeObject> T getEditableCopy(T object, NodeObjectInfo info) throws NodeException, ReadOnlyException {
		if (object == null) {
			return null;
		}
		if (object instanceof FactoryOverview) {
			EditableFactoryOverview editableCopy = new EditableFactoryOverview((FactoryOverview) object, info, false);

			return (T) editableCopy;
		} else if (object instanceof FactoryOverviewEntry) {
			EditableFactoryOverviewEntry editableCopy = new EditableFactoryOverviewEntry((FactoryOverviewEntry) object, info, false);

			return (T) editableCopy;
		} else {
			throw new NodeException("OverviewFactory cannot create editable copy for object of " + object.getObjectInfo().getObjectClass());
		}
	}

	/**
	 * Save the overview to the database
	 * @param overview overview to save
	 * @param batchUpdater optional batch updater
	 * @param before optional before handler
	 * @param after optional after handler 
	 * @throws NodeException
	 */
	private static void saveOverviewObject(EditableFactoryOverview overview, BatchUpdater batchUpdater, Operator before, Operator after) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		boolean isNew = Overview.isEmptyId(overview.getId());

		Supplier<Integer> templateTagId = () -> {
			return overview.containerClass == TemplateTag.class ? overview.containerId : 0;
		};
		Supplier<Integer> contentTagId = () -> {
			return overview.containerClass == ContentTag.class ? overview.containerId : 0;
		};
		Supplier<Integer> objTagId = () -> {
			return overview.containerClass == ObjectTag.class ? overview.containerId : 0;
		};

		int oType = t.getTType(overview.objClass);

		if (isNew) {
			assertNewGlobalId(overview);

			Supplier<Object[]> paramSupplier = () -> new Object[] { templateTagId.supply(), contentTagId.supply(),
					objTagId.supply(), oType, overview.selectionType, overview.orderKind, overview.orderWay,
					overview.maxObjects, overview.recursion, ObjectTransformer.getString(overview.getGlobalId(), "") };

			Consumer<Integer> generatedKeysHandler = key -> {
				// set the new entry id
				overview.setId(key);
				if (batchUpdater != null) {
					batchUpdater.addObjectToSynchronize(overview);
				} else {
					synchronizeGlobalId(overview);
				}
			};

			if (batchUpdater != null) {
				batchUpdater.add(INSERT_DS_WO_PARAMS_SQL, INSERT_DS_PARAMS_SQL, Transaction.INSERT_STATEMENT, paramSupplier, generatedKeysHandler, before, after);
			} else {
				if (before != null) {
					before.operate();
				}
				// insert a new record
				List<Integer> keys = DBUtils.executeInsert(INSERT_DS_SQL, paramSupplier.supply());

				if (keys.size() == 1) {
					generatedKeysHandler.accept(keys.get(0));
				} else {
					throw new NodeException("Error while inserting new overview, could not get the insertion id");
				}

				if (after != null) {
					after.operate();
				}
			}
		} else {
			if (before != null) {
				before.operate();
			}
			DBUtils.executeUpdate(UPDATE_DS_SQL,
					new Object[] { templateTagId.supply(), contentTagId.supply(), objTagId.supply(), oType,
							overview.selectionType, overview.orderKind, overview.orderWay, overview.maxObjects,
							overview.recursion, overview.getId() });
			if (after != null) {
				after.operate();
			}
		}
	}

	/**
	 * Save the overview entry to the database
	 * @param entry overview entry to save
	 * @param batchUpdater optional batch updater
	 * @param before optional before handler
	 * @param after optional after handler
	 * @throws NodeException
	 */
	private static void saveOverviewEntryObject(EditableFactoryOverviewEntry entry, BatchUpdater batchUpdater, Operator before, Operator after) throws NodeException {
		boolean isNew = Overview.isEmptyId(entry.getId());

		Supplier<Integer> templateTagId = () -> {
			return entry.containerClass == TemplateTag.class ? entry.containerId : 0;
		};
		Supplier<Integer> contentTagId = () -> {
			return entry.containerClass == ContentTag.class ? entry.containerId : 0;
		};
		Supplier<Integer> objTagId = () -> {
			return entry.containerClass == ObjectTag.class ? entry.containerId : 0;
		};

		if (isNew) {
			Supplier<Object[]> paramSupplier = () -> new Object[] { templateTagId.supply(), contentTagId.supply(),
					objTagId.supply(), entry.dsId, entry.objectId, entry.nodeId, entry.objectOrder, entry.aUserId,
					entry.aDate.getIntTimestamp() };

			Consumer<Integer> generatedKeysHandler = key -> {
				// set the new entry id
				entry.setId(key);
			};

			if (batchUpdater != null) {
				batchUpdater.add(INSERT_DS_OBJ_WO_PARAMS_SQL, INSERT_DS_OBJ_PARAMS_SQL, Transaction.INSERT_STATEMENT, paramSupplier, generatedKeysHandler, before, after);
			} else {
				if (before != null) {
					before.operate();
				}
				// insert a new record
				List<Integer> keys = DBUtils.executeInsert(INSERT_DS_OBJ_SQL, paramSupplier.supply());

				if (keys.size() == 1) {
					generatedKeysHandler.accept(keys.get(0));
				} else {
					throw new NodeException("Error while inserting new overview entry, could not get the insertion id");
				}

				if (after != null) {
					after.operate();
				}
			}
		} else {
			if (before != null) {
				before.operate();
			}
			DBUtils.executeUpdate(UPDATE_DS_OBJ_SQL,
					new Object[] { templateTagId.supply(), contentTagId.supply(), objTagId.supply(), entry.dsId,
							entry.objectId, entry.nodeId, entry.objectOrder, entry.aUserId,
							entry.aDate.getIntTimestamp(), entry.getId() });
			if (after != null) {
				after.operate();
			}
		}
	}

	@Override
	public boolean isVersioningSupported(Class<? extends NodeObject> clazz) {
		return Overview.class.equals(clazz) || OverviewEntry.class.equals(clazz);
	}
}
