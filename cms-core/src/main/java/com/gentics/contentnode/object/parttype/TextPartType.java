/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: TextPartType.java,v 1.18.2.1 2011-03-15 17:11:29 tobiassteiner Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.resolving.ResolvableGetter;
import com.gentics.contentnode.rest.model.Property;

/**
 * This parttype implements all Text-PartTypes, with the different escape and edit modes.
 *
 * TypeIDs: 1 (Text), 2 (Text/Html), 3 (HTML), 9 (Text Short), 10 (Text Html Long), 21 (HTML lang), 26 (Java), 27 (DHTML)
 **/
public abstract class TextPartType extends AbstractPartType implements PartType {

	/**
	 * do not replace any newlines
	 */
	public static final int REPLACENL_NONE = 0;

	/**
	 * turn newlines to html breaks
	 */
	public static final int REPLACENL_NL2BR = 1;

	/**
	 * turn newlines to html breaks, do not add breaks after tags, do add breaks
	 * after some specific tags. #textnl2br(String) ist used.
	 */
	public static final int REPLACENL_EXTENDEDNL2BR = 2;

	protected int replaceNewline;

	private String parsedText;

	/**
	 * create a new TextPartType from value
	 * @param value of this text part type
	 * @param replaceNewline set how newslines shall be triggered using one of
	 *        the following constants REPLACENL_NONE, REPLACENL_NL2BR or
	 *        REPLACENL_EXTENDEDNL2BR
	 * @throws NodeException
	 */
	public TextPartType(Value value, int replaceNewline) throws NodeException {
		super(value);
		this.replaceNewline = replaceNewline;
		// TODO ... super(value) would call #parseText() .. but replaceNewline & co wouldn't be set there !
		setValue(value);
	}

	public TextPartType(Value value) throws NodeException {
		super(value);
	}
	
	public void setValue(Value value) throws NodeException {
		super.setValue(value);
		parsedText = parseText();
	}

	public boolean hasTemplate() throws NodeException {
		return false;
	}

	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		// if the part is inline editable and is rendered as template for a velocity part, we make the content safe by replacing # and $
		// with the escape tool. Otherwise, if the editor entered e.g. ## into the content, this would break the velocity template
		if (getValueObject().getPart().isInlineEditable() && ObjectTransformer
				.getBoolean(TransactionManager.getCurrentTransaction().getRenderType().getParameter(VelocityPartType.SAFE_INLINE_RENDERING), false)
				&& !isVelocityTemplate()) {
			return parsedText.replaceAll("\\$", "\\${cms.imps.velocitytools.esc.d}").replaceAll("#", "\\${cms.imps.velocitytools.esc.h}");
		}
		return parsedText;
	}

	/**
	 * Returns true if the parsedText is null or empty String
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
        
		if (parsedText == null) {
			return true;
		} else {
			return "".equals(parsedText);
		}
	}

	/**
	 * Get the parsed text
	 * @return parsed text
	 */
	@ResolvableGetter
	public String getText() {
		return parsedText;
	}

	/**
	 * Set the text into the value
	 * @param text text
	 * @throws NodeException
	 */
	public void setText(String text) throws NodeException {
		getValueObject().setValueText(text);
		parsedText = parseText();
	}

	/**
	 * parse the text value depending on replaceNewline setting
	 * @return parsed text
	 */
	public String parseText() {
		Value value = getValueObject();
		String text = value != null ? value.getValueText() : null;

		if (text != null) {
			switch (replaceNewline) {
			case REPLACENL_NL2BR:
				text = mynl2br(text);
				break;

			case REPLACENL_EXTENDEDNL2BR:
				text = textnl2br(text);
				break;
			}
		}
		return text;
	}
    
	/**
	 * this function will insert html breaks preceding to all newline chars.
	 * Naming is kept from the old PHP implementation.
	 * @param str string to be transformed
	 * @return string with breaks added
	 */
	public String mynl2br(String str) {
		return StringUtils.replace(str, "\n", "<br />\n");
	}

	/**
	 * This text transformation function approximates the old textnl2br function 
	 * written in PHP. Basically newlines are transformed to breaks with some
	 * extra tweaks over here and there such as adding breaks after some special
	 * tags. Though scary, naming was kept for historical reasons.
	 * 
	 * This method was rewritten again to get rid of regexes, since regexes create
	 * an incredible overhead - even if the patterns are compiled statically.
	 * The old Java method that uses regexes can be found in the unit test for this
	 * class.
	 * 
	 * @param str string to be transformed
	 * @return converted string
	 */
	public String textnl2br(String str) {
		StringBuilder result = new StringBuilder(str.length() * 2 + 16);
        
		str = StringUtils.remove(str, '\r');
        
		int lastPos = 0;
		int cursor;

		while (-1 != (cursor = str.indexOf('\n', lastPos))) {

			String line = str.substring(lastPos, cursor);

			line = StringUtils.stripEnd(line, " ");

			int opening = line.lastIndexOf('<');

			if (-1 != opening) {
				int closing = line.indexOf('>', opening + 1);

				if (-1 == closing) {
					// we encountered a newline within a tag.
					// the previous behavior, before this method was rewritten,
					// was to strip the last newline in a tag.
					// also see: textnl2brHandleNewlineInTag()
					// lastPos = textnl2brHandleNewlineInTag(line, str, opening, cursor, lastPos, result);
					// continue;
                    
					// the new behavior will completely ignore any newlines in a tag.
					// TODO: closing angle brackets may validly occur in
					// attribute values. this means, that this will only
					// ignore newlines in a tag, up to the first closing
					// angle bracket, which may occur before the end of
					// the tag - newlines occuring between the first
					// closing angle bracket and the end of the tag are
					// not being ignored.
					// Another problem is, that GCN syntax allows all kinds
					// of things to occur in a tag, e.g. nested tags like
					// <tag attr="<node tag"> and other constructs like XNL
					// conditionals. One would have to factor-out all
					// tag-parsing logic into a general parsing class, that
					// will correctly parse all GCN tag constructs.
					cursor = str.indexOf('>', cursor + 1);
                    
					if (-1 == cursor) {
						// the tag isn't closed in this string, so we have
						// nothing more to do.
						break;
					}
                    
					// the tag is closed at some point after the separating
					// newline. we will append the tag unchanged, including the
					// closing angle bracket, and continue normally.
					result.append(str.substring(lastPos, cursor + 1));
				} else {
					// we encountered a tag which doesn't contain any newlines.
					result.append(line);
                    
					// if the separating newline doesn't immediately follow
					// a tag, or if it does follow a tag and the tag is
					// a br-tag, we insert a <br />. Note that we must make sure
					// that the tag closes at the end of the line, otherwise
					// tag-in-tag syntax causes problems.
					int lineLastOffset = line.length() - 1;

					if ('>' != line.charAt(lineLastOffset) || (closing == lineLastOffset && isBrTag(line, opening))) {
						result.append("<br />\n");
					} else {
						// the separating newline follows a tag, but it isn't
						// a br-tag.
						result.append("\n");
					}
				}
			} else if (0 == line.length() || '>' != line.charAt(line.length() - 1)) {
				// the separating newline isn't following a '>'
				result.append(line).append("<br />\n");
			} else {
				// the separating newline is following a '>'
				result.append(line).append("\n");
			}

			lastPos = cursor + 1;
		}

		String lastLine = str.substring(lastPos);

		result.append(lastLine);
        
		return result.toString();
	}
    
	/**
	 * Helper function to invoke if newlines in a tag are encountered.
	 * 
	 * Strips the last newline in the tag, and processes the rest of the
	 * tag, including a potential newline after the tag, the same way as
	 * the invoking method ({@link #textnl2br}).
	 * 
	 * @deprecated because stripping newlines within tags was previously only
	 *   done, so that a generic newline-replace pass over the text doesn't
	 *   insert <br />s inside the tag - at least, that is the assumption,
	 *   I don't know whether there was any other reason (also see the original
	 *   implementation in {@link TextPartTypeText}). The previous approach
	 *   had two bugs - only the last newline was stripped, the others were
	 *   still replaced with <br />s, and stripping the last newline may join
	 *   two attributes together (both bugs are documented in the method body).
	 *   This method should disappear if the new behaviour, where any newlines
	 *   inside a tag are ignored, doesn't cause any trouble.
	 * 
	 * @param line an already preprocessed line (\r and trailing whitespace removed)
	 *   that ends in the middle of a tag, e.g. "...<some-tag ".
	 *   
	 * @param str the entire markup string.
	 * 
	 * @param opening the offset of the last '<' character in line. 
	 * 
	 * @param cursor the position where the line ended in str (before it
	 *   was preprocessed).
	 *   
	 * @param lastPos the position where the line started in str (before it
	 *   was preprocessed).
	 *   
	 * @param result the processed markup will be appended to the result.
	 * 
	 * @return points to the first character in str that was not processed by
	 *   this method. 
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private int textnl2brHandleNewlineInTag(String line, String str, int opening, int cursor, int lastPos, StringBuilder result) {
		// find the last newline in the tag, which may be equal to
		// the current cursor position.
		int closingInStr = str.indexOf('>', cursor + 1);
		int lastNlInTag;

		if (-1 == closingInStr) {
			lastNlInTag = str.lastIndexOf('\n');
		} else {
			lastNlInTag = str.lastIndexOf('\n', closingInStr - 1);
		}

		if (lastNlInTag > cursor) {
			// there are several newlines in the tag. we will extend the line
			// until the last newline (exclusive) in order to be able to
			// process the entire tag.
			line = str.substring(lastPos, lastNlInTag);
            
			// have to strip the whitespace before the last newline since
			// the last newline isn't included in the line.
			line = StringUtils.stripEnd(line, " ");
            
			// this transformation is applied by the main loop,
			// for any tags that don't contain a newline, or for newlines
			// not contained in a tag.
			line = line.replaceAll(" *\n", "\n");

			// the newline handling within tags, in the original implementation,
			// inserted <br />s for all newlines other than the last.
			// this behaviour doesn't make any sense and this is one of the
			// reasons for deprecating this method.
			line = line.replaceAll("(?<!(>))\n", "<br />\n");            

			// re-find the first newline (previously the end of the line)
			// and from there, re-find the (previously) last opening angle
			// bracket of the line. there may now be multiple opening
			// angle brackets after extending the line, but there can't be
			// any closing angle brackets.
			int firstNl = line.indexOf('\n');

			opening = line.lastIndexOf('<', firstNl);
		}

		// the rest of the tag starts after the last newline that occurs
		// in the tag,
		int restStart = lastNlInTag + 1;
		// and extends either until the closing '>' (inclusive) or
		// until the end of the string.
		int restEnd = -1 != closingInStr ? closingInStr + 1 : str.length();
		String rest = str.substring(restStart, restEnd);

		// the entire transformed tag, excluding the last newline in the
		// tag (which gets stripped), is contained in line+rest.
        
		// simply stripping the newline is a bug, as it may join two
		// attributes together. newlines should instead be replaced with
		// a whitespace, if they separates two non-whitespace characters.
		// Otherwise, e.g. "someattr \nsomeotherattr='somevalue'" will be
		// converted to someattrsomeotherattr='somevalue'.
		// this bug is one of the reasons for deprecating this method.
		result.append(line).append(rest);

		// if the tag is followed by a newline, process it immediately.
		if (restEnd < str.length() && '\n' == str.charAt(restEnd)) {
			restEnd++;
			// Note: to be behave exactly the same as the previous
			// implementation of this method, before it was rewritten,
			// we would have to check isBrTag() against the concatenated
			// line and rest. This would only make a difference, however,
			// if the newline that was stripped occurs inside the tagname.
			if (isBrTag(line, opening)) {
				result.append("<br />\n");
			} else {
				result.append("\n");
			}
		}

		return restEnd;
	}
    
	/**
	 * @param token contains the beginning of a tag e.g "</sometag".
	 * @param off points to the opening angle bracket which starts the tag.
	 * @return whether the tag is one of a list of tags after which we want
	 *   to insert a <br /> if the tag is followed by a newline.
	 */
	private boolean isBrTag(String token, int off) {
		// the tag name starts after a '<' or after '</' 
		int tagNameOff = off + 1 < token.length() && '/' == token.charAt(off + 1) ? off + 2 : off + 1;
        
		String[] brTags = new String[] {
			"b", "strong", "i", "font", "div", "span", "br", "node"
		};
        
		int tokenLength = token.length();

		for (String brTag : brTags) {
			int separatorOff = tagNameOff + brTag.length(); 

			if (separatorOff <= tokenLength && (separatorOff == tokenLength // end of the token is assumed to be a valid separator
					|| !Character.isLetter(token.charAt(separatorOff))) && token.regionMatches(true, tagNameOff, brTag, 0, brTag.length())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the value is the "template" part of a VTL-tag
	 * @return true iff the value is template of a VTL-part
	 * @throws NodeException
	 */
	private boolean isVelocityTemplate() throws NodeException {
		if ("template".equals(getValueObject().getPart().getKeyname())
				&& getValueObject().getContainer().getConstruct().getParts().stream().filter(p -> p.getPartTypeId() == 33).findFirst().isPresent()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns True if the value of this part type matches the configured
	 * regular expression that corresponds with it, or if no regular expression
	 * constraint is configured.
	 *
	 * @throws NodeException
	 * @see com.gentics.contentnode.object.parttype.PartType#validate()
	 */
	public boolean validate() throws NodeException {
		Map<?, ?> regexConfig = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().getPropertyMap(
				"regex");

		return validate(regexConfig);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#validate()
	 */
	public boolean validate(Map<?, ?> regexConfig) throws NodeException {
		int infoInt = getValueObject().getPart().getInfoInt();

		// TODO: Code review!
		if (infoInt > 1000) {
			infoInt -= 1000;
		}
		String configKey = String.valueOf(infoInt);

		if (null == regexConfig || !regexConfig.containsKey(configKey)) {
			return true;
		}
		String regex = (String) regexConfig.get(configKey);
		String text = getValueObject().getValueText();

		if (null == regex) {
			return true;
		}

		if (null == text) {
			return false;
		}

		return Pattern.matches(regex, text);
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setStringValue(getValueObject().getValueText());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		setText(property.getStringValue());
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof TextPartType) {
			return Objects.equals(getText(), ((TextPartType) other).getText());
		} else {
			return false;
		}
	}
}
