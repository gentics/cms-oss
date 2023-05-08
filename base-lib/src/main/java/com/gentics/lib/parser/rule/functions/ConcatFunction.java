package com.gentics.lib.parser.rule.functions;

import java.util.Iterator;
import java.util.Vector;

import com.gentics.lib.parser.rule.Operand;

/**
 * created at Nov 21, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class ConcatFunction implements Function {
	public int getMaxParameterCount() {
		return -1;
	}

	public String getName() {
		return "concat";
	}

	public String[] getAllNames() {
		return new String[] { getName() };
	}

	public int getMinParameterCount() {
		return 2;
	}

	public Object execute(Vector params) {
		StringBuffer ret = new StringBuffer(512);

		Iterator it = params.iterator();

		while (it.hasNext()) {
			Operand operand = (Operand) it.next();

			// TODO support multi-values
			ret.append(operand.getValue());
		}

		return ret.toString();
	}
}
