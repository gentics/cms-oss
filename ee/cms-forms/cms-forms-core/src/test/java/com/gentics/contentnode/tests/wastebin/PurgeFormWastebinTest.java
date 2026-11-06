package com.gentics.contentnode.tests.wastebin;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.contentnode.rest.resource.impl.internal.wastebin.WastebinResource;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.WASTEBIN })
public class PurgeFormWastebinTest {
    @ClassRule
    public static DBTestContext testContext = new DBTestContext();

    protected static Map<PurgeFormWastebinTest.MaxAge, Node> nodes = new HashMap<PurgeFormWastebinTest.MaxAge, Node>();

    protected static Map<PurgeFormWastebinTest.MaxAge, Form> forms = new HashMap<PurgeFormWastebinTest.MaxAge, Form>();

    protected static Map<String, String> configMap = new HashMap<>();

    @BeforeClass
    public static void setupOnce() throws Exception {
        testContext.getContext().getTransaction().commit();

        try (Trx trx = new Trx()) {
            trx.at((int)(System.currentTimeMillis() / 1000) - 10);

            // generate test data
            for (PurgeFormWastebinTest.MaxAge maxAge : PurgeFormWastebinTest.MaxAge.values()) {
                String name = maxAge.toString();
                Node node = ContentNodeTestDataUtils.createNode(name, name, ContentNodeTestDataUtils.PublishTarget.NONE);
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
                Form object = ContentNodeTestDataUtils.createForm(node.getFolder(), "Test Form");
                forms.put(maxAge, object);

                object.delete();

                try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
                    object = trx.getTransaction().getObject(object);
                }
                assertNotNull("Object must exist", object);
                assertTrue("Object must be deleted", object.isDeleted());
            }
            trx.success();
        }

        purgeWastebin();
    }

    /**
     * Get the test parameters
     * @return collection of test parameter sets
     */
    @Parameterized.Parameters(name = "{index}: test: {0}, maxage: {1}")
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();
        for (PurgeFormWastebinTest.MaxAge maxAge : PurgeFormWastebinTest.MaxAge.values()) {
            data.add(new Object[] {maxAge});
        }
        return data;
    }

    private PurgeFormWastebinTest.MaxAge maxAge;

    /**
     * Create a test instance
     * @param maxAge
     */
    public PurgeFormWastebinTest(PurgeFormWastebinTest.MaxAge maxAge) {
        this.maxAge = maxAge;
    }

    @Test
    public void test() throws Exception {
        NodeObject testedObject = forms.get(maxAge);
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