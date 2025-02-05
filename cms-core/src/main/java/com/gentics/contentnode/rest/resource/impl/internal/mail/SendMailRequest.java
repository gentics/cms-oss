package com.gentics.contentnode.rest.resource.impl.internal.mail;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Request to send an email
 */
@XmlRootElement
public class SendMailRequest implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4977644546669007005L;

	private String to;

	private String from;

	private String subject;

	private String body;

	/**
	 * Get the to: Address
	 * @return the to address
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Set the to: Address
	 * @param to the to to set
	 * @return fluent API
	 */
	public SendMailRequest setTo(String to) {
		this.to = to;
		return this;
	}

	/**
	 * Get the from: Address
	 * @return the from address
	 */
	public String getFrom() {
		return from;
	}

	/**
	 * Set the from: Address
	 * @param from the from to set
	 * @return fluent API
	 */
	public SendMailRequest setFrom(String from) {
		this.from = from;
		return this;
	}

	/**
	 * Get the mail subject
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * Set the mail subject
	 * @param subject the subject to set
	 * @return fluent API
	 */
	public SendMailRequest setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	/**
	 * Get the body
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Set the body
	 * @param body the body to set
	 * @return fluent API
	 */
	public SendMailRequest setBody(String body) {
		this.body = body;
		return this;
	}
}
