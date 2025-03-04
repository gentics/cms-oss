/*
 * @author floriangutmann
 * @date Apr 6, 2010
 * @version $Id: ResponseCode.java,v 1.1.6.1 2011-03-15 10:36:03 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Response codes that are returned to the client as answers for requests.
 * 
 * @author floriangutmann
 */
@XmlEnum(String.class)
public enum ResponseCode {

	/**
	 * Used if everything went ok (eg. page successfully saved)
	 */
	OK, /**
	 * Used if the request was made with invalid (insufficient) data
	 */ INVALIDDATA, /**
	 * Used if insufficient permissions permitted to requested action
	 */ PERMISSION, /**
	 * Used if the maintenancemode was enabled
	 */ MAINTENANCEMODE, /**
	 * Used if the requested object was not found
	 */ NOTFOUND, /**
	 * Used if something unexpected went wrong (eg. page couldn't be successfully saved due to a database error)
	 */ FAILURE, /**
	 * Used if a request was made with missing or invalid session identification
	 */ AUTHREQUIRED, /**
	 * Used if a request was made to a resource that required additional licensing
	 */ NOTLICENSED,

	 /**
	  * The requested update could be done, because the object was locked by another user
	  */
	 LOCKED
}
