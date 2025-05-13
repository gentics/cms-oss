/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: Tag.java,v 1.36 2010-10-19 11:23:41 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.model.TagModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.LiveEditorHelper;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.DataField;
import com.gentics.contentnode.factory.object.Updateable;
import com.gentics.contentnode.factory.object.ValueFactory;
import com.gentics.contentnode.parser.tag.ParserTag;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderType.RemoveTopResolvable;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.lib.etc.StringUtils;

/**
 * This is the main tag object which implements the rendering of tags.
 */
public abstract class Tag extends ValueContainer implements ParserTag, NamedNodeObject {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5382657161978324591L;

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<Tag, TagModel, TagModel> NODE2DEVTOOL = (from, to) -> {
		to.setActive(from.isEnabled());
		to.setConstructId(from.getConstruct().getGlobalId().toString());
		to.setGlobalId(from.getGlobalId().toString());
		to.setName(from.getName());

		return to;
	};

	public final static Consumer<com.gentics.contentnode.rest.model.Tag> EMBED_CONSTRUCT = restTag -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct construct = t.getObject(Construct.class, restTag.getConstructId());
		if (construct == null) {
			return;
		}

		restTag.setConstruct(Construct.TRANSFORM2REST.apply(construct));
	};

	private static final String[] SPLITTER_TAGS = new String[0];

	private int hasPartTemplate;

	@DataField("construct_id")
	@Updateable
	protected Integer constructId;

	/**
	 * Flag to mark whether this instance has been modified
	 */
	protected boolean modified = false;

	/**
	 * flag to mark whether the tag is enabled (visible) or not
	 */
	@DataField("enabled")
	@Updateable
	protected int enabled;

	/**
	 * name of the tag
	 */
	@DataField("name")
	@Updateable
	protected String name;

	private static int uniqueCounter = 0;

	/**
	 * The ttype of the contenttag object.
	 */
	public static final int TYPE_CONTENTTAG = 10111;
    
	/**
	 * The ttype of the contenttag object as integer.
	 */
	public static final Integer TYPE_CONTENTTAG_INTEGER = new Integer(TYPE_CONTENTTAG);

	/**
	 * The ttype of the templatetag object.
	 */
	public static final int TYPE_TEMPLATETAG = 10112;
    
	/**
	 * The ttype of the templatetag object as integer.
	 */
	public static final Integer TYPE_TEMPLATETAG_INTEGER = new Integer(TYPE_TEMPLATETAG);

	/**
	 * The ttype of the objecttag object.
	 */
	public static final int TYPE_OBJECTTAG = 10113;
    
	/**
	 * The ttype of the objecttag object as integer.
	 */
	public static final Integer TYPE_OBJECTTAG_INTEGER = new Integer(TYPE_OBJECTTAG);

	protected Tag(Integer id, NodeObjectInfo info) {
		super(id, info);
		hasPartTemplate = -1;
	}

	@Override
	public Integer getConstructId() throws NodeException {
		return constructId;
	}

	/**
	 * get the name of the tag.
	 * @return the keyname of the tag.
	 */
	@FieldGetter("name")
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the tag
	 * @param name
	 * @return the old name
	 * @throws ReadOnlyException
	 */
	@FieldSetter("name")
	public String setName(String name) throws ReadOnlyException {
		assertEditable();
		if (!StringUtils.isEqual(this.name, name)) {
			String oldName = this.name;

			modified = true;
			this.name = name;
			return oldName;
		} else {
			return this.name;
		}

	}

	/**
	 * check, if the tag is enabled and ready to use.
	 * @return true, if the tag is enabled.
	 */
	public boolean isEnabled() {
		return enabled == 1;
	}

	/**
	 * Get the current value of the enabled setting
	 * @return value of the enabled setting
	 */
	@FieldGetter("enabled")
	public int getEnabledValue() {
		return enabled;
	}

	/**
	 * Enable or disable the tag
	 * @param enabled true when the tag shall be enabled, false for disabling it
	 * @return the old status of the enabled flag
	 * @throws ReadOnlyException
	 */
	public boolean setEnabled(boolean enabled) throws ReadOnlyException {
		assertEditable();
		int newEnabled = enabled ? 1 : 0;

		if (this.enabled != newEnabled) {
			boolean oldEnabled = isEnabled();

			modified = true;
			this.enabled = newEnabled;
			return oldEnabled;
		} else {
			return isEnabled();
		}
	}

	/**
	 * Set the value of the enabled setting
	 * @param enabled value for the "enabled" setting
	 * @return old value
	 * @throws ReadOnlyException
	 */
	@FieldSetter("enabled")
	public int setEnabled(int enabled) throws ReadOnlyException {
		assertEditable();
		if (this.enabled != enabled) {
			int oldEnabled = this.enabled;

			this.enabled = enabled;
			this.modified = true;
			return oldEnabled;
		} else {
			return this.enabled;
		}
	}

	/**
	 * Check whether this tag has been modified
	 * @return True if modified
	 */
	public boolean isModified() {
		return this.modified;
	}

	/**
	 * Check whether this tag and or any of its
	 * values have been modified
	 * @return True if modified
	 * @throws NodeException 
	 */
	public boolean isTagOrValueModified() throws NodeException {
		if (this.isModified()) {
			return true;
		}

		ValueList valueList = this.getValues();
		for (Value value : valueList) {
			if (value.isModified()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the tag was clicked before and will become enabled when modified
	 * @return true or false
	 */
	public boolean isUnclicked() {
		return enabled == 3;
	}

	/**
	 * Set the construct id of the tag.
	 * @param constructId construct id
	 * @return old construct id
	 * @throws ReadOnlyException when the tag was not fetched for update
	 * @throws NodeException when another error occurred
	 */
	public Integer setConstructId(Integer constructId) throws ReadOnlyException, NodeException {
		assertEditable();
		if (ObjectTransformer.getInt(this.constructId, 0) != ObjectTransformer.getInt(constructId, 0)) {
			// get the construct
			Transaction t = TransactionManager.getCurrentTransaction();
			Construct construct = (Construct) t.getObject(Construct.class, constructId);

			if (construct == null) {
				throw new NodeException("Cannot set constructId to {" + constructId + "}, no such construct exists");
			}

			Integer oldConstructId = this.constructId;

			modified = true;
			this.constructId = constructId;

			// get the values, which will generate the default values for all editable parts
			getValues();

			return oldConstructId;
		} else {
			return this.constructId;
		}
	}

	/**
	 * get the container of this tag.
	 * @return the container of this tag.
	 * @throws NodeException 
	 */
	public abstract TagContainer getContainer() throws NodeException;

	public String[] getSplitterTags() {
		return SPLITTER_TAGS;
	}

	/**
	 * Deletes the tag but <strong>doesn't</strong> perform a permission check cause of performance.
	 * Should be implemented differently when accessible through a public API.
	 * @param force
	 */
	public void delete(boolean force) throws InsufficientPrivilegesException, NodeException {
		for (Value value : getValues()) {
			value.delete();
		}
		performDelete();
	}
    
	/**
	 * Performs the delete of the Tag
	 * @throws NodeException
	 */
	protected abstract void performDelete() throws NodeException;
    
	public Object get(String key) {
		if ("empty".equals(key)) {
			try {
				RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

				try (RemoveTopResolvable ac = renderType.withPopped(this)) {
					int oldMode = renderType.getEditMode();

					renderType.setEditMode(RenderType.EM_PREVIEW);
					String renderedContent = this.render(new RenderResult());

					renderType.setEditMode(oldMode);
					return StringUtils.isEmpty(renderedContent) ? 1 : 0;
				}
			} catch (NodeException e) {
				logger.error("Error while resolving {" + key + "}", e);
				return null;
			}
		} else if ("unique_tag_id".equals(key)) {
			return getId() + "-" + getTType();
		} else if ("visible".equals(key)) {
			return getEnabledValue();
		} else if ("name".equals(key)) {
			return getName();
		} else {
			return super.get(key);
		}
	}

	public boolean hasClosingTag() throws NodeException {

		if (hasPartTemplate == -1) {
			hasPartTemplate = 0;

			for (Value v : getTagValues()) {
				if (v.hasTemplate()) {
					hasPartTemplate = 1;
					break;
				}
			}
		}

		return hasPartTemplate == 1;
	}

	public boolean isClosingTag() {
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction()
				.getRenderResult());
	}

	public String render(RenderResult renderResult) throws NodeException {
		return render(renderResult, null, null);
	}

	/**
	 * Method called right before this tag is rendered
	 * @throws NodeException
	 */
	protected void doPreRendering() throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		// add a dependency on this tag (because it is rendered)
		renderType.addElementDependency(this, null);
		if (!DependencyManager.DIRECT_DEPENDENCIES) {
			// add the tag as source element
			renderType.pushDependentElement(this);
		}
		// push this element to the renderstack
		renderType.push(this);
	}

	/**
	 * Method called right after this tag was rendered
	 * @throws NodeException
	 */
	protected void doPostRendering() throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (!DependencyManager.DIRECT_DEPENDENCIES) {
			// remove this tag from dependency stack
			renderType.popDependentObject();
		}
		// remove this tag from the renderstack
		renderType.pop();
	}

	/**
	 * Check whether the given tagcontainers are identical
	 * @param container1 first container
	 * @param container2 second container
	 * @return true when the tagcontainers are identical, false if not
	 * @throws NodeException
	 */
	protected static boolean tagContainersEqual(TagContainer container1, TagContainer container2) throws NodeException {
		if (container1 == null || container2 == null) {
			return false;
		}

		if (container1 instanceof Page) {
			container1 = ((Page) container1).getContent();
		}
		if (container2 instanceof Page) {
			container2 = ((Page) container2).getContent();
		}

		return container1.equals(container2);
	}

	public String render(RenderResult renderResult, String template, Map codeParts) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
        
		String debugTagName = System.getProperty("com.gentics.contentnode.debugtag");

		if (debugTagName != null && debugTagName.equals(getName())) {
			RenderResult result = t.getRenderResult();

			result.debug(Tag.class, "Rendering tag {" + getName() + "} template: {" + template + "}");
		}

		int editMode = renderType.getEditMode();
		boolean modifiedEditMode = false;

		// TODO when the tag's main object is not the rendered root object, we disable editmode now
		if (isEditable() && (editMode == RenderType.EM_ALOHA)) {
			TagContainer container = getContainer();
			StackResolvable rootObject = renderType.getRenderedRootObject();

			if (rootObject instanceof TagContainer && !(tagContainersEqual(container, (TagContainer) rootObject))) {
				if (editMode == RenderType.EM_ALOHA) {
					editMode = RenderType.EM_ALOHA_READONLY;
					modifiedEditMode = true;
					if (logger.isDebugEnabled()) {
						logger.debug(
								"rendering {" + this + "} in aloha readonly mode, since the tag container {" + container + "} is not rootObject {" + rootObject + "}.");
					}
				}
			}
		}

		if (editMode == RenderType.EM_ALOHA) {
			// edit mode
			if (!isEnabled() && !isUnclicked()) {
				// do not render anything (neither code nor div) when already
				// clicked and disabled
				return "";
			}
		} else {
			// preview mode
			if (!isEnabled()) {
				// this is a hack for the feature "cleanup tags": the tag is
				// just not yet enabled (but should be rendered), so we make
				// sure it is not cleaned by pushing it to the stack
				renderType.push(this);
				renderType.pop();

				// do not render when the tag is disabled (don't care about clicked/unclicked)
				return "";
			}
		}

		// check for loops in rendering
		if (renderType.find(this) > -1) {
			// TODO loop found! errorhandling          
			return "";
		}

		if (debugTagName != null && debugTagName.equals(getName())) {
			RenderResult result = t.getRenderResult();

			result.debug(Tag.class, "doPreRendering for {" + getName() + "}");
		}

		// prepare tag for rendering
		doPreRendering();

		// when the edit mode is modified, we set the new edit mode now
		if (modifiedEditMode) {
			renderType = t.getRenderType();
			renderType.setEditMode(editMode);
		}

		try {
			StringBuffer source = new StringBuffer();
    
			ValueList values = getTagValues();
            
			if (debugTagName != null && debugTagName.equals(getName())) {
				RenderResult result = t.getRenderResult();

				result.debug(Tag.class, "rendering {" + getName() + "} .. got values - count: {" + values.size() + "}");
			}

			for (Value value : values) { 
				Part part = value.getPart();
    
				if (debugTagName != null && debugTagName.equals(getName())) {
					RenderResult result = t.getRenderResult();

					result.debug(Tag.class,
							"rendering value ... part {" + part + "} visible: {" + part.isVisible() + "} ml: {" + part.getMarkupLanguage() + "} has Template: {"
							+ value.hasTemplate() + "} - value: {" + value + "}");
				}
				if (!part.isVisible()) {
					continue;
				}
    
				MarkupLanguage ml = part.getMarkupLanguage();
				String tplMl = t.getRenderType().getMarkupLanguage();

				if (ml != null && tplMl != null && !ml.getExtension().equals(tplMl)) {
					continue;
				}
    
				if (value.hasTemplate()) {
					source.append(value.render(renderResult, template));
				} else {
					source.append(value.render(renderResult, null, false, true));
				}
    
			}
			if (debugTagName != null && debugTagName.equals(getName())) {
				RenderResult result = t.getRenderResult();

				result.debug(Tag.class, "rendered all values for {" + getName() + "}: {" + source.toString() + "}");
			}

			return source.toString();
		} finally {
			// finished with tag rendering
			doPostRendering();
		}
	}

	public boolean doPostParseCode(boolean defaultValue) {
		return defaultValue;
	}

	public boolean doPreParseCode(boolean defaultValue, String part) throws NodeException {
		return false;
	}

	public String getTagEndCode() {
		return getName();
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

	/**
	 * Checks whether the current tag is an Aloha block or an Aloha editable
	 * A tag is considered an aloha editable if it has exactly only one inline editable part and no other parts.
	 * Otherwise it is considered an Aloha block.
	 * 
	 * @return True if the tag is an Aloha block. False if the tag is an Aloha editable.
	 */
	@SuppressWarnings("unchecked")
	public boolean isAlohaBlock() throws NodeException {
		List<Part> parts = this.getConstruct().getParts(); 
        
		if (parts.size() == 1) {
			ValueList values = this.getValues();
			Part part = parts.get(0);

			if (values.size() != 1) {
				logger.warn("Tag { " + this + " } has different number of parts than values");
			} else {
				// We have one inline editable, live editor capable part -> this is a  
				if (part.isInlineEditable() && values.iterator().next().getPartType().isLiveEditorCapable()) {
					return false;
				}
			}
		}
        
		return true;    
	}
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditLink(boolean, int,
	 *      boolean, boolean)
	 */
	public String getEditLink() throws NodeException {
		StringBuffer ret = new StringBuffer();
		String url = TransactionManager.getCurrentTransaction().getRenderType().getRenderUrl(getObjectInfo().getObjectClass(), getId()).toString();

		StringBuffer js = new StringBuffer();
		StringBuffer add = new StringBuffer();
		StringBuffer hopedit = new StringBuffer();

		hopedit.append("hopedit('").append(url).append("');");

		js.append(hopedit);

		ret.append("<a href=\"javascript:").append(js).append("\" onmouseover=\"self.status='").append(getName());
		ret.append(" (").append(StringUtils.escapeXML(getConstruct().getName().toString())).append(")");
		ret.append("'; return true;\" onmouseout=\"self.status=''; return true;\" ").append(add).append(">");

		if (getConstruct().getName() != null) {
			ret.append(getName() + "(" + StringUtils.escapeXML(getConstruct().getName().toString()) + ")");
		} else {
			ret.append(getName());
		}
		ret.append("</a>");

		return ret.toString();
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPrefix()
	 */
	public String getEditPrefix() throws NodeException {        
		StringBuffer prefix = new StringBuffer();

		// adapt when inline editable
		prefix.append("<" + LiveEditorHelper.getLiveeditWrapperTagName(this) + " contenteditable=false id=t_").append(getName()).append(" class=\"lock\" ");
		// If the element is not live editable stop the bubbling of events.
		// This is necessary to disable javascript functions for not inline editable elements embedded in editable ones. (eg. context menu) 
		prefix.append("oncontextmenu=\"stopBubble(event);\" ").append("onbeforepaste=\"stopBubble(event);\" ").append("onkeypress=\"stopBubble(event);\" ").append("onpaste=\"stopBubble(event);\" ").append("onkeydown=\"stopBubble(event);\" ").append("ondblclick=\"stopBubble(event);\" ").append("onkeyup=\"stopBubble(event);\" ").append(
				"onclick=\"stopBubble(event);\" ");
		prefix.append(">");
    
		return prefix.toString();      
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#getEditPostfix()
	 */
	public String getEditPostfix() throws NodeException {
		StringBuffer postfix = new StringBuffer();

		postfix.append("</" + LiveEditorHelper.getLiveeditWrapperTagName(this) + ">");
		return postfix.toString();
	}

	/**
	 * Check whether the tag contains an _EDITABLE_ overview part or not
	 * @return if at least one part is an overview, false if not
	 * @throws NodeException
	 */
	public boolean containsOverviewPart() throws NodeException {
		return getConstruct().containsOverviewPart();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.ValueContainer#resolvePartsWithShortCuts()
	 */
	protected boolean resolvePartsWithShortCuts() {
		// only resolve parts by their names (as shortcuts) when the tag is
		// enabled. this is compatible with the old behaviour of GCN 3.6
		return isEnabled();
	}

	@Override
	public boolean isTag() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.AbstractContentObject#copyFrom(com.gentics.lib.base.object.NodeObject)
	 */
	public <T extends NodeObject> void copyFrom(T original) throws ReadOnlyException, NodeException {
		super.copyFrom(original);
		Tag oTag = (Tag) original;

		// copy the meta data
		Integer constructId = getConstructId();
		if (constructId != null && constructId > 0 && ObjectTransformer.getInt(getConstructId(), 0) != ObjectTransformer.getInt(oTag.getConstructId(), 0)) {
			// if the constructs are different, we need to migrate
			migrateToConstruct(oTag.getConstruct());
		} else {
			setConstructId(oTag.getConstruct().getId());
		}

		setEnabled(oTag.getEnabledValue());
		setName(oTag.getName());

		// copy the values
		EditableValueList thisValues = (EditableValueList) getValues();
		ValueList originalValues = oTag.getValues();

		for (Value originalValue : originalValues) {
			String partKeyname = originalValue.getPart().getKeyname();
			Value thisValue = null;

			// if the part has no key, we try to get the value by part_id (this will only work, if the tags initially used the same construct)
			if (StringUtils.isEmpty(partKeyname)) {
				thisValue = thisValues.getByPartId(originalValue.getPart().getId());
			} else {
				thisValue = thisValues.getByKeyname(partKeyname);
			}

			if (thisValue != null) {
				// found the value in this tag, copy the original value over it
				thisValue.copyFrom(originalValue);
			} else {
				// did not find the value, so copy the original
				thisValues.addValue((Value) originalValue.copy());
			}
		}
	}

	/**
	 * Filter out values that are already deleted in the transaction
	 * @param values	values to filter
	 * @return	a new list containing only values not marked as deleted
	 * @throws NodeException
	 */
	protected ValueList filterDeletedValues(Collection<Value> values) throws NodeException {
		ValueFactory objFactory = (ValueFactory)getFactory().getFactoryHandle(Value.class).getObjectFactory(Value.class);
		EditableValueList result;
		if (values instanceof EditableValueList) {
			result = new EditableValueList(((EditableValueList)values).get("unique_tag_id"));
		} else {
			result = new EditableValueList(null);
		}
		for (Value v: values) {
			if (!objFactory.isInDeletedList(Value.class, v)) {
				result.addValue(v);
			}
		}
		return result;
	}

	/**
	 * Change this tag to match the Tagtype of the given templatetag. Implementations
	 * should match tag.cmd.php:tag_transform()
	 * @param newConstruct the tagtype to change this tag to
	 * @throws NodeException
	 */
	public void migrateToConstruct(Construct newConstruct) throws NodeException {
		assertEditable();
	}
}
