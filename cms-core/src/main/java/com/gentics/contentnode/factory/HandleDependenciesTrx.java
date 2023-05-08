package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;

/**
 * AutoCloseable to enable or disable dependency handling.
 */
public class HandleDependenciesTrx implements AutoCloseable {
	/**
	 * The logger.
	 */
	private final NodeLogger log = NodeLogger.getNodeLogger(HandleDependenciesTrx.class);

	/**
	 * The original value.
	 */
	private final boolean wasEnabled;

	/**
	 * Whether the value was actually changed.
	 */
	private final boolean valueChanged;

	/**
	 * Default constructor.
	 *
	 * Enable or disable dependency handling depending on the flag.
	 *
	 * @param enable <code>true</code> to enable dependency handling,
	 *		<code>false</code> to disable.
	 *
	 * @throws NodeException When no current transaction is available.
	 */
	public HandleDependenciesTrx(boolean enable) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (renderType != null) {
			wasEnabled = renderType.doHandleDependencies();
			valueChanged = wasEnabled != enable;

			if (valueChanged) {
				renderType.setHandleDependencies(enable);

				if (log.isDebugEnabled()) {
					log.debug((enable ? "En" : "Dis") + "abling dependency handling.");
				}
			} else if (log.isDebugEnabled()) {
					log.debug("Dependency handling was already " + (enable ? "en" : "dis") + "abled.");
			}
		} else {
			wasEnabled = valueChanged = false;

			if (log.isDebugEnabled()) {
				log.debug("No render type found, nothing to do.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws NodeException {
		if (valueChanged) {
			if (log.isDebugEnabled()) {
				log.debug("(Re-)" + (wasEnabled ? "en" : "dis") + "abling dependency handling.");
			}

			TransactionManager.getCurrentTransaction().getRenderType().setHandleDependencies(wasEnabled);
		}
	}
}
