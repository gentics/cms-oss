
package com.gentics.contentnode.dbcopy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für referenceType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="referenceType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="parameter" maxOccurs="unbounded" minOccurs="0">
 *           <complexType>
 *             <simpleContent>
 *               <extension base="<http://www.w3.org/2001/XMLSchema>string">
 *                 <attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *               </extension>
 *             </simpleContent>
 *           </complexType>
 *         </element>
 *       </sequence>
 *       <attribute name="target" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="col" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="class" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="deepcopy" type="{}deepcopytype" default="true" />
 *       <attribute name="foreigndeepcopy" type="{}deepcopytype" default="true" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "referenceType", propOrder = {
    "parameter"
})
public class JAXBReferenceType {

    protected JAXBReferenceType.Parameter[] parameter;
    @XmlAttribute(name = "target")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String target;
    @XmlAttribute(name = "col")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String col;
    @XmlAttribute(name = "class")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String implementationClass;
    @XmlAttribute(name = "deepcopy")
    protected JAXBDeepcopytype deepcopy;
    @XmlAttribute(name = "foreigndeepcopy")
    protected JAXBDeepcopytype foreigndeepcopy;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBReferenceType.Parameter }
     *     
     */
    public JAXBReferenceType.Parameter[] getParameter() {
        if (this.parameter == null) {
            return new JAXBReferenceType.Parameter[ 0 ] ;
        }
        JAXBReferenceType.Parameter[] retVal = new JAXBReferenceType.Parameter[this.parameter.length] ;
        System.arraycopy(this.parameter, 0, retVal, 0, this.parameter.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBReferenceType.Parameter }
     *     
     */
    public JAXBReferenceType.Parameter getParameter(int idx) {
        if (this.parameter == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.parameter[idx];
    }

    public int getParameterLength() {
        if (this.parameter == null) {
            return  0;
        }
        return this.parameter.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBReferenceType.Parameter }
     *     
     */
    public void setParameter(JAXBReferenceType.Parameter[] values) {
        if (values == null) {
            this.parameter = null;
            return ;
        }
        int len = values.length;
        this.parameter = ((JAXBReferenceType.Parameter[]) new JAXBReferenceType.Parameter[len] );
        for (int i = 0; (i<len); i ++) {
            this.parameter[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBReferenceType.Parameter }
     *     
     */
    public JAXBReferenceType.Parameter setParameter(int idx, JAXBReferenceType.Parameter value) {
        return this.parameter[idx] = value;
    }

    public boolean isSetParameter() {
        return ((this.parameter!= null)&&(this.parameter.length > 0));
    }

    public void unsetParameter() {
        this.parameter = null;
    }

    /**
     * Ruft den Wert der target-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTarget() {
        return target;
    }

    /**
     * Legt den Wert der target-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTarget(String value) {
        this.target = value;
    }

    public boolean isSetTarget() {
        return (this.target!= null);
    }

    /**
     * Ruft den Wert der col-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCol() {
        return col;
    }

    /**
     * Legt den Wert der col-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCol(String value) {
        this.col = value;
    }

    public boolean isSetCol() {
        return (this.col!= null);
    }

    /**
     * Ruft den Wert der implementationClass-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImplementationClass() {
        return implementationClass;
    }

    /**
     * Legt den Wert der implementationClass-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImplementationClass(String value) {
        this.implementationClass = value;
    }

    public boolean isSetImplementationClass() {
        return (this.implementationClass!= null);
    }

    /**
     * Ruft den Wert der deepcopy-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBDeepcopytype }
     *     
     */
    public JAXBDeepcopytype getDeepcopy() {
        if (deepcopy == null) {
            return JAXBDeepcopytype.TRUE;
        } else {
            return deepcopy;
        }
    }

    /**
     * Legt den Wert der deepcopy-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBDeepcopytype }
     *     
     */
    public void setDeepcopy(JAXBDeepcopytype value) {
        this.deepcopy = value;
    }

    public boolean isSetDeepcopy() {
        return (this.deepcopy!= null);
    }

    /**
     * Ruft den Wert der foreigndeepcopy-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBDeepcopytype }
     *     
     */
    public JAXBDeepcopytype getForeigndeepcopy() {
        if (foreigndeepcopy == null) {
            return JAXBDeepcopytype.TRUE;
        } else {
            return foreigndeepcopy;
        }
    }

    /**
     * Legt den Wert der foreigndeepcopy-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBDeepcopytype }
     *     
     */
    public void setForeigndeepcopy(JAXBDeepcopytype value) {
        this.foreigndeepcopy = value;
    }

    public boolean isSetForeigndeepcopy() {
        return (this.foreigndeepcopy!= null);
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
    public static class Parameter {

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
