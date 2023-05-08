/*
 * @author herbert
 * @date 04.09.2006
 * @version $Id: DatasourceModificationException.java,v 1.1 2006-09-05 11:15:42 herbert Exp $
 */
package com.gentics.api.lib.datasource;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Exception that is thrown if an error occurs during a modification
 * (insert, update) of a datasource record.
 * @author herbert
 */
public class DatasourceModificationException extends DatasourceException {
	private static final long serialVersionUID = 1L;
	private Resolvable record;
    
	public DatasourceModificationException(Resolvable record, String message) {
		super(message);
		this.record = record;
	}
    
	public Resolvable getRecord() {
		return this.record;
	}
}
