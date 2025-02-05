
package com.gentics.lib.formatter.dateformatter;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.gentics.lib.formatter.dateformatter package. 
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

    private static final QName _DateFormats_QNAME = new QName("", "date-formats");
    private static final QName _JAXBDateFormatTypeDate_QNAME = new QName("", "date");
    private static final QName _JAXBDateFormatTypeTime_QNAME = new QName("", "time");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.gentics.lib.formatter.dateformatter
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBDateFormatsType }
     * 
     * @return
     *     the new instance of {@link JAXBDateFormatsType }
     */
    public JAXBDateFormatsType createJAXBDateFormatsType() {
        return new JAXBDateFormatsType();
    }

    /**
     * Create an instance of {@link JAXBDateFormatType }
     * 
     * @return
     *     the new instance of {@link JAXBDateFormatType }
     */
    public JAXBDateFormatType createJAXBDateFormatType() {
        return new DateFormatConfig();
    }

    /**
     * Create an instance of {@link JAXBDateOrTimeType }
     * 
     * @return
     *     the new instance of {@link JAXBDateOrTimeType }
     */
    public JAXBDateOrTimeType createJAXBDateOrTimeType() {
        return new JAXBDateOrTimeType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JAXBDateFormatsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link JAXBDateFormatsType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "date-formats")
    public JAXBElement<JAXBDateFormatsType> createDateFormats(JAXBDateFormatsType value) {
        return new JAXBElement<>(_DateFormats_QNAME, JAXBDateFormatsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "date", scope = JAXBDateFormatType.class)
    public JAXBElement<JAXBDateOrTimeType> createJAXBDateFormatTypeDate(JAXBDateOrTimeType value) {
        return new JAXBElement<>(_JAXBDateFormatTypeDate_QNAME, JAXBDateOrTimeType.class, JAXBDateFormatType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link JAXBDateOrTimeType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "time", scope = JAXBDateFormatType.class)
    public JAXBElement<JAXBDateOrTimeType> createJAXBDateFormatTypeTime(JAXBDateOrTimeType value) {
        return new JAXBElement<>(_JAXBDateFormatTypeTime_QNAME, JAXBDateOrTimeType.class, JAXBDateFormatType.class, value);
    }

}
