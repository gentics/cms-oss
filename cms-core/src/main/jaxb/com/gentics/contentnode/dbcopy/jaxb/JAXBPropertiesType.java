
package com.gentics.contentnode.dbcopy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für PropertiesType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="PropertiesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="property" maxOccurs="unbounded">
 *           <complexType>
 *             <simpleContent>
 *               <extension base="<http://www.w3.org/2001/XMLSchema>string">
 *                 <attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *               </extension>
 *             </simpleContent>
 *           </complexType>
 *         </element>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertiesType", propOrder = {
    "propertyList"
})
public class JAXBPropertiesType {

    @XmlElement(name = "property", required = true)
    protected JAXBPropertiesType.Property[] propertyList;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBPropertiesType.Property }
     *     
     */
    public JAXBPropertiesType.Property[] getPropertyList() {
        if (this.propertyList == null) {
            return new JAXBPropertiesType.Property[ 0 ] ;
        }
        JAXBPropertiesType.Property[] retVal = new JAXBPropertiesType.Property[this.propertyList.length] ;
        System.arraycopy(this.propertyList, 0, retVal, 0, this.propertyList.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBPropertiesType.Property }
     *     
     */
    public JAXBPropertiesType.Property getPropertyList(int idx) {
        if (this.propertyList == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.propertyList[idx];
    }

    public int getPropertyListLength() {
        if (this.propertyList == null) {
            return  0;
        }
        return this.propertyList.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBPropertiesType.Property }
     *     
     */
    public void setPropertyList(JAXBPropertiesType.Property[] values) {
        if (values == null) {
            this.propertyList = null;
            return ;
        }
        int len = values.length;
        this.propertyList = ((JAXBPropertiesType.Property[]) new JAXBPropertiesType.Property[len] );
        for (int i = 0; (i<len); i ++) {
            this.propertyList[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBPropertiesType.Property }
     *     
     */
    public JAXBPropertiesType.Property setPropertyList(int idx, JAXBPropertiesType.Property value) {
        return this.propertyList[idx] = value;
    }

    public boolean isSetPropertyList() {
        return ((this.propertyList!= null)&&(this.propertyList.length > 0));
    }

    public void unsetPropertyList() {
        this.propertyList = null;
    }


    /**
     * <p>Java-Klasse für anonymous complex type.</p>
     * 
     * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
     * 
     * <pre>{@code
     * <complexType>
     *   <simpleContent>
     *     <extension base="<http://www.w3.org/2001/XMLSchema>string">
     *       <attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
     *     </extension>
     *   </simpleContent>
     * </complexType>
     * }</pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class Property {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "id", required = true)
        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        @XmlSchemaType(name = "token")
        protected String id;

        /**
         * Ruft den Wert der value-Eigenschaft ab.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getValue() {
            return value;
        }

        /**
         * Legt den Wert der value-Eigenschaft fest.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setValue(String value) {
            this.value = value;
        }

        public boolean isSetValue() {
            return (this.value!= null);
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

    }

}
