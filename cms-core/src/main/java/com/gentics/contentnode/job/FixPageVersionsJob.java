package com.gentics.contentnode.job;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.object.Page;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

/**
 * Background job that fixes the version numbers of pages
 */
public class FixPageVersionsJob extends AbstractBackgroundJob {

	/**
	 * Name of the nodesetup entry for this job
	 */
	public final static String NODESETUP = "fixpageversions";

	/**
	 * The logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	@Override
	public String getJobDescription() {
		return getClass().getName();
	}

	@Override
	protected void processAction() throws NodeException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting job " + getClass().getName());
		}
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		Transaction t = null;

		try {
			t = factory.startTransaction(true);

			// get the ids of all pages
			final List<Integer> pageIds = new Vector<Integer>();

			if (logger.isDebugEnabled()) {
				logger.debug("Start reading all page ids");
			}
			DBUtils.executeStatement("SELECT id FROM page", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException,
							NodeException {
					while (rs.next()) {
						pageIds.add(rs.getInt("id"));
					}
				}
			});

			// commit the transaction
			t.commit();
			t = null;

			int allPages = pageIds.size();

			if (logger.isInfoEnabled()) {
				logger.info("Start checking/fixing version numbers for " + allPages + " pages");
			}

			int tenPerc = 0;
			int pagesHandled = 0;

			// iterate over all page id's
			for (Integer pageId : pageIds) {
				abortWhenInterrupted();

				// create a new transaction
				t = factory.startTransaction(true);

				// get the page
				Page page = (Page) t.getObject(Page.class, pageId);

				// get the versions of the page (this will automatically generate version numbers and store them if not yet done)
				page.getVersions();

				// commit the transaction
				t.commit();

				pagesHandled++;
				if (pagesHandled * 10 / allPages > tenPerc) {
					tenPerc = pagesHandled * 10 / allPages;
					if (logger.isInfoEnabled()) {
						logger.info("Checked/fixed " + pagesHandled + "/" + allPages + " pages (" + (tenPerc * 10) + "%)");
					}
				}
			}

			if (logger.isInfoEnabled()) {
				logger.info("Job " + getClass().getName() + " finished successfully");
			}
		} finally {
			if (t != null) {
				try {
					t.commit();
				} catch (TransactionException e) {}
			}
		}
	}
}
