/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: AbstractAttributeParser.java,v 1.8 2007-08-17 10:37:24 norbert Exp $
 */
package com.gentics.contentnode.parser.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.parser.expression.Expression;
import com.gentics.contentnode.parser.expression.parser.ExpressionParser;
import com.gentics.contentnode.parser.tag.struct.TagPart;
import com.gentics.contentnode.render.RenderResult;

/**
 * This is an abstract implementation for an attribute parser. This implementation can be used to
 * build your own, xml compatible attribute parser.
 * The parser supports quotes, parenthesis, escapes, expressions and closing slashes. The preparsed
 * attributes are normalized and loaded into a map using {@link #normalizeAttributes(RenderResult, List)}
 * in the implementation of this class. The tags are expected to be closed with &gt;
 */
public abstract class AbstractAttributeParser implements AttributeParser {

	private boolean handleEscape;
	private boolean handleParenthesis;
	private boolean stripQuotes;

	/**
	 * Main constructor for this parser.
	 * @param handleEscape true, if escape chars (backslashes) should be handled.
	 * @param handleParenthesis true, if parenthesis should initialize expression-parsing.
	 * @param stripQuotes true, if surrounding quotes should be striped from the attributes.
	 */
	protected AbstractAttributeParser(boolean handleEscape, boolean handleParenthesis, boolean stripQuotes) {
		this.handleEscape = handleEscape;
		this.handleParenthesis = handleParenthesis;
		this.stripQuotes = stripQuotes;
	}

	/**
	 * This is the main parser implementation. Parse the code for attributes until the tag's end is found, and
	 * build the attributemap using {@link #normalizeAttributes(RenderResult, List)}. This parser supports
	 * the basic xml-style syntax, but also some 'advanced' compatibility syntax features.
	 *
	 * @param renderResult
	 * @param code the code to be parsed
	 * @param startPos the position where parsing is started
	 * @param isEndTag true, if the tag is a closing tag.
	 * @return a new attributeresult object with the results.
	 * @throws NodeException 
	 */
	public AttributeResult parseAttributes(RenderResult renderResult, String code, int startPos, boolean isEndTag) throws NodeException {

		char c;
		boolean closed = false;
		List attribs = new ArrayList();

		AttributePart part = new AttributePart(renderResult, code);

		int pos;

		for (pos = startPos; pos < code.length(); pos++) {
			c = code.charAt(pos);

			// if (handleParenthesis && (c == '(' || c == '[' || c == '{')) {
			if (handleParenthesis && c == '(') {

				final boolean firstChar = !part.isStarted();

				String currentKey = part.getCurrentKey(attribs, pos);

				// ExpressionParser parser = getExpressionParser(currentKey, part.isQuoted(), firstChar);

				if (firstChar) {
					part.setStartPos(pos);
				}

				// if (parser != null) {
				//
				// // small hack to support if( .. ), maybe check for quotes?
				// if ( !part.hasValue() && !firstChar ) {
				// part.finishKey(pos, pos);
				// }
				//
				// ExpressionParserResult result = parser.parse(renderResult, code, pos, part.getQuote());
				// pos = result.getPosition();
				// part.setExpression( result.getExpression() );
				// }
				// TODO find the matching end of the parenthesis and update pos to the last position
				// TODO set the expression string into the part

				// small hack to support if( .. ), maybe check for quotes? 
				if (!part.hasValue() && !firstChar) {
					part.finishKey(pos, pos);
				}

				StringBuffer expression = new StringBuffer();

				pos = findClosing(code, pos, expression, '\\', '(', ')');

				continue;

			} else if (c == '\'' || c == '"') {

				part.toggleQuote(pos, c, stripQuotes);
				continue;

			} else if (handleEscape && c == '\\') {
				if (stripQuotes && part.isQuoted()) {
					part.skipChar(pos);
				}
				pos++;

				// This is surely a very .. interesting syntax feature, no support for such mess..
				if (!part.isStarted()) {
					renderResult.warn(getClass(), "Escape character at beginning of attribute is not valid.");
				}
				continue;
			}

			// all other characters are ignored if they are inside quotes.
			if (part.isQuoted()) {
				continue;
			}

			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				part.finishAttribute(pos, attribs);
			} else if (c == '/') {
				part.finishAttribute(pos, attribs);
				closed = true;
			} else if (c == '=') {
				part.finishKey(pos, pos + 1);
			} else if (c == '>') {
				part.finishAttribute(pos, attribs);
				break;
			} else if (!part.isStarted()) {
				part.setStartPos(pos);
			}

		}

		return new AttributeResult(pos, closed, normalizeAttributes(renderResult, attribs));
	}

	/**
	 * Get an expression parser to parse a found expression in the attributes.
	 * If the first parenthesis is either the first character after a space, or follows a key
	 * without quotes (pe. 'if (..' or 'if( ..') the previous text is used as key.
	 *
	 * @param currentKey the current attribute key.
	 * @param isQuoted true, if the expression is surrounded by quotes.
	 * @param isFirstChar true, if the expression is not preceded by any text.
	 * @return an expressionparser, or null the current attribute should not be interpreted as expression.
	 */
	protected abstract ExpressionParser getExpressionParser(String currentKey, boolean isQuoted, boolean isFirstChar);

	/**
	 * Compile the list of found attributes and expressions into a map with Key->Value with strict syntax.
	 *
	 * @see AttributeResult
	 * @param renderResult
	 * @param attribs the list of {@link Attribute} of all found attributes in the order of occurrence.
	 * @return a map with key->value with String->Attribute.
	 * @throws NodeException 
	 */
	protected abstract Map normalizeAttributes(RenderResult renderResult, List attribs) throws NodeException;

	protected int findClosing(String code, int startPos, StringBuffer source, char escapeChar, char openingChar, char closingChar) {
		int pos = startPos + 1;
		boolean escaped = false;
		boolean openString = false;
		char stringQuote = '\0';
		int openCount = 0;

		for (; pos < code.length(); pos++) {
			char c = code.charAt(pos);

			if (escaped) {
				source.append(escapeChar).append(c);
				escaped = false;
				continue;
			}

			if (c == escapeChar) {
				escaped = true;
			} else if (c == '"' || c == '\'') {
				if (openString && stringQuote == c) {
					// closing the string
					openString = false;
					source.append(c);
				} else if (openString) {
					source.append(c);
				} else {
					// opening a string
					openString = true;
					source.append(c);
					stringQuote = c;
				}
			} else if (c == openingChar && !openString) {
				openCount++;
			} else if (c == closingChar && !openString) {
				if (openCount == 0) {
					// found the end
					break;
				} else {
					openCount--;
				}
			} else {
				source.append(c);
			}
		}

		return pos;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.attribute.AttributeParser#isEndTag(com.gentics.lib.parser.tag.struct.TagPart)
	 */
	public boolean isEndTag(TagPart tagPart) {
		// generally only real end parts are end parts
		return tagPart.getType() == TagPart.TYPE_END;
	}
}

/**
 * Helper class to parse attributes. This contains the code of one attribute and generates
 * an Attribute object after an attribute is finished.
 */
class AttributePart {

	private RenderResult renderResult;
	private String code;
	private StringBuffer key;
	private StringBuffer value;
	private Expression expression;
	private boolean hasValue;
	private int lastPos;
	private char quote;

	/**
	 * Constructor to generate a new attributeparser with a reference to the code-template.
	 * @param renderResult
	 * @param code the code which is parsed.
	 */
	public AttributePart(RenderResult renderResult, String code) {
		this.renderResult = renderResult;
		this.code = code;
		lastPos = 0;
		hasValue = false;
		key = new StringBuffer();
		value = new StringBuffer();
		expression = null;
		quote = 0;
	}

	/**
	 * Mark the end of an attribute. A new Attribute is generated and added to the list of attributes.
	 * The Part is prepared to parse a new attribute. If no attribute has been started, nothing is done.
	 *
	 * @param pos the position after the last character of the attribute.
	 * @param attributes a list of previously parsed attributes; the new attribute is added to this list.
	 */
	public void finishAttribute(int pos, List attributes) {

		if (lastPos == 0) {
			return;
		}

		updatePos(pos);
		attributes.add(new Attribute(key.toString(), hasValue ? value.toString() : null, expression));

		if (key.length() > 0) {
			key.delete(0, key.length());
		}
		if (value.length() > 0) {
			value.delete(0, value.length());
		}
		lastPos = 0;
		hasValue = false;
		expression = null;
		quote = 0;
	}

	/**
	 * Set the position of the first char of an attribute.
	 * @param pos position of the first char of the attribute.
	 */
	public void setStartPos(int pos) {
		lastPos = pos;
	}

	/**
	 * Split the attribute into a key and a value.
	 * @param pos the position of the character after the last char of the key.
	 * @param valuePos the position of the first char of the value.
	 */
	public void finishKey(int pos, int valuePos) {
		if (lastPos == 0) {
			renderResult.info("Invalid attribute", "Attribute starts with '='.");
			lastPos = valuePos;
			hasValue = true;
			return;
		}
		if (hasValue) {
			renderResult.info("Invalid attribute", "Attribute {" + key.toString() + "} contains '=' more than once!");
			return;
		}
		updatePos(pos);
		lastPos = valuePos;
		hasValue = true;
	}

	/**
	 * Check if the beginning of an attribute has been set.
	 * @return true, if a startposition is set, else false.
	 */
	public boolean isStarted() {
		return lastPos != 0;
	}

	/**
	 * Check, if the current attribute has key and value, or only a key so far.
	 * @return true, if the attribute has been splitted into key and value.
	 */
	public boolean hasValue() {
		return hasValue;
	}

	/**
	 * get the current key of the attribute. If the attribute has not been
	 * splitted into value and key, the code from the beginning to the current position is used as key.
	 * @return the current key.
	 */
	public String getCurrentKey() {
		return key.toString();
	}

	/**
	 * get the current key of the attribute. If the attribute has not been splitted into
	 * a value and a key, and pos == lastPos, and the last attribute has no value, the key
	 * of the last attribute is used as key for this attribute.
	 *
	 * @param attribs the list if already found attributes.
	 * @param pos current position in the code.
	 * @return the current attribute key, or null if not available.
	 */
	public String getCurrentKey(List attribs, int pos) {
		if (hasValue) {
			return key.toString();
		}
		if (pos > lastPos && lastPos > 0) {
			return code.substring(lastPos, pos);
		}
		if (attribs.size() > 0) {
			Attribute attr = (Attribute) attribs.get(attribs.size() - 1);

			return attr.getKey();
		}
		return null;
	}

	/**
	 * Set an expression for this attribute.
	 */
	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	/**
	 * Skip a character in the code. The character will not be included in the
	 * attribute's value or key.
	 *
	 * @param pos position of the char to skip.
	 */
	public void skipChar(int pos) {
		updatePos(pos);
		lastPos = pos + 1;
	}

	/**
	 * Try to toggle the quote status. This will check, if the closing
	 * quote matches the opening quote.
	 *
	 * @param pos position of the quote.
	 * @param c the quote itself.
	 * @param stripQuotes true, if quotes should not be included in the attribute's values.
	 * @return true, if the status has been toggled, else false.
	 */
	public boolean toggleQuote(int pos, char c, boolean stripQuotes) {
		if (quote == 0) {

			quote = c;

			if (!isStarted()) {
				setStartPos(stripQuotes ? pos + 1 : pos);
			} else if (stripQuotes) {
				skipChar(pos);
			}
			return true;

		} else if (quote == c) {

			quote = 0;

			if (stripQuotes) {
				skipChar(pos);
			}
			return true;
		}
		return false;
	}

	/**
	 * Check if the current code is quoted or not.
	 * @return true, if an opening quote is found and not yet closed.
	 */
	public boolean isQuoted() {
		return quote != 0;
	}

	/**
	 * Get the current quote character.
	 * @return the current quote character, or \0 if no quote is opened.
	 */
	public char getQuote() {
		return quote;
	}

	/**
	 * Update the last position in the code and add so-far found code to the attribute's values or key.
	 * This does not update the internal position pointer.
	 *
	 * @param pos new position to set current pos to.
	 */
	private void updatePos(int pos) {

		if (lastPos == 0) {
			return;
		}

		if (hasValue) {
			value.append(code.substring(lastPos, pos));
		} else {
			key.append(code.substring(lastPos, pos));
		}
	}
}
