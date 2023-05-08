/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: RenderReturnCode.java,v 1.2 2006-02-05 14:24:21 stefan Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

/**
 * ReturnCode container for structrenderer, containing an return-cause and the position
 * of the next unhandled codepart. 
 */
public class RenderReturnCode {

	/**
	 * return code if an end-tag is found
	 */
	public final static int RETURN_CLOSED = 0;

	/**
	 * return code if a splitter is found
	 */
	public final static int RETURN_SPLITTER = 1;

	/**
	 * return code if the last element is reached
	 */
	public final static int RETURN_LAST = 2;

	private int pos;
	private int reason;
	private String splitter;

	/**
	 * get a new returncode container.
	 *
	 * @param pos the position in the struct after the last handled element.
	 * @param reason the reason for the return
	 * @param splitter if the return-reason was a splitter-tag, this is the name of it, else null.
	 */
	public RenderReturnCode(int pos, int reason, String splitter) {
		this.pos = pos;
		this.reason = reason;
		this.splitter = splitter;
	}

	public int getPos() {
		return pos;
	}

	/**
	 * get the reason of the returning from the subcall.
	 * @return one of {@link #RETURN_CLOSED}, {@link #RETURN_SPLITTER}, {@link #RETURN_LAST}
	 */
	public int getReason() {
		return reason;
	}

	/**
	 * get the name of the splitter which caused the return
	 * @return the name of the splitter if reason is splitter, else null.
	 */
	public String getSplitter() {
		return splitter;
	}

}
