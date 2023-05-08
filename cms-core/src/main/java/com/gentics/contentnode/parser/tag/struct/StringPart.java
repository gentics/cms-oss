/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: StringPart.java,v 1.3 2007-03-19 07:57:35 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

/**
 * This is a special codepart for string-only code parts.
 */
public class StringPart extends CodePart {

	public StringPart(int startPos, int endPos) {
		super(startPos, endPos);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "StringPart (" + getStartPos() + " - " + getEndPos() + ")";
	}
}
