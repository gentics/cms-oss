/*
 * @author tobiassteiner
 * @date Jan 30, 2011
 * @version $Id: AntiSamy.java,v 1.1.2.1 2011-02-10 13:43:38 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.antisamy;

import java.util.Locale;

import org.owasp.validator.html.scan.Constants;

import com.gentics.contentnode.validation.util.FallbackResourceBundle;

/**
 * This will be the parent for all other locales that have a language part
 * e.g. AntiSamy_de.
 * 
 * @see AbstractAntiSamyValidator which loads resource bundles from here.
 */
public class AntiSamy extends FallbackResourceBundle {
	public AntiSamy() {
		super("AntiSamy", new Locale(Constants.DEFAULT_LOCALE_LANG, Constants.DEFAULT_LOCALE_LOC));
	}
}
