package com.gentics.contentnode.tests.publish;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.github.jknack.handlebars.helper.HelperFunction;

/**
 * Helper Source for the gtx_test_range
 */
public class RangeHelperSource {
	/**
	 * Helper to get a list containing the integers from 0 to the given length
	 * @param length length of the integer list
	 * @return integer list
	 */
	@HelperFunction("gtx_test_range")
	public static Object getRange(Integer length) {
		List<Integer> list = new ArrayList<>();
		IntStream.range(0, length).forEach(i -> list.add(i));
		return list;
	}
}
