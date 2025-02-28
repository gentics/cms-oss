package com.gentics.contentnode.tests.wastebin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.contentnode.rest.resource.impl.internal.wastebin.WastebinResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for the purge job
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.WASTEBIN })
public class PurgeWastebinTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Map<MaxAge, Node> nodes = new HashMap<MaxAge, Node>();

	protected static Map<MaxAge, Map<TestedType, NodeObject>> objects = new HashMap<MaxAge, Map<TestedType, NodeObject>>();

	protected static Map<String, String> configMap = new HashMap<>();

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		try (Trx trx = new Trx()) {
			trx.at((int)(System.currentTimeMillis() / 1000) - 10);

			// generate test data
			for (MaxAge maxAge : MaxAge.values()) {
				String name = maxAge.toString();
				Node node = ContentNodeTestDataUtils.createNode(name, name, PublishTarget.NONE);
				nodes.put(maxAge, node);
				
				switch (maxAge) {
				case off:
					break;
				case old:
					configMap.put(node.getId().toString(), "86400");
					break;
				case young:
					configMap.put(node.getId().toString(), "1");
					break;
				}

				// generate objects
				Template template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Source", "Template");
				Map<TestedType, NodeObject> maxAgeMap = new HashMap<TestedType, NodeObject>();
				objects.put(maxAge, maxAgeMap);

				for (TestedType type : TestedType.values()) {
					NodeObject object = type.create(node.getFolder(), template);
					maxAgeMap.put(type, object);
					
					// delete the object (putting it into the wastebin)
					object.delete();

					try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
						object = trx.getTransaction().getObject(object);
					}
					assertNotNull("Object must exist", object);
					assertTrue("Object must be deleted", object.isDeleted());
				}
			}
			trx.success();
		}

		purgeWastebin();
	}

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: test: {0}, maxage: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (TestedType type : TestedType.values()) {
			for (MaxAge maxAge : MaxAge.values()) {
				data.add(new Object[] {type, maxAge});
			}
		}
		return data;
	}

	private TestedType type;
	private MaxAge maxAge;

	/**
	 * Create a test instance
	 * @param type tested object type
	 * @param maxAge
	 */
	public PurgeWastebinTest(TestedType type, MaxAge maxAge) {
		this.type = type;
		this.maxAge = maxAge;
	}

	@Test
	public void test() throws Exception {
		NodeObject testedObject = objects.get(maxAge).get(type);
		assertNotNull("Tested object must not be null", testedObject);

		try (Trx trx = new Trx()) {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				testedObject = trx.getTransaction().getObject(testedObject);
			}

			switch (maxAge) {
			case off:
				// no maxage, no purging
				assertNotNull("Object must not have been deleted", testedObject);
				break;
			case old:
				// the object is not long enough in the wastebin, so no purging
				assertNotNull("Object must not have been deleted", testedObject);
				break;
			case young:
				// the object is long enough in the wastebin, so it must have been purged
				assertNull("Object must have been deleted", testedObject);
				break;
			}
			trx.success();
		}
	}

	/**
	 * Run the purge job and wait, until it is finished
	 * @throws Exception
	 */
	protected static void purgeWastebin() throws Exception {
		try (Trx trx = new Trx()) {
			NodePreferences prefs = trx.getTransaction().getNodeConfig().getDefaultPreferences();
			prefs.setPropertyMap("wastebin_maxage_node", configMap);
			WastebinResource res = new WastebinResource();

			// start the job
			JobStatus jobStatus = res.startPurge();
			ContentNodeRESTUtils.assertResponse(jobStatus, ResponseCode.OK);
			assertTrue("Job should be running", jobStatus.isRunning());

			while (jobStatus.isRunning()) {
				Thread.sleep(100);
				jobStatus = res.getStatus();
				ContentNodeRESTUtils.assertResponse(jobStatus, ResponseCode.OK);
			}
			trx.success();
		}
	}

	/**
	 * Tested maxage values
	 */
	public static enum MaxAge {
		off,
		young,
		old;
	}
}
