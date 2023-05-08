package com.gentics.lib.parser.rule;

import com.gentics.api.lib.rule.Operator;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 03.08.2004 Time: 12:46:32
 */
public class CompareOperator implements Operator {
	public static final int TYPE_EQ = 1;

	public static final int TYPE_NEQ = 2;

	public static final int TYPE_GT = 3;

	public static final int TYPE_LT = 4;

	public static final int TYPE_LTEQ = 5;

	public static final int TYPE_GTEQ = 6;

	public static final int TYPE_CONTAINS = 7;

	public static final int TYPE_NOTCONTAINS = 8;

	/*
	 * !MOD 20041119 DG
	 * @deprecated -> should ISNULL AND NOTNULL NOT be deprecated ??
	 */
	public static final int TYPE_ISNULL = 9;

	public static final int TYPE_ISNOTNULL = 10;

	/* !MOD 20041119 DG END */

	public static final int TYPE_LIKE = 11;

	public static final int TYPE_NOTLIKE = 12;

	public static final CompareOperator OPERATOR_EQ = new CompareOperator(TYPE_EQ);

	public static final CompareOperator OPERATOR_NEQ = new CompareOperator(TYPE_NEQ);

	public static final CompareOperator OPERATOR_GT = new CompareOperator(TYPE_GT);

	public static final CompareOperator OPERATOR_LT = new CompareOperator(TYPE_LT);

	public static final CompareOperator OPERATOR_LTEQ = new CompareOperator(TYPE_LTEQ);

	public static final CompareOperator OPERATOR_GTEQ = new CompareOperator(TYPE_GTEQ);

	public static final CompareOperator OPERATOR_CONTAINS = new CompareOperator(TYPE_CONTAINS);

	public static final CompareOperator OPERATOR_NOTCONTAINS = new CompareOperator(TYPE_NOTCONTAINS);

	public static final CompareOperator OPERATOR_ISNULL = new CompareOperator(TYPE_ISNULL);

	public static final CompareOperator OPERATOR_ISNOTNULL = new CompareOperator(TYPE_ISNOTNULL);

	public static final CompareOperator OPERATOR_LIKE = new CompareOperator(TYPE_LIKE);

	public static final CompareOperator OPERATOR_NOTLIKE = new CompareOperator(TYPE_NOTLIKE);

	public static final CompareOperator OPERATOR_OPERATOR = new CompareOperator(TYPE_ISNULL);

	private int type;

	public CompareOperator(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	public String toString() {
		String ret = "UNDEF";

		switch (type) {
		case TYPE_EQ: {
			ret = "==";
			break;
		}

		case TYPE_NEQ: {
			ret = "!=";
			break;
		}

		case TYPE_GT: {
			ret = ">";
			break;
		}

		case TYPE_LT: {
			ret = "<";
			break;
		}

		case TYPE_LTEQ: {
			ret = "<=";
			break;
		}

		case TYPE_GTEQ: {
			ret = ">=";
			break;
		}

		case TYPE_CONTAINS: {
			ret = "CONTAINS";
			break;
		}

		case TYPE_NOTCONTAINS: {
			ret = "NOT CONTAINS";
			break;
		}

		case TYPE_ISNULL: {
			ret = "IS NULL";
			break;
		}

		case TYPE_ISNOTNULL: {
			ret = "IS NOT NULL";
			break;
		}

		case TYPE_LIKE: {
			ret = "LIKE";
			break;
		}

		case TYPE_NOTLIKE: {
			ret = "NOT LIKE";
			break;
		}
		}
		return ret;
	}
}
