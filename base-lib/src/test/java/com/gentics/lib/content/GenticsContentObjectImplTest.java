package com.gentics.lib.content;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.contentnode.tests.category.BaseLibTest;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.content.GenticsContentObjectImpl.SortedValue;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Simple test that verifies that the doPrefetchAttributes method is invoked multiple times if
 * needed.
 *
 * @author johannes2
 */
@Category(BaseLibTest.class)
public class GenticsContentObjectImplTest {

	private static MockedStatic<DatatypeHelper> datatypeHelper;
	private static MockedStatic<DB> db;
	private static MockedStatic<SortedValue> sortedValue;


	@Before()
	public void setup(){
		datatypeHelper = mockStatic(DatatypeHelper.class);
 		db = mockStatic(DB.class);
		sortedValue = mockStatic(SortedValue.class);
	}

	@After()
	public void tearDown(){
		datatypeHelper.close();
		db.close();
		sortedValue.close();
	}

	/**
	 * Invoke the test for the given parameters.
	 *
	 * @param driverName
	 * @param nContentObjects
	 * @param nExpectedInvocations
	 * @throws Exception
	 */
	private void invokeTest(String driverName, int nContentObjects, int nExpectedInvocations)
			throws Exception {
		final String ATTRIBUTE_COLUMN = "blub";
		int timeStamp = -1;
		String[] prefetchAttribs = {"name"};

		// Create some mocks
		CNDatasource datasource = mock(CNDatasource.class);
		HandlePool handlePool = mock(HandlePool.class);
		SQLHandle sqlHandle = mock(SQLHandle.class);
		DBHandle dbHandle = mock(DBHandle.class);

		// Mock db access
		when(sqlHandle.getDBHandle()).thenReturn(dbHandle);
		when(handlePool.getHandle()).thenReturn(sqlHandle);
		when(datasource.getHandlePool()).thenReturn(handlePool);
		when(datasource.getDatabaseProductName()).thenReturn(driverName);

		// Mock the datatype helper
		AttributeType attrType = mock(AttributeType.class);
		when(attrType.getColumn()).thenReturn(ATTRIBUTE_COLUMN);

		datatypeHelper.when(() -> DatatypeHelper.getComplexDatatype(dbHandle, "name"))
				.thenReturn(attrType);

		db.when(() -> DB.getDatabaseProductName(dbHandle)).thenReturn(driverName);


		// Mock a simple contentobject. We will add it multiple times to the
		// final set of objects to save some time.
		GenticsContentObjectImpl obj = mock(GenticsContentObjectImpl.class);
		when(obj.getContentId()).thenReturn("10007.1");
		when(obj.getVersionTimestamp()).thenReturn(timeStamp);

		GenticsContentObject[] objects = new GenticsContentObject[nContentObjects];
		for (int i = 0; i < nContentObjects; i++) {
			objects[i] = obj;
		}

		// Now spy upon our class under test
		try (MockedStatic<GenticsContentObjectImpl> genticsContentObject = mockStatic(
				GenticsContentObjectImpl.class, Mockito.CALLS_REAL_METHODS)) {
			// this method is actually invoked
			GenticsContentObjectImpl.prefillContentObjects(datasource, objects, prefetchAttribs,
					timeStamp, true, false);

			// Verify that the doPrefetchAttributes was invoked exactly the expected times
			genticsContentObject.verify(
					() -> GenticsContentObjectImpl.doPrefetchAttributes(eq(dbHandle), eq(datasource),
							anyList(), anyList(), eq(ATTRIBUTE_COLUMN), anyInt(), anyList(), any(), anyMap(),
							anyMap(), anyBoolean()), times(nExpectedInvocations));
		}
	}

	/**
	 * Create some objects and verify that the doPrefetchAttributes method is only invoked a single
	 * time.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectForMySQL() throws Exception {
		invokeTest("MySQL", 2500, 1);
	}

	/**
	 * Verify that the doPrefetchAttributes is also invoked multiple times when we are above the
	 * limit.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectForMSSQL() throws Exception {
		int nTimesLimit = 4;
		int nObjectsAboveLimit = 1;
		int nContentObjects =
				GenticsContentObjectImpl.MAX_PREFETCH_SIZE_MSSQL * nTimesLimit + nObjectsAboveLimit;
		int nExpectedInvocations = nTimesLimit + nObjectsAboveLimit;
		invokeTest("Microsoft SQL Server", nContentObjects, nExpectedInvocations);
	}

	/**
	 * Test whether the max prefetch size limitation for oracle works as expected and that the
	 * doPrefetchAttributes method is invoked multiple times.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectsForOracle() throws Exception {
		// Reduce the max prefetch size for oracle to speedup tests
		GenticsContentObjectImpl.MAX_PREFETCH_SIZE_ORACLE = 200;
		// Generate some objects that would cause 5 invocations of the
		// doPrefetchAttributes method.
		int nTimesLimit = 4;
		int nObjectsAboveLimit = 1;
		int nContentObjects =
				GenticsContentObjectImpl.MAX_PREFETCH_SIZE_ORACLE * nTimesLimit + nObjectsAboveLimit;
		int nExpectedInvocations = nTimesLimit + nObjectsAboveLimit;
		invokeTest("Oracle", nContentObjects, nExpectedInvocations);
	}

	/**
	 * Verify that the doPrefetchAttributes method is only invoked a single time.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectsForOracleBelowLimit() throws Exception {
		// Reduce the max prefetch size for oracle to speedup tests
		GenticsContentObjectImpl.MAX_PREFETCH_SIZE_ORACLE = 200;
		int nContentObjects = GenticsContentObjectImpl.MAX_PREFETCH_SIZE_ORACLE - 1;
		invokeTest("Oracle", nContentObjects, 1);
	}
}
