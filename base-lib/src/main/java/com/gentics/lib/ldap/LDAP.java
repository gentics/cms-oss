package com.gentics.lib.ldap;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.datasource.LDAPHandle;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.pooling.Poolable;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.controls.LDAPSortControl;
import com.novell.ldap.controls.LDAPSortKey;

/**
 * User: haymo Date: 06.08.2004
 */
public class LDAP {

	// TODO returns
	public static final void query(LDAPHandle handle, String filter, String searchBase,
			int searchScope, LDAPResultProcessor resultProc) throws LDAPException {
		boolean attributeOnly = false;

		String attrs[] = { LDAPConnection.NO_ATTRS };

		Poolable mycon = handle.connect();

		if (mycon != null) {
			try {
				LDAPConnection con = (LDAPConnection) mycon.getObject();

				if (con == null) {
					NodeLogger.getLogger(LDAP.class).error("LDAP::query: Could not process query. Got null connection.");
					return;
				}
				LDAPSearchResults searchResults = con.search(searchBase, // container
						// to
						// search
						searchScope, // search scope
						filter, // search filter
						attrs, // "1.1" returns entry name only
						attributeOnly); // no attributes are returned
    
				// resultProc ist nicht notwendig
    
				resultProc.process(searchResults);
			} finally {
				handle.disconnect(mycon);
			}
		}
	}

	/**
	 * Count the results of the given filter
	 * @param handle ldap handle
	 * @param filter ldap filter
	 * @param searchBase search base DN
	 * @param searchScope search scope
	 * @param maxResults maximum number of results
	 * @return number of results
	 * @throws LDAPException
	 * @throws NodeIllegalArgumentException
	 * @throws DatasourceNotAvailableException
	 */
	public static final int count(LDAPHandle handle, String filter, String searchBase,
			int searchScope, int maxResults) throws LDAPException, NodeIllegalArgumentException,
				DatasourceNotAvailableException {
		int count = 0;

		if (handle == null) {
			throw new DatasourceNotAvailableException("LDAP.query - missing LDAP handle, ldap handle==null");
		} else {
			Poolable mycon = handle.connect();

			if (mycon != null) {
				LDAPConnection con = (LDAPConnection) mycon.getObject();

				if (con != null) {
					LDAPSearchConstraints constr = new LDAPSearchConstraints();

					// set the result limit
					if (maxResults >= 0) {
						constr.setMaxResults(maxResults);
					}

					LDAPSearchResults searchResults = null;

					try {
						searchResults = con.search(searchBase, // container to
								// search
								searchScope, // search scope
								filter, // search filter
								new String[] { LDAPConnection.NO_ATTRS}, // "1.1" returns entry name only
								true, // no attributes are returned
								constr); // sortorder
						// count the results
						while (searchResults.hasMore()) {
							searchResults.next();
							count++;
						}
					} catch (LDAPException ex) {
						// throw every exception except the SIZE_LIMIT_EXCEEDED
						// (which is not an "error")
						if (ex.getResultCode() != LDAPException.SIZE_LIMIT_EXCEEDED) {
							throw ex;
						}
					} finally {
						handle.disconnect(mycon);
					}

				} else {
					throw new DatasourceNotAvailableException("LDAP.query - LDAP connection == null");
				}
			} else {
				throw new DatasourceNotAvailableException("LDAP.query - poolable LDAP connection == null");
			}
		}
		return count;
	}

	/**
	 * Perform an ldap search with the given data
	 * @param handle ldap handle
	 * @param filter filter string
	 * @param searchBase search base dn
	 * @param searchScope search scope
	 * @param sortColumns sort columns (may be null for "no sorting")
	 * @param resultProc result processor that collects the search results
	 * @param attrs attributes to fetch, use empty array for all attributes
	 * @param start start index
	 * @param maxResults maximum number of results
	 * @return false
	 * @throws LDAPException
	 * @throws NodeIllegalArgumentException
	 * @throws DatasourceNotAvailableException
	 */
	public static final boolean query(LDAPHandle handle, String filter, String searchBase,
			int searchScope, Sorting[] sortColumns, LDAPResultProcessor resultProc,
			String[] attrs, int start, int maxResults) throws LDAPException,
				NodeIllegalArgumentException, DatasourceNotAvailableException {

		boolean rcBool = false;

		boolean attributeOnly = false;

		if (handle == null) {
			throw new DatasourceNotAvailableException("LDAP.query - missing LDAP handle, ldap handle==null");
		} else {

			Poolable mycon = handle.connect();

			if (mycon != null) {

				LDAPConnection con = (LDAPConnection) mycon.getObject();

				if (con != null) {

					LDAPSearchConstraints constr = new LDAPSearchConstraints();

					// set the result limit
					if (maxResults >= 0) {
						constr.setMaxResults((start > 0 ? start + maxResults : maxResults));
					}

					// set the sorting constraint if sort columns were given
					if (sortColumns != null && sortColumns.length > 0) {
						LDAPSortKey[] sortKeys = new LDAPSortKey[sortColumns.length];

						for (int i = 0; i < sortColumns.length; ++i) {
							sortKeys[i] = new LDAPSortKey(sortColumns[i].getColumnName(), sortColumns[i].getSortOrder() == Datasource.SORTORDER_DESC);
						}
						LDAPSortControl sortctrl = new LDAPSortControl(sortKeys, false);

						constr.setControls(sortctrl);
					}

					LDAPSearchResults searchResults = null;

					try {
						try {
							searchResults = con.search(searchBase, // container to
									// search
									searchScope, // search scope
									filter, // search filter
									attrs, // "1.1" returns entry name only
									attributeOnly, // no attributes are returned
									constr); // sortorder
    
						} catch (LDAPException ex) {
							// throw every exception except the SIZE_LIMIT_EXCEEDED
							// (which is not an "error")
							if (ex.getResultCode() != LDAPException.SIZE_LIMIT_EXCEEDED) {
								throw ex;
							}
						}

						// resultProc ist nicht notwendig
						resultProc.process(searchResults, start);
						rcBool = false;

					} finally {
						handle.disconnect(mycon);
					}

				} else {
					throw new DatasourceNotAvailableException("LDAP.query - LDAP connection == null");
				}
			} else {
				throw new DatasourceNotAvailableException("LDAP.query - poolable LDAP connection == null");
			}
		}
		return rcBool;
	}

	/**
	 * Fetch the objects matching the given filter
	 * @param handle ldap handle
	 * @param filter ldap filter
	 * @param searchBase search base DN
	 * @param searchScope search scope
	 * @param sortkey sortkey (mey be null)
	 * @param sortorder sortorder
	 * @param resultProc result processor
	 * @param attrs attributes to fetch, use empty array to fetch all attributes
	 * @param start start index (paging)
	 * @param maxResults maximum number of results to be fetched
	 * @return false
	 * @throws LDAPException
	 * @throws NodeIllegalArgumentException
	 * @throws DatasourceNotAvailableException
	 */
	public static final boolean query(LDAPHandle handle, String filter, String searchBase,
			int searchScope, String sortkey, int sortorder, LDAPResultProcessor resultProc,
			String[] attrs, int start, int maxResults) throws LDAPException,
				NodeIllegalArgumentException, DatasourceNotAvailableException {

		boolean rcBool = false;

		boolean attributeOnly = false;

		if (handle == null) {

			throw new DatasourceNotAvailableException("LDAP.query - missing LDAP handle, ldap handle==null");

		} else {

			Poolable mycon = handle.connect();

			if (mycon != null) {

				LDAPConnection con = (LDAPConnection) mycon.getObject();

				if (con != null) {

					boolean boolorder = false;

					if (sortorder == Datasource.SORTORDER_ASC) {
						boolorder = false;
					}
					if (sortorder == Datasource.SORTORDER_DESC) {
						boolorder = true;
					}

					LDAPSearchConstraints constr = new LDAPSearchConstraints();

					// set the result limit
					if (maxResults >= 0) {
						constr.setMaxResults((start > 0 ? start + maxResults : maxResults));
					}
					if (sortkey != null && sortkey.length() > 0) {
						LDAPSortKey ldapsortkey = new LDAPSortKey(sortkey, boolorder);
						LDAPSortControl sortctrl = new LDAPSortControl(ldapsortkey, false);

						constr.setControls(sortctrl);
					}

					LDAPSearchResults searchResults = null;

					try {
						try {
							searchResults = con.search(searchBase, // container to
									// search
									searchScope, // search scope
									filter, // search filter
									attrs, // "1.1" returns entry name only
									attributeOnly, // no attributes are returned
									constr); // sortorder
    
						} catch (LDAPException ex) {
							// throw every exception except the SIZE_LIMIT_EXCEEDED
							// (which is not an "error")
							if (ex.getResultCode() != LDAPException.SIZE_LIMIT_EXCEEDED) {
								throw ex;
							}
						}

						// resultProc ist nicht notwendig
						resultProc.process(searchResults, start);
						rcBool = false;
					} finally {
						handle.disconnect(mycon);
					}

				} else {
					throw new DatasourceNotAvailableException("LDAP.query - LDAP connection == null");
				}
			} else {
				// TODO Throw LDAP Unavailable exception
				throw new DatasourceNotAvailableException("LDAP.query - poolable LDAP connection == null");
			}
		}
		return rcBool;
	}

	/**
	 * Check whether the LDAP connection is alive
	 * @param handle handle to check
	 * @return true when the connection is alive, false if not
	 */
	public static boolean isConnectionAlive(LDAPHandle handle) throws LDAPException {
		if (handle == null) {
			return false;
		}

		Poolable mycon = null;
		LDAPConnection con = null;

		try {
			mycon = handle.connect();
			if (mycon != null) {
				con = (LDAPConnection) mycon.getObject();
				return con.isConnectionAlive();
			} else {
				return false;
			}
		} finally {
			if (mycon != null) {
				handle.disconnect(mycon);
			}
		}
	}
}
