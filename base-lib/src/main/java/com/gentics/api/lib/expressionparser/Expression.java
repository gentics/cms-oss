/*
 * @author norbert
 * @date 27.06.2006
 * @version $Id: Expression.java,v 1.1 2006-08-02 11:33:36 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

import java.io.Serializable;

/**
 * Base interface for expressions.
 */
public interface Expression extends Serializable {

	/**
	 * Get the (complete) expression as string
	 * @return expression as string
	 */
	String getExpressionString();
}
