/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: ExpressionEvaluator.java,v 1.19.4.1 2011-04-07 09:57:49 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.NestedCollection;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.functions.PostProcessorEvaluator;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * Class for expression evaluation. Implements the basic functionality to
 * evaluate an expression. Also provides static methods for type conversions.
 */
public class ExpressionEvaluator implements Changeable {

	/**
	 * constant for any value type
	 */
	public final static int OBJECTTYPE_ANY = 0;

	/**
	 * constant for the value type "null" (object is expected to be null)
	 */
	public final static int OBJECTTYPE_NULL = 1;

	/**
	 * constant for the value type "boolean"
	 */
	public final static int OBJECTTYPE_BOOLEAN = 2;

	/**
	 * constant for the value type "number"
	 */
	public final static int OBJECTTYPE_NUMBER = 3;

	/**
	 * constant for the value type "date"
	 */
	public final static int OBJECTTYPE_DATE = 4;

	/**
	 * constant for the value type "string"
	 */
	public final static int OBJECTTYPE_STRING = 5;

	/**
	 * constant for the value type "string"
	 */
	public final static int OBJECTTYPE_WILDCARDSTRING = 6;

	/**
	 * constant for the value type "collection"
	 */
	public final static int OBJECTTYPE_COLLECTION = 7;

	/**
	 * constant for the value type "assignment (result)"
	 */
	public final static int OBJECTTYPE_ASSIGNMENT = 8;

	/**
	 * constant for unknown value type
	 */
	public final static int OBJECTTYPE_UNKNOWN = 9;

	/**
	 * constant for binary value type
	 */
	public final static int OBJECTTYPE_BINARY = 10;

	/**
	 * objecttype names (used by {@link #getValuetypeName(int)}).
	 */
	private final static String[] OBJECTTYPE_NAMES = new String[] {
		"any", "null", "boolean", "number", "date", "string", "wildcardstring", "collection",
		"assignment (result)", "unknown", "binary"};

	/**
	 * base objects of the expression evaluator
	 */
	protected Map baseObjects;

	private Map requestParameters;

	/**
	 * PropertyResolver
	 */
	protected PropertyResolver resolver;

	/**
	 * Create a new instance of the expression evaluator
	 *
	 */
	public ExpressionEvaluator() {
		baseObjects = new HashMap();
	}

	/**
	 * Set a property resolver, which will be used to resolve paths in expressions
	 * @param resolver property resolver
	 */
	public void setResolver(PropertyResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Get the name of the given value type
	 * @param valueType value type
	 * @return name of the value type as string
	 */
	public static String getValuetypeName(int valueType) {
		if (valueType < 0 || valueType >= OBJECTTYPE_NAMES.length) {
			return OBJECTTYPE_NAMES[OBJECTTYPE_UNKNOWN];
		} else {
			return OBJECTTYPE_NAMES[valueType];
		}
	}
    
	/**
	 * Allows you to set parameters which will be passed to 
	 * {@link ExpressionQueryRequest}
	 * @param parameters request parameters
	 */
	public void setRequestParameters(Map parameters) {
		this.requestParameters = parameters;
	}
    
	private ExpressionQueryRequest createQueryRequest(PropertyResolver resolver) {
		ExpressionQueryRequest req = new ExpressionQueryRequest(resolver, null);

		req.setParameters(this.requestParameters);
		return req;
	}

	/**
	 * Try to match the given expression (against the added resolvables)
	 * @param expression expression to match
	 * @return true when the expression matches, false if not
	 * @throws ExpressionParserException when the expression cannot be matched (is not
	 *         boolean or contains errors)
	 */
	public boolean match(Expression expression) throws ExpressionParserException {
		return match(expression, this);
	}

	/**
	 * Try to match the given expression against the given object (and the added resolvables)
	 * @param expression expression to match
	 * @param matchedObject object the expression is matched against
	 * @return true when the expression matches, false if not
	 * @throws ExpressionParserException when the expression cannot be matched (is not
	 *         boolean or contains errors)
	 */
	public boolean match(Expression expression, Resolvable matchedObject) throws ExpressionParserException {
		List<Resolvable> objects = new ArrayList<Resolvable>();

		objects.add(matchedObject);
		filter(expression, objects);
		return objects.contains(matchedObject);
	}

	/**
	 * Filter the given list of resolvables with the given expression. The list will be modified to only contain the objects that match the given expression.
	 * If the expression contains the filter() function (PostProcessors), they will also be called with the remaining list.
	 * @param expression expression
	 * @param resolvables list of resolvables, must not be null and must be modifiable
	 * @throws ExpressionParserException when filtering fails
	 */
	public void filter(Expression expression, List<Resolvable> resolvables) throws ExpressionParserException {
		if (expression instanceof EvaluableExpression) {
			try {
				RuntimeProfiler.beginMark(ComponentsConstants.EXPRESSIONPARSER_EVALUATION);

				// create a request
				ObjectResolver objResolver = new ObjectResolver(null);
				ExpressionQueryRequest request = createQueryRequest(new PropertyResolver(objResolver));

				// iterate over the list of resolvables
				for (Iterator<Resolvable> iter = resolvables.iterator(); iter.hasNext();) {
					// set the object resolver to resolve the current object
					Resolvable res = iter.next();

					objResolver.baseObject = res;

					// evaluate the expression, if the object does not match, remove it fron the collection
					if (!ObjectTransformer.getBoolean(((EvaluableExpression) expression).evaluate(request, ExpressionEvaluator.OBJECTTYPE_BOOLEAN), false)) {
						iter.remove();
					}
				}

				// now we need to handle the post processors, first we will let the FilterFunction add the PostProcessor instances to a PostProcessorEvaluator
				// we do this by doing another request where "object" resolves to the PostProcessorEvaluator
				PostProcessorEvaluator postProcessorEvaluator = new PostProcessorEvaluator();

				objResolver.baseObject = postProcessorEvaluator;
				((EvaluableExpression) expression).evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

				// now we pass the list of resolvables to the post processor evaluator
				postProcessorEvaluator.doPostProcessing(resolvables, request);
			} finally {
				RuntimeProfiler.endMark(ComponentsConstants.EXPRESSIONPARSER_EVALUATION);
			}
		} else {
			throw new EvaluationException("The expression is not evaluable");
		}
	}

	/**
	 * Inner helper class to resolve "object." and all other names in
	 * expressions
	 */
	private class ObjectResolver implements Resolvable {

		/**
		 * base object to resolve as "object."
		 */
		protected Object baseObject;

		/**
		 * Create the object resolver
		 * @param baseObject base object
		 */
		public ObjectResolver(Object baseObject) {
			this.baseObject = baseObject;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			if ("object".equals(key)) {
				return baseObject;
			} else {
				return ExpressionEvaluator.this.get(key);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
		 */
		public Object getProperty(String key) {
			return get(key);
		}
	}

	/**
	 * Get the given object as number of the given class. When the object is no
	 * instance of the given class, its string representation is parsed as
	 * number.
	 * @param object object to interpret as number
	 * @param numberClass desired class, must be a subclass of Number
	 * @return number representation of the object
	 * @throws EvaluationException when the object cannot be interpreted as
	 *         number or the given numberClass is not derived from class Number
	 */
	public static Number getAsNumber(Object object, Class numberClass) throws EvaluationException {
		return getAsNumber(null, object, numberClass);
	}

	public static Number getAsNumber(ExpressionQueryRequest request, Object object, Class numberClass) throws EvaluationException {
		// first check whether the numberClass is really derived from class
		// Number
		if (!Number.class.isAssignableFrom(numberClass)) {
			throw new EvaluationException("Cannot convert the object into " + numberClass + ". Class must be subclass of Number");
		}

		if (numberClass.isInstance(object)) {
			return (Number) object;
		} else if (object == null) {
			return null;
		} else if ("".equals(object) && "content.node".equals(ExpressionParser.getExpressionParserMode(request))) {
			return new Integer(0);
		} else {
			try {
				Constructor constructor = numberClass.getConstructor(new Class[] { String.class});
				String numberString = object.toString();

				if (numberString != null) {
					// to allow strings that are formatted numbers, we remove
					// all , (thousand separators)
					numberString = numberString.replaceAll(",", "");
					if (numberString.endsWith(".")) {
						// cut off trailing .
						numberString = numberString.substring(0, numberString.length() - 1);
					}
				}
				return (Number) constructor.newInstance(new Object[] { numberString});
			} catch (Exception e) {
				throw new EvaluationException(
						"Error: expected Number, but got operand of type " + getTypeName(object) + " (" + object + ") that could not be interpreted as number");
			}
		}
	}

	/**
	 * Get the given object as number. When the object is no instance of class
	 * {@link Number}, its string representation is parsed as number.
	 * @param object object to interpret as number
	 * @return number representation of the object
	 * @throws EvaluationException when the object cannot be interpreted as
	 *         number
	 */
	public static Number getAsNumber(Object object) throws EvaluationException {
		if (object instanceof Number) {
			// if we already have a number, return it.
			return (Number) object;
		}
		return getAsNumber(object, Double.class);
	}

	/**
	 * Get the given object as boolean. When the object is not instance of class
	 * {@link Boolean}, its string representation is parsed as boolean. "true",
	 * "false", "1" or "0" can be interpreted as boolean.
	 * @param object object to interpret as boolean
	 * @return boolean representation of the object
	 * @throws EvaluationException when the object cannot be interpreted as
	 *         boolean
	 */
	public static Boolean getAsBoolean(Object object) throws EvaluationException {
		if (object instanceof Boolean) {
			return (Boolean) object;
		} else if (object == null) {
			return null;
		} else {
			String string = object.toString().trim();

			if ("true".equalsIgnoreCase(string) || "1".equalsIgnoreCase(string)) {
				return Boolean.TRUE;
			} else if ("false".equalsIgnoreCase(string) || "0".equalsIgnoreCase(string)) {
				return Boolean.FALSE;
			}
			throw new EvaluationException(
					"Error: expected Boolean, but got operand of type " + getTypeName(object) + " (" + object + ") that could not be interpreted as boolean");
		}
	}

	/**
	 * Get the given object as Date. This fails when the object is no instance
	 * of class {@link Date}.
	 * @param object object to interpret as Date
	 * @return Date representation of the object
	 * @throws EvaluationException when the object cannot be interpreted as Date
	 */
	public static Date getAsDate(Object object) throws EvaluationException {
		if (object instanceof Date) {
			return (Date) object;
		} else if (object == null) {
			return null;
		} else {
			throw new EvaluationException("Error: expected Date, but got operand of type " + getTypeName(object));
		}
	}

	/**
	 * Get the given object as Collection. When the object is no instance of
	 * class {@link Collection}, a new collection containing just the object is
	 * returned.
	 * @param object object to interpret as collection
	 * @return collection representation of the object
	 * @throws EvaluationException when evaluation fails
	 */
	public static Collection getAsCollection(Object object) throws EvaluationException {
		if (object instanceof Collection) {
			return (Collection) object;
			// } else if (object != null) {
		} else if (object instanceof byte[]) {
			return Collections.singleton(object);
		} else if (object != null && object.getClass().isArray() && !object.getClass().getSuperclass().isPrimitive()) {
			return Arrays.asList((Object[]) object);
		} else {
			return Collections.singleton(object);
			// } else {
			// return Collections.EMPTY_LIST;
		}
	}

	/**
	 * Get the given object as String. This only fails when the object is a
	 * {@link Collection}.
	 * @param object object to interpret as string
	 * @return string representation of the object
	 * @throws EvaluationException when the object cannot be interpreted as
	 *         string
	 */
	public static String getAsString(Object object) throws EvaluationException {
		if (object instanceof String) {
			return (String) object;
		} else if (object == null) {
			return null;
		} else if (object instanceof Collection) {
			throw new EvaluationException("Error: expected String, but got operand of type " + getTypeName(object));
		} else {
			return object.toString();
		}
	}

	/**
	 * Get the given object as binary. This will succeed, if the object either already is binary data, or is a String
	 * @param object object to get as binary data
	 * @return binary data
	 * @throws EvaluationException when evaluation fails
	 */
	public static byte[] getAsBinary(Object object) throws EvaluationException {
		if (object instanceof byte[]) {
			return (byte[]) object;
		} else if (object instanceof String) {
			try {
				return ((String) object).getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new EvaluationException("Could not convert " + object + " into binary", e);
			}
		} else if (object == null) {
			return null;
		} else {
			throw new EvaluationException("Could not convert " + object + " into binary");
		}
	}

	/**
	 * Get the given object as object of specified type. This calls one of the
	 * getAsXXX() methods. The type may be one of ({@link #OBJECTTYPE_ANY},
	 * {@link #OBJECTTYPE_BOOLEAN}, {@link #OBJECTTYPE_COLLECTION},
	 * {@link #OBJECTTYPE_DATE}, {@link #OBJECTTYPE_NULL},
	 * {@link #OBJECTTYPE_NUMBER}, {@link #OBJECTTYPE_STRING}).
	 * @param object object to interpret in the given type
	 * @param type requested type for the object
	 * @return type-representation of the object
	 * @throws EvaluationException when the object cannot be interpreted in the
	 *         given type
	 */
	public static Object getAsType(Object object, int type) throws EvaluationException {
		switch (type) {
		case OBJECTTYPE_ANY:
			return object;

		case OBJECTTYPE_BOOLEAN:
			return getAsBoolean(object);

		case OBJECTTYPE_COLLECTION:
			return getAsCollection(object);

		case OBJECTTYPE_DATE:
			return getAsDate(object);

		case OBJECTTYPE_NULL:
			return null;

		case OBJECTTYPE_NUMBER:
			return getAsNumber(object);

		case OBJECTTYPE_STRING:
		case OBJECTTYPE_WILDCARDSTRING:
			return getAsString(object);

		default:
			throw new EvaluationException("Unknown type {" + type + "}");
		}
	}

	/**
	 * Check whether the given number is an integer. This is true when the
	 * object is an instance of one of the following classes: {@link Long},
	 * {@link Integer}, {@link Short}, {@link Byte}, {@link BigInteger}
	 * @param number number to check
	 * @return true when the number is an integer, false if not
	 */
	public static boolean isInteger(Number number) {
		return (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof BigInteger || number instanceof Byte);
	}

	/**
	 * Get the type of the object as string
	 * @param object object to analyze
	 * @return type of the object in readable form
	 */
	protected static String getTypeName(Object object) {
		if (object == null) {
			return "null";
		} else if (object instanceof String) {
			return "String";
		} else if (object instanceof Number) {
			return "Number";
		} else if (object instanceof Boolean) {
			return "Boolean";
		} else if (object instanceof Date) {
			return "Date";
		} else if (object instanceof Collection) {
			return "Collection";
		} else {
			return "Object";
		}
	}

	/**
	 * Helper method to throw an Exception "Incompatible types found in
	 * comparison..."
	 * @param object1 first object in comparison
	 * @param object2 second object in comparison
	 * @return nothing, since this method always throws an exception
	 * @throws EvaluationException since this is the purpose of this method
	 */
	protected static boolean incompatibleComparisonTypes(Object object1, Object object2) throws EvaluationException {
		throw new EvaluationException("Incompatible types found in comparison: " + getTypeName(object1) + " vs. " + getTypeName(object2));
	}

	/**
	 * Perform a typesafe comparison between the given objects. The type may be
	 * one of ({@link Function#TYPE_SMALLER},
	 * {@link Function#TYPE_SMALLEROREQUAL},
	 * {@link Function#TYPE_GREATEROREQUAL}, {@link Function#TYPE_GREATER}).
	 * @param request object.
	 * @param object1 lefthand-side object
	 * @param object2 righthand-side object
	 * @param type type of the comparison
	 * @return result of the comparison
	 * @throws EvaluationException when the objects cannot be converted such
	 *         that the comparison can be done
	 */
	public static boolean typeSafeComparison(ExpressionQueryRequest request, Object object1, Object object2, int type) throws EvaluationException {
		if (object1 == null || object2 == null) {
			return false;
		}
		if (object1 instanceof Number && object2 instanceof Number) {
			switch (type) {
			case Function.TYPE_SMALLER:
				return ((Number) object1).doubleValue() < ((Number) object2).doubleValue();

			case Function.TYPE_SMALLEROREQUAL:
				return ((Number) object1).doubleValue() <= ((Number) object2).doubleValue();

			case Function.TYPE_GREATEROREQUAL:
				return ((Number) object1).doubleValue() >= ((Number) object2).doubleValue();

			case Function.TYPE_GREATER:
				return ((Number) object1).doubleValue() > ((Number) object2).doubleValue();

			default:
				return false;
			}
		} else if (object1 instanceof Date && object2 instanceof Date) {
			switch (type) {
			case Function.TYPE_SMALLER:
				return ((Date) object1).before((Date) object2);

			case Function.TYPE_SMALLEROREQUAL:
				return ((Date) object1).before((Date) object2) || ((Date) object1).equals(object2);

			case Function.TYPE_GREATEROREQUAL:
				return ((Date) object1).after((Date) object2) || ((Date) object1).equals(object2);

			case Function.TYPE_GREATER:
				return ((Date) object1).after((Date) object2);

			default:
				return false;
			}
		} else if (object1 instanceof NestedCollection) {
			// do the nested collection comparison (at least one object of
			// the nested collection must equal the right object
			for (Iterator iter = ((NestedCollection) object1).iterator(); iter.hasNext();) {
				Object nestedObject = (Object) iter.next();

				// FIXME catch and ignore nested EvaluationExceptions here?
				if (typeSafeComparison(request, nestedObject, object2, type)) {
					return true;
				}
			}
			return false;
		} else {
			try {
				switch (type) {
				case Function.TYPE_SMALLER:
					return getAsNumber(request, object1, Double.class).doubleValue() < getAsNumber(request, object2, Double.class).doubleValue();

				case Function.TYPE_SMALLEROREQUAL:
					return getAsNumber(request, object1, Double.class).doubleValue() <= getAsNumber(request, object2, Double.class).doubleValue();

				case Function.TYPE_GREATEROREQUAL:
					return getAsNumber(request, object1, Double.class).doubleValue() >= getAsNumber(request, object2, Double.class).doubleValue();

				case Function.TYPE_GREATER:
					return getAsNumber(request, object1, Double.class).doubleValue() > getAsNumber(request, object2, Double.class).doubleValue();

				default:
					return false;
				}
			} catch (Exception ex) {
				return incompatibleComparisonTypes(object1, object2);
			}
		}
	}

	/**
	 * Check whether the objects are typesafe unequal. This is not the converse
	 * as {@link #isTypeSafeEqual(Object, Object)}, when object1 is an instance
	 * of {@link NestedCollection}. In this case, the comparison is interpreted
	 * as: "true when object1 contains at least one object that is unequal
	 * object2"
	 * @param object1 lefthand-side object
	 * @param object2 righthand-side object
	 * @return true when the objects are unequal, false if they are equal
	 * @throws EvaluationException when the objects cannot be compared
	 */
	public static boolean isTypeSafeUnequal(Object object1, Object object2) throws EvaluationException {
		if (object1 instanceof NestedCollection && ((NestedCollection) object1).isEmpty()) {
			// for nested collections, the unequal comparison also fails when
			// the collection is empty
			return false;
		} else {
			return !isTypeSafeEqual(object1, object2);
		}
	}

	/**
	 * Check whether the objects are typesafe equal.
	 * @param object1 lefthand-side object
	 * @param object2 righthand-side object
	 * @return true when the objects are equal, false if not
	 * @throws EvaluationException when the objects cannot be compared
	 */
	public static boolean isTypeSafeEqual(Object object1, Object object2) throws EvaluationException {
		// when at least one object is a collection, we compare the objects with
		// containsoneof.
		// this is because datasources cannot distinguish between == and
		// containsoneof for multivalue attributes
		if (object1 instanceof Collection || object2 instanceof Collection) {
			return containsOneOf(getAsCollection(object1), getAsCollection(object2));
		}

		Comparator comparator = TypeSafeComparator.getInstance();

		if (object1 instanceof Number) {
			if (object2 instanceof Number) {
				return comparator.compare(object1, object2) == 0;
			} else if (object2 == null) {
				return false;
			} else {
				try {
					return comparator.compare(object1, getAsNumber(object2, object1.getClass())) == 0;
				} catch (Exception ex) {
					// the left side could not be interpretet as number, so the values are not equal
					return false;
				}
			}
		} else if (object1 instanceof String) {
			if (object2 instanceof String) {
				return comparator.compare(object1, object2) == 0;
			} else if (object2 == null) {
				if (ExpressionParser.isTreatEmptyStringAsNull()) {
					return ((String) object1).length() == 0;
				} else {
					return false;
				}
			} else if (object2 instanceof Number) {
				// special treatment of comparisons String <-> Number: try to
				// convert the string into a number and compare the numbers
				try {
					return comparator.compare(getAsNumber(object1, object2.getClass()), object2) == 0;
				} catch (EvaluationException e) {
					// when the string cannot be interpreted as number, the objects are not equal
					return false;
				}
			} else {
				try {
					return comparator.compare(object1, getAsString(object2)) == 0;
				} catch (Exception ex) {
					return false;
				}
			}
		} else if (object1 instanceof Boolean) {
			if (object2 instanceof Boolean) {
				return comparator.compare(object1, object2) == 0;
			} else if (object2 == null) {
				return false;
			} else {
				try {
					return comparator.compare(object1, getAsBoolean(object2)) == 0;
				} catch (Exception ex) {
					return false;
				}
			}
		} else if (object1 instanceof Date) {
			if (object2 instanceof Date) {
				return comparator.compare(object1, object2) == 0;
			} else if (object2 == null) {
				return false;
			} else {
				try {
					return comparator.compare(object1, getAsDate(object2)) == 0;
				} catch (Exception ex) {
					return false;
				}
			}
		} else if (object1 instanceof NestedCollection) {
			// do the nested collection comparison (at least one object of
			// the nested collection must equal the right object
			for (Iterator iter = ((NestedCollection) object1).iterator(); iter.hasNext();) {
				Object nestedObject = (Object) iter.next();

				// FIXME catch and ignore nested EvaluationExceptions here?
				if (isTypeSafeEqual(nestedObject, object2)) {
					return true;
				}
			}
			return false;
		} else if (object1 instanceof Collection) {
			if (object2 instanceof Collection) {
				return TypeSafeComparator.getInstance().compare(object1, object2) == 0;
			} else if (object2 == null) {
				return false;
			} else {
				return false;
			}
		} else if (object1 instanceof byte[] || object2 instanceof byte[]) {
			return comparator.compare(object1, object2) == 0;
		} else if (object1 == null) {
			if (ExpressionParser.isTreatEmptyStringAsNull()) {
				// also treat empty strings as null
				return object2 == null || object2.toString().length() == 0;
			} else {
				// only null == null
				return object2 == null;
			}
		} else {
			return object1.equals(object2);
		}
	}

	/**
	 * Analyze the given object and get the objecttype
	 * @param object object to analyze
	 * @return objecttype of the object
	 * @throws EvaluationException when evaluation fails
	 */
	protected static int getObjectType(Object object) throws EvaluationException {
		if (object == null) {
			return OBJECTTYPE_NULL;
		} else if (object instanceof String) {
			return OBJECTTYPE_STRING;
		} else if (object instanceof Number) {
			return OBJECTTYPE_NUMBER;
		} else if (object instanceof Boolean) {
			return OBJECTTYPE_BOOLEAN;
		} else if (object instanceof Date) {
			return OBJECTTYPE_DATE;
		} else if (object instanceof Collection) {
			return OBJECTTYPE_COLLECTION;
		} else {
			return OBJECTTYPE_UNKNOWN;
			// throw new EvaluationException("Incompatible type found in comparison: "
			// + getTypeName(object));
		}
	}

	/**
	 * Check whether the left collection contains at least one object of the
	 * right collection
	 * @param leftCollection left collection
	 * @param rightCollection right collection
	 * @return true when the collections contain at least one common object,
	 *         false if not
	 * @throws EvaluationException when evaluation fails
	 */
	public static boolean containsOneOf(Collection leftCollection, Collection rightCollection) throws EvaluationException {
		if (leftCollection == null || rightCollection == null || leftCollection.isEmpty() || rightCollection.isEmpty()) {
			return false;
		} else if (leftCollection.size() < rightCollection.size()) {
			Comparator typeSafeComparator = TypeSafeComparator.getInstance();

			// check whether we find a single element, which consists in both collections
			for (Object right : rightCollection) {
				for (Object left: leftCollection) {
					if (typeSafeComparator.compare(left, right) == 0) {
						return true;
					}
				}
			}
		} else {
			Comparator typeSafeComparator = TypeSafeComparator.getInstance();

			// check whether we find a single element, which consists in both collections
			for (Object left : leftCollection) {
				for (Object right: rightCollection) {
					if (typeSafeComparator.compare(left, right) == 0) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check whether the collections have no object in common
	 * @param leftCollection left collection
	 * @param rightCollection right collection
	 * @return true when the collections have no object in common, false if they
	 *         have at least one common object
	 * @throws EvaluationException when evaluation fails
	 */
	public static boolean containsNone(Collection leftCollection, Collection rightCollection) throws EvaluationException {
		return !containsOneOf(leftCollection, rightCollection);
	}

	/**
	 * Check whether the left collection contains all objects from the right
	 * collection
	 * @param leftCollection left collection
	 * @param rightCollection right collection
	 * @return true when the left collection contains all objects from the right
	 *         collection (or the right collection is empty), false if not
	 */
	public static boolean containsAll(Collection leftCollection, Collection rightCollection) {
		if (leftCollection == null || rightCollection == null) {
			return false;
		} else if (rightCollection.isEmpty()) {
			return true;
		} else {
			// sort the left collection
			List sortedLeftCollection = new Vector(leftCollection);
			Comparator typeSafeComparator = TypeSafeComparator.getInstance();

			Collections.sort(sortedLeftCollection, typeSafeComparator);

			for (Iterator iter = rightCollection.iterator(); iter.hasNext();) {
				Object element = (Object) iter.next();

				if (Collections.binarySearch(sortedLeftCollection, element, typeSafeComparator) < 0) {
					return false;
				}
			}

			return true;
		}
	}

	/**
	 * Comparator class for doing typesafe comparisons and sorting.
	 */
	public static class TypeSafeComparator implements Comparator {

		/**
		 * singleton instance
		 */
		private final static Comparator INSTANCE = new TypeSafeComparator();

		/**
		 * static method to get the singleton instance
		 * @return singleton instance
		 */
		public final static Comparator getInstance() {
			return INSTANCE;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			try {
				// first check for numbers
				if (o1 instanceof Number && o2 instanceof Number) {
					return (int) Math.signum(((Number) o2).doubleValue() - ((Number) o1).doubleValue());
				}

				// compary binary data
				if (o1 instanceof byte[] || o2 instanceof byte[]) {
					byte[] b1 = getAsBinary(o1);
					byte[] b2 = getAsBinary(o2);

					if (b1 == null) {
						return b2 == null ? 0 : -1;
					} else if (b2 == null) {
						return 1;
					} else {
						int compResult = 0;
						int i = 0;

						while (compResult == 0 && (i < b1.length && i < b2.length)) {
							if (b1[i] < b2[i]) {
								compResult = -1;
							} else if (b1[i] > b2[i]) {
								compResult = 1;
							}
							i++;
						}

						if (compResult == 0) {
							if (b1.length < b2.length) {
								compResult = -1;
							} else if (b1.length > b2.length) {
								compResult = 1;
							}
						}

						return compResult;
					}
				}

				// try to get both objects as strings
				try {
					String s1 = getAsString(o1);
					String s2 = getAsString(o2);

					// compare the strings now
					if (s1 == null) {
						// null == null < nonnull
						return s2 == null ? 0 : -1;
					} else if (s2 == null) {
						return 1;
					} else {
						// FIXME ev. compare case insensitive?
						return s1.compareTo(s2);
					}
				} catch (EvaluationException e) {
					// at least one object failed to be converted to a string

					// get the objecttypes
					int type1 = getObjectType(o1);
					int type2 = getObjectType(o2);

					// when the objecttypes are not equal, the object with the
					// "smaller" objecttype is smaller
					if (type1 != type2) {
						return type1 - type2;
					} else {
						// objecttypes are equal, sort the objects
						switch (type1) {
						case OBJECTTYPE_NULL:
							// there is only one null
							return 0;

						case OBJECTTYPE_STRING:
							// strings are compared lexicographically
							// FIXME ev. compare case insensitive?
							return ((String) o1).compareTo((String) o2);

						case OBJECTTYPE_NUMBER:
							// numbers are compared by their values
							double double1 = ((Number) o1).doubleValue();
							double double2 = ((Number) o2).doubleValue();

							return double1 < double2 ? -1 : (double1 > double2 ? 1 : 0);

						case OBJECTTYPE_BOOLEAN:
							// true comes before false
							boolean boolean1 = ((Boolean) o1).booleanValue();
							boolean boolean2 = ((Boolean) o2).booleanValue();

							return boolean1 ? (boolean2 ? 0 : -1) : (boolean2 ? 1 : 0);

						case OBJECTTYPE_DATE:
							// dates are compared by their unix-timestamps
							long time1 = ((Date) o1).getTime();
							long time2 = ((Date) o2).getTime();

							return time1 < time2 ? -1 : (time1 > time2 ? 1 : 0);

						case OBJECTTYPE_COLLECTION:
							// collections are compared entry-wise (compare first
							// object agains first, ...)
							Collection coll1 = (Collection) o1;
							Collection coll2 = (Collection) o2;
							int minSize = Math.min(coll1.size(), coll2.size());
							Iterator i1 = coll1.iterator();
							Iterator i2 = coll2.iterator();

							for (int i = 0; i < minSize; ++i) {
								int partCompare = compare(i1.next(), i2.next());

								if (partCompare != 0) {
									return partCompare;
								}
							}
							// when one collection is a subpart of the other, the
							// smaller collection comes first
							return coll1.size() - coll2.size();

						case OBJECTTYPE_UNKNOWN:
							return o1.hashCode() - o2.hashCode();

						default:
							break;
						}
					}
				}
			} catch (Exception e) {
				throw new ClassCastException(e.getLocalizedMessage());
			}

			return 0;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Changeable#setProperty(java.lang.String, java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		// TODO prohibit setting of properties with name "object"
		if (resolver != null) {
			throw new InsufficientPrivilegesException("Unable to set property into ExpressionEvaluator, when PropertyResolver is set.");
		}
		baseObjects.put(name, value);
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return resolver != null ? resolver.get(key) : baseObjects.get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		return resolver != null ? resolver.get(key) : baseObjects.get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return resolver != null ? resolver.canResolve() : baseObjects != null;
	}

	/**
	 * Clear all resolvable properties
	 */
	public void clearProperties() {
		if (resolver == null) {
			baseObjects.clear();
		}
	}
}
