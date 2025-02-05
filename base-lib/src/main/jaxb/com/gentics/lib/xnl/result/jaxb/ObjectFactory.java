
package com.gentics.lib.xnl.result.jaxb;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.gentics.lib.xnl.result.jaxb package. 
 * <p>An ObjectFactory allows you to programmatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _Result_QNAME = new QName("", "result");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.gentics.lib.xnl.result.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBResultType }
     * 
     * @return
     *     the new instance of {@link JAXBResultType }
     */
    public JAXBResultType createJAXBResultType() {
        return new JAXBResultType();
    }

    /**
     * Create an instance of {@link JAXBDependenciesType }
     * 
     * @return
     *     the new instance of {@link JAXBDependenciesType }
     */
    public JAXBDependenciesType createJAXBDependenciesType() {
        return new JAXBDependenciesType();
    }

    /**
     * Create an instance of {@link JAXBDependencyType }
     * 
     * @return
     *     the new instance of {@link JAXBDependencyType }
     */
    public JAXBDependencyType createJAXBDependencyType() {
        return new JAXBDependencyType();
    }

    /**
     * Create an instance of {@link JAXBInfosType }
     * 
     * @return
     *     the new instance of {@link JAXBInfosType }
     */
    public JAXBInfosType createJAXBInfosType() {
        return new JAXBInfosType();
    }

    /**
     * Create an instance of {@link JAXBInfoType }
     * 
     * @return
     *     the new instance of {@link JAXBInfoType }
     */
    public JAXBInfoType createJAXBInfoType() {
        return new JAXBInfoType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JAXBResultType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link JAXBResultType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "result")
    public JAXBElement<JAXBResultType> createResult(JAXBResultType value) {
        return new JAXBElement<>(_Result_QNAME, JAXBResultType.class, null, value);
    }

}
