package com.gentics.contentnode.factory;

/**
 * AutoCloseable which temporarily sets a transaction attribute
 */
public class TrxAttribute implements AutoCloseable {
	/**
	 * Modified transaction
	 */
	protected Transaction t;

	/**
	 * Attribute name
	 */
	protected String name;

	/**
	 * Old attribute value
	 */
	protected Object oldValue;

	/**
	 * Create an instance with the given attribute value
	 * @param name attribute name
	 * @param value attribute value
	 * @return instance
	 * @throws TransactionException
	 */
	public static TrxAttribute with(String name, Object value) throws TransactionException {
		return new TrxAttribute(name, value);
	}

	/**
	 * Create an instance
	 * @param name attribute name
	 * @param value attribute value
	 * @throws TransactionException
	 */
	public TrxAttribute(String name, Object value) throws TransactionException {
		t = TransactionManager.getCurrentTransaction();
		oldValue = t.getAttributes().put(name, value);
	}

	@Override
	public void close() {
		if (oldValue == null) {
			t.getAttributes().remove(name);
		} else {
			t.getAttributes().put(name, oldValue);
		}
	}
}
