/*
 * @author tobiassteiner
 * @date Jan 5, 2011
 * @version $Id: AntiSamyPolicy.java,v 1.1.2.2 2011-03-07 18:42:01 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyMapImpl;
import com.gentics.contentnode.validation.validator.ValidatorInstantiationException;

/**
 * See the policy-map.default.xml bundled with the applicaton.
 */
public class AntiSamyPolicy extends Policy {
	@XmlAttribute
	protected String policyFile = null;
	@XmlAttribute
	protected Boolean domMode = false;
	@XmlAttribute
	protected String outerElement = "div";
	@XmlTransient
	protected PolicyMapImpl map;
    
	@Override
	public AbstractAntiSamyValidator newValidator(ValidatorFactory factory) throws ValidatorInstantiationException {
		return factory.newAntiSamyValidator(this);
	}
    
	/**
	 * JAXB callback (will be discovered through introspection).
	 */
	@SuppressWarnings("unused")
	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		// we can't resolve locations now because the PolicyMap needs the URI
		// it was loaded from, and it can only get it if we set it after
		// unmarshalling, so we just store the parent and resolve later.
		map = (PolicyMapImpl) parent;
	}

	/**
	 * @return an {@link InputStream} of the AntiSamy policy file configured for this
	 *   policy, or null if no policy file has been configued. Callers should close
	 *   the stream when they are done reading from it. 
	 */
	public InputStream getPolicyFileAsInputStream() throws MalformedURLException, IOException {
		if (null == policyFile) {
			return null;
		} else {
			return map.getLocationAsStream(policyFile);
		}
	}
    
	public String getPolicyFile() {
		return policyFile;
	}
}
