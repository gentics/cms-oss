/*
 * @author norbert
 * @date 28.06.2006
 * @version $Id: FunctionRegistry.java,v 1.20 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * The FunctionRegistry is responsible for registering and managing all
 * {@link com.gentics.api.lib.expressionparser.functions.Function}s for expression
 * evaluation and filter generation. The list of registered functions contain
 * the functions provided by Portal.Node and custom functions for added/modified
 * functionality.
 */
public abstract class AbstractFunctionRegistry {

	/**
	 * map holding the function stores for all supported datasources or
	 * evaluation. Keys are the datasource classes or the class
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator}, values are
	 * instances of {@link FunctionStore}.
	 */
	private Map<Class<?>, FunctionStore> functionStores = new HashMap<Class<?>, AbstractFunctionRegistry.FunctionStore>();

	/**
	 * Private constructor for the singleton
	 */
	protected AbstractFunctionRegistry() {}

	/**
	 * Register the function with the given class. The function instance will
	 * provide the necessary information about the implemented functionality.
	 * @param functionClassName name of the function's class
	 * @throws FunctionRegistryException when the given function cannot be
	 *         registered
	 */
	public void registerFunction(String functionClassName) throws FunctionRegistryException {
		NodeLogger logger = NodeLogger.getNodeLogger(getClass());

		// 1. load the class
		Class<?> functionClass = null;

		try {
			functionClass = Class.forName(functionClassName);
		} catch (ClassNotFoundException e) {
			throw new FunctionRegistryException("Error while registering function {" + functionClassName + "}", e);
		}

		// 2. check whether the class implements the interface "Function"
		if (!Function.class.isAssignableFrom(functionClass)) {
			throw new FunctionRegistryException(
					"Cannot register function {" + functionClassName + "}. The class must implement interface {" + Function.class.getName() + "}");
		}

		// 3. create an instance of the function
		Function functionInstance = null;

		try {
			functionInstance = (Function) functionClass.newInstance();
		} catch (Throwable e) {
			throw new FunctionRegistryException("Error while instantiating function {" + functionClassName + "} for registering.", e);
		}

		// 4. get the supported classes
		Class<?>[] supportedClasses = functionInstance.getSupportedDatasourceClasses();

		if (supportedClasses == null) {
			throw new FunctionRegistryException("Cannot register function {" + functionClassName + "}, since it does not support any class.");
		}

		// 5. put the instance into the store for all supported classes
		int functionRegistrations = 0;

		for (int i = 0; i < supportedClasses.length; ++i) {
			try {
				FunctionStore store = getFunctionStore(supportedClasses[i]);

				store.addFunction(functionInstance);
				functionRegistrations++;
				if (logger.isDebugEnabled()) {
					logger.debug("Successfully registered function {" + functionClassName + "} for class {" + supportedClasses[i].getName() + "}");
				}
			} catch (FunctionRegistryException e) {
				logger.error(
						"Function {" + functionClassName + "} whished to register for unsupported class {" + supportedClasses[i] != null
								? supportedClasses[i].getName()
								: null + "}");
			}
		}
		if (functionRegistrations == 0) {
			logger.warn("Function {" + functionClassName + "} could not be registered and will not be used anywhere.");
		} else if (logger.isDebugEnabled()) {
			logger.debug("Function {" + functionClassName + "} was registered " + functionRegistrations + " times.");
		}
	}

	/**
	 * Check all function stores for completeness
	 */
	public void checkFunctionRegistration() {
		for (Map.Entry<Class<?>, FunctionStore> element : functionStores.entrySet()) {
			element.getValue().checkCompleteness(element.getKey().getName());
		}
	}

	/**
	 * Get the function store for the given class. The function store provides
	 * information about all registered functions. When the function store does
	 * not yet exist, a new one (empty) is generated and returned.
	 * @param supportedClass supported class, must be either a descendant of
	 *        Datasource or FunctionEvaluation
	 * @return the function store
	 * @throws FunctionRegistryException when the given class is neither derived
	 *         from Datasource nor FunctionEvaluation
	 */
	public FunctionStore getFunctionStore(Class<?> supportedClass) throws FunctionRegistryException {
		// first check the supportedclass
		if (supportedClass == null) {
			throw new FunctionRegistryException("Cannot fetch function store for null-class.");
		} else if (!ExpressionEvaluator.class.isAssignableFrom(supportedClass) && !Datasource.class.isAssignableFrom(supportedClass)) {
			throw new FunctionRegistryException(
					"Cannot fetch function store for class {" + supportedClass.getName() + "}. The class must either implement " + Datasource.class.getName() + " or "
					+ ExpressionEvaluator.class.getName());
		} else {
			// fetch the function store from the map
			FunctionStore store = functionStores.get(supportedClass);

			if (store == null) {
				// if the store does not yet exist, we create it and put it into
				// the map
				store = new FunctionStore();
				functionStores.put(supportedClass, store);
			}

			return store;
		}
	}

	/**
	 * Inner class for storing the instances of registered functions (for a
	 * specific datasource or evaluation)
	 */
	public class FunctionStore {

		/**
		 * class member holding the registered functions. for performance
		 * reasons every part is put into a dedicated class member and not put
		 * into a map.
		 */
		private Function andFunction;

		private Function orFunction;

		private Function equalFunction;

		private Function smallerFunction;

		private Function smallerOrEqualFunction;

		private Function greaterOrEqualFunction;

		private Function greaterFunction;

		private Function unequalFunction;

		private Function containsOneOfFunction;

		private Function containsNoneFunction;

		private Function containsAllFunction;

		private Function likeFunction;

		private Function addFunction;

		private Function subFunction;

		private Function multFunction;

		private Function divFunction;

		private Function modFunction;

		private Function minusFunction;

		private Function plusFunction;

		private Function notFunction;

		/**
		 * map of registered named functions. keys are the names and values are
		 * the registered function instances
		 */
		private Map<String, Function> namedFunctions = new HashMap<String, Function>();

		/**
		 * Add the given function to the store in the right place
		 * @function function to add to the store
		 */
		protected void addFunction(Function function) {
			NodeLogger logger = NodeLogger.getNodeLogger(AbstractFunctionRegistry.class);

			if (function == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Will not register null-function");
				}
				return;
			}

			// depending on the types of the function put the instance into
			// the right place
			int[] types = function.getTypes();

			for (int i = 0; i < types.length; ++i) {
				switch (types[i]) {
				case Function.TYPE_AND:
					andFunction = function;
					break;

				case Function.TYPE_OR:
					orFunction = function;
					break;

				case Function.TYPE_EQUAL:
					equalFunction = function;
					break;

				case Function.TYPE_SMALLER:
					smallerFunction = function;
					break;

				case Function.TYPE_SMALLEROREQUAL:
					smallerOrEqualFunction = function;
					break;

				case Function.TYPE_GREATEROREQUAL:
					greaterOrEqualFunction = function;
					break;

				case Function.TYPE_GREATER:
					greaterFunction = function;
					break;

				case Function.TYPE_UNEQUAL:
					unequalFunction = function;
					break;

				case Function.TYPE_CONTAINSONEOF:
					containsOneOfFunction = function;
					break;

				case Function.TYPE_CONTAINSNONE:
					containsNoneFunction = function;
					break;

				case Function.TYPE_LIKE:
					likeFunction = function;
					break;

				case Function.TYPE_CONTAINSALL:
					containsAllFunction = function;
					break;

				case Function.TYPE_ADD:
					addFunction = function;
					break;

				case Function.TYPE_SUB:
					subFunction = function;
					break;

				case Function.TYPE_MULT:
					multFunction = function;
					break;

				case Function.TYPE_DIV:
					divFunction = function;
					break;

				case Function.TYPE_MOD:
					modFunction = function;
					break;

				case Function.TYPE_MINUS:
					minusFunction = function;
					break;

				case Function.TYPE_PLUS:
					plusFunction = function;
					break;

				case Function.TYPE_NOT:
					notFunction = function;
					break;

				case Function.TYPE_NAMEDFUNCTION:
					// register a named function here
					String namedFunctionName = function.getName();

					if (StringUtils.isEmpty(namedFunctionName)) {
						logger.warn("Refusing to register function {" + function.getClass().getName() + "} as named function, since it has no name");
					} else {
						namedFunctions.put(namedFunctionName, function);
					}
					break;

				default:
					logger.warn("Refusing to register function {" + function.getClass().getName() + "} for unknown type {" + function.getTypes() + "}");
					break;
				}
			}
		}

		/**
		 * Check all necessary parts of the function store whether they are
		 * implemented by functions or not
		 * @param name name of the function store
		 */
		protected void checkCompleteness(String name) {
			NodeLogger logger = NodeLogger.getNodeLogger(AbstractFunctionRegistry.class);

			if (logger.isDebugEnabled()) {
				logger.debug("Checking function registration for {" + name + "}");
			}

			checkFunction(logger, andFunction, name, "AND");
			checkFunction(logger, orFunction, name, "OR");

			checkFunction(logger, equalFunction, name, "==");
			checkFunction(logger, smallerFunction, name, "<");
			checkFunction(logger, smallerOrEqualFunction, name, "<=");
			checkFunction(logger, greaterOrEqualFunction, name, ">=");
			checkFunction(logger, greaterFunction, name, ">");
			checkFunction(logger, unequalFunction, name, "!=");

			checkFunction(logger, addFunction, name, "+ (binary)");
			checkFunction(logger, subFunction, name, "- (binary)");
			checkFunction(logger, multFunction, name, "*");
			checkFunction(logger, divFunction, name, "/");
			checkFunction(logger, modFunction, name, "%");

			checkFunction(logger, plusFunction, name, "+ (unary)");
			checkFunction(logger, minusFunction, name, "- (unary)");
			checkFunction(logger, notFunction, name, "!");

			checkFunction(logger, containsOneOfFunction, name, "CONTAINSONEOF");
			checkFunction(logger, containsNoneFunction, name, "CONTAINSNONE");
			checkFunction(logger, containsAllFunction, name, "CONTAINSALL");
			checkFunction(logger, likeFunction, name, "LIKE");

			// named functions are only checked (and listed) when loglevel is
			// debug
			if (logger.isDebugEnabled()) {
				for (Map.Entry<String, Function> entry : namedFunctions.entrySet()) {
					logger.debug("named function {" + entry.getKey() + "} for {" + name + "} implemented by {" + entry.getValue().getClass().getName() + "}");
				}
			}
		}

		/**
		 * Check the single function (whether it has been set or not)
		 * @param logger logger for logging messages
		 * @param function function to check
		 * @param storeName name of the store
		 * @param functionName name of the function
		 */
		protected void checkFunction(NodeLogger logger, Function function, String storeName,
				String functionName) {
			if (function == null) {
				logger.error("No implementation found for " + functionName + " for {" + storeName + "}");
			} else if (logger.isDebugEnabled()) {
				logger.debug(functionName + " for {" + storeName + "} implemented by {" + function.getClass().getName() + "}");
			}
		}

		/**
		 * Get the "and" function
		 * @return the "and" function
		 */
		public Function getAndFunction() {
			return andFunction;
		}

		/**
		 * Get the "or" function
		 * @return "or" function
		 */
		public Function getOrFunction() {
			return orFunction;
		}

		/**
		 * Get the "equals" function
		 * @return "equals" function
		 */
		public Function getEqualFunction() {
			return equalFunction;
		}

		/**
		 * Get the "add" function
		 * @return "add" function
		 */
		public Function getAddFunction() {
			return addFunction;
		}

		/**
		 * Get the "containsnone" function
		 * @return "containsnone" function
		 */
		public Function getContainsNoneFunction() {
			return containsNoneFunction;
		}

		/**
		 * Get the "containsoneof" function
		 * @return "containsoneof" function
		 */
		public Function getContainsOneOfFunction() {
			return containsOneOfFunction;
		}

		/**
		 * Get the "containsall" function
		 * @return "containsall" function
		 */
		public Function getContainsAllFunction() {
			return containsAllFunction;
		}

		/**
		 * Get the "div" function
		 * @return "div" function
		 */
		public Function getDivFunction() {
			return divFunction;
		}

		/**
		 * Get the "greater" function
		 * @return "greater" function
		 */
		public Function getGreaterFunction() {
			return greaterFunction;
		}

		/**
		 * Get the "greaterorequal" function
		 * @return "greaterorequal" function
		 */
		public Function getGreaterOrEqualFunction() {
			return greaterOrEqualFunction;
		}

		/**
		 * Get the "like" function
		 * @return "like" function
		 */
		public Function getLikeFunction() {
			return likeFunction;
		}

		/**
		 * Get the "minus" function
		 * @return "minus" function
		 */
		public Function getMinusFunction() {
			return minusFunction;
		}

		/**
		 * Get the "mod" function
		 * @return "mod" function
		 */
		public Function getModFunction() {
			return modFunction;
		}

		/**
		 * Get the "mult" function
		 * @return "mult" function
		 */
		public Function getMultFunction() {
			return multFunction;
		}

		/**
		 * Get the "not" function
		 * @return "not" function
		 */
		public Function getNotFunction() {
			return notFunction;
		}

		/**
		 * Get the "smaller" function
		 * @return "smaller" function
		 */
		public Function getSmallerFunction() {
			return smallerFunction;
		}

		/**
		 * Get the "smallerorequal" function
		 * @return "smallerorequal" function
		 */
		public Function getSmallerOrEqualFunction() {
			return smallerOrEqualFunction;
		}

		/**
		 * Get the "sub" function
		 * @return "sub" function
		 */
		public Function getSubFunction() {
			return subFunction;
		}

		/**
		 * Get the "unequal" function
		 * @return "unequal" function
		 */
		public Function getUnequalFunction() {
			return unequalFunction;
		}

		/**
		 * Get the "plus" function
		 * @return "plus" function
		 */
		public Function getPlusFunction() {
			return plusFunction;
		}

		/**
		 * Get named function
		 * @param name name of the named function
		 * @return named function or null
		 */
		public Function getNamedFunction(String name) {
			return (Function) namedFunctions.get(name);
		}
	}
}
