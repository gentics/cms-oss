package com.gentics.lib.ldap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.lib.datasource.LDAPDatasourceRow;
import com.gentics.lib.log.NodeLogger;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

/**
 * Created by IntelliJ IDEA. User: haymo Date: 06.08.2004 Time: 14:54:29 To
 * change this template use File | Settings | File Templates.
 */
public class SimpleLDAPResultProcessor implements LDAPResultProcessor {
	private static final long serialVersionUID = 1L;

	// private

	private Vector ldapRows = new Vector();

	private LDAPDatasourceRow ldaprow;

	/**
	 * default value for the dn attribute name
	 */
	public final static String DEFAULTDNATTRIBUTENAME = "name";

	/**
	 * name of the attribute that will map entry DNs
	 */
	private String dnAttributeName = DEFAULTDNATTRIBUTENAME;

	private List binaryAttributes = null;

	/**
	 * logger
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Create an instance of the LDAP result processor
	 * @param dnAttributeName name of the attribute that will map entry DNs or
	 *        null to use the default value
	 * @see #DEFAULTDNATTRIBUTENAME
	 */
	public SimpleLDAPResultProcessor(String dnAttributeName, List binaryAttributes) {
		if (dnAttributeName != null) {
			this.dnAttributeName = dnAttributeName;
		}
		this.binaryAttributes = binaryAttributes;
	}

	public void process(LDAPSearchResults searchResults) throws LDAPException {
		process(searchResults, 0);
	}

	public void process(LDAPSearchResults searchResults, int startIndex) throws LDAPException {

		String attributeName;
		LDAPAttribute attribute;

		ldapRows = new Vector();
		// print out all the objects
		Object object;

		int counter = 0;

		while (searchResults.hasMore()) {
			LDAPEntry nextEntry = null;

			try {
				nextEntry = searchResults.next();
			} catch (LDAPException ex) {
				// throw every exception but size limit exceeded
				if (ex.getResultCode() != LDAPException.SIZE_LIMIT_EXCEEDED) {
					throw ex;
				} else {
					// when the size limit is exceeded, we are done (without
					// error)
					return;
				}
			}

			// when a startIndex > 0 is given, omit some rows
			counter++;
			if (counter <= startIndex) {
				continue;
			}

			LDAPDatasourceRow ldaprow = new LDAPDatasourceRow();
			// object = (Object) searchResults.next();
			// System.out.println("LDAP
			// SearchResult:"+object.getClass().toString());

			// System.out.println("\n" + nextEntry.getDN() + " tostring: " +
			// nextEntry.toString());// + "
			// cnapatappurl:"+nextEntry.getAttribute("cnafipappurl").getStringValue());

			LDAPAttributeSet ldapattribsset = nextEntry.getAttributeSet();

			// create new datasourcerow object
			ldaprow = new LDAPDatasourceRow();
			ldaprow.setRowElement(dnAttributeName, nextEntry.getDN());

			Iterator attribIterator = ldapattribsset.iterator();

			while (attribIterator.hasNext()) {
				attribute = (LDAPAttribute) attribIterator.next();

				// check for attributes that hide the DN
				if (dnAttributeName.equals(attribute.getName())) {
					logger.warn("Attribute {" + attribute.getName() + "} will hide the DN.");
				}

				// System.out.println("Name:"+attribute.getName()+"
				// StringValue:"+attribute.getStringValue());
				// store attribute in datasourcerow

				if (!binaryAttributes.contains(attribute.getName())) {
					// get value as strings
					String[] values = attribute.getStringValueArray();

					switch (values.length) {
					case 0:
						break;

					case 1:
						ldaprow.setRowElement(attribute.getName(), values[0]);
						break;

					default:
						Collection valueCol = new Vector();

						for (int i = 0; i < values.length; ++i) {
							valueCol.add(values[i]);
						}
						ldaprow.setRowElement(attribute.getName(), valueCol);
						break;
					}
				} else {
					// get value as binary data
					byte[][] values = attribute.getByteValueArray();

					switch (values.length) {
					case 0:
						break;

					case 1:
						ldaprow.setRowElement(attribute.getName(), values[0]);
						break;

					default:
						Collection valueCol = new Vector();

						for (int i = 0; i < values.length; ++i) {
							valueCol.add(values[i]);
						}
						ldaprow.setRowElement(attribute.getName(), valueCol);
						break;
					}
				}
			}
			ldapRows.add(ldaprow);
		}
	}

	public int size() {
		return this.ldapRows.size();
	}

	public LDAPDatasourceRow getRow(int i) {
		LDAPDatasourceRow dsRow = null;

		if (i < this.ldapRows.size()) {

			dsRow = (LDAPDatasourceRow) this.ldapRows.get(i);

		}

		return dsRow;
	}

	public Iterator getLDAPDatasourceRowIterator() {

		return this.ldapRows.iterator();

	}

	public Vector getAllLAPRows() {

		return this.ldapRows;
	}

}
