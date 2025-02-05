/*
 * @author norbert
 * @date 09.02.2011
 * @version $Id: Reference.java,v 1.1.2.1.2.4 2011-03-24 09:25:07 johannes2 Exp $
 */
package com.gentics.contentnode.rest.model;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * Enumeration of references to mark, which references shall be filled when returning objects
 */
@XmlEnum(String.class)
public enum Reference {
	TEMPLATE,
	FOLDER,
	LANGUAGEVARIANTS,
	WORKFLOW,
	TAGS,
	GROUPS,
	PAGEVARIANTS,
	TRANSLATIONSTATUS,
	VERSIONS,
	PRIVILEGES,
	PRIVILEGEMAP,
	CONTENT_TAGS,
	OBJECT_TAGS,
	OBJECT_TAGS_VISIBLE,
	TEMPLATE_TAGS,
	TEMPLATE_SOURCE,
	DESCRIPTION,
	USER_LOGIN,
	DISINHERITED_CHANNELS,
	TAG_EDIT_DATA
}
