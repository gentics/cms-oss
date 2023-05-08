package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.node.testutils.QueryCounter;

/**
 * Test cases for batch-loading of attributes
 */
@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class MCCRBatchLoadTest extends AbstractMCCRCacheTest {

	/**
	 * Number of objects
	 */
	public final static int NUM_OBJECTS = 100;

	/**
	 * Maximum number of allowed db statements for batchloading
	 */
	public final static int ALLOWED_DB_STATEMENTS = 10;

	/**
	 * Currently tested attribute
	 */
	protected String attribute;

	/**
	 * Attribute value
	 */
	protected Object value;

	/**
	 * Name of the attribute, that is combined with the current attribute in the combine test
	 */
	protected static String combiAttribute = "text";

	@Parameters(name = "{index}: {0}, empty: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new Vector<Object[]>();

		for (String name : MCCRTestDataHelper.ATTRIBUTE_NAMES) {
			// omit the link attributes here
			if (name.startsWith("link")) {
				continue;
			}
			for (boolean empty : Arrays.asList(true, false)) {
				data.add(new Object[] { name, empty });
			}
		}
		return data;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Map<Integer, List<MCCRObject>> channelObjects = new HashMap<Integer, List<MCCRObject>>();

		for (int channelId : CHANNEL_IDS) {
			List<MCCRObject> objectsInChannel = new ArrayList<MCCRObject>();

			for (int i = 1; i <= NUM_OBJECTS; i++) {
				Map<String, Object> dataMap = new HashMap<String, Object>();

				dataMap.put(attribute, getModifiedAttributeValue(i, value));
				if (!attribute.equals(combiAttribute)) {
					dataMap.put(combiAttribute, getModifiedAttributeValue(i, ATTRIBUTE_DATA.get(combiAttribute)));
				}
				// let the objects link cyclic
				dataMap.put("link", getTargetContentId(i));
				dataMap.put("link_opt", getTargetContentId(i));
				dataMap.put("link_multi", Collections.singletonList(getTargetContentId(i)));
				MCCRObject object = MCCRTestDataHelper.createObject(ds, channelId, i, getContentId(i), dataMap);

				objectsInChannel.add(object);
			}
			channelObjects.put(channelId, objectsInChannel);
		}
	}

	/**
	 * Create an instance for testing the given attribute
	 * @param attribute attribute
	 * @param empty true if the test shall use empty attribute values
	 */
	public MCCRBatchLoadTest(String attribute, boolean empty) {
		this.attribute = attribute;
		if (empty) {
			if (attribute.endsWith("_multi")) {
				value = Collections.emptyList();
			} else {
				value = null;
			}
		} else {
			value = ATTRIBUTE_DATA.get(attribute);
		}
	}

	/**
	 * Test batch loading of attributes for many objects
	 * @throws Exception
	 */
	@Test
	public void testBatchLoad() throws Exception {
		for (int channelId: CHANNEL_IDS) {
			ds.setChannel(channelId);
			// get all objects
			QueryCounter queryCounter = new QueryCounter(false, true);
			Collection<Resolvable> objects = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), new String[] { attribute });

			assertTrue("Only " + ALLOWED_DB_STATEMENTS + " db statements were allowed to batchload, but " + queryCounter.getCount() + " were used",
					queryCounter.getCount() <= ALLOWED_DB_STATEMENTS);

			// get the query counter
			queryCounter = new QueryCounter(false, true);
			// access the attribute for all objects
			for (Resolvable res : objects) {
				Object readValue = res.get(attribute);

				// do an assertion for the accessed value
				MCCRTestDataHelper.assertData("Check data",
						getModifiedAttributeValue(ObjectTransformer.getInt(res.get("obj_id"), 0), value), readValue);
			}

			// we don't expect any DB access
			assertEquals("Check # of DB statements", 0, queryCounter.getCount());
		}
	}

	/**
	 * Test batch loading multiple attributes for many objects
	 * @throws Exception
	 */
	@Test
	public void testBatchLoadMultipleAttributes() throws Exception {
		for (int channelId: CHANNEL_IDS) {
			ds.setChannel(channelId);
			// get all objects
			QueryCounter queryCounter = new QueryCounter(false, true);
			Collection<Resolvable> objects = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), new String[] { attribute,
					combiAttribute });

			assertTrue("Only " + ALLOWED_DB_STATEMENTS + " db statements were allowed to batchload, but " + queryCounter.getCount() + " were used",
					queryCounter.getCount() <= ALLOWED_DB_STATEMENTS);

			// get the query counter
			queryCounter = new QueryCounter(false, true);
			// access the attribute for all objects
			for (Resolvable res : objects) {
				Object readValue = res.get(attribute);

				// do an assertion for the accessed value
				MCCRTestDataHelper.assertData("Check data",
						getModifiedAttributeValue(ObjectTransformer.getInt(res.get("obj_id"), 0), value), readValue);

				if (!attribute.equals(combiAttribute)) {
					Object readCombiValue = res.get(combiAttribute);

					// do an assertion for the accessed value
					MCCRTestDataHelper.assertData("Check data",
							getModifiedAttributeValue(ObjectTransformer.getInt(res.get("obj_id"), 0), ATTRIBUTE_DATA.get(combiAttribute)), readCombiValue);
				}
			}

			// we don't expect any DB access
			assertEquals("Check # of DB statements", 0, queryCounter.getCount());
		}
	}

	/**
	 * Test batch loading of attributes of linked objects
	 * @Throws Exception
	 */
	@Test
	public void testBatchLoadLinked() throws Exception {
		testBatchLoadLinked("link");
	}

	/**
	 * Test batch loading of attributes for optimized linked objects
	 * @throws Exception
	 */
	@Test
	public void testBatchLoadLinkOpt() throws Exception {
		testBatchLoadLinked("link_opt");
	}

	/**
	 * Test batch loading of attributes for multiple linked objects
	 * @throws Exception
	 */
	@Test
	public void testBatchLoadMultiLink() throws Exception {
		testBatchLoadLinked("link_multi");
	}

	/**
	 * Test batch loading of attributes of linked objects for the given link attribute
	 * @param linkAttribute name of the link attribute
	 * @throws Exception
	 */
	protected void testBatchLoadLinked(String linkAttribute) throws Exception {
		for (int channelId: CHANNEL_IDS) {
			ds.setChannel(channelId);
			// get all objects
			QueryCounter queryCounter = new QueryCounter(false, true);
			Collection<Resolvable> objects = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")),
					new String[] { linkAttribute + "." + attribute});

			assertTrue("Only " + ALLOWED_DB_STATEMENTS + " db statements were allowed to batchload, but " + queryCounter.getCount() + " were used",
					queryCounter.getCount() <= ALLOWED_DB_STATEMENTS);

			// get the query counter
			queryCounter = new QueryCounter(false, true);
			// access the attribute for all objects
			for (Resolvable res : objects) {
				Object linked = res.get(linkAttribute);

				if (linked instanceof MCCRObject) {
					MCCRObject linkedObject = (MCCRObject) linked;

					// access the attribute
					Object readValue = linkedObject.get(attribute);

					// do an assertion for the accessed value
					MCCRTestDataHelper.assertData("Check data",
							getModifiedAttributeValue(ObjectTransformer.getInt(linkedObject.get("obj_id"), 0), value), readValue);
				} else if (linked instanceof List<?>) {
					for (Object obj : (List<?>) linked) {
						if (obj instanceof MCCRObject) {
							MCCRObject linkedObject = (MCCRObject) obj;

							// access the attribute
							Object readValue = linkedObject.get(attribute);

							// do an assertion for the accessed value
							MCCRTestDataHelper.assertData("Check data",
									getModifiedAttributeValue(ObjectTransformer.getInt(linkedObject.get("obj_id"), 0), value), readValue);
						} else {
							fail("Linked object is not an object");
						}
					}
				} else {
					fail("Linked object is not an object");
				}
			}

			// we don't expect any DB access
			assertEquals("Check # of DB statements", 0, queryCounter.getCount());
		}
	}

	/**
	 * Test batch loading of attributes, where attributes are cached.
	 * @throws Exception
	 */
	@Test
	public void testBatchLoadCached() throws Exception {
		// all attributes in cache, but 100 attributes to prefetch, where threshold is 99
		testBatchLoadCached(0, 99, 0, 0, true);

		// 10% attributes not in cache, threshold is 20%
		testBatchLoadCached(NUM_OBJECTS * 10 / 100, 1000, 1000, 20, false);

		// 10% attributes not in cache, threshold is 9%
		testBatchLoadCached(NUM_OBJECTS * 10 / 100, 1000, 1000, 9, true);

		// 10 attributes not in cache, threshold is 15
		testBatchLoadCached(10, 1000, 15, 100, false);

		// 10 attributes not in cache, threshold is 5
		testBatchLoadCached(10, 1000, 5, 100, true);
	}

	/**
	 * Do batchloading with some cached attributes
	 * @param uncachedObjects number of uncached attributes
	 * @param threshold prefetch threshold
	 * @param cacheMissThreshold cache miss threshold
	 * @param cacheMisThresholdPerc cache miss threshold as percentage
	 * @param expectPrefetch true if prefetching should be done, false if not
	 * @throws Exception
	 */
	protected void testBatchLoadCached(int uncachedObjects, int threshold, int cacheMissThreshold, int cacheMisThresholdPerc, boolean expectPrefetch) throws Exception {
		MCCRCacheHelper.clear(ds);
		ds.setPrefetchAttributesThreshold(threshold);
		ds.setPrefetchAttributesCacheMissThreshold(cacheMissThreshold);
		ds.setPrefetchAttributesCacheMissThresholdPerc(cacheMisThresholdPerc);

		for (int channelId: CHANNEL_IDS) {
			ds.setChannel(channelId);

			// prefetch attributes for all objects
			Collection<Resolvable> objects = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), new String[] { attribute });

			// remove some attributes from the cache
			int removeCounter = 0;
			if (uncachedObjects > 0) {
				for (Resolvable obj : objects) {
					MCCRCacheHelper.clear((MCCRObject)obj, attribute);
					removeCounter++;
					if (removeCounter >= uncachedObjects) {
						break;
					}
				}
			}

			// get all objects again
			QueryCounter queryCounter = new QueryCounter(false, true);
			objects = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true")), new String[] { attribute });

			if (expectPrefetch) {
				assertTrue("Expected SQL Statements used for batchload", queryCounter.getCount() > 0);
				assertTrue("Only " + ALLOWED_DB_STATEMENTS + " db statements were allowed to batchload, but " + queryCounter.getCount() + " were used",
						queryCounter.getCount() <= ALLOWED_DB_STATEMENTS);

				// get the query counter
				queryCounter = new QueryCounter(false, true);
				// access the attribute for all objects
				for (Resolvable res : objects) {
					Object readValue = res.get(attribute);

					// do an assertion for the accessed value
					MCCRTestDataHelper.assertData("Check data",
							getModifiedAttributeValue(ObjectTransformer.getInt(res.get("obj_id"), 0), value), readValue);
				}

				// we don't expect any DB access
				assertEquals("Check # of DB statements", 0, queryCounter.getCount());
			} else {
				assertTrue("Did not expect any SQL statements (no batchload should have been done)", queryCounter.getCount() == 0);
			}

		}
	}

	/**
	 * Modify the given value in a predictable way for the object with given index
	 * @param index object index
	 * @param value original object value
	 * @return modified object value
	 * @throws Exception
	 */
	protected Object getModifiedAttributeValue(int index, Object value) throws Exception {
		if (value == null) {
			return value;
		} else if (value instanceof List<?>) {
			List<?> oldValues = (List<?>) value;
			List<Object> newValues = new ArrayList<Object>(oldValues.size());

			for (Object old : oldValues) {
				newValues.add(getModifiedAttributeValue(index, old));
			}
			return newValues;
		} else {
			if (value instanceof String) {
				return value + " for object " + index;
			} else if (value instanceof Integer) {
				return ObjectTransformer.getInt(value, 0) + index;
			} else if (value instanceof Long) {
				return ObjectTransformer.getLong(value, 0) + index;
			} else if (value instanceof Double) {
				return ObjectTransformer.getDouble(value, 0) + index;
			} else if (value instanceof byte[]) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();

				out.write((byte[]) value);
				out.write(index);
				return out.toByteArray();
			} else if (value instanceof Date) {
				return new Date(((Date) value).getTime() + index * 1000);
			} else {
				throw new Exception("Unexpected data " + value.getClass() + " found");
			}
		}
	}

	/**
	 * Get the content id of the object with given index
	 * @param index object index
	 * @return content id
	 */
	protected String getContentId(int index) {
		return "1000." + index;
	}

	/**
	 * Get the content id of the link target for the object with given index
	 * @param index object index
	 * @return content id of the link target
	 */
	protected String getTargetContentId(int index) {
		index++;
		if (index > NUM_OBJECTS) {
			index = 1;
		}
		return getContentId(index);
	}
}
