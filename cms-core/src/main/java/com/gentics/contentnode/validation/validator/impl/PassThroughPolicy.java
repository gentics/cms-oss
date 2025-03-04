/*
 * @author tobiassteiner
 * @date Jan 5, 2011
 * @version $Id: PassThroughPolicy.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;

@XmlAccessorType(XmlAccessType.NONE)
public class PassThroughPolicy extends Policy {
	@Override
	public PassThroughValidator newValidator(ValidatorFactory factory) throws ValidatorInstantiationException {
		return factory.newPassThroughValidator(this);
	}
}
