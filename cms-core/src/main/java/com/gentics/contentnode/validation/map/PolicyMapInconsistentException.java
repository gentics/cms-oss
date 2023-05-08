/*
 * @author tobiassteiner
 * @date Jan 12, 2011
 * @version $Id: PolicyMapInconsistentException.java,v 1.1.2.1 2011-02-10 13:43:34 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.map;

public class PolicyMapInconsistentException extends RuntimeException {
	private static final long serialVersionUID = -3816396788634221596L;
	public PolicyMapInconsistentException(String cause) {
		super(cause);
	}
}
