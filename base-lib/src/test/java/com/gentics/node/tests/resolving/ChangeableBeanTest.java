/*
 * @author norbert
 * @date 24.08.2007
 * @version $Id: ChangeableBeanTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.resolving;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.ChangeableBean;
import org.junit.experimental.categories.Category;

/**
 * Testcase for testing changeable beans
 */
@Category(BaseLibTest.class)
public class ChangeableBeanTest {

	/**
	 * tested bean object
	 */
	protected MyChangeableBean myBean;

	/**
	 * value of the name property
	 */
	protected final static String NAME = "Norbert";

	/**
	 * value of the age property
	 */
	protected final static Integer AGE = new Integer(24);

	/**
	 * value of the size property
	 */
	protected final static Double SIZE = new Double(1.84);

	/**
	 * value for the weight property
	 */
	protected final static Float WEIGHT = new Float(75.3);

	@Before
	public void setUp() throws Exception {
		myBean = new MyChangeableBean();
	}

	/**
	 * Test setting of string parameter
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingString() throws Exception {
		// set the property
		assertTrue("Check setting of property", myBean.setProperty("name", NAME));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", NAME, myBean.get("name"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", NAME, myBean.getName());
	}

	/**
	 * Test setting of int parameter
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingInt() throws Exception {
		// set the property
		assertTrue("Check setting of property", myBean.setProperty("age", AGE));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", AGE, myBean.get("age"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", AGE.intValue(), myBean.getAge());
	}

	/**
	 * Test setting of double value
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingDouble() throws Exception {
		// set the property
		assertTrue("Check setting of property", myBean.setProperty("size", SIZE));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", SIZE, myBean.get("size"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", SIZE.doubleValue(), myBean.getSize(), 0.01);
	}

	/**
	 * Test setting a float value
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingFloat() throws Exception {
		// set the property
		assertTrue("Check setting of property", myBean.setProperty("weight", WEIGHT));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", WEIGHT, myBean.get("weight"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", WEIGHT, myBean.getWeight());
	}

	/**
	 * Test setting null values for string property
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingStringNull() throws Exception {
		// first set a non-null value
		myBean.setName(NAME);

		// now set null
		assertTrue("Check setting of property", myBean.setProperty("name", null));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", null, myBean.get("name"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", null, myBean.getName());
	}

	/**
	 * Test setting null value for double property (which will not change the
	 * value)
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingDoubleNull() throws Exception {
		// first set a non-null value
		myBean.setSize(SIZE.doubleValue());

		// now set null
		assertFalse("Check setting of property (which must fail here)", myBean.setProperty("size", null));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", SIZE, myBean.get("size"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", SIZE.doubleValue(), myBean.getSize(), 0.01);
	}

	/**
	 * Test setting null value for a Floag property
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingFloatNull() throws Exception {
		// first set a non-null value
		myBean.setWeight(WEIGHT);

		// now set null
		assertTrue("Check setting of property", myBean.setProperty("weight", null));

		// check resolving the property
		assertEquals("Check whether the property value is resolved correctly", null, myBean.get("weight"));

		// check getting the property from the bean
		assertEquals("Check whether the property value is set in the bean", null, myBean.getWeight());
	}

	/**
	 * Test setting of illegal value
	 *
	 * @throws Exception
	 */
	@Test
	public void testSettingIllegalValue() throws Exception {
		// first set a value
		myBean.setAge(AGE);

		// try to set an illegal value (must return false)
		assertFalse("Check whether the illegal value was not set", myBean.setProperty("age", NAME));

		// now check whether the original value was preserved
		assertEquals("Check for original value", AGE.intValue(), myBean.getAge());
	}

	/**
	 * Test setting of an unknown property
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnknownProperty() throws Exception {
		assertFalse("Check whether unknown property could not be set", myBean.setProperty("huzli", "wuzli"));
	}

	/**
	 * Test setting null value to a property with ambigous setter
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnsettingWithAmbigousSetter() throws Exception {
		// first set a value
		myBean.setAge(AGE);

		// now try to unset
		assertFalse("Check whether setting with ambigous setters failed", myBean.setProperty("age", null));

		// now check for original value
		assertEquals("Check for original value", AGE.intValue(), myBean.getAge());
	}

	/**
	 * Tested changeable bean class
	 */
	public class MyChangeableBean extends ChangeableBean {

		/**
		 * a string property
		 */
		protected String name;

		/**
		 * an integer property
		 */
		protected int age;

		/**
		 * a size property
		 */
		protected double size;

		/**
		 * a weight property (which can be unset)
		 */
		protected Float weight;

		/**
		 * Get the name
		 *
		 * @return name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Set the name
		 *
		 * @param name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Get the age
		 *
		 * @return age
		 */
		public int getAge() {
			return age;
		}

		/**
		 * Set the age
		 *
		 * @param age
		 */
		public void setAge(int age) {
			this.age = age;
		}

		/**
		 * Alternative setter for age
		 *
		 * @param age
		 *            the age as Integer
		 */
		public void setAge(Integer age) {
			this.age = ObjectTransformer.getInt(age, 0);
		}

		/**
		 * Get the size
		 *
		 * @return size
		 */
		public double getSize() {
			return size;
		}

		/**
		 * Set the size
		 *
		 * @param size
		 */
		public void setSize(double size) {
			this.size = size;
		}

		/**
		 * Set the weight
		 *
		 * @param weight
		 *            weight
		 */
		public void setWeight(Float weight) {
			this.weight = weight;
		}

		/**
		 * Get the weight
		 *
		 * @return weight
		 */
		public Float getWeight() {
			return weight;
		}
	}
}
