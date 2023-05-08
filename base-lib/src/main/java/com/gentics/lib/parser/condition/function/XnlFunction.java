package com.gentics.lib.parser.condition.function;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:33:10 PM To
 * change this template use Options | File Templates.
 */
public abstract class XnlFunction {

	/**
	 * XNL_Function::getFunctionStrings()
	 * @return the possbile names of this XNL_function
	 */
	public abstract String[] getFunctionStrings();

	/**
	 * XNL_Function::getMinParamCount()
	 * @return the count of minimal parameters to this function
	 */
	public abstract int getMinParamCount();

	/**
	 * XNL_Function::getMaxParamCount() NOTE: default value is same as
	 * getMinParamCount
	 * @return the count of maximal parameters to this function
	 */
	public int getMaxParamCount() {
		return this.getMinParamCount();
	}

	/**
	 * XNL_Function::execute() NOTE: you must not return a boolean - value!!!!
	 * they are reserved for internal error handling. return 0 | 1 instead
	 * @param params parameter array ( all gone through XNL_evaluate )
	 * @return 0|1 if the execution was successfull, false otherwise. a boolean
	 *         return value false will result in a raised parse error!
	 */
	public abstract Object execute(Vector params);

	/**
	 * XNL_Function::raiseFunctionError() yet to implement; prints error output
	 * @param description
	 */
	public void raiseFunctionError(String description) {}
}
