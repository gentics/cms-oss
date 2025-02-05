
package com.gentics.contentnode.dbcopy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für tableType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="tableType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="restrict" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         <element name="modificators" type="{}modificatorsType" minOccurs="0"/>
 *         <element name="references" type="{}referencesType" minOccurs="0"/>
 *         <element name="properties" type="{}PropertiesType" minOccurs="0"/>
 *       </all>
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="id" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="idcol" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="exportable" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       <attribute name="type" default="normal">
 *         <simpleType>
 *           <restriction base="{http://www.w3.org/2001/XMLSchema}token">
 *             <enumeration value="normal"/>
 *             <enumeration value="cross"/>
 *           </restriction>
 *         </simpleType>
 *       </attribute>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tableType", propOrder = {

})
public class JAXBTableType {

    protected String restrict;
    protected JAXBModificatorsType modificators;
    protected JAXBReferencesType references;
    protected JAXBPropertiesType properties;
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String name;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String id;
    @XmlAttribute(name = "idcol")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String idcol;
    @XmlAttribute(name = "exportable")
    protected Boolean exportable;
    @XmlAttribute(name = "type")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String type;

    /**
     * Ruft den Wert der restrict-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRestrict() {
        return restrict;
    }

    /**
     * Legt den Wert der restrict-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRestrict(String value) {
        this.restrict = value;
    }

    public boolean isSetRestrict() {
        return (this.restrict!= null);
    }

    /**
     * Ruft den Wert der modificators-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBModificatorsType }
     *     
     */
    public JAXBModificatorsType getModificators() {
        return modificators;
    }

    /**
     * Legt den Wert der modificators-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBModificatorsType }
     *     
     */
    public void setModificators(JAXBModificatorsType value) {
        this.modificators = value;
    }

    public boolean isSetModificators() {
        return (this.modificators!= null);
    }

    /**
     * Ruft den Wert der references-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBReferencesType }
     *     
     */
    public JAXBReferencesType getReferences() {
        return references;
    }

    /**
     * Legt den Wert der references-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBReferencesType }
     *     
     */
    public void setReferences(JAXBReferencesType value) {
        this.references = value;
    }

    public boolean isSetReferences() {
        return (this.references!= null);
    }

    /**
     * Ruft den Wert der properties-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBPropertiesType }
     *     
     */
    public JAXBPropertiesType getProperties() {
        return properties;
    }

    /**
     * Legt den Wert der properties-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBPropertiesType }
     *     
     */
    public void setProperties(JAXBPropertiesType value) {
        this.properties = value;
    }

    public boolean isSetProperties() {
        return (this.properties!= null);
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
     * Ruft den Wert der id-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
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
    public void setId(String value) {
        this.id = value;
    }

    public boolean isSetId() {
        return (this.id!= null);
    }

    /**
     * Ruft den Wert der idcol-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdcol() {
        return idcol;
    }

    /**
     * Legt den Wert der idcol-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdcol(String value) {
        this.idcol = value;
    }

    public boolean isSetIdcol() {
        return (this.idcol!= null);
    }

    /**
     * Ruft den Wert der exportable-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isExportable() {
        if (exportable == null) {
            return true;
        } else {
            return exportable;
        }
    }

    /**
     * Legt den Wert der exportable-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setExportable(boolean value) {
        this.exportable = value;
    }

    public boolean isSetExportable() {
        return (this.exportable!= null);
    }

    public void unsetExportable() {
        this.exportable = null;
    }

    /**
     * Ruft den Wert der type-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        if (type == null) {
            return "normal";
        } else {
            return type;
        }
    }

    /**
     * Legt den Wert der type-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    public boolean isSetType() {
        return (this.type!= null);
    }

}
