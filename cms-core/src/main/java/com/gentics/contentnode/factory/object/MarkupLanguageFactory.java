/*
 * @author Stefan Hepp
 * @date 17.01.2006
 * @version $Id: MarkupLanguageFactory.java,v 1.11.2.1 2011-01-18 13:21:53 norbert Exp $
 */
package com.gentics.contentnode.factory.object;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.DBTable;
import com.gentics.contentnode.factory.DBTables;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;

/**
 * An objectfactory to create {@link MarkupLanguage} objects, based on the {@link AbstractFactory}.
 */
@DBTables({ @DBTable(clazz = MarkupLanguage.class, name = "ml") })
public class MarkupLanguageFactory extends AbstractFactory {

	private static class FactoryMarkupLanguage extends MarkupLanguage {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -1421231262302197287L;

		private String name;
		private String extension;
		private String contentType;

		private Feature feature;

		private boolean excludeFromPublishing;

		/**
		 * Create an instance
		 * @param id ID
		 * @param info object info
		 * @param name name
		 * @param ext extension
		 * @param contentType content type
		 * @param feature optional feature
		 * @param excludeFromPublishing "exclude" flag
		 */
		public FactoryMarkupLanguage(Integer id, NodeObjectInfo info, String name, String ext, String contentType, Feature feature, boolean excludeFromPublishing) {
			super(id, info);
			this.name = name;
			this.extension = ext;
			this.contentType = contentType;
			this.feature = feature;
			this.excludeFromPublishing = excludeFromPublishing;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getExtension() {
			return extension;
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public Feature getFeature() {
			return feature;
		}

		@Override
		public boolean isExcludeFromPublishing() {
			return excludeFromPublishing;
		}

		@Override
		public String toString() {
			return "MarkupLanguage {" + getName() + ", " + getId() + "}";
		}

		@Override
		public NodeObject copy() throws NodeException {
			return null;
		}
	}

	public MarkupLanguageFactory() {
		super();
	}

	public int getTType(Class<? extends NodeObject> clazz) {
		if (MarkupLanguage.class.equals(clazz)) {
			return 10201;
		}
		return 0;
	}

	@Override
	public <T extends NodeObject> T createObject(FactoryHandle handle, Class<T> clazz) {
		return null;
	}

	@Override
	public <T extends NodeObject> T loadObject(Class<T> clazz, Integer id, NodeObjectInfo info) throws NodeException {
		return loadDbObject(clazz, id, info, "SELECT * FROM ml WHERE id = ?", null, null);
	}

	@Override
	public <T extends NodeObject> Collection<T> batchLoadObjects(Class<T> clazz, Collection<Integer> ids, NodeObjectInfo info) throws NodeException {
		return batchLoadDbObjects(clazz, ids, info, "SELECT * FROM ml WHERE id IN ");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends NodeObject> T loadResultSet(Class<T> clazz, Integer id, NodeObjectInfo info, FactoryDataRow rs, List<Integer>[] idLists) throws SQLException {

		String name = rs.getString("name");
		String ext = rs.getString("ext");
		String contentType = rs.getString("contenttype");
		Feature feature = Feature.getByName(rs.getString("feature"));
		boolean exclude = rs.getBoolean("exclude_from_publishing");

		return (T) new FactoryMarkupLanguage(id, info, name, ext, contentType, feature, exclude);
	}

}
