
package com.gentics.lib.xnl.result.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für dependenciesType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="dependenciesType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="dependency" type="{}dependencyType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dependenciesType", propOrder = {
    "dependency"
})
public class JAXBDependenciesType {

    protected JAXBDependencyType[] dependency;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBDependencyType }
     *     
     */
    public JAXBDependencyType[] getDependency() {
        if (this.dependency == null) {
            return new JAXBDependencyType[ 0 ] ;
        }
        JAXBDependencyType[] retVal = new JAXBDependencyType[this.dependency.length] ;
        System.arraycopy(this.dependency, 0, retVal, 0, this.dependency.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBDependencyType }
     *     
     */
    public JAXBDependencyType getDependency(int idx) {
        if (this.dependency == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.dependency[idx];
    }

    public int getDependencyLength() {
        if (this.dependency == null) {
            return  0;
        }
        return this.dependency.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBDependencyType }
     *     
     */
    public void setDependency(JAXBDependencyType[] values) {
        if (values == null) {
            this.dependency = null;
            return ;
        }
        int len = values.length;
        this.dependency = ((JAXBDependencyType[]) new JAXBDependencyType[len] );
        for (int i = 0; (i<len); i ++) {
            this.dependency[i] = values[i];
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBDependencyType }
     *     
     */
    public JAXBDependencyType setDependency(int idx, JAXBDependencyType value) {
        return this.dependency[idx] = value;
    }

    public boolean isSetDependency() {
        return ((this.dependency!= null)&&(this.dependency.length > 0));
    }

    public void unsetDependency() {
        this.dependency = null;
    }

}
