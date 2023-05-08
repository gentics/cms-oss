/*
 * Created on 28.07.2004
 *
 * TODO implement
 */
package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;

/**
 * @author raoul TODO implement
 */
public interface Operand {

	/**
	 * Singlevalue used for OPERATOR_* If Operand is Multivalue, only first
	 * Value is returned
	 * @return Value
	 */
	String getValue();

	/**
	 * Multivalue used for OPERATOR_CONTAINS and OPERATOR_NOTCONTAINS
	 * @see com.gentics.lib.parser.rule.CompareOperator
	 * @return Array of Values
	 */
	String[] getValues();

	/**
	 * each operand can only have one invalidate-listener for
	 * performance-reasions! if the operand is a static value, it can ignore
	 * this method, otherwise it must call invalidate() if the operand-value
	 * changes
	 */

	void setInvalidateListener(InvalidationListener listener);

	/**
	 * tells the object that no one is listening to changes anymore
	 */
	void removeListener();
    
	/**
	 * Creates a deep copy of this Operand.
	 * @param ruleTree the new RuleTree this operand will be part of.
	 * @return a deep copy
	 */
	Operand deepCopy(RuleTree ruleTree);
}
