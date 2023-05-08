/*
 * @author herbert
 * @date May 16, 2007
 * @version $Id: CNWorkPhase.java,v 1.8 2008-10-16 15:09:57 jan Exp $
 */
package com.gentics.contentnode.publish;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Responsible for calculating ETA and other statistics 
 * about a publish run. (Please see inline comment on top of
 * class for more insight into my weird thoughts)
 * 
 * @author herbert
 */
public class CNWorkPhase implements IWorkPhase, Serializable {
	/*
	 * Short description on how this should work.
	 * 
	 * During the publish process one root WorkPhase is
	 * created (e.g. 'publish').
	 * Each phase in this process should be a separate 
	 * WorkPhase - all work units are expected to be
	 * more or less on the same length within one WorkPhase
	 * 
	 * The 'id' should be unique because it is used to store
	 * the average length and work unit count into the database.
	 * 
	 * All phases should be created in an "initialization" phase and
	 * not when the phase begins. This makes it possible to calculate an
	 * ETA from the beginning on. - after this phase 'init' should be called
	 * on the root phase .. it will recursively call this for all subphases.
	 * 
	 * It would be helpful to also add the work unit count in this 
	 * initialization - esspecially if it is expected to vary much 
	 * between each run. (e.g. page rendering) - if this is not
	 * defined the number stored in the database is used.
	 * 
	 * 
	 * Every work phase has a "deviationFactor" which should give
	 * information how big the difference can be between the duration
	 * of single tasks. E.g. if it is 20 (%) it is expected that if
	 * 50% of all work units are done, we can calculate the ETA with
	 * +/- 20 % accuracy.
	 */

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6630180628121444090L;
	private int work;
	private int done = 0;
	private boolean workWasDefined = false;

	private long startTime = 0;
	private long endTime = 0;

	private List<IWorkPhase> subPhases = new ArrayList<>();
	private String id;
	private String name;
	private IWorkPhase parent;
	protected int averagems;
	protected int averageitems;
	private int deviationFactor;
	private int weight = 1;

	private static NodeLogger logger = NodeLogger.getNodeLogger(CNWorkPhase.class);

	/**
	 * true if average ms was loaded from the database.
	 */
	protected boolean averagemsrow;

	/**
	 * true if average items was loaded from database.
	 */
	protected boolean averageitemsrow;

	/**
	 * Default constructor
	 */
	public CNWorkPhase() {
	}

	/**
	 * Constructs a new WorkPhase. You need to call 
	 * {@link #addWork(int)} and {@link #begin()}
	 * before doing the actual work !
	 * 
	 * @param parent the parent of this work phase.
	 * @param id id of the workphase - needs to be unique system wide !
	 * @param name userfriendly name for this phase.
	 * @param deviationFactor the "deviationFactor" - see the inline documentation of this class for more info.
	 */
	public CNWorkPhase(IWorkPhase parent, String id, String name, int deviationFactor) {
		if (parent != null) {
			this.parent = parent;
			this.parent.addSubPhase(this);
		}
		this.id = id;
		this.name = name;
		this.deviationFactor = deviationFactor;
	}

	/**
	 * @see #WorkPhase(CNWorkPhase, String, String, int)
	 * @param parent
	 * @param id
	 * @param name
	 */
	public CNWorkPhase(IWorkPhase parent, String id, String name) {
		this(parent, id, name, 20);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#init()
	 */
	public void init() throws NodeException {
		retrieveOldAverage();

		for (IWorkPhase phase : subPhases) {
			phase.init();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#finish(boolean)
	 */
	public void finish(boolean isRepresentative) throws NodeException {
		if (isRepresentative || !averageitemsrow || !averagemsrow || averageitems == 0 || averagems == 0) {
			this.storeAverage();
		}
		for (IWorkPhase phase : subPhases) {
			phase.finish(isRepresentative);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#addWork(int)
	 */
	public void addWork(int work) {
		this.work += work;
		workWasDefined = true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#addSubPhase(com.gentics.contentnode.publish.IWorkPhase)
	 */
	public void addSubPhase(IWorkPhase workPhase) {
		subPhases.add(workPhase);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#doneWork()
	 */
	public void doneWork() {
		this.done++;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#doneWork(int)
	 */
	public void doneWork(int units) {
		this.done += units;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#begin()
	 */
	public void begin() {
		this.startTime = System.currentTimeMillis();
		logger.debug("BEGIN WORK {" + getName() + "}");
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#isCurrentlyRunning()
	 */
	public boolean isCurrentlyRunning() {
		return startTime != 0 && endTime == 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getCurrentPhase()
	 */
	public IWorkPhase getCurrentPhase() {
		if (!isCurrentlyRunning()) {
			return null;
		}
		for (IWorkPhase phase : subPhases) {
			if (phase.isCurrentlyRunning()) {
				return phase.getCurrentPhase();
			}
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#done()
	 */
	public void done() {
		this.done = this.work;
		this.endTime = System.currentTimeMillis();
		logger.debug("DONE WORK {" + getName() + "}");
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getDeviation()
	 */
	public int getDeviation() {
		return done == 0 || startTime == 0 ? deviationFactor * 3 : deviationFactor;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getETA()
	 */
	public int getETA() {
		if (endTime > 0) {
			return 0;
		}
		if ((startTime == 0 || endTime != 0 || work <= done) && subPhases.size() == 0) {
			return (averagems == 0 ? 1 : averagems) * getWork();
		}

		long currentTime = System.currentTimeMillis() - startTime;
		long msperwork = averagems;
		int eta = 0;

		// No work done yet...
		if (done > 0) {
			msperwork = currentTime / done;

			// smooth transition between old average and new average - weighted by progress.

			// done: items rendered/done
			// work: items total to do
			// averagems: average ms per item from last run
			// msperwork: average ms per item from this run
			// curenttime: duration frmo start of run until last item done

			// // stefanb:
			eta = (int) (((work - done) / (double) work) * ((averagems == 0 ? msperwork : averagems) * (work - done) + currentTime));

			// /// LAURINS try
			/*
			 float fortschritt = 1 / (work == 0 ? 1 : work) * done;
			 eta = (int) ((done * fortschritt) * msperwork + (1 - fortschritt) * (work - done)
			 * averagems);
			 */

			// /// FIRST try:
			// eta = (int) ((done * msperwork + (work - done) * averagems) - currentTime);
		} else {
			// use old average..
			eta = (int) (msperwork * (work - done));
		}
		for (IWorkPhase phase : subPhases) {
			eta += phase.getETA();
		}
		return eta;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getTotalWork()
	 */
	public int getTotalWork() {
		int totalWork = getWork();

		for (IWorkPhase phase : subPhases) {
			totalWork += phase.getTotalWork();
		}
		return totalWork;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getTotalDoneWork()
	 */
	public int getTotalDoneWork() {
		int totalDoneWork = getDoneWork();

		for (IWorkPhase phase : subPhases) {
			totalDoneWork += phase.getTotalDoneWork();
		}
		return totalDoneWork;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getWork()
	 */
	public int getWork() {
		if (work == 0 && !workWasDefined) {
			return averageitems;
		}
		return work;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getDoneWork()
	 */
	public int getDoneWork() {
		return done;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getAverageMsName()
	 */
	public String getAverageMsName() {
		return "publish:workphase:" + id + ":averagems"; 
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getAverageItemsName()
	 */
	public String getAverageItemsName() {
		return "publish:workphase:" + id + ":averageitems";
	}

	/**
	 * Determines an average by looking into the database.
	 * @throws NodeException 
	 * 
	 */
	private void retrieveOldAverage() throws NodeException {
		final String averagemsname = getAverageMsName();
		final String averageitemsname = getAverageItemsName();

		DBUtils.executeStatement("SELECT name, intvalue FROM nodesetup WHERE name IN (?,?)", new SQLExecutor() {

			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setString(1, averagemsname);
				stmt.setString(2, averageitemsname);
			}

			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				averagemsrow = false;
				averageitemsrow = false;
				while (rs.next()) {
					String name = rs.getString(1);
					int val = rs.getInt(2);

					if (name.endsWith("averagems")) {
						averagems = val;
						averagemsrow = true;
					} else if (name.endsWith("averageitems")) {
						averageitems = val;
						averageitemsrow = true;
					}
				}

				if (!averagemsrow || !averageitemsrow) {
					// If the rows do not yet exist, create them...
					DBUtils.executeStatement("INSERT INTO nodesetup (name, intvalue) values " + StringUtils.repeat("(?,?)", !averageitemsrow && !averagemsrow ? 2 : 1, ","), new SQLExecutor() {

						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							int i = 1;

							if (!averagemsrow) {
								stmt.setString(i++, averagemsname);
								stmt.setInt(i++, 0);
							}
							if (!averageitemsrow) {
								stmt.setString(i++, averageitemsname);
								stmt.setInt(i++, 0);
							}
						}

					}, Transaction.INSERT_STATEMENT);
				}
			}
		}, Transaction.INSERT_STATEMENT);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getPhaseCount(com.gentics.contentnode.publish.IWorkPhase)
	 */
	public int getPhaseCount(IWorkPhase stopPhase) {
		if (subPhases.size() == 0) {
			return 1;
		}
		int count = 0;

		for (IWorkPhase phase : subPhases) {
			count += phase.getPhaseCount(stopPhase);
			if (phase == stopPhase) {
				return count;
			}
		}
		return count;
	}

	/**
	 * Stores the current values as averages ..
	 * @throws NodeException
	 */
	private void storeAverage() throws NodeException {
		DBUtils.executeStatement("UPDATE nodesetup SET intvalue = ? WHERE name = ?", new SQLExecutor() {
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, work);
				stmt.setString(2, getAverageItemsName());
			}
		}, Transaction.UPDATE_STATEMENT);
		if (work > 0) {
			DBUtils.executeStatement("UPDATE nodesetup SET intvalue = ? WHERE name = ?", new SQLExecutor() {
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, (int) ((endTime - startTime) / work));
					stmt.setString(2, getAverageMsName());
				}
			}, Transaction.UPDATE_STATEMENT);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getSubPhases()
	 */
	public List<IWorkPhase> getSubPhases() {
		return subPhases;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getStartTime()
	 */
	public long getStartTime() {
		return startTime;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#getEndTime()
	 */
	public long getEndTime() {
		return endTime;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.publish.IWorkPhase#isDone()
	 */
	public boolean isDone() {
		return this.endTime > 0;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.IWorkPhase#createSubPhase(java.lang.String, java.lang.String)
	 */
	public IWorkPhase createSubPhase(String id, String name) {
		return new CNWorkPhase(this, id, name);
	}

	public int getProgress() {
		return ((int) (100. / this.getTotalWork() * this.getTotalDoneWork()));
	}

	public boolean isStarted() {
		return this.startTime != 0;
	}

	public int getAbsoluteProgress() {
		return getAbsoluteProgress(0, 100);
	}

	public int getAbsoluteProgress(int start, int end) {
		if (!isStarted()) {
			return start;
		}
		if (isDone()) {
			return end;
		}
		if (subPhases.size() > 0) {
			// int perPhase = (int)((end-start) / subPhases.size());
			int prevweight = 0;
			int newend;
			int newstart;

			for (IWorkPhase phase : subPhases) {
				if (!phase.isDone()) {
					int totalweight = this.getWeightOfSubPhases();
					int weight = phase.getWeight();
					int span = end - start;
					double rel = (double) span / (double) totalweight;
					int rstart = (int) (rel * prevweight);
					int rspan = (int) (rel * weight);

					newstart = start + rstart;
					newend = newstart + rspan;
					return ((CNWorkPhase)phase).getAbsoluteProgress(newstart, newend);
				}
				prevweight += phase.getWeight();
				// start = newend;
			}
			return end;
		}
		int span = end - start;

		if (getWork() == 0) {
			// if there is no work yet .. return the start value.
			return start;
		}
		return (int) (start + ((double) span / getWork() * getDoneWork()));
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	/**
	 * Calculates the total weight of all subphases.
	 * @return
	 */
	public int getWeightOfSubPhases() {
		int weight = 0;

		for (IWorkPhase phase : subPhases) {
			weight += phase.getWeight();
		}
		return weight;
	}
    
	public void renderRecursiveWorkPhase(StringBuffer ret, int depth) {
		ret.append(StringUtils.repeat("  ", depth)).append(this.isCurrentlyRunning() ? "-> " : "   ").append(this.getName()).append(" ").append(this.getDoneWork()).append('/').append(this.getWork()).append(" ETA: ").append(this.getETA() / 1000).append("  (+/-").append(this.getDeviation()).append("%)").append("  Absolute: ").append(getAbsoluteProgress()).append("%").append(" weight: ").append(getWeight()).append(
				"\n");

		for (IWorkPhase phase : subPhases) {
			phase.renderRecursiveWorkPhase(ret, depth + 1);
		}
	}
}
