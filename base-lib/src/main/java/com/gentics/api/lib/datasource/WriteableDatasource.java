/*
 * @author haymo
 * @date 26.08.2004
 * @version $Id: WriteableDatasource.java,v 1.6 2007-11-13 10:03:47 norbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.datasource;

import java.util.Collection;
import java.util.Map;

import com.gentics.api.lib.auth.GenticsUser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;

/**
 * Interface for {@link Datasource}s that allow data modifications.
 */
public interface WriteableDatasource extends Datasource {

	/**
	 * Checks if the Datasource is allowed to write to the underlying
	 * Datastorage
	 * @return true when data modifications are allowed, false if not
	 */
	public boolean canWrite();

	/**
	 * Store the objects given in the DatasourceRecordSet (does either "insert"
	 * or "update" depending on whether the object already exists in the
	 * Datasource or not)
	 * @param rst recordset holding the objects to be stored
	 * @return DatasourceInfo object holding the store results
	 * @throws DatasourceException in case of errors
	 * @deprecated Use {@link #store(Collection)} instead
	 */
	public DatasourceInfo store(DatasourceRecordSet rst) throws DatasourceException;

	/**
	 * Store the objects given in the DatasourceRecordSet (does either "insert"
	 * or "update" depending on whether the object already exists in the
	 * Datasource or not)
	 * @param rst recordset holding the data to store
	 * @param user user who performs the storing (may be null)
	 * @return datasourceinfo holding info about storing results
	 * @throws DatasourceException in case of errors
	 * @deprecated Methods with GenticsUser are deprecated. use {@link #store(Collection)}
	 */
	public DatasourceInfo store(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException;

	/**
	 * Store a collection of objects.
	 * @param objects collection of objects to store
	 * @return datasource info holding the info about storing results
	 * @throws DatasourceException in case of errors
	 */
	public DatasourceInfo store(Collection objects) throws DatasourceException;

	/**
	 * Store a collection of objects.
	 * @param objects collection of objects to store
	 * @param user user who preforms the store (may be null)
	 * @return datasource info holding the info about storing results
	 * @throws DatasourceException in case of errors
	 * @deprecated Methods with GenticsUser are deprecated. use {@link #store(Collection)}
	 */
	public DatasourceInfo store(Collection objects, GenticsUser user) throws DatasourceException;

	/**
	 * Update objects in the given DatasourceRecordSet
	 * @param rst record set holding the objects to be updated
	 * @return DatasourceInfo holding information about the updates
	 * @throws DatasourceException in case of errors
	 * @deprecated use {@link #update(Collection)} instead.
	 */
	public DatasourceInfo update(DatasourceRecordSet rst) throws DatasourceException;

	/**
	 * Update objects in the given DatasourceRecordSet
	 * @param rst recordset holding the data to update
	 * @param user use who performs the update (may be null)
	 * @return datasourceinfo holding info about update results
	 * @throws DatasourceException in case of errors
	 * @deprecated Methods with GenticsUser are deprecated, use {@link #update(Collection)}
	 */
	public DatasourceInfo update(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException;

	/**
	 * Update a collection of objects
	 * @param objects collection of objects to update
	 * @return datasource info
	 * @throws DatasourceException in case of errors
	 */
	public DatasourceInfo update(Collection objects) throws DatasourceException;

	/**
	 * Update a collection of objects
	 * @param objects collection of objects to update
	 * @param user user who performs the update (may be null)
	 * @return datasource info
	 * @throws DatasourceException in case of errors
	 * @deprecated Methods with GenticsUser are deprecated, use {@link #update(Collection)}
	 */
	public DatasourceInfo update(Collection objects, GenticsUser user) throws DatasourceException;

	/**
	 * Insert new objects given in the DatasourceRecordSet into the Datasource
	 * @param rst recordset holding new objects to be inserted
	 * @return information about the insert results
	 * @throws DatasourceException in case of errors
	 * @deprecated use {@link #insert(Collection)} instead
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst) throws DatasourceException;

	/**
	 * Insert a collection of objects
	 * @param objects collection of objects to insert
	 * @return datasource info
	 * @throws DatasourceException in case of errors
	 */
	public DatasourceInfo insert(Collection objects) throws DatasourceException;

	/**
	 * Insert new objects given in the DatasourceRecordSet into the Datasource
	 * @param rst recordset holding new objects to be inserted
	 * @param user user who performs the insert
	 * @return information about the insert results
	 * @throws DatasourceException in case of errors
	 * @deprecated methods with GenticsUser are deprecated, use {@link #insert(Collection)}
	 */
	public DatasourceInfo insert(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException;

	/**
	 * Insert a collection of objects
	 * @param objects collection of objects to insert
	 * @param user user who performs the insert (may be null)
	 * @return datasource info
	 * @throws DatasourceException in case of errors
	 * @deprecated methods with GenticsUser are deprecated, use {@link #insert(Collection)}
	 */
	public DatasourceInfo insert(Collection objects, GenticsUser user) throws DatasourceException;

	/**
	 * Deletes the objects in the given DatasourceRecordSet from the Datasource
	 * @param rst containing the rows to delete
	 * @return a datasource info containing the number of affected rows.
	 * @throws DatasourceException in case of errors
	 * @deprecated use {@link #delete(Collection)} instead
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst) throws DatasourceException;

	/**
	 * Deletes a collection of objects
	 * @param objects collection of objects to delete
	 * @return a datasource info containing the number of affected rows.
	 * @throws DatasourceException in case of errors
	 */
	public DatasourceInfo delete(Collection objects) throws DatasourceException;

	/**
	 * Deletes the rows in the given recordset
	 * @param rst containing the rows to delete
	 * @param user user who performs the delete (may be null)
	 * @return a datasource info containing the number of affected rows.
	 * @throws DatasourceException in case of errors
	 * @deprecated methods with GenticsUser are deprecated, use {@link #delete(Collection)} instead.
	 */
	public DatasourceInfo delete(DatasourceRecordSet rst, GenticsUser user) throws DatasourceException;

	/**
	 * Deletes a collection of objects
	 * @param objects collection of objects to delete
	 * @param user user who performs the delete (may be null)
	 * @return a datasource info containing the number of affected rows.
	 * @throws DatasourceException in case of errors
	 * @deprecated methods with GenticsUser are deprecated, use {@link #delete(Collection)} instead.
	 */
	public DatasourceInfo delete(Collection objects, GenticsUser user) throws DatasourceException;

	/**
	 * Deletes all objects found with the given filter.
	 * @param filter the filter defining which objets to delete.
	 * @return a datasource info containing the number of affected rows.
	 * @throws DatasourceException in case of errors
	 */
	public DatasourceInfo delete(DatasourceFilter filter) throws DatasourceException;
    
	/**
	 * Create a new object that can be stored in this datasource. The object
	 * will be created but not yet stored in the datasource.
	 * @param objectParameters datasource implementation specific object
	 *        parameters
	 * @return the create object as Changeable
	 * @throws DatasourceException when the creation fails
	 */
	Changeable create(Map objectParameters) throws DatasourceException;
}
