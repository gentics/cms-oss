/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:41
 * @version $Id: AbstractImportParser.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;
import com.gentics.lib.content.GenticsContentObject;
import com.gentics.lib.log.NodeLogger;

/**
 * A abstract base implementation of an import parser which does some format processing.
 */
public abstract class AbstractImportParser implements ContentImportParser {

	private ContentImportLogger logger;

	private String attrName;

	private String format;
    
	private String[] formatList;
    
	private boolean isBinary;
    
	private String filePrefix; 

	/**
	 * init the parser.
	 * @param logger the logger to use during parsing.
	 * @param attrName the name of the object attribute where new data is stored.
	 */
	protected AbstractImportParser(ContentImportLogger logger, String attrName) {
		super();
		this.logger = logger;
		this.attrName = attrName;
		this.isBinary = false;
		this.filePrefix = null;
	}

	public ContentImportLogger getLogger() {
		return logger;
	}

	public String getAttributeName() {
		return attrName;
	}

	public String getFilePrefix() {
		return filePrefix;
	}

	public void setFilePrefix(String prefix) {
		this.filePrefix = prefix;
	}

	public void setFormat(String format) {
		this.format = format;
        
		if (format == null || "".equals(format)) {
			this.formatList = new String[] {};
		} else {
			this.formatList = format.split(",");
		}
        
		isBinary = hasFormat("binary");
	}

	/**
	 * get the current format string.
	 * @return the current format string, or null if not set.
	 */
	public String getFormat() {
		return format;
	}
    
	/**
	 * check if a format is given in the comma-separated list of formats.
	 * @param format the format to check
	 * @return true if the format is set in the format list, else false.
	 */
	public boolean hasFormat(String format) {

		for (int i = 0; i < formatList.length; i++) {
			if (formatList[i].trim().equals(format)) {
				return true;
			}
		}
        
		return false;
	}

	/**
	 * use the current format string to preprocess the value.
	 * TODO implement, add external format parser api.
	 * @param value the value of the field to parse.
	 * @return the formatted value of the field.
	 * @throws ContentImportException 
	 */
	protected Object parseFormat(String value) throws ContentImportException {

		Object rs = value;
        
		if (isBinary && value != null && !"".equals(value.trim())) {
			boolean hasPrefix = filePrefix != null && !"".equals(filePrefix);
			String filename = hasPrefix ? filePrefix + File.separator + value : value; 
            
			File file = new File(filename);

			if (file.canRead() && file.isFile()) {
                
				try {
                    
					InputStream is = new FileInputStream(file);
                    
					if (file.length() > Integer.MAX_VALUE) {
						throw new ContentImportException("File {" + filename + "} is too large.");
					}
                    
					byte[] bytes = new byte[(int) file.length()];
                    
					int offset = 0;
					int numRead = 0;

					try {
						while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
							offset += numRead;
						}
					} catch (IOException e) {
						throw new ContentImportException("Could not read file {" + filename + "}.", e);
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							NodeLogger.getLogger(AbstractContentImport.class).warn("Could not close file.", e);
						}
					}
                    
					if (offset < bytes.length) {
						throw new ContentImportException("Could not completely read file {" + filename + "}");
					}
                    
					rs = bytes;                    
				} catch (FileNotFoundException e) {
					throw new ContentImportException("Could not read file {" + filename + "}.", e);
				}
                
			} else {
				throw new ContentImportException("Could not read file {" + filename + "}.");
			}
		}
        
		return rs;
	}

	/**
	 * use the current format string to preprocess the value.
	 * This method only returns string values, and does not resolve them
	 * into binaries or objects. 
	 * TODO implement, add external format parser api.
	 * @param value the value of the field to parse.
	 * @return the formatted value of the field.
	 * @throws ContentImportException 
	 */
	protected String parseFormatAsString(String value) throws ContentImportException {
        
		return value;
	}
    
	/**
	 * helper method to store a value to the contentobject.
	 * @param cnObj the object to store the data.
	 * @param value the value to store in the attribute.
	 * @throws ContentImportException
	 */
	protected void setAttributeValue(GenticsContentObject cnObj, Object value) throws ContentImportException {
		try {
			cnObj.setAttribute(attrName, value);
		} catch (NodeIllegalArgumentException e) {
			throw new ContentImportException("Could not append attribute.", e);
		} catch (CMSUnavailableException e) {
			throw new ContentImportException("Could not connect to db.", e);
		}
	}
}
