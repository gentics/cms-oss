//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.4-hudson-16-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.04.24 at 01:20:39 CEST 
//


package com.gentics.contentnode.dbcopy.jaxb;


/**
 * Java content class for anonymous complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/home/johannes2/workspace_cn/node/node-lib/src/main/xsd/copy_configuration.xsd line 26)
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="table" type="{}tableType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="roottable" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface TablesType {


    /**
     * 
     * @return
     *     array of
     *     {@link com.gentics.contentnode.dbcopy.jaxb.JAXBtableType}
     */
    com.gentics.contentnode.dbcopy.jaxb.JAXBtableType[] getTable();

    /**
     * 
     * @return
     *     one of
     *     {@link com.gentics.contentnode.dbcopy.jaxb.JAXBtableType}
     */
    com.gentics.contentnode.dbcopy.jaxb.JAXBtableType getTable(int idx);

    int getTableLength();

    /**
     * 
     * @param values
     *     allowed objects are
     *     {@link com.gentics.contentnode.dbcopy.jaxb.JAXBtableType}
     */
    void setTable(com.gentics.contentnode.dbcopy.jaxb.JAXBtableType[] values);

    /**
     * 
     * @param value
     *     allowed object is
     *     {@link com.gentics.contentnode.dbcopy.jaxb.JAXBtableType}
     */
    com.gentics.contentnode.dbcopy.jaxb.JAXBtableType setTable(int idx, com.gentics.contentnode.dbcopy.jaxb.JAXBtableType value);

    boolean isSetTable();

    void unsetTable();

    /**
     * Gets the value of the roottable property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getRoottable();

    /**
     * Sets the value of the roottable property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setRoottable(java.lang.String value);

    boolean isSetRoottable();

    void unsetRoottable();

}
