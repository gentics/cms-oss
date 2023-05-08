package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ObjectProperty;

/**
 * Response containing a list of object properties
 */
@XmlRootElement
public class ObjectPropertyListResponse extends AbstractListResponse<ObjectProperty> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1547443153062743935L;
}
