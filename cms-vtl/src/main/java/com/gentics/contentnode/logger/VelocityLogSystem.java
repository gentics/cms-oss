/*
 * @author herbert
 * @date Mar 20, 2008
 * @version $Id: VelocityLogSystem.java,v 1.3 2010-09-28 17:01:30 norbert Exp $
 */
package com.gentics.contentnode.logger;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolver;
import com.gentics.lib.log.NodeLogger;

public class VelocityLogSystem implements LogSystem {
    
	NodeLogger logger = NodeLogger.getNodeLogger(VelocityLogSystem.class);

	public void init(RuntimeServices rs) throws Exception {
		// TODO Auto-generated method stub
		// runtime.log.logsystem.class
		logger.info("Initializing custom velocity log system.");
	}

	/**
	 * trys to log the given message into the render result
	 * (so users can see them in the debug stream in the CMS.
	 * if we are not in a render phase, this method returns false
	 * and will not log anything.)
	 */
	private boolean internalLogRenderResult(int level, String message) {
		try {
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t == null) {
				return false;
			}

			RenderType renderType = t.getRenderType();
            
			String postfix = null;
            
			if (renderType != null) {
				StackResolver stack = renderType.getStack();

				postfix = stack.getUIReadableStack();
			}
            
			RenderResult result = t.getRenderResult();

			if (result == null) {
				return false;
			}

			if (postfix != null) {
				message = new StringBuffer("Velocity " + logLevelToString(level) + " while rendering: ").append(postfix).append(" --- ").append(message).toString();
			}

			switch (level) {
			case LogSystem.WARN_ID:
				result.warn(VelocityLogSystem.class, message);
				break;

			case LogSystem.INFO_ID:
				result.info(VelocityLogSystem.class, message);
				break;

			case LogSystem.DEBUG_ID:
				result.debug(VelocityLogSystem.class, message);
				break;

			case LogSystem.ERROR_ID:
				result.error(VelocityLogSystem.class, message);
				break;

			default:
				result.debug(VelocityLogSystem.class, message);
				break;
			}
		} catch (TransactionException e) {
			return false;
		} catch (NodeException e) {
			logger.debug("Error while trying to log message into render result.", e);
			return false;
		}
		return true;
	}
    
	private String logLevelToString(int level) {
		switch (level) {
		case LogSystem.WARN_ID:
			return "warning";

		case LogSystem.INFO_ID:
			return "info";

		case LogSystem.DEBUG_ID:
			return "debug";

		case LogSystem.ERROR_ID:
			return "error";

		default:
			return "unknown";
		}
	}

	public void logVelocityMessage(int level, String message) {
		// first try to log into the render result ..
		if (!internalLogRenderResult(level, message)) {
			switch (level) {
			case LogSystem.WARN_ID:
				logger.warn(message);
				break;

			case LogSystem.INFO_ID:
				logger.info(message);
				break;

			case LogSystem.DEBUG_ID:
				logger.debug(message);
				break;

			case LogSystem.ERROR_ID:
				logger.error(message);
				break;

			default:
				logger.debug(message);
				break;
			}
		}
	}

}
