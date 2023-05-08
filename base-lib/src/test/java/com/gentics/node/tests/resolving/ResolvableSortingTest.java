/*
 * @author norbert
 * @date 29.03.2007
 * @version $Id: ResolvableSortingTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.resolving;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.api.lib.resolving.ResolvableComparator;
import org.junit.experimental.categories.Category;

/**
 * Test case for testing the ResolvableComparator (sort Resolvables)
 */
@Category(BaseLibTest.class)
public class ResolvableSortingTest {

	/**
	 * Test sorting by a single attribute, ascending
	 * @throws Exception
	 */
	@Test
	public void testSingleAscSorting() throws Exception {
		// generate the test objects
		Resolvable a = new MyResolvable(1, "a");
		Resolvable b = new MyResolvable(2, "b");
		Resolvable c = new MyResolvable(3, "c");
		Resolvable d = new MyResolvable(4, "d");
		Resolvable e = new MyResolvable(5, "e");
		Resolvable f = new MyResolvable(6, "f");

		// generate an unsorted list
		List test = new Vector();

		test.add(d);
		test.add(b);
		test.add(a);
		test.add(e);
		test.add(c);
		test.add(f);
		// shuffle the list
		Collections.shuffle(test);

		// the reference list is sorted
		List reference = new Vector();

		reference.add(a);
		reference.add(b);
		reference.add(c);
		reference.add(d);
		reference.add(e);
		reference.add(f);

		// sort the test list
		Collections.sort(test, new ResolvableComparator("first", Datasource.SORTORDER_ASC));

		// check results
		assertEquals("Check sorted lists", reference, test);
	}

	/**
	 * Test sorting by a single attribute, descending
	 * @throws Exception
	 */
	@Test
	public void testSingleDescSorting() throws Exception {
		// generate the test objects
		Resolvable a = new MyResolvable(1, "a");
		Resolvable b = new MyResolvable(2, "b");
		Resolvable c = new MyResolvable(3, "c");
		Resolvable d = new MyResolvable(4, "d");
		Resolvable e = new MyResolvable(5, "e");
		Resolvable f = new MyResolvable(6, "f");

		// generate an unsorted list
		List test = new Vector();

		test.add(d);
		test.add(b);
		test.add(a);
		test.add(e);
		test.add(c);
		test.add(f);
		// shuffle the list
		Collections.shuffle(test);

		// the reference list is sorted
		List reference = new Vector();

		reference.add(f);
		reference.add(e);
		reference.add(d);
		reference.add(c);
		reference.add(b);
		reference.add(a);

		// sort the test list
		Collections.sort(test, new ResolvableComparator("second", Datasource.SORTORDER_DESC));

		// check results
		assertEquals("Check sorted lists", reference, test);
	}

	/**
	 * Test sorting by two attributes, ascending
	 * @throws Exception
	 */
	@Test
	public void testMultiAscSorting() throws Exception {
		// generate the test objects
		Resolvable a1 = new MyResolvable(1, "a");
		Resolvable a2 = new MyResolvable(2, "a");
		Resolvable a3 = new MyResolvable(3, "a");
		Resolvable b1 = new MyResolvable(1, "b");
		Resolvable b2 = new MyResolvable(2, "b");
		Resolvable b3 = new MyResolvable(3, "b");
		Resolvable c1 = new MyResolvable(1, "c");
		Resolvable c2 = new MyResolvable(2, "c");
		Resolvable c3 = new MyResolvable(3, "c");

		// generate an unsorted list
		List test = new Vector();

		test.add(a3);
		test.add(a2);
		test.add(a1);
		test.add(b2);
		test.add(b3);
		test.add(b1);
		test.add(c3);
		test.add(c1);
		test.add(c2);
		// shuffle the list
		Collections.shuffle(test);

		// the reference list is sorted
		List reference = new Vector();

		reference.add(a1);
		reference.add(a2);
		reference.add(a3);
		reference.add(b1);
		reference.add(b2);
		reference.add(b3);
		reference.add(c1);
		reference.add(c2);
		reference.add(c3);

		// sort the test list
		Collections.sort(test, new ResolvableComparator(new Datasource.Sorting[] {
			new Datasource.Sorting("second", Datasource.SORTORDER_ASC), new Datasource.Sorting("first", Datasource.SORTORDER_ASC)}));

		// check results
		assertEquals("Check sorted lists", reference, test);
	}

	@Test

	public void testMultiMixedSorting() throws Exception {
		// generate the test objects
		Resolvable a1 = new MyResolvable(1, "a");
		Resolvable a2 = new MyResolvable(2, "a");
		Resolvable a3 = new MyResolvable(3, "a");
		Resolvable b1 = new MyResolvable(1, "b");
		Resolvable b2 = new MyResolvable(2, "b");
		Resolvable b3 = new MyResolvable(3, "b");
		Resolvable c1 = new MyResolvable(1, "c");
		Resolvable c2 = new MyResolvable(2, "c");
		Resolvable c3 = new MyResolvable(3, "c");

		// generate an unsorted list
		List test = new Vector();

		test.add(a3);
		test.add(a2);
		test.add(a1);
		test.add(b2);
		test.add(b3);
		test.add(b1);
		test.add(c3);
		test.add(c1);
		test.add(c2);
		// shuffle the list
		Collections.shuffle(test);

		// the reference list is sorted
		List reference = new Vector();

		reference.add(a3);
		reference.add(b3);
		reference.add(c3);
		reference.add(a2);
		reference.add(b2);
		reference.add(c2);
		reference.add(a1);
		reference.add(b1);
		reference.add(c1);

		// sort the test list
		Collections.sort(test, new ResolvableComparator(new Datasource.Sorting[] {
			new Datasource.Sorting("first", Datasource.SORTORDER_DESC), new Datasource.Sorting("second", Datasource.SORTORDER_ASC)}));

		// check results
		assertEquals("Check sorted lists", reference, test);
	}

	/**
	 * Test resolvable with two attributes
	 */
	public static class MyResolvable extends ResolvableBean {
		protected int first;

		protected String second;

		/**
		 * Get the first attribute
		 * @return first attribute value
		 */
		public int getFirst() {
			return first;
		}

		/**
		 * Get the second attribute
		 * @return second attribute value
		 */
		public String getSecond() {
			return second;
		}

		/**
		 * @param first
		 * @param second
		 */
		public MyResolvable(int first, String second) {
			this.first = first;
			this.second = second;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return first + " - " + second;
		}
	}
}
