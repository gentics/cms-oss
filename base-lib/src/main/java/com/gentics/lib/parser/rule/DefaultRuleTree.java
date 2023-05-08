package com.gentics.lib.parser.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.Operator;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;
import com.gentics.lib.parser.rule.constants.Constant;
import com.gentics.lib.parser.rule.constants.ConstantFactory;
import com.gentics.lib.parser.rule.constants.ConstantOperand;
import com.gentics.lib.parser.rule.functions.Function;
import com.gentics.lib.parser.rule.functions.FunctionFactory;
import com.gentics.lib.parser.rule.functions.FunctionOperand;

/**
 * Implementation of a RuleTree.
 * 
 * Created by IntelliJ IDEA. User: erwin Date: 03.08.2004 Time: 11:42:49
 * @see com.gentics.api.lib.rule.RuleTree
 */
public class DefaultRuleTree implements RuleTree, InvalidationListener {

	/**
	 * parsed expression, when compatibility mode is off
	 */
	protected Expression expression;

	/**
	 * flag to mark RuleTrees that run in compatibility mode
	 */
	protected boolean compatiblityMode = true;

	protected class GrepStrResult {
		public String str;

		public int pos;

		public GrepStrResult(String str, int pos) {
			this.str = str;
			this.pos = pos;
		}
	}

	protected class OperatorResult {
		public Operator operator;

		public int pos;

		public OperatorResult(Operator operator, int pos) {
			this.operator = operator;
			this.pos = pos;
		}
	}

	protected class FunctionResult {
		public Function function;

		public int pos;

		public FunctionResult(Function function, int pos) {
			this.function = function;
			this.pos = pos;
		}
	}

	protected class ConstantResult {
		// public ConstantOperator constant;
		public Constant constant = null;

		public int pos;

		public ConstantResult(Constant constant, int pos) {
			this.constant = constant;
			this.pos = pos;
		}
	}

	protected class ParameterResult {
		public Vector params;

		public int pos;

		public ParameterResult(Vector params, int pos) {
			this.params = params;
			this.pos = pos;
		}

	}

	private ArrayList rule;

	private String ruleStr;

	// private ArrayList listeners;

	/**
	 * map of resolvable properties, keys are the object prefixes, values are the
	 * PropertyResolvers
	 */
	protected Map resolvableProperty;

	/**
	 * map of property operands, keys are the propertyOperand instances, values
	 * are the resolved values. Needed for {@link #hasChanged(int)}.
	 */
	protected Map propertyOperands;

	long lastChange = 0;

	public DefaultRuleTree() {
		compatiblityMode = ExpressionParser.isCompatibilityMode();
		if (compatiblityMode) {
			// listeners = new ArrayList();
			ruleStr = "";
			rule = new ArrayList();
			resolvableProperty = new HashMap();
			propertyOperands = new HashMap();
		} else {
			// listeners = new ArrayList();
			resolvableProperty = new HashMap();
			propertyOperands = new HashMap();
		}
	}

	public void concat(RuleTree ruletree, LogicalOperator operator) {
		if (compatiblityMode) {
			if (ruletree == null || ruletree.size() == 0) {
				return;
			}
			if (this.size() == 0) {
				this.addRuleTree(ruletree);
			} else {
				this.addLogicalOperator(operator);
				this.addRuleTree(ruletree);
			}
		} else {
			if (ruletree == null || ruletree.getRuleString() == null || ruletree.getRuleString().length() == 0) {
				return;
			}

			// combine the resolvable properties of the ruletrees
			if (ruletree instanceof DefaultRuleTree) {
				DefaultRuleTree defaultRuleTree = (DefaultRuleTree) ruletree;

				if (defaultRuleTree.resolvableProperty != null) {
					for (Iterator iter = defaultRuleTree.resolvableProperty.entrySet().iterator(); iter.hasNext();) {
						Map.Entry entry = (Map.Entry) iter.next();

						// check whether the entry already exists in this ruletree
						if (resolvableProperty.containsKey(entry.getKey())) {
							// check whether the values are identical and
							// give a warning if not
							Object value = resolvableProperty.get(entry.getKey());

							if ((value == null && entry.getValue() != null) || !value.equals(entry.getValue())) {
								if (entry.getValue() != null) {
									NodeLogger.getLogger(getClass()).warn(
											"Error while concatenating ruletrees with expression parser on: ruletrees resolve {" + entry.getKey()
											+ "} to different objects");
								}
							}
						} else {
							resolvableProperty.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}

			StringBuffer newRuleString = new StringBuffer();

			newRuleString.append("(").append(ruleStr).append(")");
			switch (operator.getType()) {
			case LogicalOperator.TYPE_AND:
				newRuleString.append(" AND ");
				break;

			case LogicalOperator.TYPE_OR:
				newRuleString.append(" AND ");
				break;

			default:
				return;
			}
			newRuleString.append("(").append(ruletree.getRuleString()).append(")");

			try {
				parse(newRuleString.toString());
			} catch (ParserException e) {
				NodeLogger.getNodeLogger(getClass()).error("Error while concatenating ruletrees. Ruletree remains unchanged.", e);
			}
		}
	}

	// public void addInvalidationListener(InvalidationListener l) {
	// listeners.add(l);
	// }

	public Iterator iterator() {
		if (compatiblityMode) {
			return rule.iterator();
		} else {
			return null;
		}
	}

	public int size() {
		if (compatiblityMode) {
			return rule.size();
		} else {
			return 0;
		}
	}

	/**
	 * xnl_parseParameters()
	 * @param str
	 * @param pos reference;
	 * @param maxPos length of str
	 * @return array of parameters that are seperated by a comma or false if
	 *         parse error
	 */
	protected ParameterResult parseParameters(String str, int pos, int maxPos) throws ParserException {
		Vector params = new Vector(5);

		int length = str.length();

		if (pos >= length) {
			return null;
		}
		char ch = str.charAt(pos); // substr( $e_str, $pos, 1 );

		while (ch == ' ') {
			pos++;
			if (pos >= length) {
				return null;
			}
			ch = str.charAt(pos);
		}

		if (ch == '(') {
			pos++;
			int pEndPos = pos;
			int openBraces = 0;
			boolean escaped = false;
			boolean inString = false;
			char stringOpener = '\0';

			while (true) {
				ch = str.charAt(pEndPos); // substr( $e_str, $pEndPos, 1 );

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

					case '"':
					case '\'':
						if (!inString) {
							inString = true;
							stringOpener = ch;
							pos = pEndPos + 1;
						} else if (stringOpener == ch) {
							inString = false;
							boolean found = false;
							// search for comma or closing brace
							String param = str.substring(pos, pEndPos).trim(); // substr(

							// $e_str,
							// $pos,
							// $pEndPos
							// -
							// $pos
							// );
							pos = pEndPos;
							pEndPos++;
							while (pEndPos < str.length()) {
								char c = str.charAt(pEndPos);

								if (c == ',' || c == ')') {
									found = true;
									if (param.length() > 0) {
										params.add(param);
									}
									pos = pEndPos + 1;
									if (c == ')') {
										return new ParameterResult(params, pos);
									}
									break;
								}
							}
							if (!found) {
								throw new ParserException("unexepected end of parameters or unexpected character!", str, pos);
							}
						}
						break;

					case ',':
						if (!inString) {
							String param = str.substring(pos, pEndPos).trim(); // substr(

							// $e_str,
							// $pos,
							// $pEndPos
							// -
							// $pos
							// );
							if (param.length() > 0) {
								params.add(param);
							}
							pos = pEndPos + 1;
						}
						break;

					case ')':
						if (!inString) {
							if (openBraces == 0) {
								String param = str.substring(pos, pEndPos).trim(); // substr(

								// $e_str,
								// $pos,
								// $pEndPos
								// -
								// $pos
								// );
								if (param.length() > 0) {
									params.add(param);
								}
								pos = pEndPos + 1;
								return new ParameterResult(params, pos);
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
					// xnl_raise_parse_error( $pEndPos, "xnl_parseParameters",
					// "unexpected end of expression");
					return null;
				}
			}
		} else {
			// xnl_raise_parse_error( $pos, "xnl_parseParameters", "( expected"
			// );
			return null;
		}

	}

	private GrepStrResult grepString(String expression, int start) {
		int pos = expression.indexOf('\"', start);
		String ret;

		if (pos != -1) {
			char prev_ch = expression.charAt(pos - 1);

			if (prev_ch != '\\') {
				return new GrepStrResult(expression.substring(start, pos), pos);
			} else {
				pos++;
				GrepStrResult result = grepString(expression, pos);

				ret = expression.substring(start, pos - 2) + "\"" + result.str;
				return new GrepStrResult(ret, result.pos);
			}
		} else {
			// raise_parse_error( start, "xnl_grep_string", "string has no
			// ending" );
			return null;
		}
	}

	/* !MOD 20041201 DG added factory */

	protected Collection getFunctionList() {
		return FunctionFactory.getFunctions();
		// return functionList;
	}

	protected Collection getConstantList() {
		return ConstantFactory.getConstants();
	}

	private ConstantResult getConstant(String str, int pos) {
		ConstantResult ret = null;
		Iterator it = getConstantList().iterator();

		while (it.hasNext()) {
			Constant constant = (Constant) it.next();
			String ident = constant.getConstantIdentifier();

			if (pos + ident.length() <= str.length()) {
				if (str.startsWith(ident, pos)) {
					boolean valid_const_ident = false;

					if (pos + ident.length() < str.length()) {
						// this is not the last character in the string - verify
						// ending
						char ch = str.charAt(pos + ident.length());

						if (ch == ' ') {
							valid_const_ident = true;
						}
					} else {
						// this is the last item in the string - ends correctly
						valid_const_ident = true;
					}

					ret = new ConstantResult(constant, pos + ident.length());
				}
			}
		}
		return ret;
	}

	private FunctionResult getFunction(String str, int pos) {

		Iterator it = getFunctionList().iterator();

		while (it.hasNext()) {
			Function function = (Function) it.next();
			String name = function.getName();

			if (pos + name.length() < str.length()) {
				if (str.startsWith(name, pos)) {
					char ch = str.charAt(pos + name.length());

					if (ch == ' ' || ch == '(') {
						return new FunctionResult(function, pos + name.length());
					}
				}
			}
		}
		return null;
	}

	/**
	 * find operators starting at the given position in the string
	 * @param str string to search
	 * @param pos position where possibly an operator starts
	 * @param allowWordOperators true when words as operators are allowed at the
	 *        position, false if not
	 * @return the operator (if one found) or null
	 */
	private OperatorResult getOperator(String str, int pos, boolean allowWordOperators) {
		// important: check for longer operators first!
		OperatorResult ret = null;

		if (allowWordOperators && str.startsWith("AND", pos)) {
			ret = new OperatorResult(LogicalOperator.OPERATOR_AND, pos + 3);
		} else if (allowWordOperators && str.startsWith("OR", pos)) {
			ret = new OperatorResult(LogicalOperator.OPERATOR_OR, pos + 2);
		} else if (str.startsWith("&&", pos)) {
			ret = new OperatorResult(LogicalOperator.OPERATOR_AND, pos + 2);
		} else if (str.startsWith("||", pos)) {
			ret = new OperatorResult(LogicalOperator.OPERATOR_OR, pos + 2);
		} else if (str.startsWith("==", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_EQ, pos + 2);
		} else if (str.startsWith("!=", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_NEQ, pos + 2);
		} else if (str.startsWith(">=", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_GTEQ, pos + 2);
		} else if (str.startsWith("<=", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_LTEQ, pos + 2);
		} else if (allowWordOperators && str.startsWith("CONTAINSONEOF", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_CONTAINS, pos + "CONTAINSONEOF".length());
		} else if (allowWordOperators && str.startsWith("CONTAINSNONE", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_NOTCONTAINS, pos + "CONTAINSNONE".length());
		} else if (str.startsWith(">", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_GT, pos + 1);
		} else if (str.startsWith("<", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_LT, pos + 1);
		} /* !MOD 20041119 DG added LIKE */ else if (allowWordOperators && str.startsWith("LIKE", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_LIKE, pos + "LIKE".length());
		} else if (allowWordOperators && str.startsWith("NOTLIKE", pos)) {
			ret = new OperatorResult(CompareOperator.OPERATOR_NOTLIKE, pos + "NOTLIKE".length());
		}

		/* !MOD 20041119 DG added ISEMPTY */
		
		/*
		 * !MOD 20041201 DG removed ISEMPTY as of new functionality by Erwin
		 * Mascher
		 */
		
		/*
		 * else if( str.startsWith( "ISEMPTY", pos ) ) ret = new OperatorResult(
		 * SingleObjectOperator.OPERATOR_ISEMPTY, pos + "ISEMPTY".length());
		 * else if( str.startsWith( "NOTISEMPTY", pos ) ) ret = new
		 * OperatorResult( SingleObjectOperator.OPERATOR_NOTISEMPTY, pos +
		 * "NOTISEMPTY".length());
		 */
		return ret;
	}

	/**
	 * creates an operand based on the object given. this operand should be able
	 * to invalidate it's values (TODO invalidate operand-values) you may
	 * subclass DefaultRuleTree and overwrite this method if you have specific
	 * operands to implement. the default implementation create ObjectOperands
	 * if the String starts with "object." or StringOperands otherwise to
	 * correctly overwrite this method you also have to overwrite
	 * createInstance()
	 * @param str
	 * @return
	 */
	protected Operand createOperand(String str) {
		if (str.startsWith("object.")) {
			return new ObjectOperand(str.substring("object.".length()));
		} else if (str.indexOf('.') > 0) {
			String prefix = str.substring(0, str.indexOf('.'));
			String attribute = str.substring(str.indexOf('.') + 1);

			if (resolvableProperty.containsKey(prefix)) {
				PropertyOperand propertyOperand = new PropertyOperand(prefix, this, attribute);

				// put the operand into the map
				propertyOperands.put(propertyOperand, null);
				return propertyOperand;
			}
		}
		return new StringOperand(str);
	}

	protected Operand createFunctionOperand(Function function, Vector params) {
		for (int i = 0; i < params.size(); i++) {
			// todo allow sub-functions
			params.set(i, createOperand((String) params.get(i)));
		}
		return new FunctionOperand(function, params);
	}

	protected Operand createConstantOperand(Constant constant) {
		return new ConstantOperand(constant);
	}

	private Operand createAndListenToOperand(String str) {
		Operand o = createOperand(str);

		o.setInvalidateListener(this);
		return o;
	}

	protected void finalize() throws Throwable {
		Iterator it = iterator();

		while (it != null && it.hasNext()) {
			Object o = (Object) it.next();

			if (o instanceof Condition) {
				((Condition) o).getLeftOperand().removeListener();
				((Condition) o).getRightOperand().removeListener();
			}
		}
	}

	/**
	 * one of the operands invalidated. let our listeners know!
	 */
	public void invalidate() {// Iterator itListeners = this.listeners.iterator();
		// while (itListeners.hasNext()) {
		// InvalidationListener listener = (InvalidationListener) itListeners.next();
		// listener.invalidate();
		// }
	}

	/**
	 * overwrite this method, if you overwrite any of the methods, because
	 * parse() will need to create new instances!
	 * @return
	 */
	protected DefaultRuleTree createInstance() {
		// create a new instance
		DefaultRuleTree newInstance = new DefaultRuleTree();

		// pass all resolvers to the new instance (by reference, so all
		// instances will share the same map)
		newInstance.resolvableProperty = this.resolvableProperty;
		return newInstance;
	}

	/**
	 * @param tree
	 * @return
	 */
	private ArrayList parseConditions(ArrayList tree) throws ParserException {
		ArrayList ret = new ArrayList(tree.size());
		Operand lOperand = null;
		Iterator it = tree.iterator();

		while (it.hasNext()) {
			Object obj = it.next();

			/* !MOD 20041119 DG added SingleObjectOperator */
			
			/* !MOD 20041201 DG removed single object operator */
			
			/*
			 * if ( obj instanceof SingleObjectOperator ){ Operand op = null;
			 * //only if lOperand == null if(lOperand == null){ Object
			 * rOperandObj= null; if ( !it.hasNext() ) { System.err.println("No
			 * right operand found. assuming \"\""); throw new ParserException(
			 * "expected operand!"); } else rOperandObj = it.next(); op =
			 * rOperandObj; } else{ op = lOperand; } //instanciate condition
			 * ret.add(new SingleObjectCondition(op, (SingleObjectOperator)obj) ); }
			 * else
			 */if (obj instanceof CompareOperator) {
				if (lOperand == null) {
					throw new ParserException("No left operand");
				} else {
					Object rOperandObj = null;

					if (!it.hasNext()) {
						NodeLogger.getLogger(getClass()).warn("No right operand found. assuming \"\"");
						rOperandObj = "";
					} else {
						rOperandObj = it.next();
					}
					if (rOperandObj instanceof String) {
						Operand rOperand = createAndListenToOperand((String) rOperandObj);

						ret.add(new Condition(lOperand, rOperand, (CompareOperator) obj));
					} else if (rOperandObj instanceof Operand) {
						ret.add(new Condition(lOperand, (Operand) rOperandObj, (CompareOperator) obj));
					} else {
						throw new ParserException("expected operand!");
					}
					lOperand = null;
				}

			} else if (obj instanceof DefaultRuleTree) {
				if (lOperand != null) {
					throw new ParserException("expected operand.");
				} else {
					ret.add(obj);
				}
			} else if (obj instanceof LogicalOperator) {
				if (!it.hasNext()) {
					throw new ParserException("No right operand");
				} else {
					if (lOperand != null) {
						ret.add(lOperand);
					}
					ret.add(obj);
					lOperand = null;
				}

			} else if (obj instanceof FunctionOperand) {
				if (lOperand == null) {} else {}
				ret.add(obj);
			} else // must be an operand
			{
				if (lOperand != null) {
					throw new ParserException("Found two operands without operators (" + lOperand + ") (" + obj + ")!");
				}
				if (!(obj instanceof String)) {
					throw new ParserException("Missing right operand! found " + obj.getClass() + "(" + obj.toString() + ")");
				}
				lOperand = createAndListenToOperand((String) obj);
			}

		}
		return ret;
	}

	private int parse(String ruleStr, int startPos) throws ParserException {
		String runtimeProfilerMark = ruleStr;
		int pos;

		try {
			RuntimeProfiler.beginMark(ComponentsConstants.DEFAULTRULETREE_PARSE, runtimeProfilerMark);
    		
			if (ruleStr == null) {
				throw new ParserException("ruleStr was null.");
			}
			ruleStr = MiscUtils.replaceAll(ruleStr.trim(), "\n", " ");
	
			pos = startPos;
			int count = ruleStr.length();
			boolean escaped = false;
			boolean error = false;
			String str = "";
			ArrayList rule = new ArrayList();
	
			while (pos < count && !error) {
				char ch = ruleStr.charAt(pos);

				if (!escaped) {
					// ParserResult ifunction;
					// ----------------- check for an operator at the current
					// position -------------------------
					// NOP: allow word operators only when we are at the start of a
					// new word
					OperatorResult ioperator = getOperator(ruleStr, pos, str.length() == 0);
					FunctionResult ifunction = null;
					ConstantResult iconstant = null;
	
					if (ioperator != null) {
						if (str.length() > 0) {
							rule.add(str);
						}
						rule.add(ioperator.operator);
						str = "";
						pos = ioperator.pos;
					} // ----------------- is it a function? -------------------------
					else if (str.length() == 0 && (ifunction = getFunction(ruleStr, pos)) != null) {
						Function function = ifunction.function;

						pos = ifunction.pos;
						ParameterResult fParams = parseParameters(ruleStr, pos, count);
	
						if (fParams == null) {
							throw new ParserException("Function (" + function.getName() + ") found, but invalid parameter-list!", str, pos);
							// break_on_parse_error( "xnl_parse", "at pos: start" );
						}
						pos = fParams.pos;
	
						int pCount = fParams.params.size();

						if (pCount < function.getMinParameterCount()) {
							// String[] fStrs = function.getFunctionStrings();
							// raise_parse_error( i, "xnl_parse", "too few arguments
							// to function '" +
							// fStrs[0] + "' " );
							return -1;
						} else if (pCount > function.getMaxParameterCount() && function.getMaxParameterCount() >= 0) {
							// String[] fStrs = function.getFunctionStrings();
							// raise_parse_error( i, "xnl_parse", "too many
							// arguments to function '" +
							// fStrs[0] + "' " );
							return -1;
						}
	
						// ----------------- parameters seem to be ok, lets evaluate
						// and them -------------------------
	
						/*
						 * TODO ---------------------- SUB-EXPRESSIONS NOT AVAILABLE
						 * YET --------------------------- Vector eval_fParams = new
						 * Vector(); //foreach ( fParams as fParam ) for ( int j =
						 * 0; j < fParams.params.size(); j++ ) { String fParam =
						 * (String) fParams.params.elementAt( j ); Object eval_Param =
						 * evaluate( fParam, false ); if ( eval_Param != null ) {
						 * eval_fParams.add( eval_Param ); } else {
						 * break_on_parse_error( "xnl_parse", "while evaluating
						 * parameter: fParam" ); return null; } }
						 */
						// ----------------- yes, all parameters evaluated. lets
						// call the function -------------------------
						// evolution to php-parser: use a function-operand, so
						// function are evaluated in-time
						rule.add(createFunctionOperand(function, fParams.params));
					} else if (str.length() == 0 && (iconstant = getConstant(ruleStr, pos)) != null) {
						rule.add(createConstantOperand(iconstant.constant));
						pos = iconstant.pos;
					} // ----------------- no - its a value -------------------------
					else {
						int start;

						switch (ch) {
						case '\\':
							escaped = true;
							break;

						case '\"':
							pos++;
							start = pos;
							GrepStrResult substr = grepString(ruleStr, pos);

							if (substr == null) {
								// break_on_parse_error( "xnl_parse", "at pos: " +
								// start );
								return -1;
							}
							pos = substr.pos;
							rule.add(substr.str);
							str = "";
							break;

						case ' ':
							if (str.length() > 0) {
								rule.add(str);
								str = "";
							}
							break;

						case '(':
							pos++;
							start = pos;
							DefaultRuleTree subTree = createInstance();
							int result = subTree.parse(ruleStr, pos);

							// ParserResult result = xnl_parse( e_str, i,
							// open_braces + 1 );
							if (result == -1) {
								// break_on_parse_error( "xnl_parse", "at pos: " +
								// start );
								return -1;
							}
							pos = result;
							rule.add(subTree);
							break;

						case ')':
							// if ( open_braces == 0 )
							// {
							// raise_parse_error( i, "xnl_parse", "closing brace
							// without opening brace!" );
							// return -1;
							// }
							if (str.length() > 0) {
								rule.add(str);
							}
							this.ruleStr = ruleStr.substring(startPos, pos);
							this.rule = parseConditions(rule);
							return pos;

						default:
							str += ch;
						} // switch
						pos++;
					}
				} else {
					switch (ch) {
					case '\"':
						str += ch;
						break;

					default:
						// raise_parse_error( i, "xnl_parse", "unknown escaped
						// character: ch" );
						return -1;
					} // switch
					escaped = false;
					pos++;
				}
			}
			if (str.length() > 0) {
				rule.add(str);
			}
			if (pos > 0) {
				this.ruleStr = ruleStr.substring(startPos, pos - 1);
			} else {
				// TODO: empty rule -- > error?
				this.ruleStr = "";
			}
			this.rule = parseConditions(rule);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.DEFAULTRULETREE_PARSE, runtimeProfilerMark);
		}
		return pos;
	}

	public void parse(String ruleString) throws ParserException {
		if (compatiblityMode) {
			propertyOperands.clear();
			int result = parse(ruleString, 0);

			this.ruleStr = ruleString;
		} else {
			expression = ExpressionParser.getInstance().parse(ruleString);
			this.ruleStr = ruleString;
		}
	}

	private void dump(RuleTree tree, String prefix) {
		NodeLogger logger = NodeLogger.getNodeLogger(getClass());
		Iterator it = tree.iterator();

		while (it.hasNext()) {
			Object o = it.next();

			if (o instanceof RuleTree) {
				dump((RuleTree) o, prefix + "--");
			} else if (o instanceof Condition) {
				Condition c = (Condition) o;

				logger.info(prefix + " C: " + c);
			} else if (o instanceof Operator) {
				logger.info(prefix + " O: " + o);
			}
		}
	}

	private void dump() {
		dump(this, "");
	}

	protected void addRuleTree(RuleTree tree) {
		this.rule.add(tree);
	}

	protected void addLogicalOperator(LogicalOperator op) {
		this.rule.add(op);
	}

	protected void addCondition(Condition cond) {
		this.rule.add(cond);
	}

	public String getRuleString() {
		return ruleStr;
	}

	/**
	 * add a property resolver to the map of resolvers
	 * @param objectPrefix object prefix
	 * @param resolver property resolver
	 */
	public void addResolver(String objectPrefix, PropertyResolver resolver) {
		resolvableProperty.put(objectPrefix, resolver);
	}

	/**
	 * add a property resolver to the map of resolvers
	 * @param objectPrefix object prefix
	 * @param resolvable resolvable object
	 */
	public void addResolver(String objectPrefix, Resolvable resolvable) {
		resolvableProperty.put(objectPrefix, new PropertyResolver(resolvable));
	}

	/**
	 * add resolvable properties as map to the map of resolvers
	 * @param objectPrefix object prefix
	 * @param resolvableMap object properties as map
	 */
	public void addResolver(String objectPrefix, Map resolvableMap) {
		resolvableProperty.put(objectPrefix, new PropertyResolver(new MapResolver(resolvableMap)));
	}

	/**
	 * remove a property resolver from the map of resolvers
	 * @param objectPrefix object prefix
	 */
	public void removeResolver(String objectPrefix) {
		resolvableProperty.remove(objectPrefix);
	}

	/**
	 * let this ruletree share resolvers with the given ruletree
	 * @param ruleTree
	 */
	public void shareResolvers(DefaultRuleTree ruleTree) {
		resolvableProperty = ruleTree.resolvableProperty;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		return createInstance();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.RuleTree#resolve(java.lang.String, java.lang.String)
	 */
	public Object resolve(String objectPrefix, String attribute) throws UnknownPropertyException {
		if (resolvableProperty.containsKey(objectPrefix)) {
			return ((PropertyResolver) resolvableProperty.get(objectPrefix)).resolve(attribute);
		} else {
			throw new UnknownPropertyException("no resolver found for prefix '" + objectPrefix + "'");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.rule.RuleTree#hasChanged(long)
	 */
	public boolean hasChanged(long timestamp) {
		if (compatiblityMode) {
			if (timestamp < lastChange) {
				return true;
			}
			boolean changed = false;

			// check all property operands in this ruletree
			for (Iterator iter = propertyOperands.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				PropertyOperand propertyOperand = (PropertyOperand) entry.getKey();
				Object value = entry.getValue();
				String[] propertyValues = propertyOperand.getValues();

				if (value instanceof String[]) {
					if (!StringUtils.isEqual((String[]) value, propertyValues)) {
						changed = true;
						entry.setValue(propertyValues);
					}
				} else {
					changed = true;
					entry.setValue(propertyValues);
				}
			}

			// check all subruletrees
			for (Iterator iter = rule.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (element instanceof RuleTree) {
					changed |= ((RuleTree) element).hasChanged(timestamp);
				} else if (element instanceof Condition) {
					Condition c = (Condition) element;

					if (c.getLeftOperand() instanceof FunctionOperand || c.getRightOperand() instanceof FunctionOperand) {
						changed = true;
					}
				}
			}

			if (changed) {
				lastChange = System.currentTimeMillis();
			}
			return changed;
		} else {
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see RuleTree#deepCopy()
	 */
	public RuleTree deepCopy() {
		DefaultRuleTree ruleTree = new DefaultRuleTree();

		// also copy the resolvers
		if (resolvableProperty != null) {
			for (Iterator iter = resolvableProperty.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();

				ruleTree.resolvableProperty.put(entry.getKey(), entry.getValue());
			}
		}

		if (compatiblityMode) {

			ruleTree.ruleStr = this.ruleStr;
			Iterator iterator = this.rule.iterator();

			while (iterator.hasNext()) {
				Object obj = iterator.next();

				if (obj instanceof Operand) {
					ruleTree.rule.add(((Operand) obj).deepCopy(ruleTree));
				} else if (obj instanceof Operator) {
					// No need to copy Operators ..
					ruleTree.rule.add(obj);
				} else if (obj instanceof DefaultRuleTree) {
					DefaultRuleTree copy = (DefaultRuleTree) ((DefaultRuleTree) obj).deepCopy();

					// share resolvables between all subrule trees.
					copy.resolvableProperty = ruleTree.resolvableProperty;
					ruleTree.rule.add(copy);
				} else if (obj instanceof Condition) {
					ruleTree.rule.add(((Condition) obj).deepCopy(ruleTree));
				} else {
					NodeLogger.getLogger(this.getClass()).error("Error while copying rule tree - unknown type: {" + obj.getClass().getName() + "}");
					return null;
				}
			}
		} else {
			ruleTree.ruleStr = this.ruleStr;
			ruleTree.expression = this.expression;
		}

		return ruleTree;       
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.rule.RuleTree#getExpression()
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * Get the resolvable property map
	 */
	public Map getResolvablePropertyMap() {
		return resolvableProperty;
	}
}
