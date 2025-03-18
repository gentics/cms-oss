/*
 * @author floriangutmann
 * @date Apr 2, 2010
 * @version $Id: Message.java,v 1.4 2010-05-05 15:38:27 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import com.gentics.contentnode.rest.model.User;

/**
 * Represents a message that can be displayed in the MessageLine.
 * 
 * @author floriangutmann
 */
@XmlRootElement
public class Message implements Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2635145623440334358L;

	/**
	 * Id of the message (may be null for messages not persisted)
	 */
	private Integer id;

	/**
	 * The name of the field in a REST model this message is about.
	 */
	private String fieldName;

	/**
	 * Text of the message
	 */
	private String message;
    
	/**
	 * Type of the message (severity)
	 */
	private Type type;
    
	/**
	 * Timestamp when the message was created
	 */
	private long timestamp;
    
	/**
	 * URL to the image that is displayed aside the message
	 */
	private String image;

	/**
	 * Sender of the message (if the message was sent by another user)
	 */
	private User sender;

	/**
	 * This is a boolean variable that indicates if the message is instant.
	 */
	@JsonProperty("isInstantMessage")
	private boolean isInstantMessage;

	/**
	 * Default constructor needed for JAXB
	 */
	public Message() {}
    
	/**
	 * Constructor with all parameters.
	 * 
	 * @param type The {@link Type} of the message.
	 * @param fieldName The name of the REST model field this message is about.
	 * @param message The text of the message.
	 * @param image The URL of the image that should be displayed aside the message.
	 * @param timestamp The timestamp of the message.
	 */
	public Message(Type type, String fieldName, String message, String image, long timestamp) {
		this.fieldName = fieldName;
		this.message = message;
		this.type = type;
		this.image = image;
		this.timestamp = timestamp;
	}

	/**
	 * Constructor for a message for a specific REST model field.
	 *
	 * The {@link #image} will be <code>null</code> and the {@link #timestamp}
	 * will be set to the current time.
	 *
	 * @param type The type of the message.
	 * @param fieldName The name of the REST model field this message is about.
	 * @param message The text of the message.
	 */
	public Message(Type type, String fieldName, String message) {
		this(type, fieldName, message, null, System.currentTimeMillis());
	}

	/**
	 * Constructor for a Message with an image and a timestamp.
	 * 
	 * @param type Type of the message
	 * @param message Text of the message
	 * @param image URL of the image that should be displayed aside the message
	 */
	public Message(Type type, String message, String image, long timestamp) {
		this(type, null, message, image, timestamp);
	}
    
	/**
	 * Simple constructor for a message.
	 * The timestamp of the message will be set to the actual time.
	 * Image of the message will be null.
	 * 
	 * @param type Type of the message
	 * @param message Text of the message
	 */
	public Message(Type type, String message) {
		this(type, null, message, null, System.currentTimeMillis());
	}

	/**
	 * The name of the REST model field this message is about.
	 *
	 * @return The name of the REST model field this message is about,
	 *		or <code>null</code> if this message is not about a specific
	 *		REST model field.
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Set the name of the REST model field this message is about.
	 * @param fieldName Set the name of the REST model field this message is
	 *		about (can be null).
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	/**
	 * Get the sender of the message
	 * @return message sender
	 */
	public User getSender() {
		return sender;
	}

	/**
	 * Set the sender of the message
	 * @param sender message sender
	 */
	public void setSender(User sender) {
		this.sender = sender;
	}

	/**
	 * Set the message id
	 * @param id id of the message
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Get the message id
	 * @return id of the message
	 */
	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("%s: %s", type, message);
	}


	/**
	 * Helper function to determine if a message is an instant message.
	 * If the messages is expired, it is not considered as an instant message
	 * @return returns true if the message is expired, false otherwise
	 */
	public boolean isExpired(int instantTimeMinutes) {
		// The timestamp is in seconds, hence the current timestamp
		// and the instant time are converted to seconds as well
		return (System.currentTimeMillis() / 1000) > this.timestamp + (instantTimeMinutes * 60L);
	}

	/**
	 * Sets the value of the isInstantMessage variable.
	 * @param instantMessage a boolean value that represents if the message is instant
	 */
	public void setInstantMessage(boolean instantMessage) {
		this.isInstantMessage = instantMessage;
	}

	/**
	 * Severity for a message
	 */
	@XmlType(name = "MessageType")
	public enum Type {

		/**
		 * Used for messages which display serious errors that stop the user to work.
		 */
		CRITICAL,
		/**
		 * Used for messages that inform the user about important situations 
		 * or necessary user interactions.
		 */
		WARNING,
		/**
		 * Used for messages that inform the user about application state.
		 */
		INFO,
		/**
		 * Used for messages that tell the user about success of a certain action.
		 */
		SUCCESS,
		/**
		 * Used for neutral messages like chat messages.
		 */
		NEUTRAL
	}
}
