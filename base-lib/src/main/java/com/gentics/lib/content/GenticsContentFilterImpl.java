package com.gentics.lib.content;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 20.10.2003
 */
public class GenticsContentFilterImpl implements GenticsContentFilter {
	public static class FilterCondition {
		private int m_match;

		private Object m_value = null;

		private Object[] m_values = null;

		private String m_name;

		private boolean m_multiValue = false;

		private int m_dataType;

		public FilterCondition(FilterCondition clone) {
			m_multiValue = clone.m_multiValue;
			m_match = clone.m_match;

			if (clone.m_value != null) {
				m_value = createValue(clone.m_value.toString(), clone.m_dataType);
			}
			if (clone.m_values != null) {
				m_values = new Object[clone.m_values.length];
				for (int i = 0; i < clone.m_values.length; i++) {
					if (clone.m_values[i] != null) {
						m_values[i] = createValue(clone.m_values[i].toString(), clone.m_dataType);
					} else {
						m_values[i] = null;
					}

				}
			}
			if (clone.m_name != null) {
				m_name = new String(clone.m_name);
			} else {
				m_name = null;
			}
			m_dataType = clone.m_dataType;
		}

		public FilterCondition(String name, String value, int match, int dataType) {
			m_name = name;
			m_match = match;
			m_value = createValue(value, dataType);
			m_dataType = dataType;
		}

		public FilterCondition(String name, String[] values, int match, int dataType) {
			m_name = name;
			m_match = match;
			m_values = new Object[values.length];
			for (int i = 0; i < values.length; i++) {
				m_values[i] = createValue(values[i], dataType);
			}
			m_multiValue = true;
			m_dataType = dataType;
		}

		private Object createValue(String value, int dataType) {
			switch (dataType) {
			case GenticsContentAttribute.ATTR_TYPE_INTEGER:
				return new Integer(value);

			default:
				return value;
			}
		}

		public boolean isNull() {
			return m_value == null && m_values == null;
		}

		public int getMatch() {
			return m_match;
		}

		public Object getValue() {
			return m_value;
		}

		public Object[] getValues() {
			if (this.isMultivalue()) {
				return m_values;
			}
			return new Object[] { m_value };
		}

		public String getName() {
			return m_name;
		}

		public boolean isMultivalue() {
			return m_multiValue;
		}

		public int getDatatype() {
			return m_dataType;
		}
	}

	Collection m_conditions;

	public GenticsContentFilterImpl(GenticsContentFilterImpl clone) {
		m_conditions = new Vector(clone.m_conditions.size());
		for (Iterator iterator = clone.m_conditions.iterator(); iterator.hasNext();) {
			FilterCondition condition = (FilterCondition) iterator.next();

			m_conditions.add(new FilterCondition(condition));
		}
	}

	public GenticsContentFilterImpl() {
		m_conditions = new Vector();
	}

	public void addCondition(String name, FilterCondition c) {
		m_conditions.add(c);
	}

	public Iterator iterator() {
		return m_conditions.iterator();
	}

	public static FilterCondition createFilterCondition(String name, String value, int match,
			int dataType) {
		return new FilterCondition(name, value, match, dataType);
	}

	public static FilterCondition createFilterCondition(String name, String[] value, int match,
			int dataType) {
		return new FilterCondition(name, value, match, dataType);
	}
}
