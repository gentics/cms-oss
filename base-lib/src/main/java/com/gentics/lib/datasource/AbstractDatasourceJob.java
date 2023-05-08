package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base class for all datasource related background jobs
 */
public abstract class AbstractDatasourceJob implements Runnable {
	/**
	 * logger object
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Job name
	 */
	protected String name;

	/**
	 * Datasource factory
	 */
	protected DatasourceFactory factory;

	/**
	 * Create an instance of the AbstractDatasourceJob
	 * @param factory datasource factory
	 */
	public AbstractDatasourceJob(DatasourceFactory factory) {
		this.name = String.format("%s for %s", getClass().getName(), factory.getId());
		this.factory = factory;
	}

	/**
	 * Get a datasource to be used in the job
	 * @return datasource or null in case of an error
	 */
	protected Datasource getDatasource() {
		return factory.getInstance();
	}

	/**
	 * Process the job
	 */
	protected abstract void process();

	@Override
	public void run() {
		try {
			process();
			PortalConnectorFactory.handleJobRun(getClass(), name, null);
		} catch (Throwable e) {
			logger.error(String.format("Error while running job %s", name), e);
			PortalConnectorFactory.handleJobRun(getClass(), name, e);
		}
	}
}
