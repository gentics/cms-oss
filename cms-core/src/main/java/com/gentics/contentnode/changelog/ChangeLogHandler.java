package com.gentics.contentnode.changelog;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DB.DBTrx;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.log.NodeLogger;

/**
 * Changelog handler
 */
public final class ChangeLogHandler {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ChangeLogHandler.class);

	/**
	 * Name of the php modules, used for getting the CHANGELOG files
	 */
	public final static String[] MODULE_NAMES = { "content", "node", "system" };

	/**
	 * Applies the changelog to the current node databases. The Method will only
	 * apply changes that are older than the specified minage paramter.
	 * @param skipConstraints
	 * @param handle database handle
	 * 
	 * @return Returns a list that contains all matching sql queries
	 * @throws EnvironmentException
	 * @throws SQLUtilException
	 */
	public static List<ChangeLogEntry> getSQLQueriesFromChangeLog(String[] skipConstraints, DBHandle handle) throws NodeException {
		List<ChangeLogEntry> changesToApply = new ArrayList<>();

		// At first, fetch the IDs of the already applied changelog entries
		Set<String> appliedChangelogIds = getAppliedChangelogIds(handle);

		for (String moduleName : MODULE_NAMES) {

			List<ChangeLogEntry> changes = getSQLChanges(moduleName);

			for (ChangeLogEntry change : changes) {
				if (change == null) {
					NodeException ex = new NodeException(
							"Could not get valid ChangeLogEntry from changelog '" + moduleName + "' - terminating. Please fix this.");
					logger.error("Error while handling changelog", ex);
					throw ex;
				}

				// check if current database constraint is listed within the
				// constrains we want to skip
				if (change.getDatabaseConstraint() != null && containsConstraints(skipConstraints, change.getDatabaseConstraint())) {
					logger.debug("Skipping changelog entry because we want to skip those with constraint " + change.getDatabaseConstraint());
					continue;
				}

				// Check if the change bas been applied already
				if (change.isNewMechanism()) {
					// Check if the changelog entry has already been applied
					if (isChangeApplied(appliedChangelogIds, change)) {
						// Skip the change if it has already been applied
						continue;
					}
				} else {
					// Skip the change if it is of an old type
					continue;
				}

				logger.debug("Adding change to list of changes that should be applied.");
				logger.debug("Build: " + change.getBuildNr());
				if (change.isNewMechanism()) {
					logger.debug("Change: " + change.getChangeNr());
				} else {
					logger.debug("Date: " + change.getBuildDate());
				}
				logger.debug("Ka: " + change.getBuildLine());
				logger.debug("Change: " + change.getChange());

				changesToApply.add(change);
			}
		}

		return changesToApply;
	}

	/**
	 * Get the module CHANGELOG file for the given module name. The php checkout
	 * is supposed to be located at the given baseDir
	 * 
	 * @param baseDir
	 *            base dir of the php checkout
	 * @param moduleName
	 *            module name
	 * @return nodule file
	 */
	protected static InputStream getModuleChangelog(String moduleName) {
		return ChangeLogHandler.class.getResourceAsStream("/changelog/" + moduleName + ".CHANGELOG");
	}

	/**
	 * Get a list of all changes of the given changelog file
	 * 
	 * @param module
	 * @return
	 */
	protected static List<ChangeLogEntry> getSQLChanges(String moduleName) throws NodeException {
		List<ChangeLogEntry> changes = new ArrayList<ChangeLogEntry>();

		try (InputStream in = getModuleChangelog(moduleName); Scanner scanner = new Scanner(in, "UTF-8")) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				if (line.indexOf('#') != 0 && line.trim().length() != 0) {

					if (line.indexOf("If you see this") < 0) {
						ChangeLogEntry change = new ChangeLogEntry(line, false);

						// We only want SQL changes
						if (change.getChangeType().equalsIgnoreCase("SQL")) {
							changes.add(change);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new NodeException("Error while reading changelog file.", e);
		} catch (ParseException e) {
			throw new NodeException("Error while parsing changelog file line.", e);
		}

		return changes;
	}

	/**
	 * Checks if the given constraint is contained within the string array of
	 * constraints
	 * 
	 * @param skipConstraints
	 * @param constraint
	 * @return
	 */
	protected static boolean containsConstraints(String[] skipConstraints, String constraint) {
		for (int i = 0; i < skipConstraints.length; i++) {
			if (skipConstraints[i].equalsIgnoreCase(constraint)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether a change of the new mechanism is applied
	 * 
	 * @param isChangeApplied
	 *            The list of changelog entry IDs
	 * @param change
	 *            change to be checked
	 * @return true when the change is applied, false if not (or is not of the
	 *         new mechanism)
	 */
	protected static boolean isChangeApplied(Set<String> appliedChangelogIds, ChangeLogEntry change) {
		if (!change.isNewMechanism()) {
			return false;
		}

		return appliedChangelogIds.contains(change.getChangeNr());
	}

	/**
	 * Fetch all applied changelog IDs
	 * @param handle database handle
	 * @return The applied changelog IDs
	 * @throws NodeException When the table could not be created or the fetching failed
	 */
	protected static Set<String> getAppliedChangelogIds(DBHandle handle) throws NodeException {
		// Check for changelog table and create it if it is missing
		try (DBTrx trx = new DBTrx(handle)) {
			Set<String> appliedChangelogIds = new HashSet<String>();
			DB.query(handle, "SELECT id FROM changelog_applied", new ResultProcessor() {
				@Override
				public void takeOver(ResultProcessor p) {
				}
				@Override
				public void process(ResultSet rs) throws SQLException {
					while(rs.next()) {
						appliedChangelogIds.add(rs.getString("id"));
					}
				}
			});
			return appliedChangelogIds;
		} catch (SQLException e) {
			throw new NodeException(e);
		}
	}
}
