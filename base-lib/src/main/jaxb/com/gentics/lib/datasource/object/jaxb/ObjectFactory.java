
package com.gentics.lib.datasource.object.jaxb;

import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.gentics.lib.datasource.object.jaxb package. 
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
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.gentics.lib.datasource.object.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Definition }
     * 
     * @return
     *     the new instance of {@link Definition }
     */
    public Definition createDefinition() {
        return new Definition();
    }

    /**
     * Create an instance of {@link Objecttype }
     * 
     * @return
     *     the new instance of {@link Objecttype }
     */
    public Objecttype createObjecttype() {
        return new Objecttype();
    }

    /**
     * Create an instance of {@link Attributetype }
     * 
     * @return
     *     the new instance of {@link Attributetype }
     */
    public Attributetype createAttributetype() {
        return new Attributetype();
    }

}
