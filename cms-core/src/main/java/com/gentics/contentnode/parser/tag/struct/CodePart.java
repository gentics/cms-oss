/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: CodePart.java,v 1.5 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import com.gentics.lib.etc.StringUtils;

/**
 * A CodePart stores information about a piece of code of a template. This is used by the
 * structparsers to return the list of tag-positions. The CodePart stores only positions,
 * not the code of a part itself to save memory.
 */
public abstract class CodePart {
	private int startPos;
	private int endPos;

	protected CodePart(int startPos, int endPos) {
		this.startPos = startPos;
		this.endPos = endPos;
	}

	/**
	 * get the position of the first char after the codepart.
	 * @return the position of the first char not included in the part.
	 */
	public int getEndPos() {
		return endPos;
	}

	/**
	 * get the position of the first char of the codepart.
	 * @return the position of the first char of the codepart.
	 */
	public int getStartPos() {
		return startPos;
	}

	/**
	 * Get the code of the part.
	 * @param template the parsed template.
	 * @return the code of this part.
	 */
	public String getCode(String template) {
		return template.substring(startPos, endPos);
	}

	/**
	 * Get debug output of the part
	 * @param template template
	 * @return debug output
	 */
	public String debugOutput(String template) {
		StringBuffer buffer = new StringBuffer();
		int[] pos = StringUtils.findPosition(template, startPos);

		buffer.append(getCode(template)).append(" [").append("line: ").append(pos[0]).append(", col: ").append(pos[1]).append("]");
		return buffer.toString();
	}

	/**
	 * Check whether this codepart is completely nested in the given codepart.
	 * @param part other codepart
	 * @return true when the codepart is nested in the other, false if not
	 */
	public boolean isNestedIn(CodePart part) {
		if (part == null) {
			return false;
		}
		return getStartPos() >= part.getStartPos() && getEndPos() <= part.getEndPos();
	}
}
