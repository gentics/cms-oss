/*
 * @author Stefan Hepp
 * @date ${date}
 * @version $Id: RenderableParserTag.java,v 1.6 2010-04-15 14:09:12 floriangutmann Exp $
 */
package com.gentics.contentnode.parser.tag.parsertag;

import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver.PropertyPathEntry;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.resolving.StackResolver;

/**
 * This is a wrapper implementation of a ParserTag, which renders any objects.
 * If the object implements Renderable, the render-method of the object is used to create the
 * output, else the object is rendered to a string using toString().
 */
public class RenderableParserTag implements ParserTag {

	private Object renderObject;
    
	private List resolvedPath;

	/**
	 * Constructor to create a new wrapper which renderes the given object.
	 * @param obj the object to render.
	 */
	public RenderableParserTag(Object obj) {
		this.renderObject = obj;
	}
    
	/**
	 * constructor for a renderable parser tag which also has a resolved path which will be put on the stack.
	 * @param obj
	 * @param resolvedPath
	 */
	public RenderableParserTag(Object obj, List resolvedPath) {
		this.renderObject = obj;
		this.resolvedPath = resolvedPath;
	}

	public boolean hasClosingTag() {
		return false;
	}

	public boolean isClosingTag() {
		return false;
	}

	public String getTagEndCode() {
		return null;
	}

	public String[] getSplitterTags() {
		return new String[0];
	}

	public boolean doPreParseCode(boolean defaultValue, String part) throws NodeException {
		return defaultValue;
	}

	public boolean doPostParseCode(boolean defaultValue) {
		return defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	public String render(RenderResult renderResult, String template, Map codeParts) throws NodeException {
		return render(renderResult);
	}

	/**
	 * Render the object, either using toString() or render, if the object is Renderable.
	 * @param renderResult
	 *
	 * @return the rendered object, or an empty string if the object is null.
	 */
	public String render(RenderResult renderResult) throws NodeException {

		String source = "";
        
		StackResolver stack = null;
		Tag pushedTag = null;

		if (resolvedPath != null && renderObject instanceof Value) {
			// if we are rendering a value, we need to put the tag on the
			// stack so we can resolve tag parts ..
			stack = TransactionManager.getCurrentTransaction().getRenderType().getStack();
			for (int i = resolvedPath.size() - 1; i > -1 && pushedTag == null; i--) {
				Object obj = resolvedPath.get(i);
                
				if (obj instanceof PropertyPathEntry) {
					obj = ((PropertyPathEntry) obj).getEntry();
				}
                
				if (obj instanceof Tag) {
					if (!stack.getObjectStack().contains(obj)) {
						// only push tag on stack if it isn't already there ..
						pushedTag = (Tag) obj;
						stack.push(pushedTag);
					}
				}
			}
		}

		try {
			if (renderObject instanceof GCNRenderable) {
				source = ((GCNRenderable) renderObject).render(renderResult);
			} else if (renderObject != null) {
				source = renderObject.toString();
			}
		} finally {
			if (pushedTag != null && stack != null) {
				stack.pop();
			}
		}
        
		return source;
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isAlohaBlock()
	 */
	public boolean isAlohaBlock() throws NodeException {
		return false;
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isEditable()
	 */
	public boolean isEditable() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isInlineEditable()
	 */
	public boolean isInlineEditable() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditLink(boolean, int, boolean, boolean)
	 */
	public String getEditLink() throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPostfix()
	 */
	public String getEditPostfix() throws NodeException {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPrefix()
	 */
	public String getEditPrefix() throws NodeException {
		return "";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (renderObject instanceof NodeObject) {
			return renderObject.toString();
		}
		return super.toString();
	}
}
