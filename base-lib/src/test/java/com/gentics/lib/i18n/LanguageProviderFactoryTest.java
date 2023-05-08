package com.gentics.lib.i18n;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import com.gentics.api.lib.exception.NodeException;

@Category(BaseLibTest.class)
public class LanguageProviderFactoryTest {

	@After
	public void tearDown() {
		LanguageProviderFactory.reset();
	}

	/**
	 * Test whether the wrapper's getProvider method was being invoked
	 *
	 * @throws NodeException
	 */
	@Test
	public void testSpecificLanguageProvider() throws NodeException {
		LanguageProviderWrapper wrapper = Mockito.mock(LanguageProviderWrapper.class);
		LanguageProviderFactory.getInstance().registerProviderWrapper(wrapper);
		LanguageProviderFactory.getInstance().getProvider();
		Mockito.verify(wrapper).getCurrentProvider();
	}

	/**
	 * Test invalid wrapper
	 *
	 * @throws NodeException
	 */
	@Test(expected = NodeException.class)
	public void testInvalidProvider() throws NodeException {
		LanguageProviderFactory.getInstance().registerProviderWrapper(null);
	}

	/**
	 * Test getProvider call with prior no valid registration call
	 *
	 * @throws NodeException
	 */
	@Test(expected = NodeException.class)
	public void testMissingRegistration() throws NodeException {
		LanguageProviderFactory.getInstance().getProvider();
	}
}
