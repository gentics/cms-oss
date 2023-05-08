package com.gentics.contentnode.tests.publish;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.contentnode.publish.LogFileArchiver;
import com.gentics.testutils.infrastructure.TestEnvironment;

/**
 * Test cases for the {@link LogFileArchiver}
 */
public class LogFileArchiverTest {
	protected File logDir;

	@Before
	public void setup() {
		logDir = new File(new File(System.getProperty("java.io.tmpdir")), TestEnvironment.getRandomHash(10));
		assertThat(logDir.mkdirs()).as("Creation of log directory succeeded").isTrue();
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(logDir);
	}

	/**
	 * Test that log files of the same day are not touched
	 * @throws IOException
	 */
	@Test
	public void testSameDay() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		// set to 04.07.2022 10:25:00
		cal.set(2022, 6, 4, 10, 25, 00);

		File log = createLogFile(cal.getTime());

		// run archiver an hour later
		cal.add(Calendar.HOUR_OF_DAY, 1);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(log).as("Publish log from today").exists();
		assertDailyArchive(new Date(), false);
	}

	/**
	 * Test that log files of previous days are archived in the daily archive
	 * @throws IOException
	 */
	@Test
	public void testDayInPast() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		// set to 04.07.2022 10:25:00
		cal.set(2022, 6, 4, 10, 25, 00);

		Date yesterday = cal.getTime();
		File yesterdaysLog = createLogFile(yesterday);

		// run archiver right after midnight (next day)
		cal.set(2022, 6, 5, 00, 00, 01);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(yesterdaysLog).as("Publish log from yesterday").doesNotExist();
		assertDailyArchive(yesterday, true, yesterdaysLog.getName());
	}

	/**
	 * Test adding multiple files in the past to their daily archive
	 * @throws IOException
	 */
	@Test
	public void testMultipleInPast() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		// set to 04.07.2022 10:25:00
		cal.set(2022, 6, 4, 10, 25, 00);
		Date date = cal.getTime();

		File log1 = createLogFile(cal.getTime());
		cal.add(Calendar.HOUR, 1);
		File log2 = createLogFile(cal.getTime());
		cal.add(Calendar.HOUR, 1);
		File log3 = createLogFile(cal.getTime());

		// run archiver right after midnight (next day)
		cal.set(2022, 6, 5, 00, 00, 01);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(log1).as("Publish log from yesterday").doesNotExist();
		assertThat(log2).as("Publish log from yesterday").doesNotExist();
		assertThat(log3).as("Publish log from yesterday").doesNotExist();
		assertDailyArchive(date, true, log1.getName(), log2.getName(), log3.getName());
	}

	/**
	 * Test adding multiple files of multiple days in the past
	 * @throws IOException
	 */
	@Test
	public void testMultipleDays() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		// set to 04.07.2022 10:25:00
		cal.set(2022, 6, 4, 10, 25, 00);

		Date date1 = cal.getTime();
		File log1 = createLogFile(date1);

		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date date2 = cal.getTime();
		File log2 = createLogFile(date2);

		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date date3 = cal.getTime();
		File log3 = createLogFile(date3);

		// run archiver some days later
		cal.set(2022, 6, 8, 18, 34, 00);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(log1).as("Publish log from yesterday").doesNotExist();
		assertThat(log2).as("Publish log from two days ago").doesNotExist();
		assertThat(log3).as("Publish log from three days ago").doesNotExist();
		assertDailyArchive(date1, true, log1.getName());
		assertDailyArchive(date2, true, log2.getName());
		assertDailyArchive(date3, true, log3.getName());
	}

	/**
	 * Test that daily archives, which are older than 7 days are added to the monthly archive
	 * @throws IOException
	 */
	@Test
	public void testArchiveDailyArchives() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		// set to 04.07.2022 10:25:00
		cal.set(2022, 6, 4, 10, 25, 00);

		Date date1 = cal.getTime();
		File log1 = createLogFile(date1);

		// run archiver next day
		cal.set(2022, 6, 5, 18, 34, 00);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// run archiver eight days later
		cal.set(2022, 6, 12, 18, 34, 00);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(log1).as("Publish log").doesNotExist();
		assertDailyArchive(date1, false);
		assertMonthlyArchive(date1, true, getDailyArchive(date1).getName());
	}

	/**
	 * Do a test for repeatedly adding log files and archiving (also tests appending to monthly archives)
	 * @throws IOException
	 */
	@Test
	public void testArchiveContinuously() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();

		// 30.06.2022 10:25:00
		cal.set(2022, 5, 30, 10, 25, 00);
		Date date1 = cal.getTime();
		File log1 = createLogFile(date1);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 30.06.2022 11:25:00
		cal.set(2022, 5, 30, 11, 25, 00);
		Date date2 = cal.getTime();
		File log2 = createLogFile(date2);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 01.07.2022 10:25:00
		cal.set(2022, 6, 1, 10, 25, 00);
		Date date3 = cal.getTime();
		File log3 = createLogFile(date3);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 01.07.2022 11:25:00
		cal.set(2022, 6, 1, 11, 25, 00);
		Date date4 = cal.getTime();
		File log4 = createLogFile(date4);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 02.07.2022 10:25:00
		cal.set(2022, 6, 2, 10, 25, 00);
		Date date5 = cal.getTime();
		File log5 = createLogFile(date5);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 02.07.2022 11:25:00
		cal.set(2022, 6, 2, 11, 25, 00);
		Date date6 = cal.getTime();
		File log6 = createLogFile(date6);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 06.07.2022 10:25:00
		cal.set(2022, 6, 6, 10, 25, 00);
		Date date7 = cal.getTime();
		File log7 = createLogFile(date7);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 06.07.2022 11:25:00
		cal.set(2022, 6, 6, 11, 25, 00);
		Date date8 = cal.getTime();
		File log8 = createLogFile(date8);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 07.07.2022 10:25:00
		cal.set(2022, 6, 7, 10, 25, 00);
		Date date9 = cal.getTime();
		File log9 = createLogFile(date9);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 07.07.2022 11:25:00
		cal.set(2022, 6, 7, 11, 25, 00);
		Date date10 = cal.getTime();
		File log10 = createLogFile(date10);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 09.07.2022 12:25:00
		cal.set(2022, 6, 9, 12, 25, 00);
		Date date11 = cal.getTime();
		File log11 = createLogFile(date11);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 09.07.2022 13:25:00
		cal.set(2022, 6, 9, 13, 25, 00);
		Date date12 = cal.getTime();
		File log12 = createLogFile(date12);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 10.07.2022 12:25:00
		cal.set(2022, 6, 10, 12, 25, 00);
		Date date13 = cal.getTime();
		File log13 = createLogFile(date13);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 10.07.2022 13:25:00
		cal.set(2022, 6, 10, 13, 25, 00);
		Date date14 = cal.getTime();
		File log14 = createLogFile(date14);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// only log files of last day must still be there
		assertThat(log1).as("Publish log").doesNotExist();
		assertThat(log2).as("Publish log").doesNotExist();
		assertThat(log3).as("Publish log").doesNotExist();
		assertThat(log4).as("Publish log").doesNotExist();
		assertThat(log5).as("Publish log").doesNotExist();
		assertThat(log6).as("Publish log").doesNotExist();
		assertThat(log7).as("Publish log").doesNotExist();
		assertThat(log8).as("Publish log").doesNotExist();
		assertThat(log9).as("Publish log").doesNotExist();
		assertThat(log10).as("Publish log").doesNotExist();
		assertThat(log11).as("Publish log").doesNotExist();
		assertThat(log12).as("Publish log").doesNotExist();
		assertThat(log13).as("Publish log").exists();
		assertThat(log14).as("Publish log").exists();

		// some daily archives are too old
		assertDailyArchive(date1, false);
		assertDailyArchive(date3, false);
		assertDailyArchive(date5, false);

		// some daily archives must exist
		assertDailyArchive(date7, true, log7.getName(), log8.getName());
		assertDailyArchive(date9, true, log9.getName(), log10.getName());
		assertDailyArchive(date11, true, log11.getName(), log12.getName());

		// some daily archives are too young
		assertDailyArchive(date13, false);

		// monthly archives must have been created
		assertMonthlyArchive(date1, true, getDailyArchive(date1).getName());
		assertMonthlyArchive(date3, true, getDailyArchive(date3).getName(), getDailyArchive(date5).getName());
	}

	/**
	 * Test appending files to the daily archive (if old logfiles "appear" after the log files had been archived into their daily archive)
	 * @throws IOException
	 */
	@Test
	public void testAppendToDailyArchive() throws IOException {
		Calendar cal = Calendar.getInstance();
		cal.clear();

		// 02.07.2022 10:25:00
		cal.set(2022, 6, 2, 10, 25, 00);
		Date date1 = cal.getTime();
		File log1 = createLogFile(date1);

		// archive at 03.07.2022 08:17:00
		cal.set(2022, 6, 3, 8, 17, 00);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		// 02.07.2022 10:26:00
		cal.set(2022, 6, 2, 10, 26, 00);
		Date date2 = cal.getTime();
		File log2 = createLogFile(date2);

		// archive at 03.07.2022 08:17:00 again
		cal.set(2022, 6, 3, 8, 17, 00);
		new LogFileArchiver(logDir, cal.getTimeInMillis()).archivePublishLogs();

		assertThat(log1).as("Publish log").doesNotExist();
		assertThat(log2).as("Publish log").doesNotExist();
		assertDailyArchive(date1, true, log1.getName(), log2.getName());
	}

	/**
	 * Create a log file with the given date (set as filename and modification time)
	 * @param date date
	 * @return log file
	 * @throws IOException
	 */
	protected File createLogFile(Date date) throws IOException {
		File logFile = new File(logDir, String.format("publishrun_%1$tY-%1$tm-%1$td_%1$tH-%1$tM-%1$tS.txt", date));
		logFile.createNewFile();
		logFile.setLastModified(date.getTime());
		return logFile;
	}

	/**
	 * Get the daily archive file for the given date
	 * @param date date
	 * @return daily archive file
	 */
	protected File getDailyArchive(Date date) {
		return new File(logDir, String.format("publishrun_%1$tY-%1$tm-%1$td.tar.gz", date));
	}

	/**
	 * Get the monthly archive file for the given date
	 * @param date date
	 * @return monthly archive file
	 */
	protected File getMonthlyArchive(Date date) {
		return new File(logDir, String.format("publishrun_%1$tY-%1$tm.tar", date));
	}

	/**
	 * Assert the daily archive for the given date
	 * @param date date
	 * @param exists true if the daily archive is expected to exist, false if it is expected to not exist
	 * @param archivedLogFileNames expected names of the log files contained in the daily archive (if it is expected to exist)
	 * @throws IOException
	 */
	protected void assertDailyArchive(Date date, boolean exists, String...archivedLogFileNames) throws IOException {
		File archive = getDailyArchive(date);
		if (exists) {
			assertThat(archive).as("Daily archive for %tc", date).exists();
			assertThat(getArchivedFileNames(archive)).as("Archived log files").containsOnly(archivedLogFileNames);
		} else {
			assertThat(archive).as("Daily archive for %tc", date).doesNotExist();
		}
	}

	/**
	 * Get the names of the files contained in the daily archive
	 * @param dailyArchive archive file
	 * @return set of file names
	 * @throws IOException
	 */
	protected Set<String> getArchivedFileNames(File dailyArchive) throws IOException {
		Set<String> names = new HashSet<>();
		try (FileInputStream in = new FileInputStream(dailyArchive);
				GzipCompressorInputStream gin = new GzipCompressorInputStream(in);
				TarArchiveInputStream tin = new TarArchiveInputStream(gin)) {
			TarArchiveEntry entry = null;
			while ((entry = tin.getNextTarEntry()) != null) {
				names.add(entry.getName());
			}
		}

		return names;
	}

	/**
	 * Get the names of the files contained in the monthly archive
	 * @param monthlyArchive archive file
	 * @return set of file names
	 * @throws IOException
	 */
	protected Set<String> getArchivedArchiveFileNames(File monthlyArchive) throws IOException {
		Set<String> names = new HashSet<>();
		try (FileInputStream in = new FileInputStream(monthlyArchive);
				TarArchiveInputStream tin = new TarArchiveInputStream(in)) {
			TarArchiveEntry entry = null;
			while ((entry = tin.getNextTarEntry()) != null) {
				names.add(entry.getName());
			}
		}

		return names;
	}

	/**
	 * Assert the monthly archive for the given date
	 * @param date date
	 * @param exists true if the monthly archive is expected to exist, false if it is expected to not exist
	 * @param archivedFileNames expected names of the archive files contained in the monthly archive (if it is expected to exist)
	 * @throws IOException
	 */
	protected void assertMonthlyArchive(Date date, boolean exists, String...archivedFileNames) throws IOException {
		File archive = getMonthlyArchive(date);
		if (exists) {
			assertThat(archive).as("Monthly archive for %tc", date).exists();
			assertThat(getArchivedArchiveFileNames(archive)).as("Archived archive files").containsOnly(archivedFileNames);
		} else {
			assertThat(archive).as("Monthly archive for %tc", date).doesNotExist();
		}
	}
}
