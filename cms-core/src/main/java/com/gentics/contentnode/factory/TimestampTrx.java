package com.gentics.contentnode.factory;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * AutoCloseable for temporarily changing the timestamp of a transaction
 */
public class TimestampTrx implements AutoCloseable {
	/**
	 * The transaction that was active when this object is created
	 */
	protected Transaction t = null;

	/**
	 * Old timestamp
	 */
	protected long oldTimestamp;

	/**
	 * Create an instance, that will add the given delta to the current timestamp
	 * @param delta delta to add
	 * @param unit unit of the delta
	 * @return instance
	 * @throws TransactionException
	 */
	public static TimestampTrx add(int delta, TimeUnit unit) throws TransactionException {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(TransactionManager.getCurrentTransaction().getTimestamp());
		cal.add(Calendar.MILLISECOND, (int) TimeUnit.MILLISECONDS.convert(delta, unit));

		return new TimestampTrx(cal.getTimeInMillis());
	}

	/**
	 * Create an instance with the given timestamp in milliseconds
	 * @param timestamp timestamp in ms
	 * @throws TransactionException
	 */
	public TimestampTrx(long timestamp) throws TransactionException {
		this.t = TransactionManager.getCurrentTransaction();
		oldTimestamp = t.getTimestamp();
		t.setTimestamp(timestamp);
	}

	/**
	 * Create an instance with the given timestamp in seconds
	 * @param timestamp timestamp in seconds
	 * @throws TransactionException
	 */
	public TimestampTrx(int timestamp) throws TransactionException {
		this(timestamp * 1000L);
	}

	@Override
	public void close() {
		t.setTimestamp(oldTimestamp);
	}
}
