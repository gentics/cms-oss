
package com.gentics.lib.datasource.object.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für objecttype complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="objecttype">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="attributetype" type="{}attributetype" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="excludeversioning" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "objecttype", propOrder = {
    "attributeTypes"
})
public class Objecttype {

    @XmlElement(name = "attributetype")
    protected Attributetype[] attributeTypes;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "integer")
    protected Integer id;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "excludeversioning")
    protected Boolean excludeVersioning;

    /**
     * 
     * @return
     *     array of
     *     {@link Attributetype }
     *     
     */
    public Attributetype[] getAttributeTypes() {
        if (this.attributeTypes == null) {
            return new Attributetype[ 0 ] ;
        }
        Attributetype[] retVal = new Attributetype[this.attributeTypes.length] ;
        System.arraycopy(this.attributeTypes, 0, retVal, 0, this.attributeTypes.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Attributetype }
     *     
     */
    public Attributetype getAttributeTypes(int idx) {
        if (this.attributeTypes == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.attributeTypes[idx];
    }

    public int getAttributeTypesLength() {
        if (this.attributeTypes == null) {
            return  0;
        }
        return this.attributeTypes.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Attributetype }
     *     
     */
    public void setAttributeTypes(Attributetype[] values) {
        if (values == null) {
            this.attributeTypes = null;
            return ;
        }
        int len = values.length;
        this.attributeTypes = ((Attributetype[]) new Attributetype[len] );
        for (int i = 0; (i<len); i ++) {
            this.attributeTypes[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Attributetype }
     *     
     */
    public Attributetype setAttributeTypes(int idx, Attributetype value) {
        return this.attributeTypes[idx] = value;
    }

    public boolean isSetAttributeTypes() {
        return ((this.attributeTypes!= null)&&(this.attributeTypes.length > 0));
    }

    public void unsetAttributeTypes() {
        this.attributeTypes = null;
    }

    /**
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getId() {
        return id;
    }

    /**
     * Legt den Wert der id-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(Integer value) {
        this.id = value;
    }

    public boolean isSetId() {
        return (this.id!= null);
    }

    /**
     * Ruft den Wert der name-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Legt den Wert der name-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    public boolean isSetName() {
        return (this.name!= null);
    }

    /**
     * Ruft den Wert der excludeVersioning-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isExcludeVersioning() {
        if (excludeVersioning == null) {
            return false;
        } else {
            return excludeVersioning;
        }
    }

    /**
     * Legt den Wert der excludeVersioning-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setExcludeVersioning(boolean value) {
        this.excludeVersioning = value;
    }

    public boolean isSetExcludeVersioning() {
        return (this.excludeVersioning!= null);
    }

    public void unsetExcludeVersioning() {
        this.excludeVersioning = null;
    }

}
