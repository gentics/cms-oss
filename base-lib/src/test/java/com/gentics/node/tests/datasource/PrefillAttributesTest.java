/*
 * @author norbert
 * @date 23.08.2006
 * @version $Id: PrefillAttributesTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.pool.TestDatabaseRepository;
import com.gentics.testutils.logging.LogCounterAppender;
import com.gentics.testutils.logging.LogCounterAppender.LogCounter;
import org.junit.experimental.categories.Category;

/**
 * Test case for prefilling attributes. The test fetches objects from a cndatasource with attribute prefilling. The test succeeds, when
 * access to all meta attributes and the prefilled attributes works, but access to non-prefilled attributes fails (database handle is closed
 * before attributes are accessed). All tests are done without caching.
 */
@Category(BaseLibTest.class)
public class PrefillAttributesTest {

	/**
	 * datasource with attribute prefilling enabled
	 */
	private Datasource prefillDS;

	/**
	 * datasource with attribute prefilling disabled
	 */
	private Datasource nonPrefillDS;

	/**
	 * constant for the names of prefilled attributes
	 */
	public final static String[] PREFILLED_ATTRIBUTES = new String[] { "name", "creationdate", "subnodes.name" };

	/**
	 * constant for the names of non-prefilled attributes
	 */
	public final static String[] NON_PREFILLED_ATTRIBUTES = new String[] { "age", "shortname", "parentnode.name" };

	/**
	 * constant for the names of all attributes
	 */
	public final static String[] ALL_ATTRIBUTES = new String[] { "name", "creationdate", "subnodes.name", "age", "shortname", "parentnode.name" };

	/**
	 * Test database
	 */
	private TestDatabase testDatabase;

	private SQLUtils sqlUtils;

	/**
	 * log counter appender
	 */
	protected LogCounterAppender logCounterAppender;

	/**
	 * Create instance of the test
	 *
	 * @param arg0
	 *            name of the test
	 */
	public PrefillAttributesTest() {
		// initialize the cache
		GenericTestUtils.initConfigPathForCache();

		// create an appender for the logger
		NodeLogger logger = NodeLogger.getNodeLogger("com.gentics");

		logCounterAppender = new LogCounterAppender();
		logger.addAppender(logCounterAppender);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {

		testDatabase = TestDatabaseRepository.getMySQLNewStableDatabase();
		testDatabase.setRandomDatabasename(getClass().getSimpleName());

		sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		sqlUtils.connectDatabase();
		sqlUtils.createCRDatabase(getClass());

		// create the datasource
		Properties handleProperties = testDatabase.getSettings();

		// create the datasource handle
		SQLHandle handle = new SQLHandle("ds");

		handle.init(handleProperties);

		// create the datasource without caching
		Map dsProps = new HashMap(handleProperties);

		// we do not use caching or versioning
		dsProps.put("cache", "false");
		dsProps.put("versioning", "false");
		dsProps.put("prefetchAttributes", "true");
		dsProps.put("prefetchAttribute.threshold", "0");
		prefillDS = new CNWriteableDatasource(null, new SimpleHandlePool(handle), dsProps);

		Map nonDSProps = new HashMap(handleProperties);

		// we do not use caching or versioning
		nonDSProps.put("cache", "false");
		nonDSProps.put("versioning", "false");
		nonDSProps.put("prefetchAttributes", "false");
		nonPrefillDS = new CNWriteableDatasource(null, new SimpleHandlePool(handle), nonDSProps);

		// reset the error counter
		logCounterAppender.reset();
	}

	@After
	public void tearDown() throws Exception {
		prefillDS.getHandlePool().close();
		sqlUtils.removeDatabase();
		sqlUtils.disconnectDatabase();
	}

	/**
	 * Test access to prefilled attribute when objects are fetched using a RuleTree
	 *
	 * @throws Exception
	 */
	@Test
	public void testRuleTreePrefilling() throws Exception {
		RuleTree ruleTree = new DefaultRuleTree();

		ruleTree.parse("1 == 1");
		prefillDS.setRuleTree(ruleTree);
		prefillDS.setAttributeNames(PREFILLED_ATTRIBUTES);

		Collection objects = prefillDS.getResult();

		// now close the handle (no more database access)
		prefillDS.getHandlePool().close();

		// access the attributes
		accessAtributes(objects, PREFILLED_ATTRIBUTES, NON_PREFILLED_ATTRIBUTES);

		assertNoErrors();
	}

	/**
	 * Test access to prefilled attributes when objects are fetched using a datasource filter.
	 *
	 * @throws Exception
	 */
	@Test
	public void testDatasourceFilterPrefilling() throws Exception {
		Expression expression = ExpressionParser.getInstance().parse("true");
		DatasourceFilter filter = prefillDS.createDatasourceFilter(expression);

		Collection objects = prefillDS.getResult(filter, PREFILLED_ATTRIBUTES);

		// now close the handle (no more database access)
		prefillDS.getHandlePool().close();

		// access the attributes
		accessAtributes(objects, PREFILLED_ATTRIBUTES, NON_PREFILLED_ATTRIBUTES);

		assertNoErrors();
	}

	/**
	 * Test access to attributes when prefilling is disabled (version with RuleTree)
	 *
	 * @throws Exception
	 */
	@Test
	public void testRuleTreeNonPrefilling() throws Exception {
		RuleTree ruleTree = new DefaultRuleTree();

		ruleTree.parse("1 == 1");
		nonPrefillDS.setRuleTree(ruleTree);
		nonPrefillDS.setAttributeNames(PREFILLED_ATTRIBUTES);

		Collection objects = nonPrefillDS.getResult();

		// now close the handle (no more database access)
		nonPrefillDS.getHandlePool().close();

		// access the attributes (no attribute have been prefilled)
		accessAtributes(objects, null, ALL_ATTRIBUTES);

		assertNoErrors();
	}

	/**
	 * Test access to attributes when prefilling is disabled (version with DatasourceFilter)
	 *
	 * @throws Exception
	 */
	@Test
	public void testDatasourceFilterNonPrefilling() throws Exception {
		Expression expression = ExpressionParser.getInstance().parse("true");
		DatasourceFilter filter = nonPrefillDS.createDatasourceFilter(expression);

		Collection objects = nonPrefillDS.getResult(filter, PREFILLED_ATTRIBUTES);

		// now close the handle (no more database access)
		nonPrefillDS.getHandlePool().close();

		// access the attributes
		accessAtributes(objects, null, ALL_ATTRIBUTES);

		assertNoErrors();
	}

	/**
	 * Access the attributes for all objects in the collection. Access all meta attributes, all prefilled attributes and all non-prefilled
	 * attributes. Check that access to meta or prefilled attributes work and access to non-prefilled attributes throw exceptions
	 *
	 * @param objects
	 *            collection of objects
	 * @param prefilledAttributes
	 *            names of prefilled attributes
	 * @param nonPrefilledAttributes
	 *            names of non prefilled attributes
	 * @throws Exception
	 */
	protected void accessAtributes(Collection objects, String[] prefilledAttributes, String[] nonPrefilledAttributes) throws Exception {
		for (Iterator iter = objects.iterator(); iter.hasNext();) {
			Resolvable element = (Resolvable) iter.next();
			PropertyResolver resolver = new PropertyResolver(element);

			// access the meta data
			String contentId = element.get("contentid").toString();
			String objType = element.get("obj_type").toString();
			String objId = element.get("obj_id").toString();

			// check contentid for plausibility
			if (StringUtils.isEmpty(contentId)) {
				fail("contentid must not be null or empty");
			}
			if ("0.0".equals(contentId)) {
				fail("contentid must not be 0.0");
			}
			assertEquals("contentid must be obj_type.obj_id", objType + "." + objId, contentId);

			String motherContentId = element.get("motherid").toString();
			String motherObjectType = element.get("mother_obj_type").toString();
			String motherObjectId = element.get("mother_obj_id").toString();

			if (!"0.0".equals(motherObjectType + "." + motherObjectId)) {
				// check motherid for plausibility
				assertEquals("motherid must be mother_obj_type.mother_obj_id", motherObjectType + "." + motherObjectId, motherContentId);
			}

			element.get("updatetimestamp");

			// access the prefilled attributes
			if (prefilledAttributes != null) {
				for (int i = 0; i < prefilledAttributes.length; i++) {
					resolver.resolve(prefilledAttributes[i]);
				}
			}

			// access an existing but not prefilled attribute (each access must
			// throw an exception)
			if (nonPrefilledAttributes != null) {
				for (int i = 0; i < nonPrefilledAttributes.length; i++) {
					try {
						resolver.resolve(nonPrefilledAttributes[i]);
						// must not get here
						fail("fetched non-prefilled attribute " + nonPrefilledAttributes[i] + " without db access");
					} catch (Exception ex) {}
				}
			}
		}
	}

	/**
	 * Assert that no errors where logged.
	 */
	protected void assertNoErrors() {
		LogCounter fatalCounter = logCounterAppender.getLogCounter(Level.FATAL);
		LogCounter errorCounter = logCounterAppender.getLogCounter(Level.ERROR);

		assertEquals("Check number of logged fatals (collected errors: " + fatalCounter.getLog() + ")", 0, fatalCounter.getCounter());
		assertEquals("Check number of logged errors (collected errors: " + errorCounter.getLog() + ")", 0, errorCounter.getCounter());
	}
}
