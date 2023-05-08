package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.RuleTree;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 11.08.2004
 * @deprecated use
 *             {@link com.gentics.api.lib.rule.RuleTree#concat(RuleTree, RuleTree, LogicalOperator)}
 *             instead
 */
public class RuleTreeHelper {
	public static RuleTree concat(RuleTree left, RuleTree right, LogicalOperator operator) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		// if (left.size() == 0)
		// return right;
		// if (right.size() == 0)
		// return left;
		RuleTree ret = left.deepCopy();

		ret.concat(right, operator);
		return ret;
	}
}
