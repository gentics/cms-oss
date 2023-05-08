/*
 * @author herbert
 * @date 31.05.2006
 * @version $Id: TestUtils.java,v 1.3.2.1 2011-04-07 10:09:29 norbert Exp $
 */
package com.gentics.node.testutils;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;

/**
 * A simple class with utilities useful for tests.
 * 
 * @author herbert
 * 
 */
@Deprecated
public class NodeTestUtils {

	/**
	 * Returns a dummyimage for the given filename. The image will be loaded from a classpath resource folder.
	 * 
	 * @param fileName
	 * @return
	 */
	@Deprecated
	public static InputStream getDummyImage(String fileName) {
		String imagePath = "com/gentics/tests/resources/pictures/" + fileName;
		InputStream ins = NodeTestUtils.class.getResourceAsStream(imagePath);

		assertNotNull("Could not find dummy image within classpath [" + imagePath + "]");
		return ins;
	}

	/**
	 * Creates a WriteableDatasuorce (with default properties for our junit tests)
	 * 
	 * @param withCache
	 *            if the datasource should have a cache (Note: if you need cache, you should call {@link #initConfigPathForCache()} before)
	 * @return the new WriteableDatasource
	 * @throws IOException
	 * @see #initConfigPathForCache()
	 */
	@Deprecated
	public static WriteableDatasource createWriteableDatasource(boolean withCache) throws IOException {
		Properties handleProperties = new Properties();

		handleProperties.load(NodeTestUtils.class.getResourceAsStream("jdbc-handle-writabledatasource.properties"));
		// for usage of jndi datasources:
		// handleProperties.load(DatasourceAccess.class.getResourceAsStream("jndi-handle.properties"));

		// turn on the cache
		Map dsProps = new HashMap(handleProperties);

		dsProps.put("cache", Boolean.toString(withCache));

		return NodeTestUtils.createWriteableDatasource(handleProperties, dsProps);
	}

	/**
	 * Create an invalid writeable datasource - read the handle properties from file jdbc-handle-invaliddatasource.properties
	 * 
	 * @return an invalid writeable datasource
	 */
	public static WriteableDatasource createInvalidDatasource() throws IOException {
		Properties handleProperties = new Properties();

		handleProperties.load(NodeTestUtils.class.getResourceAsStream("jdbc-handle-invaliddatasource.properties"));

		Map dsProps = new HashMap();

		return NodeTestUtils.createWriteableDatasource(handleProperties, dsProps);
	}

	@Deprecated
	public static WriteableDatasource createWriteableDatasource(Map handleprops, Map dsprops) {
		SQLHandle handle;

		// check parameters
		if (handleprops == null || dsprops == null) {
			return null;
		} else {
			// lookup existing handles in acvtivehandles map
			handle = null;
			// not found, create new handle
			if (handle == null) {
				handle = new SQLHandle("mumu");
				handle.init(handleprops);
			}
			return new CNWriteableDatasource(null, new SimpleHandlePool(handle), dsprops);
		}
	}

	/**
	 * Get the objects filtered by the given rule
	 * 
	 * @param ds
	 *            datasource
	 * @param rule
	 *            rule
	 * @return collection of DatasourceRow objects
	 * @throws Exception
	 */
	public static Collection getObjects(Datasource ds, String rule) throws Exception {
		return getObjects(ds, rule, 0, 10);
	}

	/**
	 * Get the objects filtered by the given rule
	 * 
	 * @param ds
	 *            datasource
	 * @param rule
	 *            rule
	 * @param start
	 *            start index of the first item to be returned
	 * @param count
	 *            number of items to be returned (-1 no limit)
	 * @return collection of DatasourceRow objects
	 * @throws Exception
	 */
	public static Collection getObjects(Datasource ds, String rule, int start, int count) throws Exception {
		RuleTree ruleTree = PortalConnectorFactory.createRuleTree(rule);

		ds.setRuleTree(ruleTree);

		return ds.getResult(start, count, "contentid", Datasource.SORTORDER_ASC);
	}

}
