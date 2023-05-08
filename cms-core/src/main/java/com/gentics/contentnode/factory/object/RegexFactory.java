package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.C;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.AbstractContentObject;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Regex;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.lib.genericexceptions.NotYetImplementedException;

/**
 * Factory for instances of {@link Regex}
 */
@DBTables({ @DBTable(clazz = Regex.class, name = C.Tables.REGEX)})
public class RegexFactory extends AbstractFactory {
	static {
		// register the factory class
		try {
			registerFactoryClass(C.Tables.REGEX, Regex.TYPE_REGEX, false, FactoryRegex.class);
		} catch (NodeException e) {
			logger.error("Error while registering factory", e);
		}
	}

	/**
	 * Create instance
	 */
	public RegexFactory() {
		super();
	}

	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) throws NodeException {
		throw new NotYetImplementedException("Creating Regex objects is not implemented");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists)
			throws SQLException, NodeException {
		if (Regex.class.equals(clazz)) {
			return (T) new FactoryRegex(id, info, rs.getValues());
		}
		return null;
	}

	/**
	 * Factory implementation of {@link Regex}
	 */
	private static class FactoryRegex extends AbstractContentObject implements Regex {
		/**
		 * Serial Version UID
		 */
		private static final long serialVersionUID = -4885321592387153985L;

		@DataField("name_id")
		protected int nameId;

		@DataField("desc_id")
		protected int descriptionId;

		@DataField("regex")
		protected String expression;

		@DataField("creator")
		protected int creatorId;

		@DataField("cdate")
		protected ContentNodeDate cDate = new ContentNodeDate(0);

		@DataField("editor")
		protected int editorId;

		@DataField("edate")
		protected ContentNodeDate eDate = new ContentNodeDate(0);

		protected FactoryRegex(Integer id, NodeObjectInfo info, Map<String, Object> dataMap) throws NodeException {
			super(id, info);
			setDataMap(this, dataMap);
		}

		/**
		 * Set the ID
		 * @param id ID
		 */
		@SuppressWarnings("unused")
		public void setId(Integer id) {
			if (this.id == null) {
				this.id = id;
			}
		}

		@Override
		public NodeObject copy() throws NodeException {
			throw new NotYetImplementedException("Copying Regexes is not implemented");
		}

		@Override
		public int getNameId() {
			return nameId;
		}

		@Override
		public int getDescriptionId() {
			return descriptionId;
		}

		@Override
		public String getExpression() {
			return expression;
		}

		@Override
		public SystemUser getCreator() throws NodeException {
			SystemUser creator = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, creatorId);

			// check for data consistency
			assertNodeObjectNotNull(creator, creatorId, "SystemUser");
			return creator;
		}

		@Override
		public SystemUser getEditor() throws NodeException {
			SystemUser editor = TransactionManager.getCurrentTransaction().getObject(SystemUser.class, editorId);

			// check for data consistency
			assertNodeObjectNotNull(editor, editorId, "SystemUser");
			return editor;
		}

		@Override
		public ContentNodeDate getCDate() {
			return cDate;
		}

		@Override
		public ContentNodeDate getEDate() {
			return eDate;
		}
	}
}
