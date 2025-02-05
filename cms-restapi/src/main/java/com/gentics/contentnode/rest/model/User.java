/*
 * @author norbert
 * @date 26.04.2010
 * @version $Id: User.java,v 1.1.4.1.2.1 2011-03-18 10:24:14 norbert Exp $
 */
package com.gentics.contentnode.rest.model;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * User object, representing a SystemUser in GCN
 * @author norbert
 */
@XmlRootElement
public class User implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1858349119135317984L;

	/**
	 * id of the User object
	 */
	private Integer id;

	/**
	 * Firstname of the User object
	 */
	private String firstName;

	/**
	 * Lastname of the User object
	 */
	private String lastName;

	/**
	 * Description of the User object
	 */
	private String description;

	/**
	 * Password of the user
	 */
	private String password;

	/**
	 * eMail of the User object
	 */
	private String email;

	/**
	 * login name of the User object
	 */
	private String login;

	/**
	 * Groups of the user
	 */
	private List<Group> groups;

	/**
	 * Constructor of the User object
	 */
	public User() {}

	/**
	 * User ID
	 * @return the identifier
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Firstname
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Lastname
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * eMail Adress
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Set ID
	 * @param identifier the identifier to set
	 * @return fluent API
	 */
	public User setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * Set firstname
	 * @param firstName the firstName to set
	 * @return fluent API
	 */
	public User setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	/**
	 * Set lastname
	 * @param lastName the lastName to set
	 * @return fluent API
	 */
	public User setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	/**
	 * Set description
	 * @param description the description to set
	 * @return fluent API
	 */
	public User setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Set email
	 * @param email the email to set
	 * @return fluent API
	 */
	public User setEmail(String email) {
		this.email = email;
		return this;
	}

	/**
	 * Groups the user is member of
	 * @return the groups
	 */
	public List<Group> getGroups() {
		return groups;
	}

	/**
	 * Set groups
	 * @param groups the groups to set
	 * @return fluent API
	 */
	public User setGroups(List<Group> groups) {
		this.groups = groups;
		return this;
	}

	/**
	 * Login name
	 * @return the login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Set login
	 * @param login the login to set
	 * @return fluent API
	 */
	public User setLogin(String login) {
		this.login = login;
		return this;
	}

	/**
	 * Return the plaintext password.
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the plaintext user password.
	 * 
	 * @param password
	 * @return fluent API
	 */
	public User setPassword(String password) {
		this.password = password;
		return this;
	}

	@Override
	public String toString() {
		return String.format("User %s (%d)", login, id);
	}
}
