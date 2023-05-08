package com.gentics.contentnode.events;

import java.io.File;
import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.lib.log.NodeLogger;

/**
 * Class to encapsulate the settings for logging events (for later analysis)
 */
public class EventLogging {
	/**
	 * Log
	 */
	private final static NodeLogger log = NodeLogger.getNodeLogger(EventLogging.class);

	/**
	 * threshold for dirted objects. if at least that number of objects are
	 * dirted after an event, the analysis is stored in the db
	 */
	protected int dirtThreshold;

	/**
	 * number of analysis files that are retained
	 */
	protected int retainAnalysisFiles;

	/**
	 * Flag whether event analysis shall be logged at all
	 */
	protected boolean logEventAnalysis;

	/**
	 * Get the current event logging settings
	 * @return the event logging settings
	 * @throws NodeException
	 */
	public static EventLogging getEventLogging() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
	
		boolean activated = false;
		int dirtedObjectsThreshold = 1000;
		int retainedAnalysisFiles = 10;
	
		// check whether the feature is activated
		if (!t.getNodeConfig().getDefaultPreferences().getFeature("log_dirt_events")) {
			// no feature, no logging
			return new EventLogging(dirtedObjectsThreshold, retainedAnalysisFiles, false);
		}
	
		PreparedStatement pst = null;
		ResultSet res = null;
	
		try {
			pst = t.prepareStatement("SELECT * FROM nodesetup WHERE name LIKE ?");
			pst.setString(1, "event_logging_%");
	
			res = pst.executeQuery();
			while (res.next()) {
				String name = res.getString("name");
	
				if ("event_logging_activated".equals(name)) {
					activated = ObjectTransformer.getBoolean(res.getObject("intvalue"), activated);
				} else if ("event_logging_threshold".equals(name)) {
					dirtedObjectsThreshold = res.getInt("intvalue");
				} else if ("event_logging_retainedfiles".equals(name)) {
					retainedAnalysisFiles = res.getInt("intvalue");
				}
			}
	
			return new EventLogging(dirtedObjectsThreshold, retainedAnalysisFiles, activated);
		} catch (SQLException e) {
			throw new NodeException("Error while getting event logging settings", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Create an instance of the event logging settings
	 * @param dirtThreshold dirt threshold
	 * @param retainAnalysisFiles number of retained analysis files
	 * @param logEventAnalysis whether event logging is activated at all
	 */
	public EventLogging(int dirtThreshold, int retainAnalysisFiles, boolean logEventAnalysis) {
		super();
		this.dirtThreshold = dirtThreshold;
		this.retainAnalysisFiles = retainAnalysisFiles;
		this.logEventAnalysis = logEventAnalysis;
	}

	/**
	 * Get the dirt threshold
	 * @return dirt threshold
	 */
	public int getDirtThreshold() {
		return dirtThreshold;
	}

	/**
	 * Get number of retained analysis files
	 * @return number of retained analysis files
	 */
	public int getRetainAnalysisFiles() {
		return retainAnalysisFiles;
	}

	/**
	 * Check whether logging the event analysis is activated
	 * @return true for activated event analysis, false if not
	 */
	public boolean isLogEventAnalysis() {
		return logEventAnalysis;
	}

	/**
	 * Do some cleanup:
	 * <ul>
	 *  <li>Remove too old unfinished analysis entries</li>
	 *  <li>Remove the oldest finished analysis entries, if more than retainAnalysisFiles exist</li>
	 *  <li>Remove temporary files from analysis entries which no longer exist</li>
	 * </ul>
	 * @throws NodeException
	 */
	public void doCleanup() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			// remove too old unfinished analysis entries
			pst = t.prepareDeleteStatement("DELETE FROM dirtanalysis WHERE finished = 0 AND timestamp < now() - 86400");
			pst.executeUpdate();

			// remove the oldest finished analysis entries, if more than retainAnalysisFiles exist
			t.closeStatement(pst);
			pst = t.prepareStatement("SELECT id FROM dirtanalysis WHERE finished = 1 ORDER BY timestamp DESC", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);

			res = pst.executeQuery();
			int rowsRead = 0;

			while (res.next()) {
				rowsRead++;
				if (rowsRead > retainAnalysisFiles) {
					res.deleteRow();
				}
			}

			// remove temporary files from analysis entries which no longer exist
			// TODO
		} catch (Exception e) {
			throw new NodeException("Error while cleaning up dirtanalysis entries", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Start a new analysis with the given data
	 * @param sid sid
	 * @param objType obj type
	 * @param objId obj id
	 * @param action action
	 * @param timestamp
	 * @throws NodeException
	 */
	public void startNewAnalysis(String sid, int objType, int objId, String action, int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			// cleanup all unfinished analyses with the given sid
			pst = t.prepareDeleteStatement("DELETE FROM dirtanalysis WHERE sid = ? AND finished = 0");
			pst.setString(1, sid);
			pst.executeUpdate();
			t.closeStatement(pst);

			pst = t.prepareInsertStatement("INSERT INTO dirtanalysis (sid, obj_type, obj_id, action, finished, timestamp) VALUES (?, ?, ?, ?, ?, ?)");
			pst.setString(1, sid);
			pst.setInt(2, objType);
			pst.setInt(3, objId);
			pst.setString(4, action);
			pst.setInt(5, 0);
			pst.setInt(6, timestamp);

			pst.executeUpdate();
		} catch (SQLException e) {
			throw new NodeException("Error while starting new analysis", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Add the number of dirted objects to the currently logged dirt event analysis
	 * @param sid sid identifying the analysis
	 * @param dirtedObjects number of dirted objects (by this event)
	 * @param timestamp timestamp of the event
	 */
	public void addDirtedObjects(String sid, int dirtedObjects, int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT * FROM dirtanalysis WHERE sid = ? AND finished = 0", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			pst.setString(1, sid);

			res = pst.executeQuery();
			if (res.next()) {
				int oldDirtedObjects = res.getInt("dirted");

				res.updateInt("dirted", oldDirtedObjects + dirtedObjects);
				res.updateRow();
			} else {
				res.moveToInsertRow();
				res.updateString("sid", sid);
				res.updateInt("dirted", dirtedObjects);
				res.updateInt("timestamp", timestamp);
				res.insertRow();
			}
		} catch (SQLException e) {
			throw new NodeException("Error while adding # of dirted objects to analysis", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Finalize the analysis writing: transform the analysis into a visualization xml and store both in the db
	 * @param sid sid identifying the analysis
	 * @param timestamp timestamp of the last event
	 * @throws NodeException
	 */
	public void finalizeAnalysis(String sid, int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		File loggingFile = new File(System.getProperty("java.io.tmpdir"), "tmp_analysis_" + sid + ".xml");
		File transformedLoggingFile = null;

		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT * FROM dirtanalysis WHERE sid = ? AND finished = 0", ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			pst.setString(1, sid);

			res = pst.executeQuery();
			if (res.next()) {
				// check whether the threshold was reached
				if (res.getInt("dirted") >= dirtThreshold) {
					// this is the temporary logging file
					transformedLoggingFile = new File(System.getProperty("java.io.tmpdir"), "tmp_jstree_analysis_" + sid + ".xml");

					// transform the analysis into jstree
					EventLogging.transformAnalysisFile(loggingFile, transformedLoggingFile);

					// store the files in the database
					res.updateBinaryStream("analysis", new FileInputStream(loggingFile), (int) loggingFile.length());
					res.updateBinaryStream("jstree_analysis", new FileInputStream(transformedLoggingFile), (int) transformedLoggingFile.length());
					res.updateInt("finished", 1);
					res.updateRow();
				} else {
					res.deleteRow();
				}
			}
		} catch (Exception e) {
			throw new NodeException("Error while triggering event " + this, e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);

			// delete the temporary files
			loggingFile.delete();
			if (transformedLoggingFile != null) {
				transformedLoggingFile.delete();
			}
		}

		// now do some cleanup
		doCleanup();
	}

	/**
	 * Public helper method to transform a simulation analysis file into something displayable
	 * @param analysisFile analysis file
	 * @param transformedFile transformed file
	 * @return true when the transformation succeeded, false if not
	 */
	public static boolean transformAnalysisFile(File analysisFile, File transformedFile) {
		if (log.isDebugEnabled()) {
			log.debug("Transforming analysis file {" + analysisFile.getAbsolutePath() + "} into {" + transformedFile.getAbsolutePath() + "}");
		}
		long startTime = System.currentTimeMillis();
		TransformerFactory tFactory = TransformerFactory.newInstance();
	
		try {
			Transformer transformer = tFactory.newTransformer(new StreamSource(EventLogging.class.getResourceAsStream("events2jstree.xsl")));
	
			transformer.transform(new StreamSource(new FileInputStream(analysisFile)), new StreamResult(transformedFile));
		} catch (Exception e) {
			log.error("Error while transforming simulation analysis file", e);
			return false;
		}
	
		if (log.isDebugEnabled()) {
			log.debug(
					"Transformed analysis file {" + analysisFile.getAbsolutePath() + "} into {" + transformedFile.getAbsolutePath() + "} in "
					+ (System.currentTimeMillis() - startTime) + " ms");
		}
	
		return true;
	}
}