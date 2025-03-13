
package com.gentics.lib.formatter.dateformatter;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für dateFormatsType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="dateFormatsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="date-format" type="{}dateFormatType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="default" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dateFormatsType", propOrder = {
    "dateFormat"
})
public class JAXBDateFormatsType {

    @XmlElement(name = "date-format", type = DateFormatConfig.class)
    protected JAXBDateFormatType[] dateFormat;
    @XmlAttribute(name = "default", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String _default;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBDateFormatType }
     *     
     */
    public JAXBDateFormatType[] getDateFormat() {
        if (this.dateFormat == null) {
            return new JAXBDateFormatType[ 0 ] ;
        }
        JAXBDateFormatType[] retVal = new DateFormatConfig[this.dateFormat.length] ;
        System.arraycopy(this.dateFormat, 0, retVal, 0, this.dateFormat.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBDateFormatType }
     *     
     */
    public JAXBDateFormatType getDateFormat(int idx) {
        if (this.dateFormat == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.dateFormat[idx];
    }

    public int getDateFormatLength() {
        if (this.dateFormat == null) {
            return  0;
        }
        return this.dateFormat.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBDateFormatType }
     *     
     */
    public void setDateFormat(JAXBDateFormatType[] values) {
        if (values == null) {
            this.dateFormat = null;
            return ;
        }
        int len = values.length;
        this.dateFormat = ((DateFormatConfig[]) new DateFormatConfig[len] );
        for (int i = 0; (i<len); i ++) {
            this.dateFormat[i] = ((DateFormatConfig) values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBDateFormatType }
     *     
     */
    public JAXBDateFormatType setDateFormat(int idx, JAXBDateFormatType value) {
        return this.dateFormat[idx] = ((DateFormatConfig) value);
    }

    public boolean isSetDateFormat() {
        return ((this.dateFormat!= null)&&(this.dateFormat.length > 0));
    }

    public void unsetDateFormat() {
        this.dateFormat = null;
    }

    /**
     * Ruft den Wert der default-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDefault() {
        return _default;
    }

    /**
     * Legt den Wert der default-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDefault(String value) {
        this._default = value;
    }

    public boolean isSetDefault() {
        return (this._default!= null);
    }

}
