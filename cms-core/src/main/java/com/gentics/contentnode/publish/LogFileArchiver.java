package com.gentics.contentnode.publish;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

/**
 * Log Archiver implementation.
 * <ol>
 * <li>Log Files of the same day will not be touched</li>
 * <li>Log Files of previous days will be archived into daily .tar.gz</li>
 * <li>Daily .tar.gz files, which are older than 7 days will be added to monthly .tar files</li>
 * </ol>
 */
public class LogFileArchiver {
	/**
	 * Pattern for log files, which are handled by this archiver
	 */
	public final static Pattern logFilePattern = Pattern.compile(
			"publishrun_(?<year>[0-9]{4})\\-(?<month>[0-9]{2})\\-(?<day>[0-9]{2})_(?<hour>[0-9]{2})\\-(?<minute>[0-9]{2})\\-(?<second>[0-9]{2})(.*)\\.txt");

	/**
	 * Pattern for daily archive files
	 */
	public final static Pattern dailyArchiveFilePattern = Pattern.compile(
			"publishrun_(?<year>[0-9]{4})\\-(?<month>[0-9]{2})\\-(?<day>[0-9]{2})\\.tar\\.gz");

	/**
	 * Start of "today" (where "today" is defined by the given timestamp)
	 */
	protected Calendar startOfToday;

	/**
	 * Start of "seven days ago"
	 */
	protected Calendar startOfSevenDaysAgo;

	/**
	 * Base directory containing the log files
	 */
	protected File logDir;

	/**
	 * Predicate for filtering log files
	 */
	protected Predicate<File> isLogFile = file -> {
		return logFilePattern.matcher(file.getName()).matches();
	};

	/**
	 * Predicate for filtering daily archive files
	 */
	protected Predicate<File> isDailyArchiveFile = file -> {
		return dailyArchiveFilePattern.matcher(file.getName()).matches();
	};

	/**
	 * Predicate for filtering files, which are older than "today"
	 */
	protected Predicate<File> isOlderThanToday = logFile -> {
		return getLogFileTime(logFile) < startOfToday.getTimeInMillis();
	};

	/**
	 * Predicate for filtering files, which are older than 7 days
	 */
	protected Predicate<File> isOlderThanAWeek = dailyArchive -> {
		return getDailyArchiveTime(dailyArchive) < startOfSevenDaysAgo.getTimeInMillis();
	};

	/**
	 * Create an instance with given log base dir and the current timestamp as "today"
	 * @param logDir log base directory
	 */
	public LogFileArchiver(File logDir) {
		this(logDir, System.currentTimeMillis());
	}

	/**
	 * Create an instance with given log base dir and given timestamp as "today" (this constructor is mainly used by the unit tests)
	 * @param logDir log base directory
	 * @param time timestamp of "today"
	 */
	public LogFileArchiver(File logDir, long time) {
		startOfToday = Calendar.getInstance();
		startOfToday.setTimeInMillis(time);
		startOfToday.set(Calendar.HOUR_OF_DAY, 0);
		startOfToday.set(Calendar.MINUTE, 0);
		startOfToday.set(Calendar.SECOND, 0);
		startOfToday.set(Calendar.MILLISECOND, 0);

		startOfSevenDaysAgo = Calendar.getInstance();
		startOfSevenDaysAgo.setTimeInMillis(startOfToday.getTimeInMillis());
		startOfSevenDaysAgo.add(Calendar.DAY_OF_MONTH, -7);

		this.logDir = logDir;
	}

	/**
	 * Archive the publish log files.
	 * @throws IOException
	 */
	public void archivePublishLogs() throws IOException {
		if (!logDir.exists() || !logDir.isDirectory()) {
			return;
		}

		// find all log files, that need to be archived
		Map<File, List<File>> logFilesToArchive = new HashMap<>();

		for (File logFile : logDir.listFiles(file -> {
			return isLogFile.and(isOlderThanToday).test(file);
		})) {
			Matcher matcher = logFilePattern.matcher(logFile.getName());
			if (matcher.matches()) {
				File dailyArchive = new File(logDir, "publishrun_" + matcher.group("year") + "-" + matcher.group("month") + "-"
						+ matcher.group("day") + ".tar.gz");
				logFilesToArchive.computeIfAbsent(dailyArchive, f -> new ArrayList<>()).add(logFile);
			}
		}

		// add all old log files to their daily archive files
		for (Map.Entry<File, List<File>> entry : logFilesToArchive.entrySet()) {
			addToArchive(entry.getValue(), entry.getKey(), true);
		}

		// find all daily archive files, that need to be archived
		Map<File, List<File>> dailyArchivesToArchive = new HashMap<>();

		for (File archive : logDir.listFiles(file -> {
			return isDailyArchiveFile.and(isOlderThanAWeek).test(file);
		})) {
			Matcher matcher = dailyArchiveFilePattern.matcher(archive.getName());
			if (matcher.matches()) {
				File monthlyArchive = new File(logDir, "publishrun_" + matcher.group("year") + "-" + matcher.group("month") + ".tar");
				dailyArchivesToArchive.computeIfAbsent(monthlyArchive, f -> new ArrayList<>()).add(archive);
			}
		}

		// add all old daily archives to their monthly archive files
		for (Map.Entry<File, List<File>> entry : dailyArchivesToArchive.entrySet()) {
			addToArchive(entry.getValue(), entry.getKey(), false);
		}
	}

	/**
	 * Determine the "date" of the given log file.
	 * If the log file matches the pattern for log files, the "date" is taken from the filename.
	 * Otherwise the modification time of the file itself is taken.
	 * @param logFile log file
	 * @return log file date
	 */
	protected long getLogFileTime(File logFile) {
		Matcher matcher = logFilePattern.matcher(logFile.getName());
		if (matcher.matches()) {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(Integer.parseInt(matcher.group("year")), Integer.parseInt(matcher.group("month")) - 1,
					Integer.parseInt(matcher.group("day")), Integer.parseInt(matcher.group("hour")),
					Integer.parseInt(matcher.group("minute")), Integer.parseInt(matcher.group("second")));
			return cal.getTimeInMillis();
		} else {
			return logFile.lastModified();
		}
	}

	/**
	 * Get the "date" of the given daily archive file.
	 * If the file name matches the pattern for daily archives, the date is taken from the filename.
	 * Otherwise the modification time of the file itself is taken.
	 * @param dailyArchive archive file
	 * @return archive file date
	 */
	protected long getDailyArchiveTime(File dailyArchive) {
		Matcher matcher = dailyArchiveFilePattern.matcher(dailyArchive.getName());
		if (matcher.matches()) {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(Integer.parseInt(matcher.group("year")), Integer.parseInt(matcher.group("month")) - 1,
					Integer.parseInt(matcher.group("day")), 0, 0, 0);
			return cal.getTimeInMillis();
		} else {
			return dailyArchive.lastModified();
		}
	}

	/**
	 * Add the given files to the archive
	 * @param files files to add
	 * @param archive archive file
	 * @param gzip true to gzip
	 * @throws IOException
	 */
	protected void addToArchive(List<File> files, File archive, boolean gzip) throws IOException {
		// if the archive already exists, we need to do extra work (since the Apache commons compress library is not able to append to an existing .tar.gz)
		// 1. Rename the existing file to ".old"
		// 2. Read all tar entries of the old archive and put into the new archive
		// 3. Continue with adding the new files
		File oldArchive = null;
		if (archive.exists()) {
			oldArchive = new File(archive.getParentFile(), archive.getName() + ".old");
			if (oldArchive.exists()) {
				FileUtils.deleteQuietly(oldArchive);
			}
			FileUtils.moveFile(archive, oldArchive);
		}

		if (gzip) {
			try (OutputStream fOut = Files.newOutputStream(archive.toPath());
					BufferedOutputStream buffOut = new BufferedOutputStream(fOut);
					GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
					TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut)) {
				tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
				if (oldArchive != null) {
					try (FileInputStream in = new FileInputStream(oldArchive);
							GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
							TarArchiveInputStream tIn = new TarArchiveInputStream(gzIn)) {
						writeToArchive(tIn, tOut);
					}
				}
				writeToArchive(files, tOut);
			}
		} else {
			try (OutputStream fOut = Files.newOutputStream(archive.toPath());
					BufferedOutputStream buffOut = new BufferedOutputStream(fOut);
					TarArchiveOutputStream tOut = new TarArchiveOutputStream(buffOut)) {
				tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
				if (oldArchive != null) {
					try (FileInputStream in = new FileInputStream(oldArchive);
							TarArchiveInputStream tIn = new TarArchiveInputStream(in)) {
						writeToArchive(tIn, tOut);
					}
				}
				writeToArchive(files, tOut);
			}
		}

		// if present, delete the old archive backup file
		if (oldArchive != null) {
			oldArchive.delete();
		}

		// delete all log files, that have been added to the archive
		for (File file : files) {
			file.delete();
		}
	}

	/**
	 * Write the tar entries from the input stream to the output stream
	 * @param tIn tar archive input stream
	 * @param tOut tar archive output stream
	 * @throws IOException
	 */
	protected void writeToArchive(TarArchiveInputStream tIn, TarArchiveOutputStream tOut) throws IOException {
		TarArchiveEntry oldTarEntry = null;
		while ((oldTarEntry = tIn.getNextTarEntry()) != null) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(oldTarEntry.getName());
			tarEntry.setMode(oldTarEntry.getMode());
			tarEntry.setUserId(oldTarEntry.getLongUserId());
			tarEntry.setGroupId(oldTarEntry.getLongGroupId());
			tarEntry.setSize(oldTarEntry.getSize());
			tarEntry.setModTime(oldTarEntry.getModTime());
			tarEntry.setLinkName(oldTarEntry.getLinkName());
			tarEntry.setUserName(oldTarEntry.getUserName());
			tarEntry.setGroupName(oldTarEntry.getGroupName());
			tarEntry.setDevMajor(oldTarEntry.getDevMajor());
			tarEntry.setDevMinor(oldTarEntry.getDevMinor());
			tOut.putArchiveEntry(tarEntry);

			IOUtils.copy(tIn, tOut);

			tOut.closeArchiveEntry();
		}
	}

	/**
	 * Write the given files into the archive
	 * @param files files to add the the archive
	 * @param tOut archive output stream
	 * @throws IOException
	 */
	protected void writeToArchive(List<File> files, TarArchiveOutputStream tOut) throws IOException {
		// add the files
		for (File file : files) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(file, file.getName());

			tOut.putArchiveEntry(tarEntry);
			Files.copy(file.toPath(), tOut);

			tOut.closeArchiveEntry();
		}

		// finish the archive
		tOut.finish();
	}
}
