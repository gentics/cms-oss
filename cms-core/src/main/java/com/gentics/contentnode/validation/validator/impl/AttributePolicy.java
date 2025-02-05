/*
 * @author tobiassteiner
 * @date Jan 9, 2011
 * @version $Id: AttributePolicy.java,v 1.1.2.1 2011-02-10 13:43:29 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;

/**
 * See the policy-map.default.xml bundled with the applicaton.
 */
public class AttributePolicy extends AntiSamyPolicy {
	@XmlAttribute
	protected Boolean ignoreNodeTags = false;
	@XmlElement(required = true)
	protected OccursIn occursIn;
    
	@Override
	public AttributeValidator newValidator(ValidatorFactory factory) throws ValidatorInstantiationException {
		return factory.newAttributeValidator(this);
	}
    
	protected static class OccursIn {
		@XmlAttribute(required = true)
		protected String element;
		@XmlAttribute(required = true)
		protected String attribute;
	}
}
