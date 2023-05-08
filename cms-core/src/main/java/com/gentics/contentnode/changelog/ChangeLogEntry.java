package com.gentics.contentnode.changelog;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.regex.Pattern;

import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;

/**
 * Changelog Entry
 */
public class ChangeLogEntry {

	public static SimpleDateFormat sdf = null;

	public static SimpleDateFormat sdf2 = null;

	private static final Pattern SPACE_REGEX = Pattern.compile("\\s+");

	private Date changeLogDate;

	private Date buildDate;

	private String buildLine;

	private String buildProductVersion;

	private String buildBranchName;

	private String buildNr;

	/**
	 * number of the change (new changelog mechanism)
	 */
	private String changeNr;

	private String changeBy;

	private String changeType;

	private String change;

	private String databaseConstraint;

	public ChangeLogEntry(String line, boolean parseDates) throws ParseException {
		if (parseDates && sdf == null) {
			sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf2 = new SimpleDateFormat("yyyyMMdd");
		}

		parseLine(line, parseDates);
	}

	private void parseLine(String line, boolean parseDates) throws ParseException {

		String[] parts = SPACE_REGEX.split(line, 5);

		if (parseDates) {
			changeLogDate = sdf.parse(parts[0]);
		}

		this.buildLine = parts[1];

		// detect changes of the new mechanism
		if (buildLine.startsWith("CH-")) {
			changeNr = buildLine;
		} else {
			String[] buildNrParts = buildLine.split("-");

			if (buildNrParts.length == 3) {
				buildProductVersion = buildNrParts[0];
				buildBranchName = buildNrParts[1];
				buildNr = buildNrParts[2];

				if (parseDates) {
					buildDate = sdf2.parse(buildNr);
				}
			} else {
				if (!buildLine.equalsIgnoreCase("NEW")) {
					if (parseDates) {
						buildDate = sdf2.parse(buildLine);
					}
				} else {
					// IF the Change is unmanaged (NEW) we declare it as 100% up
					// to date
					buildDate = new Date();
				}
			}
		}

		this.changeBy = parts[2];
		this.changeType = parts[3];

		// handle different databases:
		if (parts[4].indexOf("@") == 0) {
			int start = parts[4].indexOf("@");
			int stop = parts[4].indexOf(":", start);

			this.databaseConstraint = parts[4].substring(start + 1, stop);
			this.change = parts[4].substring(stop);
		} else {
			this.change = parts[4];
		}

	}

	/**
	 * Returns the database constraint if one has been defined in this change
	 * 
	 * @return
	 */
	public String getDatabaseConstraint() {
		return this.databaseConstraint;
	}

	public String getBuildNr() {
		return this.buildNr;
	}

	/**
	 * Returns the build date of the change
	 * 
	 * @return
	 */
	public Date getBuildDate() {
		return this.buildDate;
	}

	public String getBuildLine() {
		return this.buildLine;
	}

	public String getBuildBranchName() {
		return buildBranchName;
	}

	public String getBuildProductVersion() {
		return buildProductVersion;
	}

	/**
	 * Returns the full branchname aka node_41_stable / node_head
	 * 
	 * @return
	 */
	public String getFullBranchName() {
		// Since there is no pattern we can use we have to do this ugly
		// transformation
		if ("V4.1-head".equals(this.buildProductVersion + "-" + this.buildBranchName)) {
			return "contentnode_head";
		}
		if ("V4.1-stable".equals(this.buildProductVersion + "-" + this.buildBranchName)) {
			return "contentnode_41_stable";
		}
		return null;
	}

	public Date getChangeLogDate() {
		return this.changeLogDate;
	}

	public String getChangeType() {
		return this.changeType;
	}

	public String getChangeBy() {
		return this.changeBy;
	}

	public String getChange() {
		return this.change;
	}

	/**
	 * Check whether this entry is one of the new changelog mechanism
	 * 
	 * @return true for the new changelog mechanism, false for the old one
	 */
	public boolean isNewMechanism() {
		return changeNr != null;
	}

	/**
	 * Get the change nr (if this is a change of the new changelog mechanism),
	 * null otherwise
	 * 
	 * @return change nr or null
	 */
	public String getChangeNr() {
		return changeNr;
	}

	/**
	 * Apply the changelog entry
	 * @param handle database handle
	 * @throws SQLException
	 */
	public void apply(DBHandle handle) throws SQLException {
		String sqlChange = getChange();

		// removing comment of change if existing
		if (sqlChange.indexOf("##") > 0) {
			sqlChange = sqlChange.substring(0, sqlChange.indexOf("##"));
		}

		String[] sqlQueries = sqlChange.replaceAll("\\\\;", "@@GTXDELIM@@").trim().split(";");

		List<String> allQueries = new ArrayList<>();
		for (String query : sqlQueries) {
			allQueries.add(query.replaceAll("@@GTXDELIM@@", ";"));
		}

		// execute queries
		long start = System.currentTimeMillis();
		for (String query : allQueries) {
			DB.update(handle, query);
		}
		long duration = System.currentTimeMillis() - start;
		int timestamp = (int) (System.currentTimeMillis() / 1000L);

		// add entry to changelog_applied
		DB.update(handle, "INSERT INTO changelog_applied (id, timestamp, duration) VALUES (?, ?, ?)",
				new Object[] { changeNr, timestamp, duration });
	}

	public String toString() {

		String changeLogDateString = "";

		if (changeLogDate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat();
	
			sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
			sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
			changeLogDateString = sdf.format(changeLogDate) + "";
		}

		return changeLogDateString + buildLine + "  " + changeBy + "  " + changeType + "  " + change;
	}
}
