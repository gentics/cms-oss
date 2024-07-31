/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Value.java,v 1.36.6.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.devtools.model.DefaultValueModel;
import com.gentics.contentnode.devtools.model.OverviewModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * This is the value object which holds the generic values which are rendered by parttypes.
 */
@TType(Value.TYPE_VALUE)
public abstract class Value extends AbstractContentObject implements GCNRenderable, TemplateRenderer, StackResolvable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2761376733063995623L;
	public static final int TYPE_VALUE = 10026;

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Value, DefaultValueModel, DefaultValueModel> NODE2DEVTOOL = (from, to) -> {
		Transaction t = TransactionManager.getCurrentTransaction();

		Property property = from.getPartType().toProperty();
		to.setStringValue(property.getStringValue());
		to.setStringValues(property.getStringValues());
		to.setBooleanValue(property.getBooleanValue());
		to.setFileId(t.getGlobalId(ContentFile.class, property.getFileId()));
		to.setFolderId(t.getGlobalId(Folder.class, property.getFolderId()));
		to.setImageId(t.getGlobalId(ImageFile.class, property.getImageId()));
		to.setNodeId(t.getGlobalId(Node.class, property.getNodeId()));
		to.setPageId(t.getGlobalId(Page.class, property.getPageId()));
		to.setTemplateId(t.getGlobalId(Template.class, property.getTemplateId()));
		to.setContentTagId(t.getGlobalId(ContentTag.class, property.getContentTagId()));
		to.setTemplateTagId(t.getGlobalId(TemplateTag.class, property.getTemplateTagId()));
		to.setDatasourceId(t.getGlobalId(Datasource.class, property.getDatasourceId()));
		switch(property.getType()) {
		case SELECT:
		case MULTISELECT:
			to.setOptions(property.getSelectedOptions());
			break;
		case DATASOURCE:
			to.setOptions(property.getOptions());
			break;
		case OVERVIEW:
			to.setOverview(Overview.REST2DEVTOOL.apply(property.getOverview(), new OverviewModel()));
			break;
		default:
			break;
		}

		return to;
	};

	private final static String[] RENDER_KEYS = new String[0];

	private transient PartType partType;

	protected Value(Integer id, NodeObjectInfo info) {
		super(id, info);
		partType = null;
	}

	@Override
	public Set<String> getResolvableKeys() {
		try {
			return getPartType().getResolvableKeys();
		} catch (NodeException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * get the info int of the value.
	 * @return the info field.
	 */
	@FieldGetter("info")
	public abstract int getInfo();

	/**
	 * Set a new info to the value
	 * @param info new info
	 * @return old info
	 * @throws ReadOnlyException when the value was not fetched for update
	 */
	@FieldSetter("info")
	public int setInfo(int info) throws ReadOnlyException {
		failReadOnly();
		return 0;
	}

	/**
	 * check, if this value is a static default value of a part.
	 * @return true, if this value is not changeable in tags.
	 */
	@FieldGetter("static")
	public abstract boolean isStatic();

	/**
	 * Set the static flag
	 * @param stat true for static, false if not
	 * @throws ReadOnlyException
	 */
	@FieldSetter("static")
	public void setStatic(boolean stat) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the text value of this value.
	 * @return the text value field.
	 */
	@FieldGetter("value_text")
	public abstract String getValueText();

	/**
	 * Set a new text value to this value
	 * @param valueText new text value
	 * @return the old text value
	 * @throws ReadOnlyException when the value was not fetched for update
	 */
	@FieldSetter("value_text")
	public String setValueText(String valueText) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	/**
	 * get the reference id of the value.
	 * @return the valueref field.
	 */
	@FieldGetter("value_ref")
	public abstract int getValueRef();

	/**
	 * Set a new reference id to the value. Note that this method should only be used for already checked data.
	 * @param valueRef new reference id
	 * @return old reference id
	 * @throws ReadOnlyException when the value was not fetched for update
	 */
	@FieldSetter("value_ref")
	public int setValueRef(int valueRef) throws ReadOnlyException {
		failReadOnly();
		return 0;
	}

	/**
	 * get the part linked to this value.
	 * @return the part linked to this value.
	 * @throws NodeException 
	 */
	public Part getPart() throws NodeException {
		return getPart(true);
	}

	/**
	 * get the part linked to this value.
	 * @param checkForNull true if a consistency check for the part shall be done, false if not
	 * @return the part linked to this value.
	 * @throws NodeException 
	 */
	public abstract Part getPart(boolean checkForNull) throws NodeException;

	/**
	 * get the id of the part linked to this part.
	 * If you only need to know the id of this value, use this method,
	 * as the part itself doesn't need to be loaded.
	 *
	 * @return the id of the part linked to this value.
	 */
	public abstract Integer getPartId();

	/**
	 * Set the part id of this value
	 * @param partId new part id
	 * @return old part id
	 * @throws ReadOnlyException when the values was not fetched for update
	 */
	public Integer setPartId(Integer partId) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	/**
	 * Set the part of this value.
	 * @param part new part. Must be a part of the construct this value belongs to.
	 * @throws ReadOnlyException
	 */
	public void setPart(Part part) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * get the container which holds this value.
	 * @return the container of this value.
	 * @throws NodeException 
	 */
	public abstract ValueContainer getContainer() throws NodeException;

	/**
	 * Set the container
	 * @param container tag
	 * @return old container
	 * @throws ReadOnlyException
	 */
	public ValueContainer setContainer(ValueContainer container) throws NodeException {
		failReadOnly();
		return null;
	}

	@Override
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		PartType pt = getPartType(false);
		if (pt != null) {
			pt.delete();
		}
		performDelete();
	}
    
	/**
	 * Performs the delete of the Value
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;
    
	/**
	 * check, if this value's parttype needs a template to be rendered.
	 * @return true, if the value needs a template to be rendered.
	 */
	public boolean hasTemplate() throws NodeException {
		PartType type = getPartType();

		return type != null ? type.hasTemplate() : false;
	}

	/**
	 * get the parttype for this value.
	 * @return the partttype for this value.
	 */
	public <T extends PartType> T getPartType() throws NodeException {
		return getPartType(true);
	}

	/**
	 * get the parttype for this value.
	 * @param checkForNull true to check for null parttypes
	 * @return the partttype for this value.
	 */
	@SuppressWarnings("unchecked")
	public <T extends PartType> T getPartType(boolean checkForNull) throws NodeException {
		// if the parttype is already there, check whether it still matches the typeId of the part
		if (partType != null) {
			Part part = getPart(checkForNull);
			if (part != null && !part.matches(partType)) {
				partType = null;
			}
		}

		// OverviewPartTypes must be fetched new every time, because the
		// parttype contains data that may change when the part is changed
		// (sticky channel flag)
		if (partType == null || (partType instanceof OverviewPartType && !getObjectInfo().isEditable())) {
			Part part = getPart(checkForNull);
			if (part != null) {
				partType = part.getPartType(this);
			}
		}
		return (T)partType;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	public String render(RenderResult renderResult) throws NodeException {
		return render(renderResult, null, false);
	}

	public String render(RenderResult renderResult, String template) throws NodeException {
		return render(renderResult, template, false);
	}

	/**
	 * Variant of render that may force output (regardless whether the tag is enabled or not)
	 * @param renderResult render result
	 * @param template template (may be null)
	 * @param forceOutput true when the rendering shall be forced, false if not
	 * @return the rendered value
	 * @throws NodeException
	 */
	public String render(RenderResult renderResult, String template, boolean forceOutput) throws NodeException {
		return render(renderResult, template, forceOutput, false);
	}

	/**
	 * Variant of render that may enforce rendering of value without checking if the
	 * value was already rendered.
	 * @param renderResult render result
	 * @param template template (may be null)
	 * @param forceOutput true when the rendering shall be forced, false if not
	 * @param noRecursionCheck don't check if the given value is already on the render stack. Should only be true if a recursion check is already done by the caller !
	 * @return the rendered value
	 * @throws NodeException
	 */
	public String render(RenderResult renderResult, String template, boolean forceOutput, boolean noRecursionCheck) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		PartType type = getPartType();
		Part part = getPart();

		String code = "";
		boolean doNotFixEmptyCode = false;

		if (type != null) {
			RuntimeProfiler.beginMark(JavaParserConstants.VALUECLASS_RENDER, type.getClass().getName());
			if (part != null) {
				RuntimeProfiler.beginMark(JavaParserConstants.VALUE_RENDER, part.getKeyname());
			}

			final ValueContainer container = getContainer();

			if (!noRecursionCheck && renderType.find(this) > -1) {
				// TODO loop found! errorhandling
				return "";
			}

			renderType.push(this);
			int editMode = renderType.getEditMode();

			try {
				// If we are in aloha mode and have an inline editable part we add the aloha id 
				// to the renderResult so that AlohaRenderer can render the editables
				boolean partAlohaEditable = (editMode == RenderType.EM_ALOHA && getPart().isInlineEditable() && type.isLiveEditorCapable()
						&& container instanceof Tag && ((Tag) container).isEditable()); 

				if (container instanceof Tag && !((Tag) container).isEnabled() && !forceOutput) {
					code = "";
					doNotFixEmptyCode = true;
				} else {
					code = type.render(renderResult, template);
				}

				if (code == null) {
					// TODO errormessage

					code = "";
				} else {
					TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());
					code = renderer.render(renderResult, code);
				}

				if (partAlohaEditable) {
					// Part is editable in aloha .. we surround it with <gtxEditable>
					code = "<gtxEditable " + this.getId() + ">" + code + "</gtxEditable " + this.getId() + ">";
				}
			} catch (NodeException e) {
				String partDebug = "";

				if (part != null) {
					partDebug = "part key {" + part.getKeyname() + "} ";
					Construct construct = part.getConstruct();

					if (construct != null) {
						partDebug = "tagtype name {" + construct.getName() + "} tagtype keyname {" + construct.getKeyword() + "}";
					}
				}
				renderResult.error(Value.class,
						"Error while rendering part type {" + type.getClass().getName() + "} " + partDebug + " value id {" + getId() + "} - part id {" + getPartId()
						+ "}",
						e);
			} finally {
				renderType.pop();
				if (part != null) {
					RuntimeProfiler.endMark(JavaParserConstants.VALUE_RENDER, part.getKeyname());
				}
				RuntimeProfiler.endMark(JavaParserConstants.VALUECLASS_RENDER, type.getClass().getName());
			}
		} else {// TODO error message
		}

		return code;
	}

	public Object get(String key) {
		try {
			Object value = getPartType().get(key);

			// return value == null ? super.get(key) : value;
			return value;
		} catch (NodeException e) {
			logger.error("Error while resolving {" + key + "}", e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#dirtCache()
	 */
	public void dirtCache() throws NodeException {
		PartType partType = getPartType();

		if (partType != null) {
			partType.dirtCache();
		} else {
			logger.warn("Did not find parttype for Value {" + this.getId() + "}");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getKeywordResolvable(java.lang.String)
	 */
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getShortcutResolvable()
	 */
	public Resolvable getShortcutResolvable() throws NodeException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackHashKey()
	 */
	public String getStackHashKey() {
		return "value:" + getHashKey();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackKeywords()
	 */
	public String[] getStackKeywords() {
		return RENDER_KEYS;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the value's udate
		int udate = getUdate();

		// check the parttype specific udate
		udate = Math.max(udate, getPartType().getEffectiveUdate());
		return udate;
	}

	@Override
	public Integer getTType() {
		return TYPE_VALUE;
	}

	/**
	 * Check whether this tag has been modified
	 * @return False
	 */
	public boolean isModified() {
		return false;
	}
}
