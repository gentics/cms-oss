package com.gentics.lib.ldap;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import com.gentics.lib.datasource.LDAPDatasourceRow;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

/**
 * Created by IntelliJ IDEA. User: haymo Date: 06.08.2004 Time: 14:51:02 To
 * change this template use File | Settings | File Templates.
 */
public interface LDAPResultProcessor extends Serializable {

	public void process(LDAPSearchResults searchResult, int startIndex) throws LDAPException;

	public void process(LDAPSearchResults searchResult) throws LDAPException;

	public int size();

	public LDAPDatasourceRow getRow(int i);

	public Iterator getLDAPDatasourceRowIterator();

	public Vector getAllLAPRows();

}
