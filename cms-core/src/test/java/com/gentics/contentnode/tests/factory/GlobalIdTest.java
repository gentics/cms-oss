package com.gentics.contentnode.tests.factory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.contentnode.object.NodeObject.GlobalId;

public class GlobalIdTest {

	/**
	 * Tests the isGlobalId method.
	 */
	@Test
	public void testIsGlobalId() {

		assertTrue(GlobalId.isGlobalId("A547.74536"));
		assertTrue(GlobalId.isGlobalId("A547.74"));
		assertTrue(GlobalId.isGlobalId("A547.7"));
		assertFalse(GlobalId.isGlobalId("A547."));
		assertFalse(GlobalId.isGlobalId("A547.a"));
		assertFalse(GlobalId.isGlobalId("A547.aaa"));
		assertFalse(GlobalId.isGlobalId("2451214"));
		assertFalse(GlobalId.isGlobalId("."));
		assertFalse(GlobalId.isGlobalId("13113.113"));
		assertFalse(GlobalId.isGlobalId("13112."));
		assertTrue(GlobalId.isGlobalId("5D40.6d75467a-4b1c-11e5-a8d2-00270e06eab6"));
	}
}
