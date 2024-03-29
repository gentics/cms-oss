/*
 * @author norbert
 * @date 18.07.2008
 * @version $Id: OldTablePartType.java,v 1.5 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.render.renderer.NBSPFormatterRenderer;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * PartType 12 - Implementation of the old Table parttype
 */
public class OldTablePartType extends AbstractPartType {
	protected Tag tag;

	protected String tagName;

	/**
	 * Create an instance of the parttype
	 * @param value value
	 * @throws NodeException
	 */
	public OldTablePartType(Value value) throws NodeException {
		super(value);
	}

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -4472058307104949392L;

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#setValue(com.gentics.contentnode.object.Value)
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
		return tag == null;
	}
    
	private void reloadValue() throws NodeException {
		Value value = this.getValueObject();
        
		if (value != null) {
			ValueContainer valueContainer = value.getContainer();

			if (valueContainer instanceof Tag) {
				tag = (Tag) valueContainer;
				tagName = tag.getName();
			}
		} else {
			tag = null;
			tagName = null;
		}
	}

	private String getTagName(int r, int c) {
		StringBuffer subTagName = new StringBuffer();

		subTagName.append(tagName).append("_").append(r).append("_").append(c);
		return subTagName.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult,
	 *      java.lang.String)
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		super.render(renderResult, template);
		if (tag == null) {
			reloadValue();
		}
        
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		PreparedStatement pst = null;
		ResultSet rs = null;
		StringBuffer rendered = new StringBuffer();
		TemplateRenderer nbspRenderer = new NBSPFormatterRenderer();
		int editMode = renderType.getEditMode();

		TagContainer tagContainer = tag.getContainer();

		if (tagContainer instanceof Content) {
			// replace the content by the currently rendered page
			tagContainer = renderType.getTopmostTagContainer();
		}
		try {
			Value value = getValueObject();

			if (value == null) {
				return "";
			} else {
				String valueText = ObjectTransformer.getString(value.getValueText(), "");
				String[] size = valueText.split(";");
				ValueContainer container = value.getContainer();
				String part = "";
				Object partId = value.getPartId();
				Object tagId = null;

				if (container instanceof ContentTag) {
					part = "contenttag";
					tagId = ((ContentTag) container).getId();
				} else if (container instanceof TemplateTag) {
					part = "templatetag";
					tagId = ((TemplateTag) container).getId();
				} else if (container instanceof Construct) {
					part = "constructtag";
					tagId = ((Construct) container).getId();
				} else {
					return "";
				}

				int th = 0;
				String tdFEven = "";
				String tdFOdd = "";
				String thF = "";
				String tableF = "";

				String sql = "SELECT th, td_f_even, td_f_odd, th_f, td_type, th_type, table_f FROM contenttable WHERE " + part + "_id = ? AND part_id = ?";

				pst = t.prepareStatement(sql);
				pst.setObject(1, tagId);
				pst.setObject(2, partId);

				rs = pst.executeQuery();
				if (rs.next()) {
					th = rs.getInt("th");
					tdFEven = rs.getString("td_f_even");
					tdFOdd = rs.getString("td_f_odd");
					thF = rs.getString("th_f");
					tableF = rs.getString("table_f");
				}

				rendered.append("<table ").append(tableF).append(">\n");
				int rStart = th != 0 ? 0 : 1;
				int size0 = size.length >= 1 ? ObjectTransformer.getInt(size[0], 0) : 0;
				int size1 = size.length >= 2 ? ObjectTransformer.getInt(size[1], 0) : 0;

				String tdF = null;

				for (int r = rStart; r <= size1; r++) {
					if (r % 2 != 0) {
						tdF = tdFOdd;
					} else {
						tdF = tdFEven;
					}

					rendered.append("<tr>\n");
					for (int c = 1; c <= size0; c++) {
						if (r > 0) {
							rendered.append("<td ").append(tdF).append(">\n");
						} else {
							rendered.append("<th ").append(thF).append(">\n");
						}
						Tag cellTag = tagContainer.getTag(getTagName(r, c));

						if (cellTag != null) {
							// edit mode
							rendered.append(nbspRenderer.render(renderResult, cellTag.render(renderResult)));
						}

						if (r > 0) {
							rendered.append("</td>\n");
						} else {
							rendered.append("</th>\n");
						}
					}
					rendered.append("</tr>\n");
				}
				rendered.append("</table>\n");
			}
		} catch (SQLException e) {
			throw new NodeException("Error while rendering Table part " + getValueObject(), e);
		} finally {
			t.closeResultSet(rs);
			t.closeStatement(pst);
		}

		return rendered.toString();
	}

	@Override
	public Type getPropertyType() {
		return Type.UNKNOWN;
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
	}

	@Override
	public boolean hasSameContent(PartType other) {
		if (other instanceof OldTablePartType) {
			return Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}
}
