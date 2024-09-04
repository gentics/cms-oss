/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: ListPartType.java,v 1.15.6.1 2011-03-08 12:27:15 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.resolving.ResolvableGetter;
import com.gentics.contentnode.rest.model.Property;

/**
 * the listparttype creates numbered or unnumbered listings, using each line in the template as
 * an entry of the list.
 */
public abstract class ListPartType extends AbstractPartType {

	public static final int TYPE_ORDERED = 1;
	public static final int TYPE_UNORDERED = 2;

	/**
	 * ordered or unorded are defined by info in value (0 is unordered)
	 */
	public static final int TYPE_CHANGEABLE = 3;

	private int type;
	private String[] lines;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @param type type of the parttype
	 * @throws NodeException
	 */
	public ListPartType(Value value, int type) throws NodeException {
		super(value);
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
		String text = getValueObject().getValueText();

		if (text == null) {
			return true;
		} else {
			return "".equals(text);
		}
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		super.setValue(value);

		if (value == null) {
			return;
		}
		parse();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		StringBuffer txt = new StringBuffer("<");

		String l;

		if (type == TYPE_UNORDERED || (type == TYPE_CHANGEABLE && getValueObject().getInfo() == 0)) {
			l = "ul";
		} else {
			l = "ol";
		}
		txt.append(l);

		String infoText = getValueObject().getPart().getInfoText();

		if (infoText != null && !"".equals(infoText)) {
			txt.append(" class=\"");
			txt.append(infoText);
			txt.append("\"");
		}

		txt.append(">\n");

		boolean firstNewline = true;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];

			if (line == null || "".equals(line)) {
				if (firstNewline) {
					txt.append("<br /><br />\n");
					firstNewline = false;
				} else {
					txt.append("<br />\n");
				}
			} else {
				txt.append("<li>");
				txt.append(line);
				txt.append("</li>\n");
				firstNewline = true;
			}
		}

		txt.append("</");
		txt.append(l);
		txt.append(">\n");

		return txt.toString();
	}

	/**
	 * Get the number of lines in the list
	 * @return number of lines
	 */
	@ResolvableGetter
	public int getCount() {
		return lines.length;
	}

	/**
	 * Get the lines in the list
	 * @return lines in the list
	 */
	@ResolvableGetter
	public String[] getLines() {
		return lines;
	}

	/**
	 * Set the lines
	 * @param lines lines
	 * @throws NodeException
	 */
	public void setLines(String...lines) throws NodeException {
		Value value = getValueObject();
		value.setValueText(StringUtils.join(lines, "\n"));
		parse();
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof ListPartType) {
			ListPartType otherListPT = (ListPartType) other;
			return Objects.equals(type, otherListPT.type) && Objects.deepEquals(lines, otherListPT.lines)
					&& Objects.equals(getValueObject().getInfo(), other.getValueObject().getInfo());
		} else {
			return false;
		}
	}

	/**
	 * Parse the value into lines
	 */
	protected void parse() {
		Value value = getValueObject();

		if (value != null) {
			String text = ObjectTransformer.getString(value.getValueText(), "").replaceAll("\r", "");

			if (ObjectTransformer.isEmpty(text)) {
				lines = new String[0];
			} else {
				lines = text.split("\n", -1);
			}
		}
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		String[] lines = getLines();
		List<String> stringValues = new Vector<String>(lines.length);

		for (int i = 0; i < lines.length; i++) {
			stringValues.add(lines[i]);
		}
		property.setStringValues(stringValues);
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		List<String> stringValues = property.getStringValues();
		if (ObjectTransformer.isEmpty(stringValues)) {
			getValueObject().setValueText("");
		} else {
			getValueObject().setValueText(com.gentics.lib.etc.StringUtils.merge((String[]) stringValues.toArray(new String[stringValues.size()]), "\n"));
		}
	}
}
