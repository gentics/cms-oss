/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: TemplateTag.java,v 1.11.8.1.2.1 2011-03-07 16:36:42 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.devtools.model.TemplateTagModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * The Templatetag is a special tag which is linked to templates.
 */
@TType(Tag.TYPE_TEMPLATETAG)
public abstract class TemplateTag extends Tag {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6626301841809804840L;

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<TemplateTag, TemplateTagModel, TemplateTagModel> NODE2DEVTOOL = (from, to) -> {
		Tag.NODE2DEVTOOL.apply(from, to);
		to.setEditableInPage(from.isPublic());
		to.setMandatory(from.getMandatory());

		return to;
	};

	protected TemplateTag(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	public String getTypeKeyword() {
		return "templatetag";
	}

	/**
	 * get the template which contains this tag.
	 * @return the template which contains this tag.
	 * @throws NodeException 
	 */
	public abstract Template getTemplate() throws NodeException;

	/**
	 * Set the template id
	 * @param templateId new template id
	 * @return old template id
	 * @throws ReadOnlyException
	 */
	public Integer setTemplateId(Integer templateId) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	/**
	 * Gets this template tag's mandatory flag.
	 *
	 * @return boolean of value true if this flag is set for this template tag;
	 *         boolean of value false otherwise.
	 */
	@FieldGetter("mandatory")
	public abstract boolean getMandatory();

	/**
	 * Sets this template tag's mandatory flag.
	 *
	 * The mandatory flag indicates whether or not the content tags created from
	 * this template tag must have their parts filled with their required values
	 * before the page in which they are contained can be published.
	 *
	 * @param mandatory
	 *            Whether or not the flag should be set to true.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("mandatory")
	public void setMandatory(boolean mandatory) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * check, if this tag is editable in the content.
	 * @return true, if the tag can be edited in the content.
	 */
	@FieldGetter("pub")
	public abstract boolean isPublic();

	/**
	 * Set, if this tag is editable in the content
	 * @param pub true, if the tag can be edited in the content.
	 * @throws ReadOnlyException
	 */
	@FieldSetter("pub")
	public void setPublic(boolean pub) throws ReadOnlyException {
		failReadOnly();
	}

	public TagContainer getContainer() throws NodeException {
		return getTemplate();
	}

	public String getStackHashKey() {
		return "tpltag:" + getHashKey();
	}

	public boolean isEditable() throws NodeException {
		// template tags are always editable of root objects is a template
		return (TransactionManager.getCurrentTransaction().getRenderType().getRenderedRootObject() instanceof Template);
	}

	public boolean isInlineEditable() throws NodeException {
		// see isEditable
		if (TransactionManager.getCurrentTransaction().getRenderType().getRenderedRootObject() instanceof Template) {
			return getConstruct().isInlineEditable();
		} else {
			return false;
		}
	}

	/**
	 * Get the templategroup id
	 * @return templategroup id
	 */
	public abstract Integer getTemplategroupId();

	@Override
	public Integer getTType() {
		return Tag.TYPE_TEMPLATETAG;
	}

	@Override
	public void triggerEvent(DependencyObject object, String[] property, int eventMask, int depth, int channelId)
			throws NodeException {
		super.triggerEvent(object, property, eventMask, depth, channelId);

		// when a new tag was created in the template, which is not editable in pages, we trigger "tags" for all pages using the template
		if ((Events.isEvent(eventMask, Events.UPDATE) || Events.isEvent(eventMask, Events.CREATE)) && !isPublic()) {
			List<Page> pages = getTemplate().getPages();
			for (Page page : pages) {
				page.triggerEvent(new DependencyObject(page, (NodeObject) null), new String[] { "tags" }, Events.UPDATE,
						depth + 1, 0);
			}
		}
	}
}
