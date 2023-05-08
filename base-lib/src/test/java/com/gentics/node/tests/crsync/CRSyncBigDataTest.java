package com.gentics.node.tests.crsync;

import static org.junit.Assert.assertTrue;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.portalnode.connector.CRSync;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.infrastructure.TestEnvironment;
import org.junit.experimental.categories.Category;

/**
 * This class contains all crsync big data tests
 *
 * @author johannes2
 *
 */
@Ignore("Disabled this test because it fails while creating the database")
@Category(BaseLibTest.class)
public class CRSyncBigDataTest extends AbstractCRSyncTest {

	/**
	 * Create instance with test parameters
	 * @param source source database
	 * @param target target database
	 */
	public CRSyncBigDataTest(TestDatabase source, TestDatabase target) {
		super(source, target);
	}

	/**
	 * allowed times for batchsizes 1, 10, 100, 1000 (locale tests on dev6)
	 */
	public final static int[] ALLOWEDTIMES = new int[] { 80, 20, 25, 25 };

	/**
	 * allowed times for batchsizes 1, 10, 100, 1000 (remote tests from workstations)
	 */
	public final static int[] ALLOWEDTIMES_REMOTE = new int[] { 160, 160, 160, 160 };

	/**
	 * allowed times for batchsizes 1, 10, 100, 1000 (locale tests on dev6 - using remote database on dev-win)
	 */
	public final static int[] ALLOWEDTIMES_REMOTEORACLE = new int[] { 200, 115, 115, 230 };

	/**
	 * Get the allowed time for a big sync test (depending on the used batchsize)
	 *
	 * @param batchsize
	 *            batchsize used in the test, must be one of 1, 10, 100, 1000
	 * @return allowed time in s
	 */
	public int getAllowedTime(int batchsize) throws Exception {
		if (isRemoteOracle()) {
			switch (batchsize) {
			case 1:
				return ALLOWEDTIMES_REMOTEORACLE[0];

			case 10:
				return ALLOWEDTIMES_REMOTEORACLE[1];

			case 100:
				return ALLOWEDTIMES_REMOTEORACLE[2];

			case 1000:
				return ALLOWEDTIMES_REMOTEORACLE[3];

			default:
				throw new Exception("Batchsize " + batchsize + " is not supported, use one of 1, 10, 100, 1000");
			}
		} else {
			switch (batchsize) {
			case 1:
				return ALLOWEDTIMES_REMOTE[0];

			case 10:
				return ALLOWEDTIMES_REMOTE[1];

			case 100:
				return ALLOWEDTIMES_REMOTE[2];

			case 1000:
				return ALLOWEDTIMES_REMOTE[3];

			default:
				throw new Exception("Batchsize " + batchsize + " is not supported, use one of 1, 10, 100, 1000");
			}
		}
	}

	/**
	 * Check whether the sync was done within the allowed time
	 *
	 * @param allowedTime
	 *            allowed time in s
	 * @param actualTime
	 *            actual time in s
	 * @throws Exception
	 */
	protected void checkSyncTime(int allowedTime, int actualTime) throws Exception {
		// do not check the times for oracle (because it is too damn slow)
		if (!isRemoteOracle()) {
			assertTrue("Sync took more than " + allowedTime + " sec. It took " + actualTime + " sec.", actualTime <= allowedTime);
		}
	}

	/**
	 * Do a crsync test with the given batchsize, binary length, text length and number of objects. Do not use transactions
	 *
	 * @param batchSize
	 *            batchsize
	 * @param binLength
	 *            binary length per object in bytes
	 * @param textLength
	 *            text length per object in bytes
	 * @param count
	 *            number of objects synced
	 * @param sourceTransaction
	 *            boolean flag to activate transactions in source db
	 * @param targetTransaction
	 *            boolean flag to activate transactions in target db
	 * @param createData
	 *            boolean flag to enable creation of data
	 * @param setSourceTimeStamp
	 *            whether the source timestamp shall be set to now
	 * @return time the crsync took (in ms)
	 * @throws Exception
	 */
	public long bigDataTest(int batchSize, int binLength, int textLength, int count, boolean sourceTransaction, boolean targetTransaction,
			boolean createData, boolean setSourceTimeStamp, boolean lobOptimization) throws Exception {

		System.out.println(
				"\nTesting with: Batchsize: " + batchSize + ", binlength: " + binLength + ", textLength: " + textLength + ",  count: " + count
				+ ", sourceTransaction: " + sourceTransaction + " ,targetTransaction: " + targetTransaction);
		// int targetSize = 244; // GB
		// int count = ((targetSize*1024)/((binLenght/1024)+(textLenght/1024)));

		if (createData) {
			saveData(batchSize, binLength, textLength, count);
		}

		if (setSourceTimeStamp) {
			// set last updatetime stamp to now
			touchRepository(sourceDS, -1, false);
			touchRepository(sourceDS, -1, false);

		}

		sync = new CRSync(sourceDS, targetDS, "", false, false, true, false, sourceTransaction, targetTransaction, batchSize, null);
		sync.setUseLobStreams(lobOptimization);

		System.gc();
		System.gc();
		long usedBefore = getUsedMemory();

		System.out.println("Before: " + usedBefore);
		String returnstr = sync.doSync();

		System.out.println(returnstr);
		long usedAfter = getUsedMemory();
		int idx = returnstr.indexOf("CRSync finished in ");
		int idx2 = returnstr.indexOf(" ms.");

		if (idx == -1 || idx2 == -1) {
			assertTrue(false);
		}
		long time = Long.parseLong(returnstr.substring(idx + 19, idx2));

		if ((time / 1000) == 0) {
			System.out.println("BatchSize[" + batchSize + "] - CrSync Took: " + time + " ms.");
		} else {
			System.out.println("BatchSize[" + batchSize + "] - CrSync Took: " + (time / 1000) + " sec.");
		}
		System.out.println("BatchSize[" + batchSize + "] - memory before: " + usedBefore);
		System.out.println("BatchSize[" + batchSize + "] - memory after : " + usedAfter + " --- diff:" + (usedAfter - usedBefore));

		return time;

		// TODO faster compare
		// do Not Compare

	}

	/**
	 * Do a crsync test with the given batchsize, binary length, text length and number of objects. Do not use transactions
	 *
	 * @param batchSize
	 *            batchsize
	 * @param binLength
	 *            binary length per object in bytes
	 * @param textLength
	 *            text length per object in bytes
	 * @param count
	 *            number of objects synced
	 * @return time the crsync took (in ms)
	 * @throws Exception
	 */
	public long bigDataTest(int batchSize, int binLength, int textLength, int count) throws Exception {
		return bigDataTest(batchSize, binLength, textLength, count, false, false, true, false, true);
	}

	/**
	 *
	 * @param batchSize
	 * @param binLength
	 * @param textLength
	 * @param count
	 * @param sourceTransaction
	 * @param targetTransaction
	 * @return
	 * @throws Exception
	 */
	public long bigDataTest(int batchSize, int binLength, int textLength, int count, boolean sourceTransaction, boolean targetTransaction) throws Exception {
		return bigDataTest(batchSize, binLength, textLength, count, sourceTransaction, targetTransaction, true, false, true);
	}

	/**
	 * Test SyncTime on synced database
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataReSyncBatchSize100() throws Exception {

		// create data and 1. sync
		long time = bigDataTest(100, BINLENGTH, TEXTLENGTH, OBJECTS, true, true, true, true, true);

		checkSyncTime(getAllowedTime(100), (int) time / 1000);

		// resync data
		System.out.println("Starting Resync");
		time = bigDataTest(100, BINLENGTH, TEXTLENGTH, OBJECTS, true, true, false, false, true);
		assertTrue("ReSync took more than " + 20 + " sec. It took " + time / 1000 + " sec.", time / 1000 <= 20);
	}

	/**
	 * Test with batchsize 1
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize1() throws Exception {

		long time = bigDataTest(1, BINLENGTH, TEXTLENGTH, OBJECTS);

		checkSyncTime(getAllowedTime(1), (int) time / 1000);
	}

	/**
	 * Test with batchsize 1
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	@Ignore
	public void testBigDataBatchSize1_2() throws Exception {
		long time = bigDataTest(1, BINLENGTH, TEXTLENGTH, 10000);

		assertTrue("Sync took more than " + getAllowedTime(1) + " sec.", time < getAllowedTime(1) * 1000);
	}

	/**
	 * Test with batchsize 10
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize10() throws Exception {

		long time = bigDataTest(10, BINLENGTH, TEXTLENGTH, OBJECTS);

		checkSyncTime(getAllowedTime(10), (int) time / 1000);
	}

	/**
	 * Test with batchsize 100
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize100() throws Exception {

		long time = bigDataTest(100, BINLENGTH, TEXTLENGTH, OBJECTS);

		checkSyncTime(getAllowedTime(100), (int) time / 1000);
	}

	/**
	 * Test with batchsize 1000
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize1000() throws Exception {

		long time = bigDataTest(1000, BINLENGTH, TEXTLENGTH, OBJECTS);

		checkSyncTime(getAllowedTime(1000), (int) time / 1000);
	}

	/**
	 * Test with batchsize 1 and nearly no data but huge amount of objects. 2 byte each object = 20000 bytes.
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize1NoData() throws Exception {

		long time = bigDataTest(1, 1, 1, OBJECTS);

		checkSyncTime(getAllowedTime(1), (int) time / 1000);
	}

	/**
	 * Test transactions
	 *
	 * @throws Exception
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testBigDataBatchSize100TransactionBoth() throws Exception {

		long time = bigDataTest(100, BINLENGTH, TEXTLENGTH, OBJECTS, true, true);

		checkSyncTime(getAllowedTime(100), (int) time / 1000);
	}
}
