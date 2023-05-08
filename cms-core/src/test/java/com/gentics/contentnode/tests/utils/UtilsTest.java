package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.util.MiscUtils;

/**
 * Tests for utility methods
 */
public class UtilsTest {
	/**
	 * Test that doBuffered actually calls the handler with chunks of the iterable
	 * @throws NodeException
	 */
	@Test
	public void doBuffered() throws NodeException {
		StringBuffer out = new StringBuffer();
		MiscUtils.doBuffered(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3, nums -> {
			out.append(nums).append("-");
		});

		assertThat(out.toString()).as("Result").isEqualTo("[1, 2, 3]-[4, 5, 6]-[7, 8, 9]-[10]-");
	}

	/**
	 * Test that doBuffered unwraps a throws NodeException
	 */
	@Test
	public void doBufferedWithException() {
		StringBuffer out = new StringBuffer();
		try {
			MiscUtils.doBuffered(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 4, nums -> {
				if (nums.contains(9)) {
					throw new NodeException("I found 9");
				}
				out.append(nums).append("-");
			});
		} catch (NodeException e) {
			assertThat(e).as("Caught exception").hasMessage("I found 9");
		}
		assertThat(out.toString()).as("Result").isEqualTo("[1, 2, 3, 4]-[5, 6, 7, 8]-");
	}

	/**
	 * Test that doBuffered stops calling the handler, if the previous execution returned false
	 * @throws NodeException
	 */
	@Test
	public void doBufferedUntil() throws NodeException {
		StringBuffer out = new StringBuffer();
		MiscUtils.doBuffered(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 2, nums -> {
			if (nums.contains(7)) {
				return false;
			}
			out.append(nums).append("-");
			return true;
		});
		assertThat(out.toString()).as("Result").isEqualTo("[1, 2]-[3, 4]-[5, 6]-");
	}
}
