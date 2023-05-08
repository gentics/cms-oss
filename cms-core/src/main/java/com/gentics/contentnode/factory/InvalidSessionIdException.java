/*
 * @author tobiassteiner
 * @date Feb 6, 2011
 * @version $Id: InvalidSessionIdException.java,v 1.1.2.1 2011-02-10 13:43:37 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;

public class InvalidSessionIdException extends NodeException {
	private static final long serialVersionUID = -7738567995823074455L;
    
	public InvalidSessionIdException(String sessionId, Exception e) {
		super("Invalid session ID: `" + sessionId + "'", e);
	}
    
	public InvalidSessionIdException(String sessionId) {
		this(sessionId, null);
	}
}
