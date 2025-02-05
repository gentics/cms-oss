
package com.gentics.contentnode.dbcopy.jaxb;

import com.gentics.contentnode.dbcopy.Reference;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für referencesType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="referencesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="ref" type="{}referenceType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "referencesType", propOrder = {
    "ref"
})
public class JAXBReferencesType {

    @XmlElement(type = Reference.class)
    protected JAXBReferenceType[] ref;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBReferenceType }
     *     
     */
    public JAXBReferenceType[] getRef() {
        if (this.ref == null) {
            return new JAXBReferenceType[ 0 ] ;
        }
        JAXBReferenceType[] retVal = new Reference[this.ref.length] ;
        System.arraycopy(this.ref, 0, retVal, 0, this.ref.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBReferenceType }
     *     
     */
    public JAXBReferenceType getRef(int idx) {
        if (this.ref == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.ref[idx];
    }

    public int getRefLength() {
        if (this.ref == null) {
            return  0;
        }
        return this.ref.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBReferenceType }
     *     
     */
    public void setRef(JAXBReferenceType[] values) {
        if (values == null) {
            this.ref = null;
            return ;
        }
        int len = values.length;
        this.ref = ((Reference[]) new Reference[len] );
        for (int i = 0; (i<len); i ++) {
            this.ref[i] = ((Reference) values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBReferenceType }
     *     
     */
    public JAXBReferenceType setRef(int idx, JAXBReferenceType value) {
        return this.ref[idx] = ((Reference) value);
    }

    public boolean isSetRef() {
        return ((this.ref!= null)&&(this.ref.length > 0));
    }

    public void unsetRef() {
        this.ref = null;
    }

}
