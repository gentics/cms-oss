package com.gentics.lib.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.log.NodeLogger;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 13.08.2004
 */
public class CNDatasourceRecordSet implements DatasourceRecordSet {

	private ArrayList objects;

	private Iterator columnNames;

	private CNDatasource datasource;

	public CNDatasourceRecordSet(CNDatasource datasource) {
		objects = new ArrayList();
		this.datasource = datasource;
	}

	public CNDatasourceRecordSet(CNDatasource datasource, GenticsContentObject[] CNObjects) {
		objects = new ArrayList();
		for (int i = 0; i < CNObjects.length; i++) {
			GenticsContentObject cnObject = CNObjects[i];

			objects.add(new CNDatasourceRow(cnObject));
		}
		this.datasource = datasource;
	}

	public CNDatasourceRecordSet(CNDatasource datasource, SimpleResultProcessor resultProc) throws NodeIllegalArgumentException, CMSUnavailableException {
		this(datasource, resultProc, -1);
	}

	public CNDatasourceRecordSet(CNDatasource datasource, SimpleResultProcessor resultProc,
			int timestamp) throws NodeIllegalArgumentException, CMSUnavailableException {
		Iterator it = resultProc.iterator();

		objects = new ArrayList(resultProc.size());
		while (it.hasNext()) {
			SimpleResultRow row = (SimpleResultRow) it.next();

			if (row != null) {
				GenticsContentObject gco = null;

				gco = GenticsContentFactory.createContentObject(row, datasource, timestamp);
				if (gco != null) {
					objects.add(new CNDatasourceRow(gco));
				}
			} else {
				throw new NodeIllegalArgumentException("A Result Row ist not a Row Objects.");
			}

		}
	}

	public DatasourceRow getRow(int rowNum) {
		return (CNDatasourceRow) objects.get(rowNum);
	}

	public void addRow(DatasourceRow dsRow) {
		if (dsRow instanceof CNDatasourceRow) {
			add(dsRow);
			return;
		}
		String contentid = dsRow.getString("contentid");
		int obj_type = dsRow.getInt("obj_type");
		Object obj = dsRow.toObject();
		GenticsContentObject gco = null;

		if (obj instanceof GenticsContentObject) {
			gco = (GenticsContentObject) dsRow.toObject();
		}
		try {
			if (contentid != null && contentid != "") {
				add(new CNDatasourceRow(GenticsContentFactory.createContentObject(contentid, datasource)));
			} else if (obj_type != 0) {
				add(new CNDatasourceRow(GenticsContentFactory.createContentObject(obj_type, datasource)));
			} else if (gco != null) {
				if (gco.getObjectType() != 0) {
					add(new CNDatasourceRow(GenticsContentFactory.createContentObject(gco.getObjectType(), datasource)));
				}
			}
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		}
	}

	public Iterator iterator() {
		return objects.iterator();
	}

	public int size() {
		return objects.size();
	}

	public void clear() {
		objects.clear();
	}

	public boolean isEmpty() {
		return objects.isEmpty();
	}

	public Object[] toArray() {
		return objects.toArray();
	}

	//
	public boolean add(Object o) {
		if (!(o instanceof DatasourceRow)) {
			NodeLogger.getLogger(getClass()).error("CNDatasourceRecordSet: Cannot add non-DatasourceRow");
			return false;
		}
		return objects.add(o);
	}

	public boolean contains(Object o) {
		return objects.contains(o);
	}

	public boolean remove(Object o) {
		return objects.remove(o);
	}

	public boolean addAll(Collection c) {
		return objects.addAll(c);
	}

	public boolean containsAll(Collection c) {
		return objects.containsAll(c);
	}

	public boolean removeAll(Collection c) {
		return objects.removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return objects.retainAll(c);
	}

	public Object[] toArray(Object a[]) {
		return objects.toArray(a);
	}

	// muss mit 0 anfangen
	public Object get(int index) {
		return objects.get(index);
	}

	public Object remove(int index) {
		return objects.remove(index);
	}

	public void add(int index, Object element) {
		objects.add(index, element);
	}

	public int indexOf(Object o) {
		return objects.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return objects.lastIndexOf(o);
	}

	public boolean addAll(int index, Collection c) {
		return objects.addAll(index, c);
	}

	public List subList(int fromIndex, int toIndex) {
		return objects.subList(fromIndex, toIndex);
	}

	public ListIterator listIterator() {
		return objects.listIterator();
	}

	public ListIterator listIterator(int index) {
		return objects.listIterator(index);
	}

	public Object set(int index, Object element) {
		return objects.set(index, element);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return objects.equals(obj);
	}
}
