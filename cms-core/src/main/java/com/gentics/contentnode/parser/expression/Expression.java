/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Expression.java,v 1.6 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression;

import java.util.Set;

/**
 * An expression is a container of an operand-tree. Like an operand, it can
 * be evaluated, but also defines methods to handle variables in the operand-tree.
 *
 * Before any values can be set to the variables using the bind-methods, the variables
 * must be registered. This is usually done by the expressionparser who creates the
 * expression tree.
 */
public interface Expression extends Operand {

	/**
	 * Bind a registered variable with a new value.
	 *
	 * @param name name of the variable.
	 * @param value the value to set to the variable.
	 * @return the old value of the variable, or null if it was not set.
	 */
	Object bindVariable(String name, Object value);

	/**
	 * Get the current value of a registered variable.
	 * @param name the name of the variable.
	 * @return the current value of the variable.
	 */
	Object readVariable(String name);

	/**
	 * bind an expression to a variable. This is same as {@link #bindVariable}, with the
	 * exception that variables of the sub-expression are also registered to this expression.
	 * Note that the variable-names of registered variables of both expressions must be mutually
	 * exclusive. If the same variable name appears in both expressions, this expression
	 * will bind both variables with the same value.
	 *
	 * @param name name of the variable.
	 * @param expression expression to set to the variable.
	 * @return the old value of the variable.
	 */
	Object bindExpression(String name, Expression expression);

	/**
	 * bind an expression to a variable. If registerVariables is true, this is the same
	 * as {@link #bindExpression(String, Expression)}, else this is the same as {@link #bindVariable(String, Object)}.
	 *
	 * @param name the name of the variable.
	 * @param expression the expression to bind.
	 * @param registerVariables true, if the variables of the expressions should be registered to this expression.
	 * @return the old value of the variable.
	 */
	Object bindExpression(String name, Expression expression, boolean registerVariables);

	/**
	 * Register a variable to this expression. The variable is registered with the name of the
	 * VariableOperand. If more than one variables are registered, all values will be updated with
	 * the same value. Note that if you update the value of a registered variable, the other
	 * variables of the same name will not be affected.
	 *
	 * TODO find a way to 'connect' variable values when registering to an expression,
	 *  so that all values will be updated when changing a variable's value.
	 *
	 * @param variable the variable to registered.
	 */
	void registerVariable(VariableOperand variable);

	/**
	 * unregister a variable from this expression. Note: This only removes the variable
	 * from the list of currently known variables, not from the expression-tree itself.
	 * Further updates of the variable's value through the expression will not affect the
	 * unregistered variable.
	 *
	 * @param variable the variable to unregister.
	 * @return the registered variable.
	 */
	boolean unregisterVariable(VariableOperand variable);

	/**
	 * Get a list of all known variable names.
	 * @return a set of variable names as String.
	 */
	Set getVariables();

	/**
	 * get the registered variableoperands for a given name.
	 * @param name the name of the variable.
	 * @return the set of all registered VariableOperands, or an empty set if none are registered.
	 */
	Set getVariableOperands(String name);
}
