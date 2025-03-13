
package com.gentics.lib.xnl.result.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für resultType complex type.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <complexType name="resultType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <all>
 *         <element name="dependencies" type="{}dependenciesType"/>
 *         <element name="infos" type="{}infosType"/>
 *         <element name="returnvalue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       </all>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resultType", propOrder = {

})
public class JAXBResultType {

    @XmlElement(required = true)
    protected JAXBDependenciesType dependencies;
    @XmlElement(required = true)
    protected JAXBInfosType infos;
    @XmlElement(required = true)
    protected String returnvalue;

    /**
     * Ruft den Wert der dependencies-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBDependenciesType }
     *     
     */
    public JAXBDependenciesType getDependencies() {
        return dependencies;
    }

    /**
     * Legt den Wert der dependencies-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBDependenciesType }
     *     
     */
    public void setDependencies(JAXBDependenciesType value) {
        this.dependencies = value;
    }

    public boolean isSetDependencies() {
        return (this.dependencies!= null);
    }

    /**
     * Ruft den Wert der infos-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBInfosType }
     *     
     */
    public JAXBInfosType getInfos() {
        return infos;
    }

    /**
     * Legt den Wert der infos-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBInfosType }
     *     
     */
    public void setInfos(JAXBInfosType value) {
        this.infos = value;
    }

    public boolean isSetInfos() {
        return (this.infos!= null);
    }

    /**
     * Ruft den Wert der returnvalue-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReturnvalue() {
        return returnvalue;
    }

    /**
     * Legt den Wert der returnvalue-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReturnvalue(String value) {
        this.returnvalue = value;
    }

    public boolean isSetReturnvalue() {
        return (this.returnvalue!= null);
    }

}
