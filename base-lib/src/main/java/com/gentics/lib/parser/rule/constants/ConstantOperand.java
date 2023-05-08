/*
 * ConstantOperand.java
 *
 * Created on 08. Dezember 2004, 16:39
 */

package com.gentics.lib.parser.rule.constants;

import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;
import com.gentics.lib.parser.rule.Operand;

/**
 * @author Dietmar
 */
public class ConstantOperand implements Operand {

	protected Constant constant = null;

	/** Creates a new instance of ConstantOperand */
	public ConstantOperand(Constant constant) {
		this.constant = constant;
	}

	public String getValue() {
		return constant.getStringValue();
	}

	public String[] getValues() {
		return new String[] { constant.getConstantIdentifier() };
	}

	public void removeListener() {}

	public void setInvalidateListener(InvalidationListener listener) {}

	public String toString() {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#deepCopy(com.gentics.api.lib.rule.RuleTree)
	 */
	public Operand deepCopy(RuleTree ruleTree) {
		// constant can't be changed anyway ..
		// so no need to create copy
		return this;
	}
}
