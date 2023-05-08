package com.gentics.contentnode.object;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.gentics.api.lib.exception.NodeException;

/**
 * Tests the functionality of the EditableValueList class
 * @author escitalopram
 *
 */
public class EditableValueListTest {
	@Mock
	private Value v1;
	@Mock
	private Value v2;
	@Mock
	private Value v3;

	EditableValueList testObject;

	/**
	 * Prepare mocked objects
	 * @throws NodeException
	 */
	@Before
	public void setupObjects() throws NodeException {
		MockitoAnnotations.initMocks(this);
		when(v1.getId()).thenReturn(1);
		when(v2.getId()).thenReturn(2);
		when(v3.getId()).thenReturn(3);

		Part p1 = mock(Part.class);
		Part p2 = mock(Part.class);
		Part p3 = mock(Part.class);

		when(p1.getKeyname()).thenReturn("key1");
		when(p2.getKeyname()).thenReturn("key2");
		when(p3.getKeyname()).thenReturn("key3");

		when(p1.getPartOrder()).thenReturn(10);
		when(p2.getPartOrder()).thenReturn(5);
		when(p3.getPartOrder()).thenReturn(5);

		when(p1.getId()).thenReturn(11);
		when(p2.getId()).thenReturn(12);
		when(p3.getId()).thenReturn(13);
		when(v1.getPartId()).thenReturn(11);
		when(v2.getPartId()).thenReturn(12);
		when(v3.getPartId()).thenReturn(13);

		when(v1.getPart()).thenReturn(p1);
		when(v2.getPart()).thenReturn(p2);
		when(v3.getPart()).thenReturn(p3);
		when(v1.getPart(false)).thenReturn(p1);
		when(v2.getPart(false)).thenReturn(p2);
		when(v3.getPart(false)).thenReturn(p3);

		testObject = new EditableValueList("blargh");
	}

	/**
	 * test the add(all) methods
	 */
	@Test
	public void testAddAddAll() {
		assertTrue("Must return true for new object", testObject.add(v1));
		assertEquals("size() must be correct", 1, testObject.size());
		assertEquals("key must be accessible", v1, testObject.getByKeyname("key1"));
		assertEquals("part id must be accessible", v1, testObject.getByPartId(11));
		assertTrue("value must be contained", testObject.contains(v1));
		assertFalse("Must return false for already contained object", testObject.add(v1));
		assertTrue("addAll must return true for new object", testObject.addAll(Arrays.asList(v1, v2, v3)));
		assertEquals("size() must be correct", 3, testObject.size());
		assertFalse("addAll must return false when not changing anything", testObject.addAll(Arrays.asList(v1, v2, v3)));
		assertEquals("size() must be correct", 3, testObject.size());
		
	}

	/**
	 * test the remove method
	 */
	@Test
	public void testRemoveAll() {
		testObject.add(v1);
		testObject.add(v2);
		assertTrue("Must return true for removed objects", testObject.removeAll(Arrays.asList(v1, v3)));
		assertEquals("size() must be correct", 1, testObject.size());
		assertNull("key must be removed", testObject.getByKeyname("key1"));
		assertNull("partid must be removed", testObject.getByPartId(11));
		assertFalse("Must not contain removed object", testObject.contains(v1));
		assertFalse("Must return true for not removed object", testObject.remove(v1));
		assertTrue("Other object must still be contained", testObject.contains(v2));
		assertFalse("removeAll with no change must return false", testObject.removeAll(Arrays.asList(v1,v3)));
	}

	/**
	 * test the clear method
	 */
	@Test
	public void testClear() {
		testObject.add(v1);
		testObject.clear();
		assertEquals("size() must be correct", 0, testObject.size());
		assertNull("key must be removed", testObject.getByKeyname("key1"));
		assertNull("partid must be removed", testObject.getByPartId(11));
		assertFalse("Must return true for not removed object", testObject.remove(v1));
		assertFalse("Must return true for not removed object", testObject.remove(v2));
		
	}

	/**
	 * test iteration (including order)
	 */
	@Test
	public void testIterator() {
		testObject.add(v1);
		testObject.add(v3);
		testObject.add(v2);
		Iterator<Value> it = testObject.iterator();
		assertTrue("iterator must have first object", it.hasNext());
		assertSame("expected object not encountered", v2, it.next());
		assertTrue("iterator must have second object", it.hasNext());
		assertSame("expected object not encountered", v3, it.next());
		assertTrue("iterator must have third object", it.hasNext());
		assertSame("expected object not encountered", v1, it.next());
		assertFalse("iterator must not have fourth object", it.hasNext());
	}

	/**
	 * test removing using an iterator
	 */
	@Test
	public void testIteratorRemove() {
		testObject.add(v1);
		testObject.add(v2);
		testObject.add(v3);

		Iterator<Value> it = testObject.iterator();
		assertTrue("iterator must have first object", it.hasNext());
		assertSame("expected object not encountered", v2, it.next());
		it.remove();
		// Iteration must continue normally
		assertTrue("iterator must have second object", it.hasNext());
		assertSame("expected object not encountered", v3, it.next());
		assertTrue("iterator must have third object", it.hasNext());
		assertSame("expected object not encountered", v1, it.next());
		assertFalse("iterator must not have fourth object", it.hasNext());

		it = testObject.iterator();
		assertTrue("iterator must have first object", it.hasNext());
		assertSame("expected object not encountered", v3, it.next());
		assertTrue("iterator must have second object", it.hasNext());
		assertSame("expected object not encountered", v1, it.next());
		assertFalse("iterator must not have a third object", it.hasNext());

		assertEquals("size() must be correct", 2, testObject.size());
		assertNull("key must be removed", testObject.getByKeyname("key2"));
		assertNull("partid must be removed", testObject.getByPartId(22));
	}

	/**
	 * test isEmpty method
	 */
	@Test
	public void testIsEmpty(){
		assertTrue("newly created object must be empty", testObject.isEmpty());
		testObject.add(v1);
		assertFalse("object must be empty", testObject.isEmpty());
		testObject.clear();
		assertTrue("cleared object must be empty", testObject.isEmpty());
	}

	/**
	 * test the retainAll method
	 */
	@Test
	public void testRetainAll() {
		testObject.add(v1);
		testObject.add(v2);
		assertFalse("retainAll without change must return false", testObject.retainAll(Arrays.asList(v1,v2,v3)));
		assertEquals("wrong size detected", 2, testObject.size());
		assertTrue("retainAll with change must return true", testObject.retainAll(Arrays.asList(v1)));
		assertEquals("wrong size detected", 1, testObject.size());
		assertTrue("v1 must be contained", testObject.contains(v1));
	}

	/**
	 * test Resolvable functionality
	 */
	@Test
	public void testResolvable() {
		testObject.add(v1);
		assertTrue("must be able to resolve", testObject.canResolve());
		assertEquals("unique_tag_id must be returned", "blargh", testObject.get("unique_tag_id"));
		assertSame("v1 must be returned", v1, testObject.get("key1"));
		assertNull("Non existing value must return as null", testObject.get("non-existing-key"));
		assertSame("get must return same thing as getProperty", testObject.get("unique_tag_id"), testObject.getProperty("unique_tag_id"));
		assertSame("get must return same thing as getProperty", testObject.get("key1"), testObject.getProperty("key1"));
		assertSame("get must return same thing as getProperty", testObject.get("non-existing-key"), testObject.getProperty("non-existing-key"));
	}
}
