/*
 * @author tobiassteiner
 * @date Jan 5, 2011
 * @version $Id: Policy.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

import java.net.URI;
import java.net.URISyntaxException;

import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.validator.Validator;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;
import com.gentics.lib.i18n.CNI18nString;

public abstract class Policy extends PolicyMap.PolicyModel {

	/**
	 * If a user configures a policy for a tag-part, this URI will be used
	 * to persist the choice.
	 * @return the global identifier URI which will be used by the system
	 *   to refer to this policy henceforth.
	 * @throws URISyntaxException
	 */
	public URI getURI() {
		return uri;
	}
    
	/**
	 * If this setting is activated, tags of the form
	 *   &lt;node tag.name>
	 * will be converted to
	 *   &lt;node tag="tag.name"/>
	 * @return whether node tags should be converted to XML form with reversed
	 *   tag-name attribute.
	 */
	public boolean getConvertNodeTags() {
		return convertNodeTags;
	}
    
	/**
	 * @return the name to be displayed as the visual representation for this
	 *   policy before going through internationalization. May be null.
	 * @see #toString()
	 */
	public String getDisplayName() {
		return displayName;
	}
    
	/**
	 * @return the effective name is the internationalized version of
	 *   {@link #getDisplayName()}, or the XML ID if no display name was
	 *   provided.
	 */
	public String getEffectiveName() {
		if (null != getDisplayName()) {
			return new CNI18nString(getDisplayName()).toString();
		} else {
			return id;
		}
	}

	/**
	 * Used to map a policy to the apropriate validator.
	 * @return a new Validator for this policy, possibly by calling the
	 * appropriate constructor method of the given factory. 
	 * @param factory a factory which should implement constructors for
	 *   all validators.
	 * @throws ValidatorInstantiationException
	 */
	public abstract Validator newValidator(ValidatorFactory factory) throws ValidatorInstantiationException;
    
	@Override
	public String toString() {
		return getEffectiveName();
	}
    
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Policy) {
			return uri.equals(((Policy) obj).uri);
		} else {
			return false;
		}
	}
    
	@Override
	public int hashCode() {
		return uri.hashCode();
	}
}
