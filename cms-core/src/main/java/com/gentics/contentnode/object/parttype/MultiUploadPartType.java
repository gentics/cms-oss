/*
 * @author johannes2
 * @date Aug 30, 2010
 * @version $Id: MultiUploadPartType.java,v 1.1 2010-09-03 15:02:17 johannes2 Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;

/**
 * Parttype 38 - File (Multiupload)
 * @author johannes2
 *
 */
public class MultiUploadPartType extends FileURLPartType {

	private static final long serialVersionUID = 1L;

	public MultiUploadPartType(Value value) throws NodeException {
		super(value);
		// TODO Auto-generated constructor stub
	}

}
