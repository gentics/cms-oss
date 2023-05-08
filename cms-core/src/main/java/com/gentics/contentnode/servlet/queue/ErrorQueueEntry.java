/*
 * @author herbert
 * @date Apr 15, 2008
 * @version $Id: ErrorQueueEntry.java,v 1.2 2008-05-26 15:05:57 norbert Exp $
 */
package com.gentics.contentnode.servlet.queue;

public class ErrorQueueEntry extends AbstractInvokerQueueEntry {

	public String getType() {
		return "error";
	}

	public void invoke() {
		throw new Error("testing errors");
	}

}
