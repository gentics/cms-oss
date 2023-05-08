/*
 * @author erwin @date 10.11.2003
 * @version $Id: GenticsAttributeComparator.java,v 1.2 2005/06/01 08:34:08
 *          laurin Exp $
 */

package com.gentics.lib.content;

import java.sql.SQLException;
import java.util.Comparator;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.db.DBHandle;

public class GenticsAttributeComparator implements Comparator {
	private String m_attribute;

	private int m_datatype;

	private DBHandle m_handle;

	public GenticsAttributeComparator(DBHandle m_handle) {
		this.m_handle = m_handle;
	}

	public GenticsAttributeComparator(String attribute) throws SQLException {
		m_attribute = attribute;
		try {
			m_datatype = DatatypeHelper.getDatatype(m_handle, attribute);
		} catch (CMSUnavailableException e) {
			e.printStackTrace();
		} catch (NodeIllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof GenticsAttributeComparator) {
			return m_attribute.equals(((GenticsAttributeComparator) obj).m_attribute);
		}
		return false;
	}

	public void printStatistics() {}

	public int compare(Object o1, Object o2) {
		// if ( 1 < 2 ) return 0;
		if (o1 instanceof GenticsContentObject && o2 instanceof GenticsContentObject) {
			GenticsContentObject gco1 = (GenticsContentObject) o1, gco2 = (GenticsContentObject) o2;

			try {
				return DatatypeHelper.compare(gco1.getAttribute(m_attribute), gco2.getAttribute(m_attribute), m_datatype);
			} catch (CMSUnavailableException cue) {
				return 0;
			} catch (NodeIllegalArgumentException e) {
				return 0;
			}

		}
		return 0;
	}
}
