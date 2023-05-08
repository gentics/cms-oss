/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: TagStructParser.java,v 1.17 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.contentnode.parser.attribute.AttributeParser;
import com.gentics.contentnode.parser.attribute.AttributeResult;
import com.gentics.contentnode.parser.tag.TagParser;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * An implementation of a StructParser, which search for xml-like tags.
 */
public class TagStructParser implements StructParser {

	private static StructParser parser = new TagStructParser();
    
	private static NodeLogger logger = NodeLogger.getNodeLogger(TagStructParser.class);

	public TagStructParser() {}

	/**
	 * get a static instance of this parser.
	 * @return a reference to a static instance of this parser.
	 */
	public static StructParser getInstance() {
		return parser;
	}

	public List parseToStruct(TagParser tagParser, RenderResult renderResult, String template, String keyname,
			AttributeParser parser) throws NodeException {

		List tagsOpen = loadTagParts(renderResult, template, keyname, false, parser);

		List tagsClosed = loadTagParts(renderResult, template, keyname, true, parser);

		// TODO find templatePart (code which is not to be translated..)

		List struct = new ArrayList((tagsOpen.size() + tagsClosed.size()) * 2);

		// merge the lists, add stringparts
		Iterator itOpen = tagsOpen.iterator();
		Iterator itClosed = tagsClosed.iterator();

		TagPart open = itOpen.hasNext() ? (TagPart) itOpen.next() : null;
		TagPart closed = itClosed.hasNext() ? (TagPart) itClosed.next() : null;

		int codePos = 0;
		CodePart nextTag = null;

		while (open != null || closed != null) {
			// remove nested tagparts
			while ((open != null && open.isNestedIn(closed)) || (closed != null && closed.isNestedIn(open))) {
				if (open != null && open.isNestedIn(closed)) {
					logger.warn(
							"Dropping {" + open + " (" + open.getCode(template) + ")}, which is nested in {" + closed + " (" + closed.getCode(template) + ")}");
					open = itOpen.hasNext() ? (TagPart) itOpen.next() : null;
				} else if (closed != null && closed.isNestedIn(open)) {
					logger.warn(
							"Dropping {" + closed + " (" + closed.getCode(template) + ")}, which is nested in {" + open + " (" + open.getCode(template) + ")}");
					closed = itClosed.hasNext() ? (TagPart) itClosed.next() : null;
				}
			}

			if (open == null || (closed != null && open.getStartPos() > closed.getStartPos())) {

				nextTag = closed;

				closed = itClosed.hasNext() ? (TagPart) itClosed.next() : null;

			} else if (closed == null || (open != null && open.getStartPos() < closed.getStartPos())) {

				nextTag = open;

				open = itOpen.hasNext() ? (TagPart) itOpen.next() : null;
			}

			if (nextTag.getStartPos() - codePos > 0) {
				struct.add(new StringPart(codePos, nextTag.getStartPos()));
			}
			struct.add(nextTag);

			codePos = nextTag.getEndPos();
		}

		if (template.length() - codePos > 0) {
			struct.add(new StringPart(codePos, template.length()));
		}

		int level = 0;
		int pos = 0;
		// find closing tags for opening tags and set tags to closed, when no suitable closing tag found
		boolean debug = logger.isDebugEnabled();
		Stack openedTags = new Stack();

		for (Iterator iter = struct.iterator(); iter.hasNext();) {
			CodePart codePart = (CodePart) iter.next();

			if (codePart instanceof TagPart) {
				TagPart tagPart = (TagPart) codePart;

				// check the part, it might be an end (but notated like open tags)
				if (parser.isEndTag(tagPart)) {
					tagPart.setType(TagPart.TYPE_END);
				}

				if (tagPart.getType() == TagPart.TYPE_OPEN) {
					if (parser.isSplitterTag(tagPart)) {
						// found a splitter tag
						tagPart.setType(TagPart.TYPE_SPLITTER);
						if (debug) {
							level = openedTags.size();
							logger.debug(StringUtils.repeat("  ", level - 1) + tagPart.debugOutput(template) + " (splitter @pos " + pos + ")");
						}
					} else {
						if (debug) {
							level = openedTags.size();
							logger.debug(StringUtils.repeat("  ", level) + tagPart.debugOutput(template) + " (start @pos " + pos + ")");
						}
						// push opening tags onto the stack
						openedTags.push(tagPart);
					}
				} else if (tagPart.getType() == TagPart.TYPE_END) {
					// TODO find the topmost matching opening tag and remove all opening tags in between (they are closed).
					if (openedTags.size() == 0) {
						if (logger.isDebugEnabled()) {
                        	
							logger.debug(
									"position: {"
											+ template.substring(Math.max(0, tagPart.getStartPos() - 100), Math.min(template.length() - 1, tagPart.getEndPos() + 100)) + "}");
							logger.debug("Template: " + template + "  ... position: {" + tagPart.getStartPos() + "}");
							logger.debug("Tagname: {" + tagPart.getKeyname() + "}");
						}
						renderResult.error(TagStructParser.class, "Found an endtag without an opening tag.",
								new ParserException("Found end tag without opening tag."));
					}
					int openedTagsSize = openedTags.size();

					for (int i = openedTagsSize - 1; i >= 0; --i) {
						TagPart opener = (TagPart) openedTags.get(i);

						if (parser.isMatchingPair(opener, tagPart)) {
							// found the matching start tag
							// remove all tags from the stack (including the found one)
							TagPart poped = null;

							while (poped != opener) {
								poped = (TagPart) openedTags.pop();
								if (poped != opener) {
									// all tags removed in between are
									// automatically closed.
									poped.setType(TagPart.TYPE_CLOSED);
								}
								if (debug) {
									level = openedTags.size();
									logger.debug(StringUtils.repeat("  ", level) + tagPart.debugOutput(template) + " (end @pos " + pos + ")");
								}
							}
							break;
						}
					}
				}
			}
			pos++;
		}

		// all remaining opened tags are closed now
		while (!openedTags.isEmpty()) {
			TagPart poped = (TagPart) openedTags.pop();

			poped.setType(TagPart.TYPE_CLOSED);
		}

		return struct;
	}

	/**
	 * find the tags of a given keyname in the code and return all found tags as list of TagParts.
	 *
	 * @param renderResult the current renderresult.
	 * @param template the template to parse.
	 * @param keyname the name of the tags to be found.
	 * @param closed true, if closing tags should be searched, else false.
	 * @param parser the attributeparser to use to parse the attributes of tags.
	 * @return a list of found tags as {@link TagPart}, or an empty list of no tags have been found.
	 * @throws NodeException 
	 */
	private List loadTagParts(RenderResult renderResult, String template, String keyname, boolean closed, AttributeParser parser) throws NodeException {

		List tags = new ArrayList(20);

		String search = (closed ? "</" : "<") + keyname;

		int pos = 0;

		while ((pos = template.indexOf(search, pos)) >= 0) {
			int startPos = pos + search.length();
			if (startPos >= template.length()) {
				// do nothing because we are at the end of the template and we have not found a full node-tag
				break;
			}
			char c = template.charAt(startPos);

			if (c == ' ') {
				startPos++;
				while (startPos < template.length() && template.charAt(startPos) == ' ') {
					startPos++;
				}

				if (startPos >= template.length()) {
					// do nothing because we are at the end of the template and we have not found a full node-tag
					break;
				}
				AttributeResult result = parser.parseAttributes(renderResult, template, startPos, closed);

				// TODO parse attribute-values (this does not allow to completely build up attributes, only its values!)
                

				if (result == null || result.getEndPos() < 0) {
					// whoo, severe error in tag!
					renderResult.error("Invalid attribute", "Could not parse attributes of " + keyname + "-tag.");
					break;
				}

				int type = closed ? TagPart.TYPE_END : TagPart.TYPE_OPEN;

				if (!closed && result.isClosed()) {
					type = TagPart.TYPE_CLOSED;
				}

				// restrict the endpost of the TagPart with the template length
				TagPart tag = new TagPart(pos, Math.min(result.getEndPos() + 1, template.length()), keyname, type, result.getAttributes());

				tags.add(tag);

				pos = tag.getEndPos();

			} else if (c == '>') {

				int type = closed ? TagPart.TYPE_END : TagPart.TYPE_OPEN;
				TagPart tag = new TagPart(pos, pos + search.length() + 1, keyname, type, Collections.EMPTY_MAP);

				tags.add(tag);

				pos = tag.getEndPos();
			} else if (c == ':') {
				// TODO support for namespaces ..
				pos += search.length() + 1;
			} else {
				// not a valid tag of this keyname
				pos += search.length();

			}
		}

		return tags;
	}

}
