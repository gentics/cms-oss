
package com.gentics.contentnode.dbcopy.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für modificatorsType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="modificatorsType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="modificator" type="{http://www.w3.org/2001/XMLSchema}token" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "modificatorsType", propOrder = {
    "modificator"
})
public class JAXBModificatorsType {

    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String[] modificator;

    /**
     * 
     * @return
     *     array of
     *     {@link String }
     *     
     */
    public String[] getModificator() {
        if (this.modificator == null) {
            return new String[ 0 ] ;
        }
        String[] retVal = new String[this.modificator.length] ;
        System.arraycopy(this.modificator, 0, retVal, 0, this.modificator.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link String }
     *     
     */
    public String getModificator(int idx) {
        if (this.modificator == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.modificator[idx];
    }

    public int getModificatorLength() {
        if (this.modificator == null) {
            return  0;
        }
        return this.modificator.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link String }
     *     
     */
    public void setModificator(String[] values) {
        if (values == null) {
            this.modificator = null;
            return ;
        }
        int len = values.length;
        this.modificator = ((String[]) new String[len] );
        for (int i = 0; (i<len); i ++) {
            this.modificator[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public String setModificator(int idx, String value) {
        return this.modificator[idx] = value;
    }

    public boolean isSetModificator() {
        return ((this.modificator!= null)&&(this.modificator.length > 0));
    }

    public void unsetModificator() {
        this.modificator = null;
    }

}
