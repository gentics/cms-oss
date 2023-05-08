/*
 * @author raoul
 * @date 28.07.2004
 * @version $Id: LogicalOperator.java,v 1.1 2006-01-13 15:25:41 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.rule;

/**
 * Class for logical rule operators
 */
public class LogicalOperator implements Operator {

	/**
	 * type of an undefined logical rule operator
	 */
	public static final int TYPE_UNDEF = 0;

	/**
	 * type of the operator AND
	 */
	public static final int TYPE_AND = 1;

	/**
	 * type of the operator OR
	 */
	public static final int TYPE_OR = 2;

	/**
	 * constant logical AND operator
	 */
	public static final LogicalOperator OPERATOR_AND = new LogicalOperator(TYPE_AND);

	/**
	 * constant logical OR operator
	 */
	public static final LogicalOperator OPERATOR_OR = new LogicalOperator(TYPE_OR);

	/**
	 * constant logical undefined operator
	 */
	public static final LogicalOperator OPERATOR_UNDEF = new LogicalOperator(TYPE_UNDEF);

	private int type;

	/**
	 * create an instance of a logical operator with given type. The type should
	 * be one of {@link #TYPE_AND},{@link #TYPE_OR}or {@link #TYPE_UNDEF}.
	 * @param type type of the operator
	 */
	public LogicalOperator(int type) {
		super();
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operator#getType()
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * get a string representation of the logical operator
	 * @return string representation
	 */
	public String toString() {
		switch (type) {
		case TYPE_AND:
			return " AND ";

		case TYPE_OR:
			return " OR ";
		}
		return "UNDEF";
	}
}
