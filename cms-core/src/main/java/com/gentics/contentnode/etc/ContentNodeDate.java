/*
 * @author clemens
 * @date 25.01.2007
 * @version $Id: ContentNodeDate.java,v 1.7.6.1 2011-03-09 17:42:43 norbert Exp $
 */
package com.gentics.contentnode.etc;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.ResolvableBean;

/**
 * represents a Content.Node standard date (dd.M.yyyy)
 * @author clemens
 *
 */
public class ContentNodeDate extends ResolvableBean implements Comparable {

	/**
	 * generated serial version UID
	 */
	private static final long serialVersionUID = 8902247044149538928L;

	/**
	 * one date formatter will fit for all instances...
	 */
	protected transient static SimpleDateFormat df = new SimpleDateFormat("d.M.yyyy");

	/**
	 * another date formatter for compatibility with xnl properties
	 */
	protected transient static SimpleDateFormat fullDf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	protected int timestamp;
	protected String formattedDate;

	protected String fullFormattedDate;

	/**
	 * generates a ContentNodeDate object from a unix timestamp
	 * @param timestamp
	 */
	public ContentNodeDate(int timestamp) {
		this.timestamp = timestamp;
		formatDate();
	}

	/**
	 * Create a ContentNodeDate object from the given date
	 * @param date date object
	 */
	public ContentNodeDate(Date date) {
		if (date == null) {
			this.timestamp = 0;
		} else {
			this.timestamp = (int) (date.getTime() / 1000L);
		}
		formatDate();
	}

	/**
	 * render string representation (dd.M.yyyy)
	 */
	public String toString() {
		return formattedDate;
	}

	/**
	 * Get the date in full format
	 * @return full format date
	 */
	public String getFullFormat() {
		fullFormatDate();
		return fullFormattedDate;
	}

	/**
	 * retrieve internal timestamp
	 * @return unix timestamp
	 */
	public Integer getTimestamp() {
		return new Integer(timestamp);
	}
    
	public int getIntTimestamp() {
		// getTimestamp() generated too much garbage for my taste
		return timestamp;
	}

	/**
	 * Get the date as Date object
	 * @return Date object or null if the timestamp is set to 0
	 */
	public Date getDate() {
		return timestamp == 0 ? null : new Date((long) timestamp * 1000);
	}

	/**
	 * format the date in the full format (if not yet done)
	 */
	protected void fullFormatDate() {
		if (fullFormattedDate == null) {
			synchronized (fullDf) {
				fullFormattedDate = fullDf.format(new Date(timestamp * 1000L));
			}
		}
	}

	/**
	 * turn internal timestamp into a formatted date
	 */
	protected void formatDate() {
		if (formattedDate == null) {
			synchronized (df) {
				formattedDate = df.format(new Date(timestamp * 1000L));
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof ContentNodeDate) {
			return timestamp - ((ContentNodeDate) o).timestamp;
		} else if (o instanceof Date) {
			return timestamp - (int) (((Date) o).getTime() / 1000L);
		} else if (o instanceof Number) {
			return timestamp - ((Number) o).intValue();
		} else {
			Number num = ObjectTransformer.getNumber(o, null);

			if (num == null) {
				throw new ClassCastException("Cannot compare ContentNodeDate with {" + o + "}");
			} else {
				return timestamp - num.intValue();
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ContentNodeDate) {
			return ((ContentNodeDate) o).getIntTimestamp() == timestamp;
		} else {
			Integer oTime = ObjectTransformer.getInteger(0, null);
			if (oTime != null) {
				return oTime.intValue() == timestamp;
			} else {
				return false;
			}
		}
	}
}
