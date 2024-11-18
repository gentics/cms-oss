/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: TablePartType.java,v 1.27 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * PartType 23 - Table ext This parttype implements the extended table module.
 */
public class TablePartType extends AbstractPartType {

	private static final String LINE_SEPARATOR = "\n";

	private static final String TABLE_PREFIX = "table.";

	private static final String TABLEHEADER_PREFIX = "table+.";

	private static final String STYLE_POSTFIX = ".style";

	private int rows = -1;

	private int cols = -1;

	/**
	 * Threadlocal for the current row counter
	 */
	private ThreadLocal<Integer> rowLocal = new ThreadLocal<Integer>();

	/**
	 * Threadlocal for the current col counter
	 */
	private ThreadLocal<Integer> colLocal = new ThreadLocal<Integer>();

	private Tag tag;

	private static final String TR_PREFIX = "tr.";

	private String tagName;

	private static final String TD_PREFIX = "td.";

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public TablePartType(Value value) throws NodeException {
		super(value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		super.setValue(value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
        
		reloadSettings();
       
		if (rows <= 0 || cols <= 0) {
			return true;
		} else {
			return false;
		}
	}
    
	/**
	 * Reload the table
	 * @throws NodeException
	 */
	private void reloadValue() throws NodeException {
		reloadSettings();
        
		tag = null;
		tagName = "";

		ValueContainer valueContainer = getValueObject().getContainer();

		if (valueContainer instanceof Tag) {
			tag = (Tag) valueContainer;
			tagName = tag.getName();
		}
	}

	/**
	 * Reload the settings for the table
	 */
	private void reloadSettings() {
		String[] parts = ObjectTransformer.getString(getValueObject().getValueText(), "").split(";");
		String sCols = parts.length > 0 ? parts[0] : "0";
		String sRows = parts.length > 1 ? parts[1] : "0";

		try {
			rows = Integer.parseInt(sRows);
			cols = Integer.parseInt(sCols);
		} catch (NumberFormatException e) {
			rows = 0;
			cols = 0;
			NodeLogger.getLogger(this.getClass()).warn("Could not determine number of rows/columns: " + e.getMessage(), e);
		}
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult,
	 *      java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		if (tag == null) {
			reloadValue();
		}

		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		// TODO get the edit color from the config?
		String editCol = "#DDDDDD";
		StringWriter JS = new StringWriter();

		if (tag == null) {
			// TODO error handling
			// cannot render a table without other tags
			return "";
		}

		// special hack to prevent rendering of table tag within .style-tags
		if (tagName.endsWith(STYLE_POSTFIX)) {
			return "";
		}

		StringBuffer code = new StringBuffer();

		List parts = getValueObject().getPart().getConstruct().getParts();

		TagContainer tagContainer = tag.getContainer();
		String containerType = "content";

		if (tagContainer instanceof Content) {
			// replace the content by the currently rendered page
			tagContainer = renderType.getTopmostTagContainer();
		} else if (tagContainer instanceof Template) {
			containerType = "template";
		}

		// edit mode
		int editMode = renderType.getEditMode();

		code.append("<table");
		String additionalTags = "";

		for (int i = 0; i < parts.size(); i++) {
			Part part = (Part) parts.get(i);
			String keyname = part.getKeyname();

			if (keyname == null) {
				continue;
			}

			if (keyname.startsWith(TABLE_PREFIX)) {

				appendProperty(renderType, result, code, tag, keyname, TABLE_PREFIX);

			} else if (keyname.startsWith(TABLEHEADER_PREFIX)) {
				additionalTags += addInnerTableTag(part, result);
			}
		}

		code.append(">");
		code.append(LINE_SEPARATOR);
		code.append(additionalTags);
		code.append(LINE_SEPARATOR);

		int row;

		for (row = 1; row <= rows; row++) {
			rowLocal.set(row);

			code.append("<tr");

			Tag rowStyle = tagContainer.getTag(getTagName(row, 0, true));

			// this is a bit of a hack, we push the tag to the rendertype
			// and remove it from the stack immediately. this is only to
			// make sure that the caller noticed that we used it
			if (rowStyle != null) {
				renderType.push(rowStyle);
				renderType.pop();
			}

			appendProperties(renderType, result, code, rowStyle, parts, TR_PREFIX);

			code.append(">");
			code.append(LINE_SEPARATOR);

			// store lenght of code before tag content is added
			int lengthBefore;

			int col;

			for (col = 1; col <= cols; col++) {
				colLocal.set(col);

				Tag cellStyle = tagContainer.getTag(getTagName(row, col, true));

				// this is a bit of a hack, we push the tag to the rendertype
				// and remove it from the stack immediately. this is only to
				// make sure that the caller noticed that we used it
				if (cellStyle != null) {
					renderType.push(cellStyle);
					renderType.pop();
				}

				StringBuffer cellCode = new StringBuffer();

				String cellhtml = "td";

				if (appendProperties(renderType, result, cellCode, cellStyle, parts, TD_PREFIX) && row == 1) {
					cellhtml = "th";
				}

				code.append('<').append(cellhtml).append(cellCode);
				code.append(">");
				code.append(LINE_SEPARATOR);

				Tag cellTag = tagContainer.getTag(getTagName(row, col, false));

				lengthBefore = code.length();
				if (cellTag != null) {
					// edit mode
					if (editMode == RenderType.EM_ALOHA || editMode == RenderType.EM_ALOHA_READONLY) {
						AlohaRenderer alohaRenderer = (AlohaRenderer) RendererFactory.getRenderer(ContentRenderer.RENDERER_ALOHA);
						code.append(alohaRenderer.block(cellTag.render(result), cellTag, result));
					} else {
						code.append(cellTag.render(result));
					}
				}
				if (lengthBefore == code.length()) {
					// fill empty cells with spaces to prevent them being
					// invisible
					code.append("&nbsp;");
				}

				code.append("</").append(cellhtml).append(">");
				code.append(LINE_SEPARATOR);

			}

			code.append("</tr>");
			code.append(LINE_SEPARATOR);
		}

		code.append("</table>");
		code.append(LINE_SEPARATOR);

		return code.toString();
	}

	/**
	 * now that a part with special naming has been found it will be added right
	 * after the <table> tag
	 * @param part to be added
	 * @param result RenderResult which is needed to render the tag
	 * @return html tag with content
	 * @throws NodeException on rendering glitches
	 */
	private String addInnerTableTag(Part part, RenderResult result) throws NodeException {
		String tagName = part.getKeyname().substring(TABLE_PREFIX.length() + 1);
		Value value = tag.getValues().getByKeyname(part.getKeyname());
		String val = "";

		if (null != value) {
			val = value.render(result);
		}
		if ("".equals(val)) {
			return "";
		} else {
			return "<" + tagName + ">" + val + "</" + tagName + ">";
		}
	}

	private String getCellName(int row, int col) {
		String name;

		if (col > 0) {
			name = Character.toString((char) ('A' + col - 1));
		} else {
			name = "";
		}
		name += Integer.toString(row);
		return name;
	}

	private String getTagName(int row, int col, boolean style) {
		StringBuffer name = new StringBuffer(tagName);

		name.append(".");
		name.append(getCellName(row, col));

		if (style) {
			name.append(STYLE_POSTFIX);
		}

		return name.toString();
	}

	/**
	 * appends all properties of td.* to the given 'code' string buffer.
	 * 
	 * will return 'true' if there is a property called 'header' with a true value.
	 */
	private boolean appendProperties(RenderType renderType, RenderResult result,
			StringBuffer code, ValueContainer container, List parts, String prefix) throws NodeException {

		if (container == null || parts == null) {
			return false;
		}
        
		boolean ret = false;

		for (Iterator i = parts.iterator(); i.hasNext();) {
			Part part = (Part) i.next();
			String keyname = part.getKeyname();

			if (keyname == null) {
				continue;
			}

			if (keyname.startsWith(prefix)) {
				// if there is a td.header we want to render a <th> not a <td>
				if (keyname.equals(prefix + "header")) {
					String content = getRenderedProperty(result, container, keyname);

					// false: null, "", 0, "off"
					if (!ObjectTransformer.isEmpty(content) && ObjectTransformer.getBoolean(content, true)) {
						ret = true;
					}
				} else if (keyname.equals(prefix + "alternaterows")) {
					String val = getRenderedProperty(result, container, keyname);

					if ("true".equals(val) || "1".equals(val)) {
						int row = ObjectTransformer.getInt(rowLocal.get(), 0);

						// mark odd and even rows with appropriate classes
						if (row % 2 == 0) {
							code.append(" class=\"even\"");
						} else {
							code.append(" class=\"odd\"");
						}
					}
				} else {
					appendProperty(renderType, result, code, container, keyname, prefix);
				}

			}
		}
		return ret;
	}

	private void appendProperty(RenderType renderType, RenderResult result, StringBuffer code,
			ValueContainer container, String keyname, String prefix) throws NodeException {
		String propCode = getRenderedProperty(result, container, keyname);

		if (propCode == null) {
			return;
		}

		// add prop="<value>"
		String prop = keyname.substring(prefix.length());

		code.append(" ");
		code.append(prop);
		code.append("=\"");
		code.append(propCode);
		code.append("\"");
	}

	private String getRenderedProperty(RenderResult result, ValueContainer container,
			String keyname) throws NodeException {
		if (container == null) {
			return null;
		}

		Value propValue = container.getTagValues().getByKeyname(keyname);

		if (propValue == null) {
			return null;
		}

		// force the value output (style tags never get enabled)
		String propCode = propValue.render(result, null, true);

		if (propCode == null || "".equals(propCode)) {
			return null;
		}
		return propCode;
	}

	public Object get(String key) {
		if (rows == -1) {
			reloadSettings();
		}
		// TODO move this to javabean getters
		if ("rows".equals(key)) {
			return new Integer(rows);
		} else if ("cols".equals(key)) {
			return new Integer(cols);
		} else if ("row".equals(key)) {
			return ObjectTransformer.getInteger(rowLocal.get(), 0);
		} else if ("col".equals(key)) {
			return ObjectTransformer.getInteger(colLocal.get(), 0);
		}
		return null;
	}

	@Override
	public Type getPropertyType() {
		return Type.TABLE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setStringValue(getValueObject().getValueText());
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		getValueObject().setValueText(property.getStringValue());
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof TablePartType) {
			return Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}

	/**
	 * Helper class for i18n in javascript template
	 */
	public class I18nWrapper {

		/**
		 * Get an I18nString for the given key
		 * @param key key
		 * @return i18n
		 */
		public I18nString get(String key) {
			return new CNI18nString(key);
		}
	}
}
