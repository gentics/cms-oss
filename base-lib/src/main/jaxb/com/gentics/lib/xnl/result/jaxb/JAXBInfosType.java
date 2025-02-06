
package com.gentics.lib.xnl.result.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für infosType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="infosType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="info" type="{}infoType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "infosType", propOrder = {
    "info"
})
public class JAXBInfosType {

    protected JAXBInfoType[] info;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBInfoType }
     *     
     */
    public JAXBInfoType[] getInfo() {
        if (this.info == null) {
            return new JAXBInfoType[ 0 ] ;
        }
        JAXBInfoType[] retVal = new JAXBInfoType[this.info.length] ;
        System.arraycopy(this.info, 0, retVal, 0, this.info.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBInfoType }
     *     
     */
    public JAXBInfoType getInfo(int idx) {
        if (this.info == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.info[idx];
    }

    public int getInfoLength() {
        if (this.info == null) {
            return  0;
        }
        return this.info.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBInfoType }
     *     
     */
    public void setInfo(JAXBInfoType[] values) {
        if (values == null) {
            this.info = null;
            return ;
        }
        int len = values.length;
        this.info = ((JAXBInfoType[]) new JAXBInfoType[len] );
        for (int i = 0; (i<len); i ++) {
            this.info[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBInfoType }
     *     
     */
    public JAXBInfoType setInfo(int idx, JAXBInfoType value) {
        return this.info[idx] = value;
    }

    public boolean isSetInfo() {
        return ((this.info!= null)&&(this.info.length > 0));
    }

    public void unsetInfo() {
        this.info = null;
    }

}
