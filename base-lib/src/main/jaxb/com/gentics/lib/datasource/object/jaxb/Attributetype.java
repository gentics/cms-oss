
package com.gentics.lib.datasource.object.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für attributetype complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="attributetype">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="foreignlinkattribute" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="foreignlinkattributerule" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="quickname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </all>
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="optimized" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       <attribute name="multivalue" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       <attribute name="linkedobjecttype" type="{http://www.w3.org/2001/XMLSchema}integer" default="0" />
 *       <attribute name="attributetype" use="required" type="{}attributetypedef" />
 *       <attribute name="excludeVersioning" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       <attribute name="filesystem" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributetype", propOrder = {

})
public class Attributetype {

    @XmlElement(name = "foreignlinkattribute")
    protected String foreignLinkAttribute;
    @XmlElement(name = "foreignlinkattributerule")
    protected String foreignLinkAttributeRule;
    @XmlElement(name = "quickname")
    protected String quickName;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "optimized")
    protected Boolean optimized;
    @XmlAttribute(name = "multivalue")
    protected Boolean multivalue;
    @XmlAttribute(name = "linkedobjecttype")
    @XmlJavaTypeAdapter(Adapter1 .class)
    @XmlSchemaType(name = "integer")
    protected Integer linkedobjecttype;
    @XmlAttribute(name = "attributetype", required = true)
    @XmlJavaTypeAdapter(Adapter1 .class)
    protected Integer attributetype;
    @XmlAttribute(name = "excludeVersioning")
    protected Boolean excludeVersioning;
    @XmlAttribute(name = "filesystem")
    protected Boolean filesystem;

    /**
     * Ruft den Wert der foreignLinkAttribute-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForeignLinkAttribute() {
        return foreignLinkAttribute;
    }

    /**
     * Legt den Wert der foreignLinkAttribute-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForeignLinkAttribute(String value) {
        this.foreignLinkAttribute = value;
    }

    public boolean isSetForeignLinkAttribute() {
        return (this.foreignLinkAttribute!= null);
    }

    /**
     * Ruft den Wert der foreignLinkAttributeRule-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getForeignLinkAttributeRule() {
        return foreignLinkAttributeRule;
    }

    /**
     * Legt den Wert der foreignLinkAttributeRule-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setForeignLinkAttributeRule(String value) {
        this.foreignLinkAttributeRule = value;
    }

    public boolean isSetForeignLinkAttributeRule() {
        return (this.foreignLinkAttributeRule!= null);
    }

    /**
     * Ruft den Wert der quickName-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQuickName() {
        return quickName;
    }

    /**
     * Legt den Wert der quickName-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQuickName(String value) {
        this.quickName = value;
    }

    public boolean isSetQuickName() {
        return (this.quickName!= null);
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
     * Ruft den Wert der optimized-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isOptimized() {
        if (optimized == null) {
            return false;
        } else {
            return optimized;
        }
    }

    /**
     * Legt den Wert der optimized-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOptimized(boolean value) {
        this.optimized = value;
    }

    public boolean isSetOptimized() {
        return (this.optimized!= null);
    }

    public void unsetOptimized() {
        this.optimized = null;
    }

    /**
     * Ruft den Wert der multivalue-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isMultivalue() {
        if (multivalue == null) {
            return false;
        } else {
            return multivalue;
        }
    }

    /**
     * Legt den Wert der multivalue-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMultivalue(boolean value) {
        this.multivalue = value;
    }

    public boolean isSetMultivalue() {
        return (this.multivalue!= null);
    }

    public void unsetMultivalue() {
        this.multivalue = null;
    }

    /**
     * Ruft den Wert der linkedobjecttype-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public int getLinkedobjecttype() {
        if (linkedobjecttype == null) {
            return new Adapter1().unmarshal("0");
        } else {
            return linkedobjecttype;
        }
    }

    /**
     * Legt den Wert der linkedobjecttype-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinkedobjecttype(Integer value) {
        this.linkedobjecttype = value;
    }

    public boolean isSetLinkedobjecttype() {
        return (this.linkedobjecttype!= null);
    }

    /**
     * Ruft den Wert der attributetype-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getAttributetype() {
        return attributetype;
    }

    /**
     * Legt den Wert der attributetype-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttributetype(Integer value) {
        this.attributetype = value;
    }

    public boolean isSetAttributetype() {
        return (this.attributetype!= null);
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

    /**
     * Ruft den Wert der filesystem-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isFilesystem() {
        if (filesystem == null) {
            return false;
        } else {
            return filesystem;
        }
    }

    /**
     * Legt den Wert der filesystem-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFilesystem(boolean value) {
        this.filesystem = value;
    }

    public boolean isSetFilesystem() {
        return (this.filesystem!= null);
    }

    public void unsetFilesystem() {
        this.filesystem = null;
    }

}
