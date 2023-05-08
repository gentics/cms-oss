/*
 * @author herbert
 * @date Feb 12, 2008
 * @version $Id: SleepingQueueEntry.java,v 1.1 2008-02-15 09:37:59 herbert Exp $
 */
package com.gentics.contentnode.servlet.queue;

public class SleepingQueueEntry extends AbstractInvokerQueueEntry implements InvokerQueueEntry {

	public String getType() {
		return "sleep";
	}

	public void invoke() {
		int secs = Integer.parseInt(getParameter("secs"));

		try {
			Thread.sleep(secs * 1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
