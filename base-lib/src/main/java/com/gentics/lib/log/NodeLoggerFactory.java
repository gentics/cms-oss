/*
 * @author herbert
 * @date 29.03.2007
 * @version $Id: NodeLoggerFactory.java,v 1.2 2007-04-13 09:42:30 norbert Exp $
 */
package com.gentics.lib.log;

import org.apache.logging.log4j.Logger;

/**
 * Draft for a non-static node logger factory ..
 * usable for junit tests were we want to fail each time a
 * fatal or error is logged.
 * 
 * @author herbert
 * @see com.gentics.lib.log.NodeLogger
 */
public interface NodeLoggerFactory {
	public Logger getLogger(String name);
	public Logger getLogger(Class clazz);
    
	public NodeLogger getNodeLogger(Class clazz);
	public NodeLogger getNodeLogger(String name);
}
