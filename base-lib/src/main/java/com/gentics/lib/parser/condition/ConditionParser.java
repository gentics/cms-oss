package com.gentics.lib.parser.condition;

import java.util.Vector;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.parser.condition.function.XnlFunction;
import com.gentics.lib.parser.condition.function.XnlFunctionFactory;
import com.gentics.lib.parser.condition.operator.BinaryOperator;
import com.gentics.lib.parser.condition.operator.LeftUnaryOperator;
import com.gentics.lib.parser.condition.operator.XnlOperator;
import com.gentics.lib.parser.condition.operator.XnlOperatorFactory;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 28.05.2003 <p/>
 * Time: 14:56:07 <p/>To change this template use Options | File Templates.
 * @deprecated new parser on the way 
 */
public class ConditionParser {

	/**
	 * xnl_dump_syntax_tree() <p/><p/><p/>this function dumps a whole
	 * expression tree @ param tree @ return string a printable dump-string
	 */
	
	/*
	 * function xnl_dump_syntax_tree( tree, prefix = "" ) { if ( is_array( tree ) ) {
	 * ret = ""; foreach ( tree as subtree ) ret .= "prefix&gt; ( " .
	 * xnl_dump_syntax_tree( subtree, "--prefix" ) . " ) "; } else if (
	 * is_subclass_of( tree, "XNL_Operator" ) ) { oStrs =
	 * tree.getOperatorStrings(); ret = oStrs[0]; } else if (
	 * xnl_is_instance_of( tree, "XNL_FunctionResult" ) ) { func =
	 * tree.getFunction(); fStr = func.getFunctionStrings(); ret = fStr[0] .
	 * xnl_dump_syntax_tree( tree.getParameters(), "--prefix" ); } else ret =
	 * "prefix&gt; ".tree; return ret; } /** xnl_create_expression_dump() @param
	 * array expression the expression array to dump @param integer pos the
	 * position at which the error occured @param integer leftDump how many
	 * items left of the error pos should me maximum dumped @param integer
	 * rightDump analogue to leftDump @return string a printable dump
	 */
	
	/*
	 * function xnl_create_expression_dump( expression, pos, leftDump=1,
	 * rightDump=2 ) { c = count(expression); for ( i = pos - leftDump; i < pos +
	 * rightDump && i < c; i++ ) { if ( i >= 0 ) { ret .= xnl_dump_syntax_tree(
	 * expression[i] ) . " "; } } return ret; }
	 */
	private static void raise_parse_error(int pos, String where, String output) {
		NodeLogger logger = NodeLogger.getNodeLogger(ConditionParser.class);

		logger.warn("Parse error in " + where + " at pos " + pos);
		logger.warn("Details: " + output);

		/*
		 * global XNL_SHOW_ERRORS, DEBUG; if ( XNL_SHOW_ERRORS || DEBUG["xnl"] ) {
		 * echo " <br> Parse error in <b>where </b> at pos <b>pos </b>! <br> ";
		 * echo "Details: output <br> "; }
		 */
	}

	private static void break_on_parse_error(String where, String comment) {/*
		 * global
		 * DEBUG;
		 * if (
		 * DEBUG["xnl"] )
		 * echo
		 * "Function
		 * called
		 * by:
		 * <b>where
		 * </b>.
		 * <br> ";
		 * if (
		 * DEBUG["xnl"] )
		 * echo
		 * "Details:
		 * comment
		 * <br> ";
		 */}

	public static class XNL_FunctionResult {
		Object d_result;

		XnlFunction d_func;

		Vector d_params;

		public XNL_FunctionResult(Object result, XnlFunction func, Vector params) {
			d_result = result;
			d_func = func;
			d_params = params;
		}

		public Object getResult() {
			return this.d_result;
		}

		public XnlFunction getFunction() {
			return this.d_func;
		}

		public Vector getParameters() {
			return this.d_params;
		}
	}

	/**
	 * xnl_grep_string() <p/><p/><p/>gets the whole string with respect to
	 * escaped " - characters
	 * @param expression where to search in
	 * @param start where to search from ( excluding the " at the begin of the
	 *        string!! )
	 * @return
	 */
	private static ParserResult grep_string(String expression, int start) {
		int pos = expression.indexOf('\"', start);
		String ret;

		if (pos != -1) {
			char prev_ch = expression.charAt(pos - 1);

			if (prev_ch != '\\') {
				return new ParserResult(expression.substring(start, pos), pos);
			} else {
				pos++;
				ParserResult result = grep_string(expression, pos);

				ret = expression.substring(start, pos - 2) + "\"" + result.str;
				return new ParserResult(ret, result.pos);
			}
		} else {
			raise_parse_error(start, "xnl_grep_string", "string has no ending");
			return null;
		}
	}

	public static class ParserResult {
		Vector params;

		String str;

		Object object;

		int pos;

		public ParserResult(Object o, int pos) {
			this.object = o;
			this.pos = pos;
			if (o instanceof String) {
				this.str = (String) o;
			}
			if (o instanceof Vector) {
				this.params = (Vector) o;
			}
		}
	}

	/**
	 * xnl_parseParameters()
	 * @param e_str
	 * @param pos reference;
	 * @param maxPos length of e_str
	 * @return array of parameters that are seperated by a comma or false if
	 *         parse error
	 */
	private static ParserResult parseParameters(String e_str, int pos, int maxPos) {
		Vector params = new Vector();
		char ch = e_str.charAt(pos);

		while (ch == ' ') {
			pos++;
			ch = e_str.charAt(pos);
		}
		if (ch == '(') {
			pos++;
			int pEndPos = pos;
			int openBraces = 0;
			boolean escaped = false;
			boolean inString = false;

			while (true) {
				ch = e_str.charAt(pEndPos);
				if (!escaped) {
					switch (ch) {
					case '(':
						if (!inString) {
							openBraces++;
						}
						break;

					case '\\':
						escaped = true;
						break;

					case '\'':
						inString = !inString;
						break;

					case ',':
						if (!inString) {
							String param = e_str.substring(pos, pEndPos);

							if (param.trim().length() > 0) {
								params.add(param);
							}
							pos = pEndPos + 1;
						}
						break;

					case ')':
						if (!inString) {
							if (openBraces == 0) {
								String param = e_str.substring(pos, pEndPos);

								if (param.trim().length() > 0) {
									params.add(param);
								}
								pos = pEndPos + 1;
								return new ParserResult(params, pos);
							} else {
								openBraces--;
							}
						}
						break;
					} // switch
				} else {
					escaped = false;
				}
				pEndPos++;
				if (pEndPos > maxPos) {
					raise_parse_error(pEndPos, "xnl_parseParameters", "unexpexted end of expression");
					return null;
				}
			}
		} else {
			raise_parse_error(pos, "xnl_parseParameters", "( expected");
			return null;
		}
	}

	private static ParserResult xnl_parse(String e_str, int pos) {
		return xnl_parse(e_str, pos, 0);
	}

	/**
	 * xnl_parse() <p/><p/><p/>parses the given string, starting at the
	 * position pos an returns an array containing all <p/>parsed elements.
	 * <p/>those element may be strings, or objects that extend class Operator
	 * and are known <p/>by the function is_operator or an array, which
	 * represent subexpressions, that are under braces <p/>this function
	 * recognizes braces,
	 * @param e_str
	 * @param pos
	 * @return
	 */
	private static ParserResult xnl_parse(String e_str, int pos, int open_braces) {
		int i = pos;
		int count = e_str.length();
		boolean escaped = false;
		boolean error = false;
		String str = "";
		Vector ret = new Vector();

		while (i < count && !error) {
			char ch = e_str.charAt(i);

			if (!escaped) {
				ParserResult ifunction;

				// ----------------- check for an operator at the current
				// position -------------------------

				ParserResult ioperator = XnlOperatorFactory.isOperator(e_str, i);

				if (ioperator != null) {
					if (str.length() > 0) {
						ret.add(str);
					}
					ret.add(ioperator.object);
					str = "";
					i = ioperator.pos;
				} // ----------------- is it a function? -------------------------
				else if ((ifunction = XnlFunctionFactory.isFunction(e_str, i)) != null) {
					XnlFunction function = (XnlFunction) ifunction.object;

					i = ifunction.pos;
					int start = i;
					ParserResult fParams = parseParameters(e_str, i, count);

					i = fParams.pos;
					if (fParams == null) {
						break_on_parse_error("xnl_parse", "at pos: start");
						return null;
					}
					int pCount = fParams.params.size();

					if (pCount < function.getMinParamCount()) {
						String[] fStrs = function.getFunctionStrings();

						raise_parse_error(i, "xnl_parse", "too few arguments to function '" + fStrs[0] + "' ");
						return null;
					} else if (pCount > function.getMaxParamCount()) {
						String[] fStrs = function.getFunctionStrings();

						raise_parse_error(i, "xnl_parse", "too many arguments to function '" + fStrs[0] + "' ");
						return null;
					}

					// ----------------- parameters seem to be ok, lets evaluate
					// and them -------------------------

					Vector eval_fParams = new Vector();

					// foreach ( fParams as fParam )

					for (int j = 0; j < fParams.params.size(); j++) {
						String fParam = (String) fParams.params.elementAt(j);
						Object eval_Param = evaluate(fParam, false);

						if (eval_Param != null) {
							eval_fParams.add(eval_Param);
						} else {
							break_on_parse_error("xnl_parse", "while evaluating parameter: fParam");
							return null;
						}
					}

					// ----------------- yes, all parameters evaluated. lets
					// call the function -------------------------

					Object fresult = function.execute(eval_fParams);

					if (fresult != null) {
						XNL_FunctionResult xx = new XNL_FunctionResult(fresult, function, eval_fParams);

						ret.add(xx);
					} else {
						String[] fStrs = function.getFunctionStrings();

						break_on_parse_error("xnl_parse", "while executing function '" + fStrs[0] + "'");
					}
				} // ----------------- no - its a value -------------------------
				else {
					int start;

					switch (ch) {
					case '\\':
						escaped = true;
						break;

					case '\"':
						i++;
						start = i;
						ParserResult substr = grep_string(e_str, i);

						if (substr == null) {
							break_on_parse_error("xnl_parse", "at pos: " + start);
							return null;
						}
						i = substr.pos;
						ret.add(substr.str);
						str = "";
						break;

					case ' ':
						if (str.length() > 0) {
							ret.add(str);
							str = "";
						}
						break;

					case '(':
						i++;
						start = i;
						ParserResult result = xnl_parse(e_str, i, open_braces + 1);

						if (result == null) {
							break_on_parse_error("xnl_parse", "at pos: " + start);
							return null;
						}
						i = result.pos;
						ret.add(result.object);
						break;

					case ')':
						if (open_braces == 0) {
							raise_parse_error(i, "xnl_parse", "closing brace without opening brace!");
							return null;
						}
						if (str.length() > 0) {
							ret.add(str);
						}
						pos = i;
						return new ParserResult(ret, pos);

					default:
						str += ch;
					} // switch
					i++;
				}
			} else {
				switch (ch) {
				case '\"':
					str += ch;
					break;

				default:
					raise_parse_error(i, "xnl_parse", "unknown escaped character: ch");
					return null;
				} // switch
				escaped = false;
				i++;
			}
		}
		if (str.length() > 0) {
			ret.add(str);
		}
		pos = i;
		return new ParserResult(ret, pos);
	}

	/**
	 * xnl_get_operator_id() <p/><p/><p/>retrieves the id of the given
	 * operator
	 * @param operator an object, that might (shall) be an operator
	 * @return 1 if the given operator is of type LeftUnaryOperator <p/>2 if
	 *         the given operator is of type BinaryOperator <p/>0 if the given
	 *         operator is no operator at all
	 */
	private static int get_operator_id(Object operator) {
		if (operator instanceof LeftUnaryOperator) {
			return 1;
		} else if (operator instanceof BinaryOperator) {
			return 2;
		} else {
			return 0;
		}
	}

	/**
	 * xnl_fetch_right_operand() <p/><p/><p/>fetches a ready-to-use right
	 * operand at the given position, and returns the position to <p/>which
	 * parsing was neccesary. <p/>This means that if the right operand is an
	 * expression itself, it will be evaluated first, <p/>and the result will
	 * be returned.
	 * @param parts
	 * @param parts_count
	 * @param pos reference;
	 * @return
	 */
	private static ParserResult fetch_right_operand(Vector parts, int parts_count, int pos) {
		if (pos < parts_count) {
			ParserResult result = eval_expression(parts, pos);

			if (result == null) {
				return null;
			}
			Object roperand = result.object;

			if (roperand instanceof XnlOperator) {
				raise_parse_error(pos, "xnl_fetch_right_operand", "right operand is an Operator!");
				return null;
			} else if (roperand instanceof XNL_FunctionResult) {
				return new ParserResult(((XNL_FunctionResult) roperand).getResult(), result.pos);
			}
			return result;
		} else {
			raise_parse_error(pos, "xnl_fetch_right_operand", "no right operand present at the end of the line!");
			return null;
		}
	}

	private static ParserResult parse_left_unary_operator(Vector parts, int parts_count, int pos) {
		LeftUnaryOperator operator = (LeftUnaryOperator) parts.get(pos);

		pos++;
		int start = pos;
		ParserResult result = fetch_right_operand(parts, parts_count, pos);

		if (result == null) {
			break_on_parse_error("xnl_parse_left_unary_operator", "at pos: " + start);
			return null;
		}
		return new ParserResult(operator.performOperation(result.str), result.pos);
	}

	private static ParserResult parse_binary_operator(Vector parts, int parts_count,
			Object my_loperand, int pos) {
		int i = pos;
		int operator_id = get_operator_id(my_loperand);

		if (operator_id == 0) {
			// ----------------- we have a left operand, and await a
			// binary-operator -------------------------

			if (i < parts_count) {
				Object boperator = parts.get(i);

				// ----------------- if the next item is no binary operator,
				// raise an error -------------------------

				if (boperator instanceof BinaryOperator) {
					BinaryOperator operator = (BinaryOperator) boperator;

					// ----------------- ok, we have a left operand and an
					// operator, all we need now -------------------------

					// ----------------- is a right operand
					// -------------------------

					i++;
					int start = i;
					ParserResult result = fetch_right_operand(parts, parts_count, i);

					if (result == null) // catch parse error
					{
						break_on_parse_error("xnl_parse_binary_operator", "at pos: " + start);
						return null;
					}
					i = result.pos;
					Object roperand = result.object;

					// ----------------- code for respecting priority rules
					// -------------------------

					// if the next (binary) operator has a higher priority than
					// the current operator

					// evaluate it and use the result as right operand
					// (roperand)

					if (i < parts_count) {
						Object o_next_operator = parts.get(i);
						boolean higher_priority_found;

						do {
							higher_priority_found = false;
							if (o_next_operator instanceof BinaryOperator) {
								BinaryOperator next_operator = (BinaryOperator) o_next_operator;

								if (next_operator.getPriority() > operator.getPriority()) {
									higher_priority_found = true;
									start = i;
									result = parse_binary_operator(parts, parts_count, roperand, i);
									if (result == null) // on parse error
									{
										break_on_parse_error("xnl_parse_binary_operator", Integer.toString(start));
										return null;
									}
									i = result.pos;
									roperand = result.object;
									if (i < parts.size()) {
										o_next_operator = parts.get(i);
									} else {
										break;
									}
								}
							}
						} while (higher_priority_found);
					}

					// finally perform the operation, and return its result

					Object oresult = operator.performOperation(my_loperand, roperand);

					pos = i;
					return new ParserResult(oresult, i);
				} else {
					raise_parse_error(i, "xnl_parse_binary_operator", "no binary operator present: ");
					return null;
				}
			} else {
				raise_parse_error(i, "xnl_parse_binary_operator", "no binary operator present!");
				return null;
			}
		} else {
			raise_parse_error(i, "xnl_parse_binary_operator", "not a left operand!");
			return null;
		}
	}

	/**
	 * xnl_eval_expression() <p/><p/><p/>parses the expression at the given
	 * position in the expression array <p/>if this is an array ( a parser
	 * outputed subexpression, e.g. surrounded by braces )
	 * @param expression
	 * @param pos by reference; <p/>IN: the position where the expression which
	 *        should be evaluated starts <p/>OUT: the first position in
	 *        expression that was not parsed by this run
	 * @return an evaluated expression. this may be a number, string or whatever
	 *         a operator can return <p/>i.e. everything but a boolean
	 */
	private static ParserResult eval_expression(Vector expression, int pos) {
		if (expression == null) {
			return new ParserResult("", 0);
		}
		if (expression.size() <= pos) {
			return new ParserResult("", 0);
		}
		if (expression.get(pos) instanceof Vector) {
			Vector v = (Vector) expression.get(pos);
			int count = v.size();
			int subexpr_pos = 0;
			ParserResult result = eval_expression(v, subexpr_pos);

			if (result == null) {
				break_on_parse_error("eval_expression", "at pos: " + subexpr_pos);
				return null;
			}
			subexpr_pos = result.pos;
			while (subexpr_pos < count) {
				result = parse_binary_operator(v, count, result.object, subexpr_pos);
				if (result == null) {
					// ex = xnl_create_expression_dump( expression[pos],
					// subexpr_pos, 3, 1 );

					break_on_parse_error("eval_expression", "at pos: " + subexpr_pos);
					pos++;
					return null;
				}
				subexpr_pos = result.pos;
			}
			pos++;
			if (result.object instanceof XNL_FunctionResult) {
				return new ParserResult(((XNL_FunctionResult) result.object).getResult(), pos);
			}
			return new ParserResult(result.object, pos);
		} else if (expression.get(pos) instanceof LeftUnaryOperator) {
			ParserResult result = parse_left_unary_operator(expression, expression.size(), pos);

			if (result != null) {
				return new ParserResult(result.object, result.pos);
			} else {
				return null;
			}
		} else {
			Object v = expression.get(pos);

			pos++;
			if (v instanceof XNL_FunctionResult) {
				return new ParserResult(((XNL_FunctionResult) v).getResult(), pos);
			}
			return new ParserResult(v, pos);
		}
	}

	/**
	 * eval_whole_expression() <p/><p/><p/>evaluates a whole expression. just
	 * wraps the call to eval_expression so that <p/>eval_expression knows it
	 * has to evaluate the whole expression
	 * @param expression a parsed expression
	 * @return
	 */
	private static Object xnl_eval_whole_expression(Vector expression) {
		Vector expression_array = new Vector();

		expression_array.add(expression);
		ParserResult result = eval_expression(expression_array, 0);

		if (result == null) {
			return "";
		}
		return result.object;
	}

	/*
	 * TREE_COLORS = array( "000000", "DD0000", "0000DD", "00DD00" ); function
	 * xnl_print_syntaxtree( parts, prefix="", colIdx = 0 ) { global
	 * TREE_COLORS; if ( colIdx >= count(TREE_COLORS) ) colIdx = 0; color =
	 * TREE_COLORS[colIdx]; if ( is_array( parts ) ) { foreach ( parts as key =>
	 * dmp ) ret .= " <font color=color>prefix&gt; key: </font> <br> " .
	 * xnl_print_syntaxtree( dmp, "prefix--", colIdx+1 ); return ret; } else if (
	 * is_object( parts ) ) { ret .= " <font color=color>"; c =
	 * get_class(parts); ret .= "prefix&gt" . c . ": <br> "; vs =
	 * get_object_vars(parts); ret .= " </font>"; foreach ( vs as k=>v ) { ret .= "
	 * <font color=color>prefix--&gt; k: </font> <br> ".xnl_print_syntaxtree(v,
	 * "prefix--", colIdx+1)." <br> "; } return ret; } return " <font
	 * color=color>prefix--&gt; k:'".parts."' <br> </font>"; }
	 */

	/**
	 * evaluate() <p/><p/><p/>this is the function to call, if you want to
	 * evaluate a string
	 * @param str
	 * @return
	 */
	public static Object evaluate(String str, boolean errorOutput) {
		int i = 0;
		ParserResult parts = xnl_parse(str, i);

		if (parts != null) {
			return xnl_eval_whole_expression(parts.params);
		} else {
			return null;
		}
	}

	private static Integer isInteger(String s) {
		try {
			return new Integer(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Boolean isBoolean(String s) {
		if (s.equalsIgnoreCase("true")) {
			return new Boolean(true);
		}
		if (s.equalsIgnoreCase("false")) {
			return new Boolean(false);
		}
		return null;
	}

	public static Boolean isTrue(Object result) {
		if (result instanceof Boolean) {
			return (Boolean) result;
		}
		if (result instanceof Integer) {
			return new Boolean(((Integer) result).intValue() != 0);
		}
		if (result instanceof String) {
			String str = (String) result;

			if (str.length() == 0) {
				return new Boolean(false);
			}
			Integer i = isInteger(str);

			if (i != null) {
				return new Boolean(i.intValue() != 0);
			}
			Boolean bool = isBoolean(str);

			if (bool != null) {
				return bool;
			}
			return new Boolean(str.length() != 0);
		}
		return null;
	}
}
