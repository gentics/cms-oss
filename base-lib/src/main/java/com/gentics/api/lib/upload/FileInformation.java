package com.gentics.api.lib.upload;

import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.lib.log.NodeLogger;
import jakarta.activation.MimetypesFileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.fileupload2.core.DiskFileItem;

/**
 * Provides information about an uploaded file.
 */
@SuppressWarnings("serial")
public class FileInformation extends ResolvableBean {

	public static NodeLogger logger = NodeLogger.getNodeLogger(FileInformation.class);

	/**
	 * Splits the file name from the path.
	 */
	Pattern fileNamePathPattern = Pattern.compile("^(.*)[/\\\\](.*)$");

	/**
	 * Holds the file item
	 */
	private DiskFileItem fileItem;

	private String fileName;
    
	/**
	 * Contains the bare file name (ie. without path information)
	 */
	private String bareFileName;

	private String filePath = null;

	private long fileSize = 0;

	private String contentType;

	/**
	 * The file data cached in-memory
	 */
	private byte[] fileData;
    
	private FileUploadProvider fileUploadProvider;

	/**
	 * The file data saved in a temporary file.
	 */
	private File file;

	/**
	 * when true, the file will be deleted in {@link #invalidate()}, when false
	 * the file will not be deleted
	 */
	private boolean deleteFileOnInvalidate = true;

	/**
	 * Called by the GC - invalidates this instance to delete tmp files.
	 */
	protected void finalize() {
		invalidate();
	}

	/**
	 * Create a FileInformation object with preset data
	 * @param fileName name of the file
	 * @param fileSize size of the file
	 * @param contentType content type
	 * @param fileData file data as byte array
	 */
	public FileInformation(String fileName, long fileSize, String contentType, byte[] fileData) {
		this(fileName, fileSize, contentType, fileData, null, true);
	}

	/**
	 * Create a FileInformation object base of the given FileItem
	 * @param myFileItem file item
	 * @param fileUploadProvider file upload provider
	 */
	public FileInformation(final DiskFileItem myFileItem, FileUploadProvider fileUploadProvider) {
		this.fileItem = myFileItem;
		this.fileUploadProvider = fileUploadProvider;
		this.fileName = fileItem.getName();
	}

	/**
	 * Create an instance of fileinformation
	 * @param fileName filename
	 * @param fileSize filesize
	 * @param contentType contenttype
	 * @param fileData filedata
	 * @param file file
	 * @param deleteFileOnInvalidate true when the file will be deleted, false if not
	 */
	public FileInformation(String fileName, long fileSize, String contentType, byte[] fileData, File file, boolean deleteFileOnInvalidate) {
		this.fileItem = null;
		this.fileName = fileName;
		this.filePath = file != null ? file.getAbsolutePath() : fileName;
		this.fileSize = fileSize;
		this.contentType = contentType;
		this.fileData = fileData;
		this.file = file;
		this.deleteFileOnInvalidate = deleteFileOnInvalidate;
	}

	/**
	 * Create an instance of fileinformation based on the given file. The file
	 * will not be deleted on {@link #invalidate()}.
	 * @param file file
	 */
	public FileInformation(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		this.fileItem = null;
		this.fileName = file.getName();
		this.bareFileName = file.getName();
		this.filePath = file.getAbsolutePath();
		this.fileSize = file.length();
		this.contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file);
		this.fileData = null;
		this.file = file;
		this.deleteFileOnInvalidate = false;
	}

	/**
	 * Get the file size in bytes.
	 * @return file size in bytes
	 */
	public final long getFileSize() {
		return fileItem != null ? fileItem.getSize() : fileSize;
	}

	/**
	 * Returns the delivered ContentType of the File
	 * @return contentType
	 */
	public final String getContentType() {
		return fileItem != null ? fileItem.getContentType() : contentType;
	}
    
	/**
	 * Returns the "original" filename which was passed by the browser.
	 * (E.g. Opera / IE will give you the full path name, while firefox only sends the file name)
	 * @return filename as sent by the browser/client.
	 */
	public final String getOriginalFileName() {
		return fileName;
	}
    
	/**
	 * Splits the filename as passed by the browser into a bare filename
	 * and path.
	 */
	private void splitFileName() {
		Matcher matcher = fileNamePathPattern.matcher(fileName);

		if (matcher.find() && matcher.groupCount() == 2) {
			bareFileName = matcher.group(2);
			filePath = matcher.group(1);
		} else {
			bareFileName = fileName;
			filePath = "";
		}
	}

	/**
	 * Returns the relative Path of the File. Take care -&gt; currently MacOS
	 * delivers only relative Paths relative path? shouldn't this be the only
	 * the filename, without path? TODO this method should be the solution to
	 * this problem, by astrahating this, so applications don't have to take
	 * care of that.
	 * @return name of the uploaded file
	 */
	public final String getFileName() {
		if (bareFileName == null) {
			splitFileName();
		}
		return bareFileName;
	}

	/**
	 * Returns the upload Path if available or null if not. Currently Windows
	 * sends the complete path in the HTTP request, while MacOS does not. Unsure
	 * about other Systems. Unix is supposed to send only relative paths, but it
	 * is not clarified if this is correct.
	 * @return filePath
	 */
	public final String getFilePath() {
		if (filePath == null) {
			splitFileName();
		}
		return filePath;
	}

	/**
	 * Get an input stream for reading the content of an uploaded file
	 * @return input stream or null if no data available
	 * @throws IOException when fetching the input stream fails
	 */
	public final InputStream getInputStream() throws IOException {
		if (fileItem != null) {
			return fileItem.getInputStream();
		}
		if (fileData != null && fileData.length > 0) {
			return new ByteArrayInputStream(fileData);
		}
		if (file != null && file.exists()) {
			return new FileInputStream(file);
		}
		return null;
	}

	/**
	 * Can be used to retrieve a clone of a file item if it is needed longer
	 * than one request long.
	 * @return a temporary file with the content of the uploaded file. (null if an error occurred.)
	 */
	public FileInformation cloneFileInformation() {
		if (fileItem == null) {
			return new FileInformation(fileName, fileSize, contentType, fileData, file, deleteFileOnInvalidate);
		}
		byte[] memoryCache = null;
		File tmpfile = null;
		// HP 20060906 - after calling fileItem.write(..) some information is not available, so store them before.
		String fileName = fileItem.getName();
		long size = fileItem.getSize();
		String contentType = fileItem.getContentType();

		if (fileItem.isInMemory()) {
			memoryCache = fileItem.get();
		} else {
			try {
				if (fileUploadProvider == null) {
					tmpfile = File.createTempFile("upload-", ".tmp");
				} else {
					tmpfile = File.createTempFile("upload-", ".tmp", new File(fileUploadProvider.getRepositoryPath()));
				}
				fileItem.write(tmpfile.toPath());
				tmpfile.deleteOnExit();
			} catch (Exception e) {
				logger.error("Error while writing to temporary file.", e);
				return null;
			}
		}
		FileInformation fileInformation = new FileInformation(fileName, size, contentType, memoryCache, tmpfile, true);

		fileInformation.setFileUploadProvider(fileUploadProvider);
		return fileInformation;
	}

	/**
	 * Sets the FileUploadProvider used by this instance of FileInformation.
	 * @param fileUploadProvider the fileUploadProvider used to query threshold for memory cache and repositoryPath for tmp files.
	 */
	private void setFileUploadProvider(FileUploadProvider fileUploadProvider) {
		this.fileUploadProvider = fileUploadProvider;
	}

	/**
	 * Invalidates this instance of FileInformation. (ie. deletes temp files,
	 * unreferences memory buffer, etc.)
	 */
	public void invalidate() {
		if (fileItem != null) {
			try {
				fileItem.delete();
			} catch (IOException e) {
				logger.warn("Error while deleting file " + fileItem.getName(), e);
			}
		}
		if (fileData != null) {
			fileData = null;
		}
		if (file != null && deleteFileOnInvalidate) {
			file.delete();
		}
	}
}
