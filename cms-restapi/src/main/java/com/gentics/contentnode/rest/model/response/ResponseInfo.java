/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: ResponseInfo.java,v 1.1 2010-04-08 14:33:12 floriangutmann Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import com.webcohesion.enunciate.metadata.DocumentationExample;

/**
 * Response information that contains a response code and a response message. <br /><br />
 * 
 * The response message should not be internationalized.
 * If you want to provide a user friendly message use the message object instead. 
 * 
 * @author floriangutmann
 */
@XmlRootElement
public class ResponseInfo implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6153978606248563639L;

	/**
	 * Response code 
	 */
	private ResponseCode responseCode;
    
	/**
	 * Response message
	 */
	private String responseMessage;

	/**
	 * Name of the property that caused the error
	 */
	private String property;

	/**
	 * Create an instance with {@link ResponseCode#OK} and message
	 * @param message response message
	 * @return instance
	 */
	public static ResponseInfo ok(String message) {
		return new ResponseInfo(ResponseCode.OK, message);
	}

	/**
	 * Empty construtor.
	 * Necessary for JAXB.
	 */
	public ResponseInfo() {}

	/**
	 * Constructor for a ResponseInfo with all parameters.
	 * 
	 * @param responseCode Code for the response
	 * @param responseMessage Message for the response
	 */
	public ResponseInfo(ResponseCode responseCode, String responseMessage) {
		super();
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
	}

	/**
	 * Create an instance
	 * @param responseCode response code
	 * @param responseMessage response message
	 * @param property property name
	 */
	public ResponseInfo(ResponseCode responseCode, String responseMessage, String property) {
		this(responseCode, responseMessage);
		this.property = property;
	}

	/**
	 * Response code
	 * @return response code
	 */
	@DocumentationExample("OK")
	public ResponseCode getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}

	/**
	 * Response message
	 * @return response message
	 */
	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	/**
	 * Property, that caused the request to fail (may be null)
	 * @return property name
	 */
	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}
}
