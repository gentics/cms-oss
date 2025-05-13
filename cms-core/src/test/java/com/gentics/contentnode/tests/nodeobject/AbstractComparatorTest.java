package com.gentics.contentnode.tests.nodeobject;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.etc.StringUtils;

/**
 * Abstract base class for comparator tests
 *
 * @param <T> type of sorted elements
 */
public abstract class AbstractComparatorTest<T> {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	/**
	 * Comparator for checking ascending order
	 */
	protected Comparator<String> ascending = new Comparator<String>() {
		@Override
		public int compare(String s1, String s2) {
			return StringUtils.mysqlLikeCompare(s1, s2);
		}
	};

	/**
	 * Comparator for checking descending order
	 */
	protected Comparator<String> descending = new Comparator<String>() {
		@Override
		public int compare(String s1, String s2) {
			return -StringUtils.mysqlLikeCompare(s1, s2);
		}
	};

	/**
	 * Objects, which will be sorted
	 */
	protected List<T> objects;

	@Before
	public void setup() throws NodeException {
		objects = getObjects();
		assertThat(objects).as("Objects to be sorted").areAtLeast(2, new Condition<>(s -> true, "items"));
	}

	/**
	 * Test ascending order
	 * @throws NodeException
	 */
	@Test
	public void testAscending() throws NodeException {
		operate(() -> sortAscending());
		assertThat(getNames()).as("Object names").isSortedAccordingTo(ascending);
	}

	/**
	 * Test descending order
	 * @throws NodeException
	 */
	@Test
	public void testDescending() throws NodeException {
		operate(() -> sortDescending());
		assertThat(getNames()).as("Object names").isSortedAccordingTo(descending);
	}

	/**
	 * Test ascending order with null elements
	 * @throws NodeException
	 */
	@Test
	public void testAscendingWithNull() throws NodeException {
		objects.add(null);
		operate(() -> sortAscending());
		assertThat(getNames()).as("Object names").isSortedAccordingTo(ascending);
	}

	/**
	 * Test descending order with null elements
	 * @throws NodeException
	 */
	@Test
	public void testDescendingWithNull() throws NodeException {
		objects.add(null);
		operate(() -> sortDescending());
		assertThat(getNames()).as("Object names").isSortedAccordingTo(descending);
	}

	/**
	 * Get the names of the items (in order)
	 * @return sorted list of names
	 * @throws NodeException
	 */
	protected List<String> getNames() throws NodeException {
		return supply(() -> objects.stream().map(c -> Optional.ofNullable(c).map(o -> getName(o)).orElse(null))
				.collect(Collectors.toList()));
	}

	/**
	 * Get the name of the object
	 * @param object object
	 * @return name
	 */
	protected abstract String getName(T object);

	/**
	 * Get the list of objects to be sorted
	 * @return list of objects
	 * @throws NodeException
	 */
	protected abstract List<T> getObjects() throws NodeException;

	/**
	 * Sort the objects in ascending order
	 * @throws NodeException
	 */
	protected abstract void sortAscending() throws NodeException;

	/**
	 * Sort the objects in descending order
	 * @throws NodeException
	 */
	protected abstract void sortDescending() throws NodeException;
}
