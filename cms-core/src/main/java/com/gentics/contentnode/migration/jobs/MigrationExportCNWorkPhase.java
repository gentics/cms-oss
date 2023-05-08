package com.gentics.contentnode.migration.jobs;

import com.gentics.contentnode.publish.CNWorkPhase;
import com.gentics.lib.etc.IWorkPhase;
import com.gentics.lib.log.NodeLogger;

/**
 * A simple custom CNWorkerPhase that is used to log the export progress using the given logger
 */
public class MigrationExportCNWorkPhase extends CNWorkPhase {

	NodeLogger logger;
	Thread loggerThread;

	public MigrationExportCNWorkPhase(NodeLogger logger, IWorkPhase parent, String id, String name) {
		super(parent, id, name);
		this.logger = logger;
	}

	@Override
	public void begin() {
		logger.debug("Backup export: Starting export");
		// Create and start the logger thread
		Runnable r = new Runnable() {
			public void run() {
				boolean interrupted = false;
				while (!interrupted) {
					logger.debug("Backup export: " + getName() + " progress: " + getProgress() + " eta: " + getETA());
					try {
						Thread.sleep(2500);
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
			}
		};
		loggerThread = new Thread(r);
		loggerThread.start();
		super.begin();
	}

	@Override
	public void done() {
		logger.debug("Backup export: Finished export");
		stopLogger();
		super.done();
	}

	/**
	 * Stops the logging thread
	 */
	public void stopLogger() {
		if (loggerThread != null && loggerThread.isAlive()) {
			loggerThread.interrupt();
		}
	}

	@Override
	public IWorkPhase createSubPhase(String id, String name) {
		logger.debug("Backup export: " + name);
		return super.createSubPhase(id, name);
	}

	@Override
	public void addWork(int work) {
		logger.debug("Backup export: " + getName() + " " + getProgress());
		super.addWork(work);
	}
}
