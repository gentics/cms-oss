/*
 * @author norbert
 * @date 18.07.2006
 * @version $Id: AssignmentTest.java,v 1.2 2010-09-28 17:08:15 norbert Exp $
 */
package com.gentics.node.tests.expressionparser;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.ChangeableBean;
import com.gentics.api.lib.resolving.PropertySetter;
import com.gentics.lib.base.MapResolver;
import com.gentics.testutils.GenericTestUtils;
import org.junit.experimental.categories.Category;

/**
 * Test case for assignment tests
 */
@Category(BaseLibTest.class)
public class AssignmentTest {

	/**
	 * Create an instance of the test case
	 */
	@BeforeClass
	public static void testSetup() {
		// initialize the cache
		GenericTestUtils.initConfigPathForCache();
	}

	/**
	 * Test assignment of a single value
	 *
	 * @throws Exception
	 */
	@Test
	public void testSinglevalueAssign() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue = 'modified'", object);
		assertEquals("Check modified value", "modified", object.get("singleValue"));
	}

	@Test
	public void testFromArrayWithSortedSet() throws Exception {
		Changeable object = generateObject();
		Collection collection = new TreeSet(java.util.Arrays.asList(new String[] { "a", "b", "c", "d" }));

		object.setProperty("other", collection);
		testAssignment("data.singleValue = fromArray(data.other, 0)", object);
		assertEquals("Check that first element of collection is 'a'", "a", object.get("singleValue"));
		testAssignment("data.singleValue = fromArray(data.other, 3)", object);
		assertEquals("Check that last element of collection is 'd'", "d", object.get("singleValue"));
	}

	/**
	 * Test assignment of special characters (",')
	 *
	 * @throws Exception
	 */
	@Test
	public void testSpecialCharacterAssign() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue = '\"\\''", object);
		assertEquals("Check modified value", "\"'", object.get("singleValue"));
		testAssignment("data.singleValue = \"'\\\"\"", object);
		assertEquals("Check modified value", "'\"", object.get("singleValue"));
	}

	/**
	 * Test assignment of a multivalue value
	 *
	 * @throws Exception
	 */

	@Test
	public void testMultivalueAssign() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.multiValue = [\"firstmodified\", \"secondmodified\"]", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("firstmodified");
		modifiedValue.add("secondmodified");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));
	}

	/**
	 * Test adding of single value to single value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testSinglevalueAddtoSinglevalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue += \"modified\"", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("single");
		modifiedValue.add("modified");
		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));
	}

	/**
	 * Test adding of single value to multi value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testSinglevalueAddtoMultivalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.multiValue += \"modified\"", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("first");
		modifiedValue.add("second");
		modifiedValue.add("third");
		modifiedValue.add("modified");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));
	}

	/**
	 * Test adding of multi value to single value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueAddtoSinglevalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue += [\"firstmodified\", \"secondmodified\"]", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("single");
		modifiedValue.add("firstmodified");
		modifiedValue.add("secondmodified");
		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));
	}

	/**
	 * Test adding of multi value to multi value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueAddtoMultivalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.multiValue += [\"firstmodified\", \"secondmodified\"]", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("first");
		modifiedValue.add("second");
		modifiedValue.add("third");
		modifiedValue.add("firstmodified");
		modifiedValue.add("secondmodified");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));
	}

	/**
	 * Test removing of single value from single value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testSinglevalueRemovefromSinglevalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue -= \"single\"", object);
		Collection modifiedValue = new Vector();

		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));

		object = generateObject();
		testAssignment("data.singleValue -= \"different\"", object);
		modifiedValue = new Vector();
		modifiedValue.add("single");
		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));
	}

	/**
	 * Test removing of single value from multi value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testSinglevalueRemovefromMultivalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.multiValue -= \"second\"", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("first");
		modifiedValue.add("third");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));

		object = generateObject();
		testAssignment("data.multiValue -= \"different\"", object);
		modifiedValue = new Vector();
		modifiedValue.add("first");
		modifiedValue.add("second");
		modifiedValue.add("third");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));
	}

	/**
	 * Test removing of multi value from single value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueRemovefromSinglevalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue -= [\"single\", \"different\"]", object);
		Collection modifiedValue = new Vector();

		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));

		object = generateObject();
		testAssignment("data.singleValue -= [\"different\", \"otherdifferent\"]", object);
		modifiedValue = new Vector();
		modifiedValue.add("single");
		assertEquals("Check modified value", modifiedValue, object.get("singleValue"));
	}

	/**
	 * Test removing of multi value from multi value attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueRemovefromMultivalue() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.multiValue -= [\"first\", \"third\"]", object);
		Collection modifiedValue = new Vector();

		modifiedValue.add("second");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));

		object = generateObject();
		testAssignment("data.multiValue -= [\"different\", \"third\"]", object);
		modifiedValue = new Vector();
		modifiedValue.add("first");
		modifiedValue.add("second");
		assertEquals("Check modified value", modifiedValue, object.get("multiValue"));
	}

	/**
	 * Test assignment
	 *
	 * <pre>
	 * if(data.multiValue CONTAINSONEOF &quot;first&quot;, data.singleValue = &quot;yes&quot; ,data.singleValue = &quot;no&quot;)
	 * </pre>
	 *
	 * (conditional assignment)
	 *
	 * @throws Exception
	 */
	@Test
	public void testConditionalAssignment() throws Exception {
		Changeable object = generateObject();

		testAssignment("if(data.multiValue CONTAINSONEOF \"first\", data.singleValue = \"yes\" ,data.singleValue = \"no\")", object);
		assertEquals("Check modified value", "yes", object.get("singleValue"));

		object = generateObject();
		testAssignment("if(data.multiValue CONTAINSONEOF \"notfound\", data.singleValue = \"yes\" ,data.singleValue = \"no\")", object);
		assertEquals("Check modified value", "no", object.get("singleValue"));
	}

	/**
	 * Test assignment
	 *
	 * <pre>
	 * data.singleValue = if(data.multiValue CONTAINSONEOF "first", "yes", "no")
	 * </pre>
	 *
	 * (conditional assignment)
	 *
	 * @throws Exception
	 */
	@Test
	public void testConditionalAssignment2() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.singleValue = if(data.multiValue CONTAINSONEOF \"first\", \"yes\", \"no\")", object);
		assertEquals("Check modified value", "yes", object.get("singleValue"));

		object = generateObject();
		testAssignment("data.singleValue = if(data.multiValue CONTAINSONEOF \"notfound\", \"yes\", \"no\")", object);
		assertEquals("Check modified value", "no", object.get("singleValue"));
	}

	/**
	 * Test map assignments
	 *
	 * @throws Exception
	 */
	@Test
	public void testMapAssignment() throws Exception {
		Changeable object = generateObject();
		Map otherMap = new HashMap();

		otherMap.put("one", "one");
		otherMap.put("two", "two");
		object.setProperty("other", otherMap);
		testAssignment("data.other += \"three\"", object);

		Map modifiedMap = new HashMap();

		modifiedMap.putAll(otherMap);
		modifiedMap.put("three", "three");
		assertEquals("Check modified value", modifiedMap, object.get("other"));

		testAssignment("data.other -= [\"one\", \"two\"]", object);
		modifiedMap.remove("one");
		modifiedMap.remove("two");
		assertEquals("Check modified value", modifiedMap, object.get("other"));
	}

	/**
	 * Test assignment and resolving of properties that have numbers as names
	 *
	 * @throws Exception
	 */
	@Test
	public void testNumberProperty() throws Exception {
		Changeable object = generateObject();

		testAssignment("data.0 = 'zero'", object);

		assertEquals("Check modified value", "zero", object.get("0"));
	}

	/**
	 * Perform the assignment given in the testExpressionString
	 *
	 * @param testExpressionString
	 *            test expression (should be an assignment)
	 * @param object
	 *            object to modify
	 * @throws Exception
	 */
	public void testAssignment(String testExpressionString, Changeable object) throws Exception {
		// parse the expression
		Expression testExpression = ExpressionParser.getInstance().parse(testExpressionString);

		// generate the property setter
		Map properties = new HashMap();

		properties.put("data", object);
		PropertySetter propertySetter = new PropertySetter(new MapResolver(properties));

		// perform the assignment
		propertySetter.performAssignment(testExpression);
	}

	/**
	 * Generate an object to test
	 *
	 * @return the object
	 */
	protected Changeable generateObject() {
		Collection multiValue = new Vector();

		multiValue.add("first");
		multiValue.add("second");
		multiValue.add("third");
		return new MyChangeableBean("single", multiValue);
	}

	/**
	 * Inner helper class for a changeable bean
	 */
	protected class MyChangeableBean extends ChangeableBean {

		/**
		 * serial version id
		 */
		private static final long serialVersionUID = 3829268242070245324L;

		/**
		 * single value property
		 */
		protected Object singleValue;

		/**
		 * multi value property
		 */
		protected Object multiValue;

		/**
		 * other property
		 */
		protected Object other;

		/**
		 * 0 property
		 */
		protected String theNull;

		/**
		 * Create an instance of the bean
		 *
		 * @param singleValue
		 *            single value property
		 * @param multiValue
		 *            multi value property
		 */
		public MyChangeableBean(Object singleValue, Object multiValue) {
			this.singleValue = singleValue;
			this.multiValue = multiValue;
		}

		/**
		 * @return Returns the multiValue.
		 */
		public Object getMultiValue() {
			return multiValue;
		}

		/**
		 * @param multiValue
		 *            The multiValue to set.
		 */
		public void setMultiValue(Object multiValue) {
			this.multiValue = multiValue;
		}

		/**
		 * @return Returns the singleValue.
		 */
		public Object getSingleValue() {
			return singleValue;
		}

		/**
		 * @param singleValue
		 *            The singleValue to set.
		 */
		public void setSingleValue(Object singleValue) {
			this.singleValue = singleValue;
		}

		/**
		 * Get the other property
		 *
		 * @return other property value
		 */
		public Object getOther() {
			return other;
		}

		/**
		 * Set the other property
		 *
		 * @param other
		 *            other property value
		 */
		public void setOther(Object other) {
			this.other = other;
		}

		/**
		 * Get the property "0"
		 *
		 * @return 0 property
		 */
		public String get0() {
			return theNull;
		}

		/**
		 * Set the 0 property
		 *
		 * @param theNull
		 */
		public void set0(String theNull) {
			this.theNull = theNull;
		}
	}
}
