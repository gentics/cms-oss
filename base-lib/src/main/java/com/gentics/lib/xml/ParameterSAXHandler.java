/*
 * ParameterSAXHandler.java
 *
 * Created on 23. August 2004, 18:57
 */

package com.gentics.lib.xml;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Dietmar
 */
public class ParameterSAXHandler extends org.xml.sax.helpers.DefaultHandler {
	protected static int STATUS_NORMAL = 0;

	protected static int STATUS_PARAMETER = 1;

	public static String DefaultParameterIdentifier = "parameter";

	public static String DefaultIDIdentifier = "id";

	protected int status = 0;

	protected String ParameterIdentifier = null;

	protected String IDIdentifier = null;

	protected StringBuffer CurrentParameterValue = null;

	protected String CurrentID = null;

	protected java.util.Properties SavedProperties = new java.util.Properties();

	public ParameterSAXHandler() {
		this(DefaultParameterIdentifier, DefaultIDIdentifier);
	}

	/** Creates a new instance of ParameterSAXHandler */
	public ParameterSAXHandler(String ParameterIdentifier, String IDIdentifier) {
		// set id strings
		this.ParameterIdentifier = ParameterIdentifier;
		this.IDIdentifier = IDIdentifier;
	}

	/**
	 * Receive notification of the end of an element.
	 * <p>
	 * The SAX parser will invoke this method at the end of every element in the
	 * XML document; there will be a corresponding
	 * {@link #startElement startElement}event for every endElement event (even
	 * when the element is empty).
	 * </p>
	 * <p>
	 * For information on the names, see startElement.
	 * </p>
	 * @param uri The Namespace URI, or the empty string if the element has no
	 *        Namespace URI or if Namespace processing is not being performed.
	 * @param localName The local name (without prefix), or the empty string if
	 *        Namespace processing is not being performed.
	 * @param qName The qualified XML 1.0 name (with prefix), or the empty
	 *        string if qualified names are not available.
	 * @exception org.xml.sax.SAXException Any SAX exception, possibly wrapping
	 *            another exception.
	 */
	public void endElement(String namespaceURI, String localName, String qName) {
		if (localName.equals(ParameterIdentifier)) {
			// go back to normal status
			status = STATUS_NORMAL;
			SavedProperties.setProperty(CurrentID, CurrentParameterValue.toString());
			CurrentParameterValue = null;
		}
	}

	/**
	 * Receive notification of character data.
	 * <p>
	 * The Parser will call this method to report each chunk of character data.
	 * SAX parsers may return all contiguous character data in a single chunk,
	 * or they may split it into several chunks; however, all of the characters
	 * in any single event must come from the same external entity so that the
	 * Locator provides useful information.
	 * </p>
	 * <p>
	 * The application must not attempt to read from the array outside of the
	 * specified range.
	 * </p>
	 * <p>
	 * Note that some parsers will report whitespace in element content using
	 * the {@link #ignorableWhitespace ignorableWhitespace}method rather than
	 * this one (validating parsers <em>must</em> do so).
	 * </p>
	 * @param ch The characters from the XML document.
	 * @param start The start position in the array.
	 * @param length The number of characters to read from the array.
	 * @exception org.xml.sax.SAXException Any SAX exception, possibly wrapping
	 *            another exception.
	 * @see #ignorableWhitespace
	 * @see org.xml.sax.Locator
	 */
	public void characters(char[] ch, int start, int length) {
		// get string
		if (status == STATUS_PARAMETER) {
			CurrentParameterValue.append(ch, start, length);
		}
	}

	/**
	 * Receive notification of the beginning of an element.
	 * <p>
	 * The Parser will invoke this method at the beginning of every element in
	 * the XML document; there will be a corresponding
	 * {@link #endElement endElement}event for every startElement event (even
	 * when the element is empty). All of the element's content will be
	 * reported, in order, before the corresponding endElement event.
	 * </p>
	 * <p>
	 * This event allows up to three name components for each element:
	 * </p>
	 * <ol>
	 * <li>the Namespace URI;</li>
	 * <li>the local name; and</li>
	 * <li>the qualified (prefixed) name.</li>
	 * </ol>
	 * <p>
	 * Any or all of these may be provided, depending on the values of the
	 * <var>http://xml.org/sax/features/namespaces </var> and the
	 * <var>http://xml.org/sax/features/namespace-prefixes </var> properties:
	 * </p>
	 * <ul>
	 * <li>the Namespace URI and local name are required when the namespaces
	 * property is <var>true </var> (the default), and are optional when the
	 * namespaces property is <var>false </var> (if one is specified, both must
	 * be);</li>
	 * <li>the qualified name is required when the namespace-prefixes property
	 * is <var>true </var>, and is optional when the namespace-prefixes property
	 * is <var>false </var> (the default).</li>
	 * </ul>
	 * <p>
	 * Note that the attribute list provided will contain only attributes with
	 * explicit values (specified or defaulted): #IMPLIED attributes will be
	 * omitted. The attribute list will contain attributes used for Namespace
	 * declarations (xmlns* attributes) only if the
	 * <code>http://xml.org/sax/features/namespace-prefixes</code> property is
	 * true (it is false by default, and support for a true value is optional).
	 * </p>
	 * @param uri The Namespace URI, or the empty string if the element has no
	 *        Namespace URI or if Namespace processing is not being performed.
	 * @param localName The local name (without prefix), or the empty string if
	 *        Namespace processing is not being performed.
	 * @param qName The qualified name (with prefix), or the empty string if
	 *        qualified names are not available.
	 * @param atts The attributes attached to the element. If there are no
	 *        attributes, it shall be an empty Attributes object.
	 * @exception org.xml.sax.SAXException Any SAX exception, possibly wrapping
	 *            another exception.
	 * @see #endElement
	 * @see org.xml.sax.Attributes
	 */
	public void startElement(String namespaceURI, String localName, String qName,
			org.xml.sax.Attributes atts) {
		if (localName.equals(ParameterIdentifier)) {
			status = STATUS_PARAMETER;
			// set the parameter temporary values
			CurrentParameterValue = new StringBuffer("");
			CurrentID = atts.getValue(this.IDIdentifier);
		}
	}

	public java.util.Properties getProperties() {
		return SavedProperties;
	}

	public static java.util.Properties loadProperties(String ParameterIdentifier,
			String IDIdentifier, String XMLString) {
		java.util.Properties ret = null;

		try {
			XMLReader XMLReader = XMLReaderFactory.createXMLReader();
			// create a handle
			ParameterSAXHandler SAXHandle = new ParameterSAXHandler(ParameterIdentifier, IDIdentifier);

			XMLReader.setContentHandler(SAXHandle);
			XMLReader.setErrorHandler(SAXHandle);
			// open the sax parse procedure
			// System.out.println("DatasourceHANDLE: XML" + XMLString);
			XMLReader.parse(new InputSource(new java.io.StringReader(XMLString)));
			ret = SAXHandle.getProperties();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}

	public static java.util.Properties loadProperties(String XMLString) {
		return loadProperties(DefaultParameterIdentifier, DefaultIDIdentifier, XMLString);
	}

}
