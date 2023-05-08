package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.lib.log.NodeLogger;

public class TestRegularExpressions {

	private static final NodeLogger LOGGER = NodeLogger.getNodeLogger(TestRegularExpressions.class);

	@Test
	public void testRegularExpressions() {

		String[][] regexTests = {
			// Zahl (natürlich)
			{ "^[1-9][0-9]{0,8}$", "12345" }, // E-Mail Adresse       
			{ "^([-_.&0-9a-zA-Z\\+])+@[0-9a-z]([-.]?[0-9a-z])*.[a-z]{2,}$", "b.kaszt-test@gentics.com" }, // Nicht leer
			{ ".+", "nicht leer" }, // Text (kurz)    
			{ "^.{1,255}$", "ein kurzer Text" }, // Zahl (reell)   
			{ "^[-+]{0,1}[0-9]{1,9}.{0,1}[0-9]{0,2}$", "1234567" }, // Text (einfach)
			{ "^[ßäöüÄÖÜa-zA-Z .-]{1,50}$", "ein einfacher Text ." }, // Zahl (ganz)         
			{ "^[+-]{0,1}[0-9]{1,9}$", "12345" }, // Text (Benutzername)
			{ "^[a-zA-Z0-9\\._@\\+\\-]{4,40}$", "benutzername" }, // Text (Passwort)
			{ "^[a-zA-Z0-9\\._@\\+\\-]{4,40}$", "passwort1234" }, // Web Adresse       
			{ "^[a-zA-Z0-9\\._-]{1,64}$", "www.gentics.com" }, // Ordnername
			{ "^[0-9ßäöüÄÖÜa-zA-Z \\.-]{1,255}$", "Ordenrname" }, // Dateiname
			{ "^([a-zA-Z0-9\\._-]){0,64}$", "Dateiname" }, // Datum (fix)
			{ "^([0-9]{1,2})[.,_/ -]([0-9]{1,2})[.,_/ -]([0-9]{2}|[0-9]{4})$", "14.5.2013" }, // Uhrzeit (fix)
			{ "^([0-9]{1,2}):([0-9]{1,2})$", "13:45" }, // Uhrzeit (alternativ)
			{ "^([0-1]?[0-9]|2[0-3])(:([0-5]?[0-9])|())$", "13:45" }, // Zahl (natürlich, klein)
			{ "^[1-5]{0,1}[0-9]$", "12" }, // Datum (alternativ)
			{ "^([0-2]?[0-9]|3[0-1])([.:_ /-]([0]?[0-9]|1[0-2])([.:_ /-]([0-9]{1,2}|[0-9]{4})|[.:_ /-]|())|[.:_ /-]|())$", "12.12.12" }, // Verzeichnispfad
			{ "^/{0,1}([a-zA-Z0-9\\._-]{1,64}/{0,1}){0,127}$", "/home/gentics" }, // Hostname
			{ "^[0-9a-z]([-.]?[0-9a-z:])*$", "www.gentics.com" }, // Text (eindeutig)
			{ "^[a-z0-9]{3,64}$", "text" }, // Postleitzahl
			{ "[A-Z]{0,2}[\\-]{0,1}[0-9]{4,6}", "1010" }, // Telefon/Faxnummer
			{ "^[0-9\\-\\/ \\+\\(\\)]{4,25}$", "06912931" }, // Preis (SN)
			{ "^[0-9]{1,12}.{0,1}[0-9]{0,2}$", "1234" }, // Fileshare
			{ "^\\\\.*$", "\\www." }, // Node Tag
			{ "^[a-zA-Z0-9\\_\\-]{3,255}$", "asdasd" }
		};

		for (int i = 0; i < regexTests.length - 1; i++) {
			
			String pattern = regexTests[i][0];
			String text = regexTests[i][1];

			assertTrue("String \"" + text + "\" matches pattern \"" + pattern + "\"", text.matches(pattern));
		}
		
		LOGGER.debug("finished");
	}
}
