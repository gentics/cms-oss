/*
 * @author tobiassteiner
 * @date Jan 1, 2011
 * @version $Id: AbstractAntiSamyValidator.java,v 1.1.2.2 2011-03-07 18:42:01 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.InternalPolicy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.scan.Constants;
import org.owasp.validator.html.scan.MagicSAXFilter;

import com.gentics.contentnode.validation.validator.Validator;
import com.gentics.lib.log.NodeLogger;

/**
 * Some AntiSamy specific utility functions for other Validators that use AntiSamy
 * for DOM validation.
 */
public abstract class AbstractAntiSamyValidator implements Validator {
	private static NodeLogger logger = NodeLogger.getNodeLogger(AbstractAntiSamyValidator.class);

	protected final Policy policy;
	protected final AntiSamyPolicy config;
	protected final AntiSamy antiSamy;
	protected final Locale locale;
    
	public AbstractAntiSamyValidator(AntiSamyPolicy config, Policy policy, Locale locale) {
		this.policy = policy;
		this.config = config;
		this.locale = locale;
		this.antiSamy = new AntiSamy(policy);
	}
    
	protected MagicSAXFilter newAntiSamyFilter(Policy p) {
		MagicSAXFilter saxFilter = new MagicSAXFilter(getAntiSamyBundle());

		saxFilter.reset((InternalPolicy) p);
		return saxFilter;
	}

	protected ResourceBundle getAntiSamyBundle() {
		// AntiSamy defines resource bundles of the form AntiSamy_en_US.
		// To load these, at least the language and country part in the
		// Locale must be set (variant is optional).
		// That is because ResourceBundle.getBundle() falls-back to
		// less-specific bundles, but never tries more specific bundle
		// names (...en_US is more specific than ...en).
		// So we have our own resource bundles of the form AntiSamy_en
		// that each wrap a more specific resource bundle provided by AntiSamy.
		// We need this because GCN Locales generally have no country part
		// (UserLanguage).
		try {
			return ResourceBundle.getBundle("com.gentics.contentnode.validation.util.antisamy.AntiSamy", locale);
		} catch (MissingResourceException e) {
			try {
				// try load the resource bundles provided with AntiSamy.
				// at the time of this writing this is unnecessary because
				// we have our own bundle wrapper for each language provided
				// by AntiSamy (but not for each country, however these aren't
				// relevant for GCN since we have no notion of a language's
				// country).
				// also we have our own catch-all resource bundle that will
				// catch any missing resource exceptions above.
				logger.error(
						"Unable to find AntiSamy i18n resource bundle for locale " + locale + "."
						+ " The package name for the custom resource budles is probably wrong.");
				return ResourceBundle.getBundle("AntiSamy", locale);
			} catch (MissingResourceException e2) {
				// there should always be a resource bundle for the AntiSamy
				// default locale. we have defined this as a bundle wrapper
				// in ...validation.util.antisamy too. 
				return ResourceBundle.getBundle("AntiSamy", new Locale(Constants.DEFAULT_LOCALE_LANG, Constants.DEFAULT_LOCALE_LOC));
			}
		}
	}
}
