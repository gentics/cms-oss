
package com.gentics.lib.formatter.dateformatter;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für dateFormatType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="dateFormatType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence maxOccurs="unbounded" minOccurs="0">
 *         <choice>
 *           <element name="date" type="{}dateOrTimeType"/>
 *           <element name="time" type="{}dateOrTimeType"/>
 *         </choice>
 *       </sequence>
 *       <attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       <attribute name="defaultdate" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="defaulttime" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dateFormatType", propOrder = {
    "dateOrTime"
})
public class JAXBDateFormatType {

    @XmlElementRefs({
        @XmlElementRef(name = "date", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "time", type = JAXBElement.class, required = false)
    })
    protected JAXBElement<JAXBDateOrTimeType> [] dateOrTime;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String id;
    @XmlAttribute(name = "defaultdate")
    protected String defaultdate;
    @XmlAttribute(name = "defaulttime")
    protected String defaulttime;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     
     */
    public JAXBElement<JAXBDateOrTimeType> [] getDateOrTime() {
        if (this.dateOrTime == null) {
            return new JAXBElement[ 0 ] ;
        }
        JAXBElement<JAXBDateOrTimeType> [] retVal = new JAXBElement[this.dateOrTime.length] ;
        System.arraycopy(this.dateOrTime, 0, retVal, 0, this.dateOrTime.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     
     */
    public JAXBElement<JAXBDateOrTimeType> getDateOrTime(int idx) {
        if (this.dateOrTime == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.dateOrTime[idx];
    }

    public int getDateOrTimeLength() {
        if (this.dateOrTime == null) {
            return  0;
        }
        return this.dateOrTime.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     
     */
    public void setDateOrTime(JAXBElement<JAXBDateOrTimeType> [] values) {
        if (values == null) {
            this.dateOrTime = null;
            return ;
        }
        int len = values.length;
        this.dateOrTime = ((JAXBElement<JAXBDateOrTimeType> []) new JAXBElement[len] );
        for (int i = 0; (i<len); i ++) {
            this.dateOrTime[i] = ((JAXBElement<JAXBDateOrTimeType> ) values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     *     
     */
    public JAXBElement<JAXBDateOrTimeType> setDateOrTime(int idx, JAXBElement<JAXBDateOrTimeType> value) {
        return this.dateOrTime[idx] = ((JAXBElement<JAXBDateOrTimeType> ) value);
    }

    public boolean isSetDateOrTime() {
        return ((this.dateOrTime!= null)&&(this.dateOrTime.length > 0));
    }

    public void unsetDateOrTime() {
        this.dateOrTime = null;
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
     * Ruft den Wert der defaultdate-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaultdate() {
        return defaultdate;
    }

    /**
     * Legt den Wert der defaultdate-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaultdate(String value) {
        this.defaultdate = value;
    }

    public boolean isSetDefaultdate() {
        return (this.defaultdate!= null);
    }

    /**
     * Ruft den Wert der defaulttime-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefaulttime() {
        return defaulttime;
    }

    /**
     * Legt den Wert der defaulttime-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefaulttime(String value) {
        this.defaulttime = value;
    }

    public boolean isSetDefaulttime() {
        return (this.defaulttime!= null);
    }

}
