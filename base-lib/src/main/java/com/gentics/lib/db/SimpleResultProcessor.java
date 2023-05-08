package com.gentics.lib.db;

import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) <p/>Date: 11.12.2003
 */
public class SimpleResultProcessor implements ResultProcessor, Serializable, Iterable<SimpleResultRow> {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 5797564132003050668L;

	private List<SimpleResultRow> values = null;

	private TObjectIntHashMap nameHash = null;

	private int upperLimit = 0;

	private int lowerLimit = 0;

	private class ResultIterator implements Iterator<SimpleResultRow> {
		private SimpleResultProcessor m_result;

		private int pos = 1;

		public ResultIterator(SimpleResultProcessor result) {
			m_result = result;
		}

		public boolean hasNext() {
			return pos <= m_result.size();
		}

		public SimpleResultRow next() {
			if (!hasNext()) {
				throw new IllegalStateException("end of result");
			}
			return m_result.getRow(pos++);
		}

		public void remove() {
			throw new UnsupportedOperationException("not yet implemented");
		}
	}

	public void merge(SimpleResultProcessor p) {
		this.values.addAll(p.values);
	}

	public void takeOver(ResultProcessor p) {
		if (!(this instanceof SimpleResultProcessor)) {
			return;
		}
		SimpleResultProcessor toP = (SimpleResultProcessor) p;

		this.values = toP.values;
		this.upperLimit = toP.upperLimit;
		this.lowerLimit = toP.lowerLimit;
	}

	public SimpleResultProcessor() {
		values = new ArrayList<SimpleResultRow>();
	}

	public void process(ResultSet rs) throws SQLException {
		ResultSetMetaData data = rs.getMetaData();

		this.values = new ArrayList<SimpleResultRow>();
		this.nameHash = new TObjectIntHashMap(data.getColumnCount());

		for (int i = 0; i < data.getColumnCount(); i++) {
			nameHash.put(data.getColumnLabel(i + 1).toLowerCase(), i);
		}

		boolean hasNext = rs.next();

		if (!hasNext) {
			return;
		}

		int count = 0;
		int maxcount;

		if (lowerLimit > 0) {
			if (!rs.relative(lowerLimit)) {
				return;
			}
		}
		if (upperLimit > 0) {
			maxcount = upperLimit - lowerLimit;
		} else {
			maxcount = -1;
		}

		while (hasNext && (count < maxcount || maxcount < 0)) {
			addRow(nameHash, rs);
			count++;
			hasNext = rs.next();
		}
	}

	/**
	 * Adds a row to the given result from the given rowData.
	 * This method must not be called before {@link #process(ResultSet)} is called.
	 * 
	 * Only values mapped to column names that were present in the ResultSet of the last call to {@link #process(ResultSet)} will be taken from the given mao
	 * @param rowData
	 *            The values for the row to be added indexed by column name.
	 */
	public void addRow(Map<String, Object> rowData) throws SQLException {
		if (nameHash == null) {
			throw new SQLException("Cannot add additional rows before process() was called on the result processor");
		}
		Object[] values = new Object[nameHash.size()];

		for (int i = 0; i < nameHash.size(); i++) {
			Object key = nameHash.keys()[i];
			Object value = rowData.get(key);
			values[nameHash.get(key)] = value;
		}
		addRow(nameHash, values);
	}

	public void addRow(TObjectIntHashMap nameHash, Object[] values) {
		this.values.add(new SimpleResultRow(nameHash, values));
	}

	private void addRow(TObjectIntHashMap nameHash, ResultSet rs) throws SQLException {
		Object[] values = new Object[nameHash.size()];

		for (int i = 1; i <= nameHash.size(); i++) {
			Object value = rs.getObject(i);

			// if returned value is of type clob we read the string
			if (value instanceof Clob) {
				Clob clob = (Clob) value;

				values[i - 1] = clob.getSubString(1, (int) clob.length());
			} else if (value instanceof Blob) {
				// if returned value is of type Blob, we read the binary data
				Blob blob = (Blob) value;
				// check for very large data
				long length = blob.length();

				if (length > Integer.MAX_VALUE) {
					throw new SQLException("binary data with length > " + Integer.MAX_VALUE + " bytes not supported");
				} else {
					int iLength = (int) length;

					values[i - 1] = blob.getBytes(1, iLength);
				}
			} else {
				values[i - 1] = value;
			}
		}
		addRow(nameHash, values);
	}

	public void addRow(ResultSet rs) throws SQLException {
		ResultSetMetaData data = rs.getMetaData();
		TObjectIntHashMap names = new TObjectIntHashMap(data.getColumnCount());

		for (int i = 0; i < names.size(); i++) {
			names.put(data.getColumnLabel(i + 1).toLowerCase(), i);
		}
		addRow(names, rs);
	}

	public SimpleResultRow getRow(int nr) {
		return (SimpleResultRow) values.get(nr - 1);
	}

	public void addColumns(String[] columns) {
		for (int i = 1; i <= this.size(); i++) {
			SimpleResultRow row = this.getRow(i);

			row.expandCapacity(columns.length);
		}
		int j = nameHash.size();

		for (int i = 0; i < columns.length; i++) {
			String column = columns[i];

			nameHash.put(column, j++);
		}
	}

	public void addToRow(int nr, String colName, Object value) {
		SimpleResultRow m = getRow(nr);

		m.add(colName, value);
	}

	public void append(SimpleResultProcessor proc) {
		// if ( nameHash.equals( ))
		values.addAll(proc.values);
	}

	public int size() {
		if (values == null) {
			return 0;
		}
		return values.size();
	}

	public void setLimit(int lowerLimit, int upperLimit) {
		this.lowerLimit = lowerLimit;
		this.upperLimit = upperLimit;
	}

	public Iterator<SimpleResultRow> iterator() {
		return new ResultIterator(this);
	}

	public void clear() {
		this.values.clear();
	}

	/**
	 * Get the values of the result processor as modifiable list.
	 * Changes made to the list will be reflected by the result processor
	 * @return list of result rows
	 */
	public List<SimpleResultRow> asList() {
		return values;
	}
}
