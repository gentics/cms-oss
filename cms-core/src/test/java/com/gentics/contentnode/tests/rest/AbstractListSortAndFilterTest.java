package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.lib.etc.StringUtils.mysqlLikeCompare;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.RandomStringGenerator;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract base class for tests that get filtered and sorted list of entities from REST Resources
 *
 * @param <T> type of the returned entities
 */
@RunWith(Parameterized.class)
public abstract class AbstractListSortAndFilterTest<T> {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	/**
	 * Random string generator
	 */
	public final static RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
			.withinRange(new char[][] { { 'a', 'z' }, { 'A', 'Z' } }).build();

	/**
	 * Number of generated items (for tests, that generate items)
	 */
	public final static int NUM_ITEMS = 100;

	protected final static Random random = new Random();

	/**
	 * Tested items. The items are either generated via {@link #createItem()} or may be filled into the list via {@link #fillItemsList(List)}
	 */
	protected static List<? super Object> items = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();
	}

	/**
	 * Generate the test variation data for the sorted and filtered attributes
	 * @param <V> type of tested entity
	 * @param sortAttributes list of sortable attributes (pairs of name and function, which extracts the value from an entity)
	 * @param filterAttributes list of filterable attributes (pairs of name and function, which extracts the value from an entity)
	 * @return collection of variation data
	 */
	protected static <V> Collection<Object[]> data(List<Pair<String, Function<V, String>>> sortAttributes,
			List<Pair<String, Function<V, String>>> filterAttributes) {
		Collection<Object[]> data = new ArrayList<>();
		for (Pair<String, Function<V, String>> sortBy : sortAttributes) {
			for (boolean ascending : Arrays.asList(true, false)) {
				for (Pair<String, Function<V, String>> filterBy : filterAttributes) {
					data.add(new Object[] { sortBy.getLeft(), sortBy.getRight(), ascending, filterBy.getLeft(),
							filterAttributes });
				}
				data.add(new Object[] { sortBy.getLeft(), sortBy.getRight(), ascending, null, null });
			}
		}
		for (Pair<String, Function<V, String>> filterBy : filterAttributes) {
			data.add(new Object[] { null, null, true, filterBy.getLeft(), filterAttributes });
		}
		return data;
	}

	/**
	 * Helper method to remove leading zeros from the given value
	 * @param value value
	 * @return value with leading zeros removed
	 */
	protected static String removeLeadingZeros(String value) {
		while (StringUtils.startsWith(value, "0")) {
			value = StringUtils.removeStart(value, "0");
		}
		return value;
	}

	/**
	 * Pad the given number with leading zeros. This method must be used for any integer attributes, so that sorting the values (lexicographically) is identical with sorting the numbers
	 * @param number number
	 * @return number as string padded with leading zeros
	 */
	protected static String addLeadingZeros(int number) {
		return StringUtils.leftPad(Integer.toString(number), 10, "0");
	}

	/**
	 * Get a random entry from the given list
	 * @param <R> type of entries in the list
	 * @param items list
	 * @return random entry
	 */
	protected static <R> R getRandomEntry(List<R> items) {
		if (items.isEmpty()) {
			return null;
		}
		return items.get(random.nextInt(items.size()));
	}

	/**
	 * Get a random entry from the given array
	 * @param <R> type of entries in the array
	 * @param items array
	 * @return random entry
	 */
	protected static <R> R getRandomEntry(R[] items) {
		if (items.length == 0) {
			return null;
		}
		return items[random.nextInt(items.length)];
	}

	/**
	 * Do test setup
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		// if items list is still empty, we run setup the first time, so fill the items with some data
		if (items.isEmpty()) {
			fillItemsList(items);
		}

		// if the result shall be filtered, select a random element from the list and get the value of the filtered attribute
		if (!StringUtils.isEmpty(filterBy)) {
			// select a random item
			@SuppressWarnings("unchecked")
			T selectedItem = (T) items.get(random.nextInt(items.size()));

			// get the filtered attribute value from the item
			query = filterAttributes.stream().filter(pair -> StringUtils.equals(pair.getLeft(), filterBy)).findFirst()
					.map(pair -> pair.getRight().apply(selectedItem)).orElse(null);
			// remove leading zeros
			query = removeLeadingZeros(query);
		}
	}

	/**
	 * Attribute to be sorted (may be null)
	 */
	@Parameter(0)
	public String sortBy;

	/**
	 * Function that extracts the sorted attribute from entities (null when {@link #sortBy} is null)
	 */
	@Parameter(1)
	public Function<T, String> sortAttributeExtractor;

	/**
	 * True to sort ascending, false for descending
	 */
	@Parameter(2)
	public boolean sortAscending;

	/**
	 * Attribute which is filtered (may be null). The attribute is not used in the
	 * call to the REST API, but is used to extract the value from a random entry,
	 * which is then used as search string
	 */
	@Parameter(3)
	public String filterBy;

	/**
	 * Filter attributes list
	 */
	@Parameter(4)
	public List<Pair<String, Function<T, String>>> filterAttributes;

	/**
	 * Value to be used in the REST call (may be null for no filtering)
	 */
	protected String query;

	/**
	 * Test sorting an filtering
	 * @throws NodeException
	 */
	@Test
	public void testSortingAndFiltering() throws NodeException {
		AbstractListResponse<T> result = supply(() -> {
			SortParameterBean sort = new SortParameterBean();
			if (sortBy != null) {
				sort.setSort(String.format("%s%s", sortAscending ? "+" : "-", sortBy));
			}
			FilterParameterBean filter = new FilterParameterBean().setQuery(query);
			PagingParameterBean paging = new PagingParameterBean();
			return getResult(sort, filter, paging);
		});
		assertResponseOK(result);

		int expectedItems = StringUtils.isEmpty(query) ? items.size() : 1;
		Condition<String> items = new Condition<>(s -> true, "items");

		String description = StringUtils.isEmpty(query) ? "Result list (unfiltered)" : String.format("Result list filtered by '%s'", query);

		if (sortBy != null) {
			assertThat(result.getItems().stream().map(sortAttributeExtractor::apply)).as(description)
					.areAtLeast(expectedItems, items).isSortedAccordingTo((s1, s2) -> {
						if (s1 == null && s2 == null) {
							return 0;
						} else if (s1 == null) {
							return sortAscending ? -1 : 1;
						} else if (s2 == null) {
							return sortAscending ? 1 : -1;
						} else {
							return mysqlLikeCompare(s1, s2) * (sortAscending ? 1 : -1);
						}
					});
		}

		if (!StringUtils.isEmpty(query)) {
			assertThat(result.getItems().stream().map(item -> {
				return filterAttributes.stream().map(Pair::getRight).map(extractor -> extractor.apply(item))
						.map(AbstractListSortAndFilterTest::removeLeadingZeros).collect(Collectors.toList())
						.toArray(new String[filterAttributes.size()]);
			})).as(description).isNotEmpty().allMatch(values -> {
				return Stream.of(values).filter(value -> StringUtils.containsIgnoreCase(value, query)).findAny().isPresent();
			});
		}
	}

	/**
	 * Method to fill the entity list {@link #items} with items. The default
	 * implementation will call {@link #createItem()} multiple times to create a
	 * random item and fill it into the list. This method may be overwritten to fill
	 * the list with existing items
	 * 
	 * @param items list of items to be filled
	 * @throws NodeException
	 */
	protected void fillItemsList(List<? super Object> items) throws NodeException {
		for (int i = 0; i < NUM_ITEMS; i++) {
			items.add(createItem());
		}
	}

	/**
	 * Create a random test item
	 * @return random test item (or null, if {@link #fillItemsList(List)} is overwritten to use existing items)
	 * @throws NodeException
	 */
	protected abstract T createItem() throws NodeException;

	/**
	 * Call the tested method of the resource implementation
	 * @param sort sorting parameters
	 * @param filter filtering parameters
	 * @param paging paging parameters
	 * @return list response
	 * @throws NodeException
	 */
	protected abstract AbstractListResponse<T> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException;
}
