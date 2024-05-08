/*
 * @author herbert
 * @date 29.03.2007
 * @version $Id: UnitTestNodeLogger.java,v 1.1 2010-02-04 14:25:04 norbert Exp $
 */
package com.gentics.node.tests;

import com.gentics.lib.log.NodeLogger;

public class UnitTestNodeLogger extends NodeLogger {

	protected UnitTestNodeLogger(String name) {
		super(getLogger(UnitTestNodeLogger.class), UnitTestNodeLogger.class);
	}

}
