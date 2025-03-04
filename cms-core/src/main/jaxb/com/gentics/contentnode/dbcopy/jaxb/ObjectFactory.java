
package com.gentics.contentnode.dbcopy.jaxb;

import com.gentics.contentnode.dbcopy.Reference;
import com.gentics.contentnode.dbcopy.Table;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.gentics.contentnode.dbcopy.jaxb package. 
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


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.gentics.contentnode.dbcopy.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBPropertiesType }
     * 
     * @return
     *     the new instance of {@link JAXBPropertiesType }
     */
    public JAXBPropertiesType createJAXBPropertiesType() {
        return new JAXBPropertiesType();
    }

    /**
     * Create an instance of {@link JAXBReferenceType }
     * 
     * @return
     *     the new instance of {@link JAXBReferenceType }
     */
    public JAXBReferenceType createJAXBReferenceType() {
        return new Reference();
    }

    /**
     * Create an instance of {@link com.gentics.contentnode.dbcopy.jaxb.Tables }
     * 
     * @return
     *     the new instance of {@link com.gentics.contentnode.dbcopy.jaxb.Tables }
     */
    public com.gentics.contentnode.dbcopy.jaxb.Tables createTables() {
        return new com.gentics.contentnode.dbcopy.Tables();
    }

    /**
     * Create an instance of {@link JAXBTableType }
     * 
     * @return
     *     the new instance of {@link JAXBTableType }
     */
    public JAXBTableType createJAXBTableType() {
        return new Table();
    }

    /**
     * Create an instance of {@link JAXBModificatorsType }
     * 
     * @return
     *     the new instance of {@link JAXBModificatorsType }
     */
    public JAXBModificatorsType createJAXBModificatorsType() {
        return new JAXBModificatorsType();
    }

    /**
     * Create an instance of {@link JAXBReferencesType }
     * 
     * @return
     *     the new instance of {@link JAXBReferencesType }
     */
    public JAXBReferencesType createJAXBReferencesType() {
        return new JAXBReferencesType();
    }

    /**
     * Create an instance of {@link JAXBPropertiesType.Property }
     * 
     * @return
     *     the new instance of {@link JAXBPropertiesType.Property }
     */
    public JAXBPropertiesType.Property createJAXBPropertiesTypeProperty() {
        return new JAXBPropertiesType.Property();
    }

    /**
     * Create an instance of {@link JAXBReferenceType.Parameter }
     * 
     * @return
     *     the new instance of {@link JAXBReferenceType.Parameter }
     */
    public JAXBReferenceType.Parameter createJAXBReferenceTypeParameter() {
        return new JAXBReferenceType.Parameter();
    }

}
