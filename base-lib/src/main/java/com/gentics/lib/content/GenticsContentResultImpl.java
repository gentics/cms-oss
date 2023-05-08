package com.gentics.lib.content;

import java.util.Collection;
import java.util.Iterator;

import com.gentics.lib.base.CMSUnavailableException;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 20.10.2003
 */
public class GenticsContentResultImpl implements GenticsContentResult {
	private Collection m_contentObjects;

	private int m_status;

	private Iterator m_iterator;

	private int m_limit = 0;

	private int m_start = 0;

	private int m_count = 0;

	public GenticsContentResultImpl(Collection contentObjects) {
		this.m_contentObjects = contentObjects;
		if (m_contentObjects.size() == 0) {
			m_status = STATUS_NOTFOUND;
		} else {
			m_status = STATUS_OK;
		}
		m_iterator = m_contentObjects.iterator();
	}

	public GenticsContentResultImpl(Collection contentObjects, int limit) {
		this(contentObjects);
		m_limit = limit;
	}

	public GenticsContentResultImpl(Collection contentObjects, int start, int count) {
		this(contentObjects);
		if (count > 0) {
			m_limit = start + count;
		}
		m_start = start;
		for (int i = 0; i < start; i++) {
			if (!m_iterator.hasNext()) {
				break;
			}
			m_iterator.next();
		}
	}

	public GenticsContentResultImpl(GenticsContentResultImpl clone) {
		this(clone.m_contentObjects, clone.m_limit);
	}

	public GenticsContentResultImpl(GenticsContentResultImpl clone, int limit) {
		this(clone.m_contentObjects, limit);
	}

	public GenticsContentResultImpl(GenticsContentResultImpl clone, int start, int count) {
		this(clone.m_contentObjects, start, count);
	}

	public int getStatus() {
		return m_status;
	}

	public int size() {
		if (m_limit > 0) {
			return Math.min(m_contentObjects.size(), m_limit);
		}
		return m_contentObjects.size();
	}

	public GenticsContentObject getNextObject() throws CMSUnavailableException {
		if (!m_iterator.hasNext() || (m_count >= m_limit && m_limit > 0)) {
			return null;
		}
		m_count++;
		return (GenticsContentObject) m_iterator.next();
	}
}
