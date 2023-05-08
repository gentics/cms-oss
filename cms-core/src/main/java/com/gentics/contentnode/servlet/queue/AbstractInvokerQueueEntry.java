/*
 * @author herbert
 * @date Feb 12, 2008
 * @version $Id: AbstractInvokerQueueEntry.java,v 1.2 2008-03-05 10:49:52 norbert Exp $
 */
package com.gentics.contentnode.servlet.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public abstract class AbstractInvokerQueueEntry implements InvokerQueueEntry {
    
	protected String idParameter = null;
    
	protected Properties additionalParameters = new Properties();
    
	protected void init(String idParameter, String additionalParameters) {
		this.setIdParameter(idParameter);
		this.setAdditionalParameters(additionalParameters);
	}

	public InvokerQueueEntry createQueueEntry(String type, String idParameter,
			String additionalParameters) {
		try {
			AbstractInvokerQueueEntry entry = (AbstractInvokerQueueEntry) this.getClass().newInstance();

			entry.init(idParameter, additionalParameters);
			return entry;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getAdditionalParameters() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			additionalParameters.store(out, null);
		} catch (IOException e) {
			// Can't happen with a bytearrayoutput stream ?!
			throw new RuntimeException(e);
		}
		try {
			return new String(out.toByteArray(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// I'm pretty sure that ISO-8859-1 is always supported
			throw new RuntimeException(e);
		}
	}
    
	protected void setAdditionalParameters(String additionalParametersStr) {
		this.additionalParameters = new Properties();
		try {
			additionalParameters.load(new ByteArrayInputStream(additionalParametersStr.getBytes("ISO-8859-1")));
		} catch (Exception e) {
			// this can't happen with a bytearray input stream.. and.. ISO-8859-1 should be known !
			throw new RuntimeException(e);
		}
	}
    
	public void setIdParameter(String id) {
		this.idParameter = id;
	}

	public String getIdParameter() {
		return idParameter;
	}
    
	public String getParameter(String key) {
		return additionalParameters.getProperty(key);
	}

	public void setParameter(String key, String value) {
		additionalParameters.setProperty(key, value);
	}

	public String toString() {
		return this.getClass().getName() + " type: {" + getType() + "} idparam: {" + getIdParameter() + "} additionalparams: {" + getAdditionalParameters()
				+ "}";
	}
    
	public int getProgress() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.servlet.queue.InvokerQueueEntry#cleanUp()
	 */
	public void cleanUp() {// the default implementation is empty
	}
}
