/*
 * @author tobiassteiner
 * @date Jan 5, 2011
 * @version $Id: XmlProperties.java,v 1.1.2.1 2011-02-10 13:43:28 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Implements the Xml Property syntax with JAXB annotations.
 * 
 * More info about the Syntax at:
 * http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html
 */
@XmlAccessorType(XmlAccessType.NONE)
public class XmlProperties {
	@XmlElement(name = "entry")
	protected List<Entry> entries;
    
	public java.util.Properties getProperties() {
		java.util.Properties props = new java.util.Properties();

		for (Entry entry : entries) {
			props.setProperty(entry.key, entry.value);
		}
		return props;
	}
    
	protected static class Entry {
		@XmlAttribute
		protected String key;
		@XmlValue
		protected String value;
	}
}
