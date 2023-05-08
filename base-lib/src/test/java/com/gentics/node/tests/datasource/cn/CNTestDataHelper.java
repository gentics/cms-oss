package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;

/**
 * Static helper class to provide test data
 */
public class CNTestDataHelper {

	/**
	 * Names of all attributes
	 */
	public final static String[] ATTRIBUTE_NAMES = {
		"text", "link", "int", "longtext", "blob", "long", "double", "date", "text_opt", "link_opt", "int_opt",
		"long_opt", "double_opt", "date_opt", "text_multi", "link_multi", "int_multi", "longtext_multi", "blob_multi", "long_multi", "double_multi",
		"date_multi", "longtext_fs", "blob_fs", "longtext_fs_multi", "blob_fs_multi" };

	/**
	 * Number of attribute values for multivalue attributes
	 */
	public final static int NUM_MULTIVALUE = 3;

	/**
	 * Link targets, keys object indices, values are lists of link targets
	 */
	public static Map<Integer, List<GenticsContentObject>> linkTargets = new HashMap<Integer, List<GenticsContentObject>>();

	/**
	 * Setup the link targets for the given list of objects.
	 * Every object will link to the next three object in the list in a cyclic manner (so the last object will link to the first three objects in the list)
	 * @param objects list of objects
	 */
	public static void setupLinkTargets(List<GenticsContentObject> objects) {
		for (int index = 0; index < objects.size(); index++) {
			List<GenticsContentObject> objectTargets = new ArrayList<GenticsContentObject>(3);

			linkTargets.put(index + 1, objectTargets);

			for (int j = 1; j <= NUM_MULTIVALUE; j++) {
				objectTargets.add(objects.get((index + j) % objects.size()));
			}
		}
	}

	/**
	 * Import the object types
	 * @param ds datasource
	 * @throws ObjectManagementException
	 */
	public static void importTypes(CNDatasource ds) throws ObjectManagementException {
		ObjectManagementManager.importTypes(ds, CNTestDataHelper.class.getResourceAsStream("cr_tests_structure.xml"));
	}

	/**
	 * Get the attribute value
	 * @param attrType attribute
	 * @param index index of the object
	 * @return attribute value
	 */
	public static Object getValue(ObjectAttributeBean attrType, int index) throws Exception {
		if (attrType.getMultivalue()) {
			List<Object> values = new Vector<Object>(NUM_MULTIVALUE);

			for (int i = 0; i < NUM_MULTIVALUE; i++) {
				values.add(getSingleValue(attrType, index, i));
			}
			return values;
		} else {
			return getSingleValue(attrType, index, 0);
		}
	}

	/**
	 * Get a single attribute value
	 * @param attrType attribute type
	 * @param index object index
	 * @param sortOrder sortorder
	 * @return attribute value
	 */
	public static Object getSingleValue(ObjectAttributeBean attrType, int index, int sortOrder) throws Exception {
		switch (attrType.getAttributetype()) {
		case GenticsContentAttribute.ATTR_TYPE_BLOB:
			return ("Value #" + sortOrder + " of " + attrType.getName() + " of object #" + index).getBytes("UTF-8");

		case GenticsContentAttribute.ATTR_TYPE_DATE:
			return getTimestamp(2013, 1, 0, 0, 0, sortOrder + index * 10);

		case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			return Double.valueOf(index * 10 + sortOrder + .42);

		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			return index * 10 + sortOrder;

		case GenticsContentAttribute.ATTR_TYPE_LONG:
			return Long.valueOf(index * 10 + sortOrder);

		case GenticsContentAttribute.ATTR_TYPE_OBJ:
			return linkTargets.get(index).get(sortOrder);

		case GenticsContentAttribute.ATTR_TYPE_TEXT:
		case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
			return "Value #" + sortOrder + " of " + attrType.getName() + " of object #" + index;

		default:
			fail("Could not fill attribute of type " + attrType.getAttributetype());
		}
		return null;
	}

	/**
	 * Create an object in the datasource
	 * @param ds datasource
	 * @param contentId contentid
	 * @param additionalData optional additional data
	 * @return created object
	 * @throws Exception
	 */
	public static GenticsContentObject createObject(CNWriteableDatasource ds, String contentId, Map<String, Object> additionalData) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("contentid", contentId);
		if (!ObjectTransformer.isEmpty(additionalData)) {
			data.putAll(additionalData);
		}
		Changeable object = ds.create(data, -1, false);

		assertNotNull("Object must be created", object);
		ds.store(Collections.singleton(object));
		return (GenticsContentObject) object;
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 * @param message message
	 * @param expected expected data
	 * @param data actual data
	 */
	public static void assertData(String message, Object expected, Object actual) {
		if (expected instanceof GenticsContentObject && actual instanceof GenticsContentObject) {
			assertEquals(message, ((GenticsContentObject) expected).getContentId(), ((GenticsContentObject) actual).getContentId());
		} else if (expected instanceof GenticsContentObject && actual instanceof String) {
			assertEquals(message, ((GenticsContentObject) expected).getContentId(), actual);
		} else if (expected instanceof String && actual instanceof GenticsContentObject) {
			assertEquals(message, expected, ((GenticsContentObject) actual).getContentId());
		} else if (expected instanceof byte[] && actual instanceof byte[]) {
			byte[] expectedBytes = (byte[]) expected;
			byte[] actualBytes = (byte[]) actual;

			assertEquals(message + " length", expectedBytes.length, actualBytes.length);
			for (int i = 0; i < expectedBytes.length; i++) {
				assertEquals(message + " data[" + i + "]", expectedBytes[i], actualBytes[i]);
			}
		} else if (expected instanceof List && actual instanceof List) {
			List<?> expectedColl = (List<?>) expected;
			List<?> actualColl = (List<?>) actual;

			assertEquals(message + " length", expectedColl.size(), actualColl.size());
			for (int i = 0; i < expectedColl.size(); i++) {
				assertData(message + " data[" + i + "]", expectedColl.get(i), actualColl.get(i));
			}
		} else {
			assertEquals(message, expected, actual);
		}
	}

	/**
	 * Get a sql timestamp object for the given date
	 * @param year year
	 * @param month month (1 is january)
	 * @param day day in the month
	 * @param hour hour
	 * @param minute minute
	 * @param second second
	 * @return timestamp object
	 */
	protected static Timestamp getTimestamp(int year, int month, int day, int hour, int minute, int second) {
		Calendar cal = Calendar.getInstance();

		cal.set(year, month - 1, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, 0);
		return new Timestamp(cal.getTimeInMillis());
	}
}
