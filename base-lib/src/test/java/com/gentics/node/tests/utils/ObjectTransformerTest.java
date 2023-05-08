package com.gentics.node.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class ObjectTransformerTest {
	@Test
	public void testNullEmpty() {
		assertTrue("Check whether null is empty", ObjectTransformer.isEmpty(null));
	}

	@Test
	public void testObjectNotEmpty() {
		assertFalse("Check whether an object is empty", ObjectTransformer.isEmpty(new Object()));
	}

	@Test
	public void testEmptyStringEmpty() {
		assertTrue("Check whether an empty string is empty", ObjectTransformer.isEmpty(""));
	}

	@Test
	public void testNonEmptyStringNotEmpty() {
		assertFalse("Check whether a non empty string is empty", ObjectTransformer.isEmpty("this is not empty"));
	}

	@Test
	public void testEmptyCollectionEmpty() {
		assertTrue("Check whether an empty collection is empty", ObjectTransformer.isEmpty(new Vector<Object>()));
		assertTrue("Check whether Collections.emptyList() is empty", ObjectTransformer.isEmpty(Collections.emptyList()));
		assertTrue("Check whether Collections.emptySet() is empty", ObjectTransformer.isEmpty(Collections.emptySet()));
	}

	@Test
	public void testFloatConversion() {
		assertEquals(0.5f, ObjectTransformer.getFloat(Float.NaN, 0.5f), 0f);
		assertEquals(0.5f, ObjectTransformer.getFloat("NaN", 0.5f), 0f);
		assertEquals(0.3f, ObjectTransformer.getFloat("0.3f", 0.5f), 0f);
	}

	@Test
	public void testNonEmptyCollectionNotEmpty() {
		List<Object> nonEmpty = new Vector<Object>(1);

		nonEmpty.add(new Object());
		assertFalse("Check whether a non empty collection is empty", ObjectTransformer.isEmpty(nonEmpty));
		assertFalse("Check whether Collections.singleton() is empty", ObjectTransformer.isEmpty(Collections.singleton(new Object())));
		assertFalse("Check whether Collections.singletonList() is empty", ObjectTransformer.isEmpty(Collections.singletonList(new Object())));
	}

	@Test
	public void testEmptyArrayEmpty() {
		assertTrue("Check whether an empty array is empty", ObjectTransformer.isEmpty(new Object[] {}));
	}

	@Test
	public void testNonEmptyArrayNotEmpty() {
		assertFalse("Check whether a non empty array is empty", ObjectTransformer.isEmpty(new Object[1]));
	}

	@Test
	public void testEmptyMapEmpty() {
		assertTrue("Check whether an empty map is empty", ObjectTransformer.isEmpty(new HashMap<Object, Object>()));
		assertTrue("Check whether Collections.emptyMap() is empty", ObjectTransformer.isEmpty(Collections.emptyMap()));
	}

	@Test
	public void testNonEmptyMapNotEmpty() {
		Map<Object, Object> nonEmpty = new HashMap<Object, Object>();

		nonEmpty.put(new Object(), new Object());
		assertFalse("Check whether a non empty map is empty", ObjectTransformer.isEmpty(nonEmpty));
		assertFalse("Check whether Collections.singletonMap() is empty", ObjectTransformer.isEmpty(Collections.singletonMap(new Object(), new Object())));
	}

	@Test
	public void testNumberNotEmpty() {
		assertFalse("Check whether 0 is empty", ObjectTransformer.isEmpty(0));
		assertFalse("Check whether -1 is empty", ObjectTransformer.isEmpty(-1));
		assertFalse("Check whether 1 is empty", ObjectTransformer.isEmpty(1));
	}
}
