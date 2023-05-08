/*
 * @author tobiassteiner
 * @date Jan 9, 2011
 * @version $Id: AttributeValidator.java,v 1.1.2.2 2011-03-07 18:42:01 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.validator.impl;

import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.owasp.validator.html.Policy;
import org.owasp.validator.html.scan.MagicSAXFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gentics.contentnode.validation.util.ErrorMessagesView;
import com.gentics.contentnode.validation.util.NodeTagUtils;
import com.gentics.contentnode.validation.util.StringViewCollection;
import com.gentics.contentnode.validation.util.sax.xerces.SAX2DOMHandler;
import com.gentics.contentnode.validation.util.sax.xerces.XercesUtils;
import com.gentics.contentnode.validation.validator.ValidationException;

public class AttributeValidator extends AbstractAntiSamyValidator {

	protected final AttributePolicy config;
    
	public AttributeValidator(AttributePolicy config, Policy policyFile, Locale locale) {
		super(config, policyFile, locale);
		this.config = config;
		if (config.domMode) {
			throw new IllegalArgumentException("domMode not supported by the attribute policy");
		}
	}
    
	public AttributeValidationResult validate(String markup) throws ValidationException {
		// config.convertNodeTags is ignored - doesn't make sense to convert node tags
		// in attributes if they are not going to be parsed.
        
		if (config.ignoreNodeTags) {
			markup = NodeTagUtils.textifyNodeTags(markup);
		}
        
		Document document;

		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new ValidationException(e);
		}

		String attrName = config.occursIn.attribute;
        
		Element occursIn = document.createElementNS(null, config.occursIn.element);

		occursIn.setAttribute(attrName, markup);
		Element container = document.createElementNS(null, "container");

		SAX2DOMHandler assembler = new SAX2DOMHandler(container);
		MagicSAXFilter antiSamyFilter = newAntiSamyFilter(policy);

		antiSamyFilter.setDocumentHandler(assembler);

		XercesUtils.generateSAXEvents(occursIn, antiSamyFilter);
        
		// the assembler has its own root, which may be another element or
		// possibly a document or document fragment. since we only generate
		// events for an element, we only take out the generated element.
		Node assembled = assembler.getRoot().getFirstChild(); 
		String validatedAttr = ((Element) assembled).getAttribute(attrName);

		if (null == validatedAttr) {
			validatedAttr = "";
		}
        
		ArrayList<Object> errors = new ArrayList<Object>();

		errors.addAll(new StringViewCollection(antiSamyFilter.getErrorMessages()));
        
		if (config.ignoreNodeTags) {
			validatedAttr = NodeTagUtils.untextifyNodeTags(validatedAttr);
		}
        
		return new AttributeValidationResult(new ErrorMessagesView(errors), validatedAttr);
	}
}
