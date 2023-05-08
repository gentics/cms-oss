package com.gentics.lib.parser.condition.operator;

import java.util.Vector;

import com.gentics.lib.parser.condition.ConditionParser;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:45:50 PM To
 * change this template use Options | File Templates.
 */
public final class XnlOperatorFactory {
	private static XnlOperator[] d_ops = null;

	static {
		Vector d_ops_vec = new Vector();

		d_ops_vec.add(new BinaryLtOperator());
		d_ops_vec.add(new BinaryLtEqOperator());
		d_ops_vec.add(new BinaryGtOperator());
		d_ops_vec.add(new BinaryGtEqOperator());
		d_ops_vec.add(new BinaryAndOperator());
		d_ops_vec.add(new BinaryOrOperator());
		d_ops_vec.add(new BinaryEqOperator());
		d_ops_vec.add(new BinaryNeqOperator());
		d_ops_vec.add(new BinaryModOperator());
		d_ops_vec.add(new LeftUnaryNotOperator());
		d_ops = (XnlOperator[]) d_ops_vec.toArray(new XnlOperator[d_ops_vec.size()]);
	}

	public static ConditionParser.ParserResult isOperator(String str, int pos) {
		String substr = str.substring(pos);
		XnlOperator found = null;
		int foundLength = 0;

		for (int i = 0; i < d_ops.length; i++) {
			XnlOperator xnloperator = d_ops[i];
			String[] names = xnloperator.getOperatorStrings();

			for (int j = 0; j < names.length; j++) {
				String name = names[j];

				if (name.length() > foundLength) {
					if (substr.startsWith(name)) {
						found = xnloperator;
						foundLength = name.length();
					}
				}
			}

		}
		if (found != null) {
			return new ConditionParser.ParserResult(found, pos + foundLength);
		}
		return null;
	}
}
