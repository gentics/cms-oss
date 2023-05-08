package com.gentics.lib.datasource;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.gentics.api.lib.datasource.DatasourceRecordSet;
import com.gentics.api.lib.datasource.DatasourceRow;

/**
 * created at Oct 17, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class DefaultDatasourceRecordSet implements DatasourceRecordSet {
	List data;

	public DefaultDatasourceRecordSet() {
		data = new Vector();
	}

	/**
	 * @param data a list of DatasourceRows
	 */
	public DefaultDatasourceRecordSet(List data) {
		this.data = data;
	}

	public DatasourceRow getRow(int rowNum) {
		return (DatasourceRow) data.get(rowNum);
	}

	public void addRow(DatasourceRow dsRow) {
		data.add(dsRow);
	}

	public int size() {
		return data.size();
	}

	public void clear() {
		data.clear();
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public Object[] toArray() {
		return data.toArray();
	}

	public Object get(int index) {
		return data.get(index);
	}

	public Object remove(int index) {
		return data.remove(index);
	}

	public void add(int index, Object element) {
		data.add(index, element);
	}

	public int indexOf(Object o) {
		return data.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return data.lastIndexOf(o);
	}

	public boolean add(Object o) {
		return data.add(o);
	}

	public boolean contains(Object o) {
		return data.contains(o);
	}

	public boolean remove(Object o) {
		return data.remove(o);
	}

	public boolean addAll(int index, Collection c) {
		return data.addAll(index, c);
	}

	public boolean addAll(Collection c) {
		return data.addAll(c);
	}

	public boolean containsAll(Collection c) {
		return data.containsAll(c);
	}

	public boolean removeAll(Collection c) {
		return data.removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return data.retainAll(c);
	}

	public Iterator iterator() {
		return data.iterator();
	}

	public List subList(int fromIndex, int toIndex) {
		return data.subList(fromIndex, toIndex);
	}

	public ListIterator listIterator() {
		return data.listIterator();
	}

	public ListIterator listIterator(int index) {
		return data.listIterator(index);
	}

	public Object set(int index, Object element) {
		return data.set(index, element);
	}

	public Object[] toArray(Object a[]) {
		return data.toArray(a);
	}
}
