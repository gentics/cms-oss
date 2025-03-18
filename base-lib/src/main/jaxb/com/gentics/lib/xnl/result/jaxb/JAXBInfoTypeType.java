
package com.gentics.lib.xnl.result.jaxb;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * 
 * 
 * <p>Java-Klasse für infoTypeType.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * <pre>{@code
 * <simpleType name="infoTypeType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="error"/>
 *     <enumeration value="warning"/>
 *     <enumeration value="info"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "infoTypeType")
@XmlEnum
public enum JAXBInfoTypeType {

    @XmlEnumValue("error")
    ERROR("error"),
    @XmlEnumValue("warning")
    WARNING("warning"),
    @XmlEnumValue("info")
    INFO("info");
    private final String value;

    JAXBInfoTypeType(String v) {
        value = v;
    }

    /**
     * Gets the value associated to the enum constant.
     * 
     * @return
     *     The value linked to the enum.
     */
    public String value() {
        return value;
    }

    /**
     * Gets the enum associated to the value passed as parameter.
     * 
     * @param v
     *     The value to get the enum from.
     * @return
     *     The enum which corresponds to the value, if it exists.
     * @throws IllegalArgumentException
     *     If no value matches in the enum declaration.
     */
    public static JAXBInfoTypeType fromValue(String v) {
        for (JAXBInfoTypeType c: JAXBInfoTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
