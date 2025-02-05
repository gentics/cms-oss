package com.gentics.lib.upload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;

import com.gentics.api.lib.upload.FileInformation;
import com.gentics.api.lib.upload.FileUploadProvider;
import com.gentics.lib.log.NodeLogger;

import jakarta.servlet.http.HttpServletRequest;

/**
 * concrete implementation of a FileUploadProvider which uses jakarta-commons
 * FileUpload package to retrieve uploaded files and additional data from a
 * mime-encoded ActionRequest Any exception thrown by jakarta-commons-fileupload
 * is stored inside the object and any getter should check first if there is any
 * exception
 */
public class FileUploadProviderImpl implements FileUploadProvider {
	private Map<String, List<String>> parameters = new HashMap<>();

	private Map<String, DiskFileItem> fileFields = new HashMap<>();

	private List<DiskFileItem> fileItems = null;

	private HttpServletRequest request;

	private long maxFilesize;

	private int sizeThreshold;

	private String repositoryPath;

	private FileUploadException caughtException = null;

	/**
	 * Concrete implementation of interface FileUploadProvider that uses the
	 * JakartaCommons FileUpload package
	 */
	public FileUploadProviderImpl() {}

	/**
	 * @param myRequest sets the private field request also parse the request
	 *        using jakarta-commons-upload normal parameters are stored in
	 *        parameters hashmap file parameters are stored in fileFields
	 *        hashmap exception is stored in field caughtException
	 */
	public final void setHttpServletRequest(final HttpServletRequest myRequest) {
		this.request = myRequest;

		JakartaServletDiskFileUpload diskFileUpload = new JakartaServletDiskFileUpload();

		// maximum size before a FileUploadException will be thrown
		diskFileUpload.setSizeMax(maxFilesize);
		// maximum size that will be stored in memory
//		diskFileUpload.setSizeThreshold(sizeThreshold);
		// the location for saving data that is larger than getSizeThreshold()
//		diskFileUpload.setRepositoryPath(repositoryPath);
		// set encoding manually, according to bug http://issues.apache.org/bugzilla/show_bug.cgi?id=23255 
		diskFileUpload.setHeaderCharset(Charset.forName(request.getCharacterEncoding()));

		try {
			fileItems = diskFileUpload.parseRequest(request);
		} catch (FileUploadException e) {
			caughtException = e;
			byte[] buf = new byte[1024];

			try {
				while (myRequest.getInputStream().read(buf) > 0) {
					;
				}
			} catch (IOException e1) {
				NodeLogger.getNodeLogger(FileUploadProvider.class).error("Error while reading from input stream", e1);
			}
		}

		if (caughtException == null) {
			Iterator<DiskFileItem> iterator = fileItems.iterator();

			while (iterator.hasNext()) {
				DiskFileItem fileItem = iterator.next();
				String fieldName = fileItem.getFieldName();

				/*
				 * if the parameter's name starts with "p." this 2 caracters
				 * will be ignored
				 */
				if (fieldName.startsWith("p.")) {
					// TODO remove this EVEL hack and find a better solution for
					// this
					fieldName = fieldName.substring(2);
				}

				if (fileItem.isFormField()) {

					/*
					 * if the parameter's name starts with "p." this 2 caracters
					 * will be ignored
					 */
					List<String> valueList = parameters.get(fieldName);

					if (valueList == null) {
						valueList = new ArrayList<>();
						parameters.put(fieldName, valueList);
					}
					try {
						valueList.add(fileItem.getString(Charset.forName(myRequest.getCharacterEncoding())));
					} catch (IOException e) {
						NodeLogger.getLogger(getClass()).error("cannot get value for fileItem '" + fileItem.getFieldName() + "'", e);
					}
				} else {
					fileFields.put(fieldName, fileItem);
				}
			}
		}
	}

	/**
	 * set the max file size
	 * @param myMaxFilesize in bytes
	 */
	public final void setMaxFilesize(final long myMaxFilesize) {
		this.maxFilesize = myMaxFilesize;
	}

	/**
	 * set the max size for threshold
	 * @param mySizeThreshold in bytes
	 */
	public final void setSizeThreshold(final int mySizeThreshold) {
		this.sizeThreshold = mySizeThreshold;
	}
    
	/**
	 * returns the current max limit for files hold in memory.
	 */
	public int getSizeThreshold() {
		return this.sizeThreshold;
	}

	/**
	 * (non-Javadoc) set the repository path for the uploaded file
	 * @param myRepositoryPath absolute path
	 */
	public final void setRepositoryPath(final String myRepositoryPath) {
		this.repositoryPath = myRepositoryPath;
	}

	/**
	 * called (by the portal) when the provided information is no longer needed.
	 * (when instance is given back to the portal). this removes all information
	 * prepared in {@link #setActionRequest()}by calling delete() for all
	 * FileItems
	 */
	public final void invalidate() {
		if (fileItems != null) {
			Iterator<DiskFileItem> iterator = fileItems.iterator();

			while (iterator.hasNext()) {
				DiskFileItem fileItem = iterator.next();

				try {
					fileItem.delete();
				} catch (IOException e) {
					NodeLogger.getNodeLogger(getClass()).warn("Error while deleting fileItem " + fileItem.getName(), e);
				}
			}
		}
	}

	/**
	 * @param fieldName name of the HTTP field
	 * @return input stream from the fieldname specified
	 * @throws FileNotFoundException when the file is not found
	 */
	public final InputStream getStream(final String fieldName) throws FileNotFoundException {
		if (caughtException == null) {
			if (fileFields.containsKey(fieldName)) {
				DiskFileItem fileItem = fileFields.get(fieldName);

				try {
					return fileItem.getInputStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	/**
	 * @param fieldName name of the HTTP field
	 * @return FileInformation of the uploaded file
	 * @throws FileNotFoundException when the file is not found
	 * @throws FileUploadException when the upload fails
	 */
	public final FileInformation getFileInformation(final String fieldName) throws FileUploadException {
		if (caughtException == null) {
			if (fileFields.containsKey(fieldName)) {
				DiskFileItem fileItem = fileFields.get(fieldName);
				FileInformation fileInformation = new FileInformation(fileItem, this);

				return fileInformation;
			}
		} else {
			throw caughtException;
		}

		return null;
	}

	/**
	 * @param fieldName name of the HTTP field
	 * @return parameter value
	 */
	public final String getParameter(final String fieldName) {
		List<String> valueList = parameters.get(fieldName);

		if (!CollectionUtils.isEmpty(valueList)) {
			return valueList.get(0);
		} else {
			return null;
		}
	}

	/**
	 * @param fieldName name of the HTTP field
	 * @return Array of Parameters
	 */
	public final String[] getParameterValues(final String fieldName) {
		List<String> valueList = parameters.get(fieldName);

		if (!CollectionUtils.isEmpty(valueList)) {
			return valueList.toArray(new String[valueList.size()]);
		} else {
			return null;
		}
	}

	/**
	 * @return all non file parameters or null if there was any upload error
	 */
	public final Map<String, List<String>> getParameterMap() {
		return parameters;
	}

	/**
	 * @return parameterNames
	 */
	public final Enumeration<String> getParameterNames() {
		return request.getParameterNames();
	}

	/**
	 * @param fieldName ENTER FULL fieldName (e.g.: p.file)
	 * @param path ENTER ONLY PATH NAME (NO NEED TO ADD ANY '/' OR '\'
	 * @return file object
	 * @throws Exception on Errors
	 */
	public final File save(final String fieldName, final String path) throws Exception {

		if (caughtException != null) {
			throw caughtException;
		}

		if (fileFields.containsKey(fieldName)) {
			File file = null;
			String fileName = "";
			DiskFileItem fileItem = fileFields.get(fieldName);

			StringTokenizer st = new StringTokenizer(fileItem.getName(), "\\");

			while (st.hasMoreTokens()) {
				fileName = st.nextToken();
			}

			file = new File(repositoryPath + "\\" + path);
			file.mkdir();

			file = new File(repositoryPath + "\\" + path + "\\" + fileName);
			if (file.exists()) {
				throw new FileUploadException("File already exists");
			} else {
				file.createNewFile();
			}

			try {
				fileItem.write(file.toPath());
			} catch (Exception e) {
				e.printStackTrace();
			}

			return file;
		} else {
			return null;
		}
	}

	/**
	 * directly store the specified uploaded file into the filepath and return
	 * the representing file object. filePath is the complete path and filename.
	 * @param fieldName name of the HTTP field
	 * @param filePath absolute path
	 * @return File a file Object
	 * @throws Exception on Errors
	 */
	public final File saveAs(final String fieldName, final String filePath) throws Exception {
		if (caughtException != null) {
			throw caughtException;
		}

		if (fileFields.containsKey(fieldName)) {
			File file = null;
			DiskFileItem fileItem = fileFields.get(fieldName);

			try {
				// TODO see that happents when filepath points to a directory
				// that doesn't exist
				file = new File(repositoryPath + "\\" + filePath);
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				fileItem.write(file.toPath());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return file;
		}

		return null;
	}

	/**
	 * @return Returns the fileItems.
	 */
	public final List<DiskFileItem> getFileItems() {
		return fileItems;
	}

	/**
	 * @return Returns the maxFilesize.
	 */
	public long getMaxFilesize() {
		return maxFilesize;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}
}
