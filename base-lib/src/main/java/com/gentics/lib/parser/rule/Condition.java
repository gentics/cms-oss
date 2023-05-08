/*
 * Created on 28.07.2004
 *
 * TODO implement
 */
package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.RuleTree;

/**
 * @author raoul TODO implement
 */
public class Condition {
	private Operand leftOperand;

	private Operand rightOperand;

	private CompareOperator operator;

	/**
	 * @param leftOperand
	 * @param rightOperand
	 * @param operator
	 */
	public Condition(Operand leftOperand, Operand rightOperand, CompareOperator operator) {
		super();
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
		this.operator = operator;
	}

	public Operand getLeftOperand() {
		return leftOperand;
	}

	public Operand getRightOperand() {
		return rightOperand;
	}

	public CompareOperator getOperator() {
		return operator;
	}

	public String toString() {
		return leftOperand + " " + operator + " " + rightOperand;
	}
    
	/**
	 * Creates a deep copy of this Condition.
	 * @return
	 */
	public Condition deepCopy(RuleTree ruleTree) {
		return new Condition(leftOperand.deepCopy(ruleTree), rightOperand.deepCopy(ruleTree), operator);
	}
}
