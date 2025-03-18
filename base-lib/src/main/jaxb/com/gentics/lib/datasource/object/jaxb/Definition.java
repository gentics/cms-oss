
package com.gentics.lib.datasource.object.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für anonymous complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="objecttype" type="{}objecttype" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "objectTypes"
})
@XmlRootElement(name = "definition")
public class Definition {

    @XmlElement(name = "objecttype")
    protected Objecttype[] objectTypes;

    /**
     * 
     * @return
     *     array of
     *     {@link Objecttype }
     *     
     */
    public Objecttype[] getObjectTypes() {
        if (this.objectTypes == null) {
            return new Objecttype[ 0 ] ;
        }
        Objecttype[] retVal = new Objecttype[this.objectTypes.length] ;
        System.arraycopy(this.objectTypes, 0, retVal, 0, this.objectTypes.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link Objecttype }
     *     
     */
    public Objecttype getObjectTypes(int idx) {
        if (this.objectTypes == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.objectTypes[idx];
    }

    public int getObjectTypesLength() {
        if (this.objectTypes == null) {
            return  0;
        }
        return this.objectTypes.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link Objecttype }
     *     
     */
    public void setObjectTypes(Objecttype[] values) {
        if (values == null) {
            this.objectTypes = null;
            return ;
        }
        int len = values.length;
        this.objectTypes = ((Objecttype[]) new Objecttype[len] );
        for (int i = 0; (i<len); i ++) {
            this.objectTypes[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link Objecttype }
     *     
     */
    public Objecttype setObjectTypes(int idx, Objecttype value) {
        return this.objectTypes[idx] = value;
    }

    public boolean isSetObjectTypes() {
        return ((this.objectTypes!= null)&&(this.objectTypes.length > 0));
    }

    public void unsetObjectTypes() {
        this.objectTypes = null;
    }

}
