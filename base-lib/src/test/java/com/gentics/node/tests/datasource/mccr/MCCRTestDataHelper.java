package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementException;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.datasource.object.jaxb.Definition;
import com.gentics.lib.datasource.object.jaxb.Objecttype;

/**
 * Static helper class to provide test data
 */
public class MCCRTestDataHelper {

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
	 * Link targets, keys are the channel ids, values are maps of object index to lists of link targets
	 */
	public static Map<Integer, Map<Integer, List<MCCRObject>>> linkTargets = new HashMap<Integer, Map<Integer, List<MCCRObject>>>();

	/**
	 * Setup the link targets for the given list of objects.
	 * Every object will link to the next three object in the list in a cyclic manner (so the last object will link to the first three objects in the list)
	 * @param channelId channel id
	 * @param objects list of objects
	 */
	public static void setupLinkTargets(int channelId, List<MCCRObject> objects) {
		Map<Integer, List<MCCRObject>> channelTargets = new HashMap<Integer, List<MCCRObject>>();

		linkTargets.put(channelId, channelTargets);
		for (int index = 0; index < objects.size(); index++) {
			List<MCCRObject> objectTargets = new ArrayList<MCCRObject>(3);

			channelTargets.put(index + 1, objectTargets);

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
	public static void importTypes(MCCRDatasource ds) throws ObjectManagementException {
		ObjectManagementManager.importTypes(ds, MCCRTest.class.getResourceAsStream("mccr_tests_structure.xml"));
	}

	/**
	 * Get the attribute value
	 * @param attrType attribute
	 * @param channelId channel id
	 * @param index index of the object
	 * @return attribute value
	 */
	public static Object getValue(ObjectAttributeBean attrType, int channelId, int index) throws Exception {
		if (attrType.getMultivalue()) {
			List<Object> values = new Vector<Object>(NUM_MULTIVALUE);

			for (int i = 0; i < NUM_MULTIVALUE; i++) {
				values.add(getSingleValue(attrType, channelId, index, i));
			}
			return values;
		} else {
			return getSingleValue(attrType, channelId, index, 0);
		}
	}

	/**
	 * Get a single attribute value
	 * @param attrType attribute type
	 * @param channelId channel id
	 * @param index object index
	 * @param sortOrder sortorder
	 * @return attribute value
	 */
	public static Object getSingleValue(ObjectAttributeBean attrType, int channelId, int index, int sortOrder) throws Exception {
		switch (attrType.getAttributetype()) {
		case GenticsContentAttribute.ATTR_TYPE_BLOB:
			return ("Value #" + sortOrder + " of " + attrType.getName() + " in channel " + channelId + " object #" + index).getBytes("UTF-8");

		case GenticsContentAttribute.ATTR_TYPE_DATE:
			return getTimestamp(2013 + channelId, channelId, 0, 0, 0, sortOrder + index * 10);

		case GenticsContentAttribute.ATTR_TYPE_DOUBLE:
			return Double.valueOf(channelId * 100000 + index * 10 + sortOrder + .42);

		case GenticsContentAttribute.ATTR_TYPE_INTEGER:
			return channelId * 100000 + index * 10 + sortOrder;

		case GenticsContentAttribute.ATTR_TYPE_LONG:
			return Long.valueOf(channelId * 1000000 + index * 10 + sortOrder);

		case GenticsContentAttribute.ATTR_TYPE_OBJ:
			return linkTargets.get(channelId).get(index).get(sortOrder);

		case GenticsContentAttribute.ATTR_TYPE_TEXT:
		case GenticsContentAttribute.ATTR_TYPE_TEXT_LONG:
			return "Value #" + sortOrder + " of " + attrType.getName() + " in channel " + channelId + " object #" + index;

		default:
			fail("Could not fill attribute of type " + attrType.getAttributetype());
		}
		return null;
	}

	/**
	 * Create an object in the datasource
	 * @param ds datasource
	 * @param channelId channel id
	 * @param channelsetId channelset id
	 * @param contentId contentid
	 * @param additionalData optional additional data
	 * @return created object
	 * @throws Exception
	 */
	public static MCCRObject createObject(WritableMCCRDatasource ds, int channelId, int channelsetId, String contentId, Map<String, Object> additionalData) throws Exception {
		return createObject(ds, channelId, channelsetId, contentId, additionalData, true);
	}

	/**
	 * Create an object in the datasource
	 * @param ds datasource
	 * @param channelId channel id
	 * @param channelsetId channelset id
	 * @param contentId contentid
	 * @param additionalData optional additional data
	 * @param doStore true if the single object shall be stored, false if not
	 * @return created object
	 * @throws Exception
	 */
	public static MCCRObject createObject(WritableMCCRDatasource ds, int channelId, int channelsetId, String contentId, Map<String, Object> additionalData,
			boolean doStore) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("contentid", contentId);
		data.put(WritableMCCRDatasource.MCCR_CHANNELSET_ID, channelsetId);
		data.put(WritableMCCRDatasource.MCCR_CHANNEL_ID, channelId);
		if (!ObjectTransformer.isEmpty(additionalData)) {
			data.putAll(additionalData);
		}
		Changeable object = ds.create(data);

		assertNotNull("Object must be created", object);
		if (doStore) {
			ds.store(Collections.singleton(object));
		}
		return (MCCRObject) object;
	}

	/**
	 * Assert equality of the data. Special handling of multivalue and byte[] data
	 * @param message message
	 * @param expected expected data
	 * @param data actual data
	 */
	public static void assertData(String message, Object expected, Object actual) {
		if (expected instanceof MCCRObject && actual instanceof MCCRObject) {
			assertEquals(message, ((MCCRObject) expected).getId(), ((MCCRObject) actual).getId());
		} else if (expected instanceof MCCRObject && actual instanceof String) {
			assertEquals(message, ((MCCRObject) expected).get("contentid"), actual);
		} else if (expected instanceof String && actual instanceof MCCRObject) {
			assertEquals(message, expected, ((MCCRObject) actual).get("contentid"));
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

	/**
	 * Load the object types from the given input stream
	 * @param in input stream
	 * @return map of object types
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public static Map<Integer, ObjectTypeBean> loadObjectTypes(InputStream in) throws Exception {
		Map<Integer, ObjectTypeBean> objectTypes = new HashMap<Integer, ObjectTypeBean>();

		JAXBContext context = JAXBContext.newInstance(ObjectManagementManager.JAXB_PACKAGE);
		Unmarshaller unmarshaller = context.createUnmarshaller();

		unmarshaller.setValidating(false);
		Object importedObject = unmarshaller.unmarshal(in);

		if (!(importedObject instanceof Definition)) {
			throw new ObjectManagementException("Input stream did not contain an object definition");
		}
		Definition importedDefinition = (Definition) importedObject;

		if (importedDefinition.isSetObjectTypes()) {
			Objecttype[] importedObjectTypes = importedDefinition.getObjectTypes();

			// all previous checks went fine, so save the objecttypes and attributetypes now
			for (int i = 0; i < importedObjectTypes.length; ++i) {
				ObjectTypeBean objectType = new ObjectTypeBean(importedObjectTypes[i]);

				objectTypes.put(objectType.getType(), objectType);
			}
		}

		return objectTypes;
	}
}
