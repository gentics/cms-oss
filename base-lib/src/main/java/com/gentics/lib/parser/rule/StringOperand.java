package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 03.08.2004 Time: 13:13:00
 */
public class StringOperand implements Operand {
	private String value;

	public StringOperand(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public String[] getValues() {
		return new String[] { this.value };
	}

	public void setInvalidateListener(InvalidationListener listener) {}

	public void removeListener() {}

	public String toString() {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#deepCopy(com.gentics.api.lib.rule.RuleTree)
	 */
	public Operand deepCopy(RuleTree ruleTree) {
		// No need to copy.
		return this;
	}
}
