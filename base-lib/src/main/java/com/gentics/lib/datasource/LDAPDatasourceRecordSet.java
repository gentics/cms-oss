package com.gentics.lib.datasource;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;
import com.gentics.lib.ldap.LDAPResultProcessor;

/**
 * @author haymo
 * @date 04.08.2004
 */
public class LDAPDatasourceRecordSet implements DatasourceRecordSet, Serializable {
	private static final long serialVersionUID = 1L;

	private LDAPResultProcessor resultProc;

	public LDAPDatasourceRecordSet(LDAPResultProcessor resultProc) {
		this.resultProc = resultProc;

	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.DatasourceRecordSet#getRow(int)
	 */
	public DatasourceRow getRow(int rowNum) {
		return this.resultProc.getRow(rowNum);
	}

	public void addRow(DatasourceRow dsRow) {// Todo fro writeable Datasources
	}

	/*
	 * public Iterator getAttributeNames() { this.columnNames=null;
	 * LDAPDatasourceRow row; if
	 * (this.resultProc.getAllLAPRows().firstElement()!=null) { row =
	 * (LDAPDatasourceRow) this.resultProc.getAllLAPRows().firstElement();
	 * this.columnNames = row.getColumnNames(); } return this.columnNames; }
	 */

	public Iterator iterator() {
		return this.resultProc.getLDAPDatasourceRowIterator();
	}

	public int size() {
		return this.resultProc.size();
	}

	public void clear() {}

	public boolean isEmpty() {
		return this.resultProc.getAllLAPRows().isEmpty();
	}

	public Object[] toArray() {
		return this.resultProc.getAllLAPRows().toArray();
	}

	//
	public boolean add(Object o) {

		return false;
	}

	public boolean contains(Object o) {
		return this.resultProc.getAllLAPRows().contains(o);
	}

	public boolean remove(Object o) {
		return this.resultProc.getAllLAPRows().remove(o);
	}

	public boolean addAll(Collection c) {
		return false;
	}

	public boolean containsAll(Collection c) {
		return this.resultProc.getAllLAPRows().containsAll(c);
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

	// muss mit 0 anfangen
	public Object get(int index) {
		return this.resultProc.getRow(index);
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
