package com.gentics.node.tests.datasource.mccr;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractDatabaseVariationTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class MCCRDatasourceWriteVariantBTest extends AbstractMCCRDatasourceWriteTest {

	public static final boolean USE_DIFFERENT_CONTENTID = false;

	/**
	 * Default constructor used for parameterized junit tests
	 *
	 * @param testDatabase
	 *            test database
	 * @param attribute
	 *            name of the tested attribute
	 * @throws SQLUtilException
	 * @throws JDBCMalformedURLException
	 */
	public MCCRDatasourceWriteVariantBTest(TestDatabase testDatabase, String attribute, boolean useDiffetentContentId, boolean useDifferentAttrValue,
			int ignoreChannelId, int channelIdToUpdate) throws SQLUtilException, JDBCMalformedURLException {
		super(testDatabase, attribute, useDiffetentContentId, useDifferentAttrValue, ignoreChannelId, channelIdToUpdate);
	}

	/**
	 * Get the test parameters. Every item of the collection will contain the TestDatabase, attribute name, flag that indicates if the contentID each object should be
	 * different, flag that indicates if the attribute values should be different and ID of the channel that should be ignored (no objects should be written in that
	 * channel) during tests - if ID is 0 - the objects will be written in all channels.
	 *
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}, {1}, differentContentId: {2}, differentAttrValues: {3}, ignoreChannelId: {4}, channelIdToUpdate: {5}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		Map<String, TestDatabase> variations = AbstractDatabaseVariationTest.getVariations(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.MSSQL_VARIATIONS, TestDatabaseVariationConfigurations.ORACLE_VARIATIONS);

		Collection<Object[]> data = new Vector<Object[]>();

		for (TestDatabase testDB : variations.values()) {
			for (String attribute : EXPECTEDDATA_MASTER.keySet()) {
				if (!isExcluded(testDB, attribute)) {

					boolean useDifferentAttrValue = true;
					for (int ignoreChannelId = 0; ignoreChannelId <= 3; ignoreChannelId++) {
						for (int channelIdToUpdate = 1; channelIdToUpdate <= 3 && channelIdToUpdate != ignoreChannelId; channelIdToUpdate++) {
							data.add(new Object[] { testDB, attribute, USE_DIFFERENT_CONTENTID, useDifferentAttrValue, ignoreChannelId,
									channelIdToUpdate });
						}
					}

				}
			}
		}
		return data;
	}
}
