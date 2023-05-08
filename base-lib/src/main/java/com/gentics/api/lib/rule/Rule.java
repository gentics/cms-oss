/*
 * @date Created on 21.07.2004
 * @author robert
 * @version $Id: Rule.java,v 1.10 2009-12-16 16:12:08 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.rule;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.ChangeableMap;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.rule.CompareOperator;
import com.gentics.lib.parser.rule.Condition;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.parser.rule.ObjectOperand;
import com.gentics.lib.parser.rule.Operand;
import com.gentics.lib.parser.rule.constants.ConstantOperand;
import com.gentics.lib.parser.rule.functions.FunctionOperand;

/**
 * Instances of Class Rule can be used to match {@link Resolvable}s against a given {@link RuleTree}.
 */
public class Rule {

	/**
	 * ruletree of the rule
	 */
	private RuleTree ruleTree;

	/**
	 * expression evaluator when a ruletree was set, that is not in
	 * compatibility mode
	 */
	private ExpressionEvaluator expressionEvaluator;

	/**
	 * Expression contained in the ruletree
	 */
	private Expression expression;

	/**
	 * Create an instance that matches against the given RuleTree
	 * @param ruleTree rule tree
	 */
	public Rule(RuleTree ruleTree) {
		this.ruleTree = ruleTree;
		if (ruleTree != null && ruleTree.getExpression() != null && ruleTree instanceof DefaultRuleTree) {
			expression = ruleTree.getExpression();
			expressionEvaluator = new WrapperExpressionEvaluator(((DefaultRuleTree) ruleTree).getResolvablePropertyMap());
		}
	}

	/**
	 * Match the given Resolvable against the rule tree
	 * @param object object to insert into rule for all object.* variables
	 * @return true when the object matches or false if not
	 */
	public boolean match(Resolvable object) {
		if (expressionEvaluator != null && expression != null) {
			try {
				return expressionEvaluator.match(expression, object);
			} catch (ExpressionParserException e) {
				NodeLogger.getNodeLogger(getClass()).error("Error while matching a Rule", e);
				return false;
			}
		} else {
			return matchTree(this.ruleTree, object);
		}
	}

	/**
	 * Match the rule
	 * @return true when the rule matches, or false if not
	 */
	public boolean match() {
		return match(null);
	}

	/**
	 * Match the given ruletree against the given resolvable
	 * @param tree ruletree
	 * @param object to match
	 * @return true when the object matches, false if not
	 */
	private boolean matchTree(RuleTree tree, Resolvable object) {
		boolean ret = true;
		int operator = LogicalOperator.TYPE_AND;
		Iterator it = tree.iterator();

		while (it.hasNext()) {
			Object o = (Object) it.next();

			if (o instanceof RuleTree) {
				boolean cResult = matchTree((RuleTree) o, object);

				switch (operator) {
				case LogicalOperator.TYPE_AND:
					ret &= cResult;
					break;

				case LogicalOperator.TYPE_OR:
					ret |= cResult;
					break;
				}
			} else if (o instanceof Condition) {
				Condition c = (Condition) o;
				boolean cResult = evalCondition(c, object);

				switch (operator) {
				case LogicalOperator.TYPE_AND:
					ret &= cResult;
					break;

				case LogicalOperator.TYPE_OR:
					ret |= cResult;
					break;
				}
			} else if (o instanceof FunctionOperand) {
				boolean cResult = ObjectTransformer.getBoolean(((FunctionOperand) o).getValue(), false);

				switch (operator) {
				case LogicalOperator.TYPE_AND:
					ret &= cResult;
					break;

				case LogicalOperator.TYPE_OR:
					ret |= cResult;
					break;
				}
			} else if (o instanceof LogicalOperator) {
				operator = ((LogicalOperator) o).getType();
			}
		}
		return ret;
	}

	/**
	 * guarantees a array-size of >= 1, but may contain null values
	 * @param o
	 * @param r
	 * @return
	 */
	private String[] getValues(Operand o, PropertyResolver r) {
		if (o instanceof ObjectOperand) {
			try {
				Object prop = r.resolve(((ObjectOperand) o).getValue());

				if (prop instanceof Collection) {
					Collection c = (Collection) prop;
					Iterator it = (c).iterator();
					String[] ret = new String[c.size()];
					int i = 0;

					while (it.hasNext()) {
						ret[i++] = ObjectTransformer.getString(it.next(), null);
					}
					return ret;
				} else {
					return new String[] { ObjectTransformer.getString(prop, null) };
				}
			} catch (UnknownPropertyException e) {
				e.printStackTrace();
			}
		} else if (o instanceof ConstantOperand) {
			// System.out.println("Test ConstantOperand");
			return new String[] { o.getValue() };
		} else {
			String[] ret = o.getValues();

			if (ret != null) {
				if (ret.length > 0) {
					return ret;
				}
			}
		}
		return new String[] { null };
	}

	/**
	 * evaluate the given condition against the given resolvable
	 * @param c condition
	 * @param obj resolvable object
	 * @return true when the condition evaluates to true, false if not
	 */
	private boolean evalCondition(Condition c, Resolvable obj) {
		PropertyResolver resolver = new PropertyResolver(obj);
		Operand left = c.getLeftOperand();
		Operand right = c.getRightOperand();
		int oType = c.getOperator().getType();

		switch (oType) {
		case CompareOperator.TYPE_EQ:
			return compareEq(getValues(left, resolver), getValues(right, resolver));

		case CompareOperator.TYPE_NEQ:
			return !compareEq(getValues(left, resolver), getValues(right, resolver));

		case CompareOperator.TYPE_LT:
		case CompareOperator.TYPE_GT:
		case CompareOperator.TYPE_LTEQ:
		case CompareOperator.TYPE_GTEQ:
			return compareBiggness(getValues(left, resolver), getValues(right, resolver), oType);

		case CompareOperator.TYPE_CONTAINS:
			return contains(getValues(left, resolver), getValues(right, resolver));

		case CompareOperator.TYPE_NOTCONTAINS:
			return !contains(getValues(left, resolver), getValues(right, resolver));

		case CompareOperator.TYPE_LIKE:
			return compareLike(getValues(left, resolver), getValues(right, resolver));
		}
		return true;
	}

	/**
	 * evaluate a contains operator. contains evaluates to true when at least
	 * one of the values in the left array is contained in the right array does
	 * not take null values into consideration.
	 * @param left left array
	 * @param right right array
	 * @return true when the left array contains at least one value of the right
	 *         array
	 */
	private boolean contains(String[] left, String[] right) {
		for (int i = 0; i < left.length; i++) {
			if (left[i] != null) {
				for (int j = 0; j < right.length; j++) {
					if (left[i].equals(right[j])) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * do a number compare of the given operatortype
	 * @param left left values
	 * @param right right values
	 * @param oType compare operator
	 * @return result of the comparison
	 */
	private boolean compareBiggness(String[] left, String[] right, int oType) {
		int iLeft = ObjectTransformer.getInt(left[0], 0);
		int iRight = ObjectTransformer.getInt(right[0], 0);

		switch (oType) {
		case CompareOperator.TYPE_LT:
			return iLeft < iRight;

		case CompareOperator.TYPE_GT:
			return iLeft > iRight;

		case CompareOperator.TYPE_LTEQ:
			return iLeft <= iRight;

		case CompareOperator.TYPE_GTEQ:
			return iLeft >= iRight;
		}
		return false;
	}

	/**
	 * Do a LIKE comparison
	 * @param left left values
	 * @param right right values
	 * @return result of the comparison
	 */
	private boolean compareLike(String[] left, String[] right) {
		String leftOperand = ObjectTransformer.getString(left[0], "").toLowerCase();
		String rightOperand = ObjectTransformer.getString(right[0], "").toLowerCase();

		try {
			return leftOperand.matches(StringUtils.likeStringToRegex(rightOperand.replace('*', '%')));
		} catch (PatternSyntaxException ex) {
			NodeLogger.getLogger(getClass()).error("Error while evaluating {" + leftOperand + " LIKE " + rightOperand + "}", ex);
			return false;
		}
	}

	/**
	 * compare equality of the given values
	 * @param left left values
	 * @param right right values
	 * @return true when the values are considered to be equal
	 */
	private boolean compareEq(String[] left, String[] right) {
		if (left == null && right == null) {
			return true;
		}
		if (left.length != right.length) {
			return false;
		}
		for (int i = 0; i < left.length; i++) {
			// null and empty string are supposed to be equal
			if (left[i] == null && "".equals(right[i])) {
				continue;
			}
			if ("".equals(left[i]) && right[i] == null) {
				continue;
			}
			if (left[i] == null && right[i] != null) {
				return false;
			}
			if (left[i] == null && right[i] == null) {
				continue;
			}
			if (!left[i].equals(right[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the RuleTree this Rule uses
	 * @return returns RuleTree object of this Rule
	 */
	public RuleTree getRuleTree() {
		return this.ruleTree;
	}

	/**
	 * Check whether the rule is constant or not. A constant rule will or will
	 * not match against any resolvable object independent from the object's
	 * properties.
	 * @return true for constant rules, false otherwise
	 */
	public boolean isConstant() {
		if (expression != null) {
			// TODO can we do this also for expressions?
			return false;
		} else {
			return isTreeConstant(ruleTree);
		}
	}

	/**
	 * Internal method the check whether a given ruletree is constant or not.
	 * Null as tree is considered constant.
	 * @param tree ruletree to test
	 * @return true when the ruletree is constant, false if not
	 */
	private final static boolean isTreeConstant(RuleTree tree) {
		boolean isConstant = true;

		if (tree == null) {
			return isConstant;
		}

		for (Iterator iter = tree.iterator(); iter.hasNext() && isConstant;) {
			Object element = (Object) iter.next();

			if (element instanceof RuleTree) {
				isConstant = isTreeConstant((RuleTree) element);
			} else if (element instanceof Condition) {
				// do the real check here
				Condition c = (Condition) element;

				isConstant &= isOperandConstant(c.getLeftOperand());
				isConstant &= isOperandConstant(c.getRightOperand());
			} else if (element instanceof FunctionOperand) {
				// functions are never constant TODO maybe we have constant functions?
				isConstant = false;
			}
		}

		return isConstant;
	}

	/**
	 * Internal method to check whether a specific operand is constant or not
	 * @param operand operand to check
	 * @return true when the operand is constant, false if not
	 */
	private final static boolean isOperandConstant(Operand operand) {
		// ObjectOperands and FunctionOperands are considered to be non-constant
		if (operand instanceof ObjectOperand) {
			return false;
		} else if (operand instanceof FunctionOperand) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Helper class that shared the base objects with the given map
	 */
	private static class WrapperExpressionEvaluator extends ExpressionEvaluator {

		/**
		 * Create instance of the wrapper class
		 * @param sharedObjects map of shared objects
		 */
		public WrapperExpressionEvaluator(Map sharedObjects) {
			baseObjects = new ChangeableMap(sharedObjects);
		}
	}
}
