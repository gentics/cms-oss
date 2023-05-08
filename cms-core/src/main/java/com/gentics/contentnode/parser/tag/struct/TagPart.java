/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: TagPart.java,v 1.7 2008-03-07 12:53:42 norbert Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.attribute.Attribute;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.parser.tag.ParserTagFactory;

/**
 * A TagPart is a special codepart containing additional informations about a tag,
 * like the attributes, the tag-type (open, closed, closing) or the keyname.
 * The Tagpart can use a ParserTagFactory to get the corresponding {@link ParserTag}.
 */
public class TagPart extends CodePart {
	private String keyname;
	private int type;
	private Map attributes;
	private ParserTag parserTag;

	/**
	 * Type for tags which have a template or other sub-elements.
	 */
	public static final int TYPE_OPEN = 1;

	/**
	 * Type for tags which do not have any sub-elements and do not need to be closed by a closing tag.
	 */
	public static final int TYPE_CLOSED = 2;

	/**
	 * Type for tags which close a template of mark the end of nested tags.
	 */
	public static final int TYPE_END = 3;

	/**
	 * Type for tags that split the content of tags into two (or more) parts
	 */
	public static final int TYPE_SPLITTER = 4;

	/**
	 * create a new Tagpart and fill it with the tag information.
	 *
	 * @param startPos startposition in the code of the tag.
	 * @param endPos the position of the next char after the tag.
	 * @param keyname the name of the tag.
	 * @param type the type flag of the tag, defined by the syntax of the tag.
	 * @param attributes the keynames and the attribute-values of the tag, as String->{@link Attribute}.
	 */
	public TagPart(int startPos, int endPos, String keyname, int type, Map attributes) {
		super(startPos, endPos);
		this.keyname = keyname;
		this.type = type;
		this.attributes = attributes;
		this.parserTag = null;
	}

	/**
	 * Get the matching parsertag for this parttype, using a given ParserTagFactory.
	 *
	 * @param factory the factory which should be used to create the parsertag.
	 * @return a new parsertag, or null if no parsertag could be created.
	 */
	public ParserTag getParserTag(ParserTagFactory factory) throws NodeException {

		if (parserTag == null && factory != null) {
			parserTag = factory.getParserTag(keyname, attributes);
		}

		return parserTag;
	}

	/**
	 * get the list of attributes of this tag.
	 * @return the attributekeys and their values as String->{@link Attribute}.
	 */
	public Map getAttributes() {
		return attributes;
	}

	/**
	 * get the name of this tag.
	 * @return the name of this tag.
	 */
	public String getKeyname() {
		return keyname;
	}

	/**
	 * get the tag-type of this tag. This may be one of {@link #TYPE_CLOSED},
	 * {@link #TYPE_END} or {@link #TYPE_OPEN}.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set the tag-type of this tag. May be on of {@link #TYPE_CLOSED},
	 * {@link #TYPE_END} or {@link #TYPE_END}.
	 * @param type type
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * check if this tag matches the given end-code.
	 *
	 * @see com.gentics.contentnode.parser.tag.ParserTag#getTagEndCode()
	 * @param endCode code to check this tag against.
	 * @return true if this is the search end-tag.
	 */
	public boolean matchEndCode(String endCode) {
		if (endCode == null) {
			return false;
		}
		return type == TYPE_END && attributes.containsKey(endCode);
	}

	/**
	 * check if this tag matches a splitter-code list.
	 *
	 * @see com.gentics.contentnode.parser.tag.ParserTag#getSplitterTags()
	 * @param splitter list of splitter-codes
	 * @return the key of the matching splitter, or null if not matching
	 */
	public String matchSplitterCodes(String[] splitter) {
		if (type == TYPE_END || splitter == null) {
			return null;
		}
		for (int i = 0; i < splitter.length; i++) {
			String key = splitter[i];

			if (attributes.containsKey(key)) {
				// thou shalt be a splitter, exitus.
				return key;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String typeString = null;

		switch (type) {
		case TYPE_CLOSED:
			typeString = "closed";
			break;

		case TYPE_OPEN:
			typeString = "open";
			break;

		case TYPE_END:
			typeString = "end";
			break;

		case TYPE_SPLITTER:
			typeString = "splitter";
			break;

		default:
			typeString = "undefined";
			break;
		}
		return typeString + " TagPart [" + keyname + "] (" + getStartPos() + " - " + getEndPos() + ") attributes {" + String.valueOf(attributes) + "}";
	}
}
