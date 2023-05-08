package com.gentics.contentnode.tests.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.logging.LogCounterAppender;
import com.gentics.testutils.logging.LogCounterAppender.LogCounter;

public class CNI18nStringTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected LogCounterAppender logCounterAppender;

	@Before
	public void testSetup() {
		// create an appender for the logger
		NodeLogger logger = NodeLogger.getNodeLogger("com.gentics");

		logCounterAppender = new LogCounterAppender();
		logger.addAppender(logCounterAppender);
	}
	
	@Test
	public void testEnglishCNI18nString() {
		ContentNodeHelper.setLanguageId(CNDictionary.LANGUAGE_ENGLISH);
		I18nString translatedI18NString = new CNI18nString("rest.general.insufficientdata");

		assertNotNull("The i18n string should not be null", translatedI18NString);
		assertEquals("Insufficient data provided.", translatedI18NString.toString());
	}

	@Test
	public void testGermanCNI18nString() {
		ContentNodeHelper.setLanguageId(CNDictionary.LANGUAGE_GERMAN);
		I18nString translatedI18NString = new CNI18nString("rest.general.insufficientdata");

		assertNotNull("The i18n string should not be null", translatedI18NString);
		assertEquals("Unzureichende Daten angegeben.", translatedI18NString.toString());
	}

	@Test
	public void testEscapedCNI18nString() {
		ContentNodeHelper.setLanguageId(CNDictionary.LANGUAGE_ENGLISH);
		I18nString translatedI18NString = new CNI18nString("no_perm_del_files_images");

		assertNotNull("The i18n string should not be null", translatedI18NString);
		assertEquals("You don't have permissions to delete images or pages from the folder '{0}'.", translatedI18NString.toString());
	}
	
	@Test
	public void testUTF8CNI18nString() {
		ContentNodeHelper.setLanguageId(CNDictionary.LANGUAGE_GERMAN);
		I18nString translatedI18NString = new CNI18nString("unlink");

		assertNotNull("The i18n string should not be null", translatedI18NString);
		assertEquals("Entknüpfen", translatedI18NString.toString());
	}
	
	@Test
	public void testBracketCNI18nString() {
		CNI18nString i18n = new CNI18nString("This {string} has some brackets");
		String processedI18NString = i18n.toString();
        
		assertNotNull("The i18n string should not be null", processedI18NString);
		assertEquals(i18n.toString(), processedI18NString);
		assertNoErrors();
	}
	
	@Test
	public void testApostropheCNI18nString() {
		CNI18nString i18n = new CNI18nString("This 'string' has some apostrophes");
		String processedI18NString = i18n.toString();
        
		assertNotNull("The i18n string should not be null", processedI18NString);
		assertEquals(i18n.toString(), processedI18NString);
		assertNoErrors();
	}
	
	@Test
	public void testParameterCNI18nString() {
		CNI18nString i18n = new CNI18nString("This string has a parameter: {0}");

		i18n.addParameter("test parameter");
		String processedI18NString = i18n.toString();
 	 	
		assertNotNull("The i18n string should not be null", processedI18NString);
		assertEquals(i18n.toString(), processedI18NString);
		assertNoErrors();
	}
	
	@Test
	public void testParameterWithBracketCNI18nString() {
		CNI18nString i18n = new CNI18nString("This string has a parameter with a bracket: {0}");

		i18n.addParameter("{");
		String processedI18NString = i18n.toString();
        
		assertNotNull("The i18n string should not be null", processedI18NString);
		assertEquals(i18n.toString(), processedI18NString);
		assertNoErrors();
	}
	
	@Test
	public void testParameterWithApostropheCNI18nString() {
		CNI18nString i18n = new CNI18nString("This string has a parameter with an apostrophe: {0}");

		i18n.addParameter("'");
		String processedI18NString = i18n.toString();
        
		assertNotNull("The i18n string should not be null", processedI18NString);
		assertEquals(i18n.toString(), processedI18NString);
		assertNoErrors();
	}
	
	protected void assertNoErrors() {
		LogCounter errorCounter = logCounterAppender.getLogCounter(Level.ERROR);
		
		assertEquals("Check number of logged errors (collected errors: " + errorCounter.getLog() + ")", 0, errorCounter.getCounter());
	}
	
}
