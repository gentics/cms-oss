package com.gentics.lib.content;

import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.lib.content.DatatypeHelper.AttributeType;
import com.gentics.lib.content.GenticsContentObjectImpl.SortedValue;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;

/**
 * Simple test that verifies that the doPrefetchAttributes method is invoked
 * multiple times if needed.
 *
 * @author johannes2
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatatypeHelper.class, DB.class, GenticsContentObjectImpl.class, SortedValue.class })
// Powermock issue 288 - httpclient compatibility
@PowerMockIgnore("javax.*")
@Category(BaseLibTest.class)
public class GenticsContentObjectImplTest {

	/**
	 * Invoke the test for the given parameters.
	 *
	 * @param driverName
	 * @param nContentObjects
	 * @param nExpectedInvocations
	 * @throws Exception
	 */
	private void invokeTest(String driverName, int nContentObjects, int nExpectedInvocations) throws Exception {

		int timeStamp = -1;
		String[] prefetchAttribs = { "name" };

		// Create some mocks
		CNDatasource datasource = mock(CNDatasource.class);
		HandlePool handlePool = mock(HandlePool.class);
		SQLHandle sqlHandle = mock(SQLHandle.class);
		DBHandle dbHandle = mock(DBHandle.class);

		// Mock db access
		Mockito.when(sqlHandle.getDBHandle()).thenReturn(dbHandle);
		Mockito.when(handlePool.getHandle()).thenReturn(sqlHandle);
		Mockito.when(datasource.getHandlePool()).thenReturn(handlePool);

		// Mock the datatype helper
		AttributeType attrType = PowerMockito.mock(AttributeType.class);
		when(attrType.getColumn()).thenReturn("blub");
		mockStatic(DatatypeHelper.class);
		when(DatatypeHelper.getComplexDatatype(dbHandle, "name")).thenReturn(attrType);

		// Mock sort class to avoid NPE
		mockStatic(SortedValue.class);

		// Mock the DB class
		mockStatic(DB.class);
		when(DB.getDatabaseProductName(dbHandle)).thenReturn(driverName);
		Mockito.when(datasource.getDatabaseProductName()).thenReturn(driverName);

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
		PowerMockito.spy(GenticsContentObjectImpl.class);
		GenticsContentObjectImpl.prefillContentObjects(datasource, objects, prefetchAttribs, timeStamp, true, false);

		// Verify that the doPrefetchAttributes was invoked exactly the expected
		// times
		PowerMockito.verifyPrivate(GenticsContentObjectImpl.class, Mockito.times(nExpectedInvocations)).invoke("doPrefetchAttributes", eq(dbHandle), eq(datasource),
				Mockito.notNull(), Mockito.notNull(), eq("blub"), eq(timeStamp), Mockito.notNull(), Mockito.notNull(), Mockito.notNull(), Mockito.notNull(), eq(false));
	}

	/**
	 * Create some objects and verify that the doPrefetchAttributes method is
	 * only invoked a single time.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectForMySQL() throws Exception {
		invokeTest("MySQL", 2500, 1);
	}

	/**
	 * Verify that the doPrefetchAttributes is also invoked multiple times when
	 * we are above the limit.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefillContentObjectForMSSQL() throws Exception {
		int nTimesLimit = 4;
		int nObjectsAboveLimit = 1;
		int nContentObjects = GenticsContentObjectImpl.MAX_PREFETCH_SIZE_MSSQL * nTimesLimit + nObjectsAboveLimit;
		int nExpectedInvocations = nTimesLimit + nObjectsAboveLimit;
		invokeTest("Microsoft SQL Server", nContentObjects, nExpectedInvocations);
	}

	/**
	 * Test whether the max prefetch size limitation for oracle works as
	 * expected and that the doPrefetchAttributes method is invoked multiple
	 * times.
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
		int nContentObjects = GenticsContentObjectImpl.MAX_PREFETCH_SIZE_ORACLE * nTimesLimit + nObjectsAboveLimit;
		int nExpectedInvocations = nTimesLimit + nObjectsAboveLimit;
		invokeTest("Oracle", nContentObjects, nExpectedInvocations);
	}

	/**
	 * Verify that the doPrefetchAttributes method is only invoked a single
	 * time.
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
