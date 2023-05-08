package com.gentics.lib.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.auth.GenticsUser;
import com.gentics.api.lib.datasource.DatasourceInfo;
import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;

/**
 * @deprecated not yet implemented.
 * @author haymo
 * @date 27.08.2004
 * @version $Id: SQLWriteableDatasource.java,v 1.16 2007-11-13 10:03:41 norbert Exp $
 */
public class SQLWriteableDatasource extends SQLDatasource implements WriteableDatasource {

	public SQLWriteableDatasource(String id, SQLHandle handle, Map parameters) {
		super(id, handle, parameters);
	}

	public boolean canWrite() {
		return true;
	}

	public DatasourceInfo delete(DatasourceFilter filter) throws DatasourceException {
		// untested
		return delete(getResult(filter, null));
	}
    
	public DatasourceInfo store(DatasourceRecordSet rst) throws DatasourceException {
		return store(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo store(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(java.util.Collection)
	 */
	public DatasourceInfo update(Collection objects) throws DatasourceException {
		return update(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(java.util.Collection,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo update(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new SQLDatasourceRecordSet(null);

		set.addAll(objects);

		return update(set, user);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(java.util.Collection)
	 */
	public DatasourceInfo store(Collection objects) throws DatasourceException {
		return store(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#store(java.util.Collection,
	 *      com.gentics.lib.user.GenticsUser)
	 */
	public DatasourceInfo store(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new SQLDatasourceRecordSet(null);

		set.addAll(objects);

		return store(set, user);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst) throws DatasourceException {
		return update(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#update(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo update(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(java.util.Collection,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo insert(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new SQLDatasourceRecordSet(null);

		set.addAll(objects);

		return insert(set);
	}

	public DatasourceInfo insert(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		try {
			StringBuffer sql = new StringBuffer(500);
			StringBuffer sqlValues = new StringBuffer(1000);
			ArrayList values = new ArrayList();
			Iterator it = rst.iterator();
			String[] attr;

			while (it.hasNext()) {
				DatasourceRow row = (DatasourceRow) it.next();

				attr = this.getAttributeNames();
				// Clear buffers
				sql.delete(0, sql.length());
				sqlValues.delete(0, sqlValues.length());

				sql.append("INSERT INTO ");
				sql.append(this.getTable());
				sql.append(" ( ");
				sqlValues.append(" VALUES (");

				for (int i = 0; i < attr.length; i++) {
					String name = attr[i];

					if (i > 0) {
						sql.append(" ,");
						sqlValues.append(" ,");
					}
					sql.append(name);
					sqlValues.append("?");
					values.add(row.getObject(name));
				}

				sql.append(")");
				sqlValues.append(")");
				sql.append(sqlValues);
			}
		} catch (RuntimeException e1) {
			logger.error("error while inserting records", e1);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(java.util.Collection)
	 */
	public DatasourceInfo insert(Collection objects) throws DatasourceException {
		return insert(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#insert(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst) throws DatasourceException {
		return insert(rst, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(java.util.Collection,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo delete(Collection objects, GenticsUser user) throws DatasourceException {
		DatasourceRecordSet set = new SQLDatasourceRecordSet(null);

		set.addAll(objects);
		return delete(set, user);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(com.gentics.lib.datasource.DatasourceRecordSet,
	 *      com.gentics.lib.user.User)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException {
		throw new RuntimeException("not yet implemented");
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(java.util.Collection)
	 */
	public DatasourceInfo delete(Collection objects) throws DatasourceException {
		return delete(objects, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.datasource.WriteableDatasource#delete(com.gentics.lib.datasource.DatasourceRecordSet)
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst) throws DatasourceException {
		return delete(rst, null);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.WriteableDatasource#create(java.util.Map)
	 */
	public Changeable create(Map objectParameters) throws DatasourceException {
		throw new DatasourceException("not yet implemented");
	}
}
