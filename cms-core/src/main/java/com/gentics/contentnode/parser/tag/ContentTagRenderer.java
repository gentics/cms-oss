/*
 * @author Stefan Hepp
 * @date 05.02.2006
 * @version $Id: ContentTagRenderer.java,v 1.17 2009-12-16 16:12:13 herbert Exp $
 */
package com.gentics.contentnode.parser.tag;

import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver.PropertyPathEntry;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.parser.attribute.Attribute;
import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.tag.parsertag.FormatParserTagFactory;
import com.gentics.contentnode.parser.tag.parsertag.RenderableParserTag;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;

/**
 * This is the renderer implementation for node-tags. The ContentTagRenderer uses
 * the AbstractTagParser to find and render tags. This class is also the ParserTagFactory
 * which generates ParserTags for found tags.
 */
public class ContentTagRenderer extends AbstractTagParser implements ParserTagFactory {

	private String[] keyname;
	private AttributeParser attributeParser;

	public ContentTagRenderer(NodePreferences nodePreferences) {
		super(false, false, false);
		attributeParser = new NodeAttributeParser();
		try {
			this.keyname = new String[] { ObjectTransformer.getString(nodePreferences.getProperty("contentnode.tagprefix"), null)};
		} catch (Exception e) {
			NodeLogger.getLogger(getClass()).error("could not retrieve keyname");
		} 
	}
    
	public ContentTagRenderer(String keyname) {
		super(false, false, false);
		attributeParser = new NodeAttributeParser();
		this.keyname = new String[] { keyname };
	}

	protected AttributeParser getAttributeParser(String keyname) {
		return attributeParser;
	}

	protected String[] getKeynames() {
		return keyname;
	}

	public ParserTag getParserTag(String keyname, Map params) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		ParserTag tag = null;

		Attribute property = (Attribute) params.get("property");

		if (property == null) {
			// this is an invalid tag, should not happen.
			// TODO throw exception
			return null;
		}

		Attribute container = (Attribute) params.get("container");
		// TODO handle container

		Object rs = renderType.getStack().resolve(property.getValue(), true);
		String debugTagName = System.getProperty("com.gentics.contentnode.debugtag");
		boolean debugTag = false;
		RenderResult result = null;

		if (debugTagName != null && debugTagName.equals(property.getValue())) {
			debugTag = true;
			result = TransactionManager.getCurrentTransaction().getRenderResult();
			result.debug(ContentTagRenderer.class, "resolved {" + debugTagName + "} - got: {" + rs + "}");
		}
		if (rs != null) {
			List resolvedPath = null;

			if (rs instanceof List) {
				resolvedPath = (List) rs;
				rs = resolvedPath.get(resolvedPath.size() - 1);
				if (rs instanceof PropertyPathEntry) {
					rs = ((PropertyPathEntry) rs).getEntry();
				}
			}
			if (rs instanceof ParserTag) {
				tag = (ParserTag) rs;
			} else {
				tag = new RenderableParserTag(rs, resolvedPath);
			}
		} 

		// TODO handle several format options
		if (tag != null && params.containsKey("format")) {
			Attribute format = (Attribute) params.get("format");

			tag = FormatParserTagFactory.getFormatterParserTag(format.getValue(), tag);
		}

		return tag;
	}

	public ParserTagFactory getParserTagFactory() {
		return this;
	}
}
