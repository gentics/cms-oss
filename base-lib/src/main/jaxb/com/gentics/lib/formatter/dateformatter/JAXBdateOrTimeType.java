//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.4-hudson-16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.04.25 at 01:39:22 CEST 
//


package com.gentics.lib.formatter.dateformatter;


/**
 * Java content class for dateOrTimeType complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/home/johannes2/workspace_cn/node/node-lib/src/main/xsd/date-format.xsd line 48)
 * <p>
 * <pre>
 * &lt;complexType name="dateOrTimeType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="language" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface JAXBdateOrTimeType {


    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getValue();

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setValue(java.lang.String value);

    boolean isSetValue();

    void unsetValue();

    /**
     * Gets the value of the language property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getLanguage();

    /**
     * Sets the value of the language property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setLanguage(java.lang.String value);

    boolean isSetLanguage();

    void unsetLanguage();

}
