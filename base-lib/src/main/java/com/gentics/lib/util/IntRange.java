package com.gentics.lib.util;

/**
 * created at Dec 9, 2003
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class IntRange {
	private int lowerBound, upperBound;

	public IntRange(int lowerBound, int upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public boolean contains(int value) {
		return value >= lowerBound && value <= upperBound;
	}
}
