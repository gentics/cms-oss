package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 04.08.2004 Time: 11:58:37
 */
public class ObjectOperand implements Operand {
	String objectName;

	public ObjectOperand(String objectName) {
		this.objectName = objectName;
	}

	public String getValue() {
		return objectName;
	}

	public String[] getValues() {
		return new String[] { objectName };
	}

	public String toString() {
		return "[object:name=" + getValue() + "]";
	}

	public void setInvalidateListener(InvalidationListener listener) {}

	public void removeListener() {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#deepCopy(com.gentics.api.lib.rule.RuleTree)
	 */
	public Operand deepCopy(RuleTree ruleTree) {
		// No need to create copy .. no changeable values.
		return this;
	}
}
