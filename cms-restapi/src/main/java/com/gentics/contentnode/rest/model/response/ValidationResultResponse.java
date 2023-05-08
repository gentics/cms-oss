/*
 * @author tobiassteiner
 * @date Jan 18, 2011
 * @version $Id: ValidationResultResponse.java,v 1.1.2.3 2011-02-10 13:43:41 tobiassteiner Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "validationResult")
@XmlType(propOrder = { "messages", "cleanMarkup", "formattedError" })
public class ValidationResultResponse {
	@XmlElementWrapper(name = "messages")
	@XmlElement(name = "message")
	public List<ValidationMessageResponse> messages = new ArrayList<ValidationMessageResponse>();
	@XmlElement
	public String cleanMarkup;
	@XmlElement
	public String formattedError;
    
	public ValidationResultResponse() {}

	@XmlType(propOrder = { "message", "fatal" })
	public static class ValidationMessageResponse {
		@XmlElement
		public String message;
		public Boolean fatal;
		public ValidationMessageResponse() {}
	}
}
