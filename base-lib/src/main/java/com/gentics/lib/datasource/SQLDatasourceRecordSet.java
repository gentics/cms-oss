package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.lib.db.SimpleResultProcessor;

/**
 * @author haymo
 */
public class SQLDatasourceRecordSet implements DatasourceRecordSet {
	private class SqlDSIterator implements Iterator {
		int size;

		int pos;

		public SqlDSIterator() {
			size = size();
			pos = 1;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			return pos <= size;
		}

		public Object next() {
			return getRow(pos++);
		}
	}

	protected SimpleResultProcessor resultProc;

	public SQLDatasourceRecordSet(SimpleResultProcessor resultProc) {
		this.resultProc = resultProc;
	}

	public DatasourceRow getRow(int rowNum) {
		return null;
	}

	public void addRow(DatasourceRow dsRow) {// Todo fro writeable Datasources
	}

	/*
	 * public Iterator getAttributeNames() { return null; }
	 */
	public Iterator iterator() {
		return new SqlDSIterator();
	}

	public int size() {
		return this.resultProc.size();
	}

	public void clear() {}

	public boolean isEmpty() {
		return false;
	}

	public Object[] toArray() {
		return new Object[0];
	}

	public boolean add(Object o) {
		return false;
	}

	public boolean contains(Object o) {
		return false;
	}

	public boolean remove(Object o) {
		return false;
	}

	public boolean addAll(Collection c) {
		return false;
	}

	public boolean containsAll(Collection c) {
		return false;
	}

	public boolean removeAll(Collection c) {
		return false;
	}

	public boolean retainAll(Collection c) {
		return false;
	}

	public Object[] toArray(Object a[]) {
		return new Object[0];
	}

	/**
	 * @param index
	 * @return
	 */
	public Object get(int index) {
		// convert index from interface List which starts with 0 to JDBC index,
		// which starts with 1
		DatasourceRow row = getRow(index + 1);

		if (row == null) {
			return null;
		}
		return row.toObject();
	}

	public Object remove(int index) {
		return null;
	}

	public void add(int index, Object element) {}

	public int indexOf(Object o) {
		return 0;
	}

	public int lastIndexOf(Object o) {
		return 0;
	}

	public boolean addAll(int index, Collection c) {
		return false;
	}

	public List subList(int fromIndex, int toIndex) {
		return null;
	}

	public ListIterator listIterator() {
		return null;
	}

	public ListIterator listIterator(int index) {
		return null;
	}

	public Object set(int index, Object element) {
		return null;
	}
}
