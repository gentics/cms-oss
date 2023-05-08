package com.gentics.lib.parser.tag.xnl;

import java.util.Map;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.condition.ConditionParser;
import com.gentics.lib.parser.tag.MyGenericParser;
import com.gentics.lib.parser.tag.MyObjectParser;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 28.05.2003 <p/>
 * Time: 12:20:28 <p/>To change this template use Options | File Templates.
 * @deprecated new xnl parser in cn.node
 */
public class XnlIfParser extends MyObjectParser {
	String d_else = "";

	public XnlIfParser() {
		this.d_else = "";
	}

	public Object parse(String input, Map props) {
		String condition = (String) ((props.get("cond") != null) ? props.get("cond") : props.get("condition"));
		String rs = "";
		MyGenericParser parser = this.getParser();

		if (parser.doEvaluate()) {
			Object result = ConditionParser.evaluate(condition, false); // this.xnlEval(unhtmlentities(condition));
			Boolean trueResult = ConditionParser.isTrue(result);

			if (trueResult == null) {
				NodeLogger.getLogger(getClass()).warn("could not evaluate condition: " + condition + "\n");
				return "";
			}
			if (trueResult.booleanValue()) {
				rs = input;
			} else if (this.d_else.length() > 0) {
				rs = this.d_else;
			}
		} else {
			rs = input + this.d_else;
		}
		return rs;
	}

	public void addCodePart(String keyname, String code, Map params) {
		if (keyname.equalsIgnoreCase("else")) {
			this.d_else = code;
		}
	}

	public void clearCodeParts() {
		this.d_else = "";
	}

	public boolean keyExists() {
		return true;
	}

	private boolean xnlEval(String condition) {

		/*
		 * ret = false; if (FEATURE["force_node2"]) { if (DEBUG["xnl"]) echo
		 * "XNL cond(con) <br> \n"; eval("\ret = (condition);"); } else { parser =&
		 * this.getParser(); ret = xnl_evaluate(condition, FALSE); } return ret;
		 */

		return true;
	}
}
