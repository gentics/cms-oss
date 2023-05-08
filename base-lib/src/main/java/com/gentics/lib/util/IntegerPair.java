package com.gentics.lib.util;

import java.io.Serializable;

/**
 * Class for pairs of integers
 */
public class IntegerPair implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6438961585303302777L;

	/**
	 * First Integer
	 */
	protected int i1;

	/**
	 * Second Integer
	 */
	protected int i2;

	/**
	 * Create an instance
	 * @param i1 first integer
	 * @param i2 second integer
	 */
	public IntegerPair(int i1, int i2) {
		this.i1 = i1;
		this.i2 = i2;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IntegerPair) {
			return this.i1 == ((IntegerPair) o).i1 && this.i2 == ((IntegerPair) o).i2;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return i1 + i2;
	}
}
