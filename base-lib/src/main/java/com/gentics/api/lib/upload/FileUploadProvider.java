/*
 * @author Stefan Hurjui
 * @date 10.01.2005
 * @version: $Id: FileUploadProvider.java,v 1.2 2006-04-07 14:45:45 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import org.apache.commons.fileupload.FileUploadException;

import com.gentics.lib.upload.FileUploadProviderImpl;

/**
 * The Interface FileUploadProvider defines the API for retrieving files that
 * were uploaded via a HTTP POST. Gentics portlets and plugins may obtain an
 * instance of a FileUploadProvider via
 * com.gentics.api.portalnode.portlet.GenticsPortlet#getFileUploadProvider() or
 * com.gentics.api.portalnode.plugin.GenticsPlugin#getFileUploadProvider() respectively.
 */
public interface FileUploadProvider {

	/**
	 * Sets a max limit for a fileupload
	 * @param maxFilesize set max file size in bytes
	 */
	void setMaxFilesize(long maxFilesize);

	/**
	 * Sets a max limit for the size threshold.
	 * @param sizeThreshold in bytes
	 */
	void setSizeThreshold(int sizeThreshold);
    
	/**
	 * Returns the current max limit for files hold in memory.
	 * @return the max limit for files hold in memory.
	 */
	int getSizeThreshold();

	/**
	 * Sets the repository path for the uploaded file
	 * @param repositoryPath absolute Path
	 */
	void setRepositoryPath(String repositoryPath);
    
	/**
	 * Returns the current repository path used for uploaded files.
	 * @return the current repository path used for uploaded files.
	 */
	String getRepositoryPath();

	/**
	 * Get an input stream for retrieving the file content by field name
	 * @param fieldName name of the upload field
	 * @return input stream or null when the upload field was left empty
	 * @throws FileNotFoundException when the file was not found (field not
	 *         present in form)
	 */
	InputStream getStream(String fieldName) throws FileNotFoundException;

	/**
	 * Get the file information of an uploaded file by field name.<br>The retrieved value will depend on the client's operating system:
	 * Windows delivers Full Path, *NX delivers only FileName
	 * @param fieldName name of the upload field
	 * @return FileInformation object or null if the upload field was left empty
	 * @throws FileNotFoundException when the file was not found (field not
	 *         present in form)
	 * @throws FileUploadException when the upload fails
	 */
	FileInformation getFileInformation(String fieldName) throws FileNotFoundException,
				FileUploadException;

	/**
	 * Get the value of a non-file parameter (similar to
	 * {@link javax.portlet.PortletRequest#getParameter(java.lang.String)})
	 * @param fieldName name of the field
	 * @return value of the "normal" form field
	 */
	String getParameter(String fieldName);

	/**
	 * Get values of a non-file parameter (similar to
	 * {@link javax.portlet.PortletRequest#getParameterValues(java.lang.String)})
	 * @param fieldName name of the field
	 * @return String array of values
	 */
	String[] getParameterValues(String fieldName);

	/**
	 * Get all non-file parameters as map (similar to
	 * {@link javax.portlet.PortletRequest#getParameterMap()})
	 * @return parameter map
	 */
	Map getParameterMap();

	/**
	 * Get all parameter names of non-file parameters (similar to
	 * {@link javax.portlet.PortletRequest#getParameterNames()})
	 * @return enumeration of parameter names
	 */
	Enumeration getParameterNames();

	/**
	 * TODO: difference to {@link #saveAs(String, String)}
	 * @param fieldName fieldname of the uploaded file
	 * @param path file path to save the file
	 * @return file object if created or null
	 * @throws Exception an Execption ???
	 */
	File save(String fieldName, String path) throws Exception;

	/**
	 * Directly store the specified uploaded file into the filepath and return
	 * the representing file object. filePath is the complete path and filename.
	 * @param fieldName ENTER FULL fieldName (e.g.: p.file)
	 * @param filePath ENTER ONLY PATH NAME (NO NEED TO ADD ANY '/' OR '\'
	 * @return file object if created or null
	 * @throws Exception an Execption ???
	 */
	File saveAs(String fieldName, String filePath) throws Exception;

	/**
	 * Called (by the portal) when the provided information is no longer needed.
	 * (when instance is given back to the portal). this removes all information
	 * prepared in {@link FileUploadProviderImpl#setHttpServletRequest(javax.servlet.http.HttpServletRequest)} by calling delete()
	 * for all FileItems
	 */
	void invalidate();

	/**
	 * Get the maximum allowed filesize
	 * @return maximum allowed filesize
	 */
	long getMaxFilesize();
}
