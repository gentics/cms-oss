/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: TemplatePart.java,v 1.2 2006-02-05 14:24:21 stefan Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

/**
 * A special codepart for inline-code-templates which should be parsed differently.
 */
public class TemplatePart extends CodePart {

	public TemplatePart(int startPos, int endPos) {
		super(startPos, endPos);
	}

}
