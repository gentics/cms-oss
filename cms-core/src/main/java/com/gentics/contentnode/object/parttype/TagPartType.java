/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: TagPartType.java,v 1.23 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.resolving.StackResolvable;

/**
 * The tag parttype implements a tag which renders tags from another page or
 * template.
 */
public abstract class TagPartType extends AbstractPartType {

	public static final int TYPE_TEMPLATE = 1;

	public static final int TYPE_PAGE = 2;

	private final static Set<String> resolvableKeys = SetUtils.unmodifiableSet("id", "page_id", "tag", "container");

	private int type;

	private Class<? extends Tag> tagClass;

	private Integer tagId;

	public TagPartType(Value value, int type) throws NodeException {
		super(value);
		this.type = type;
		reloadValue();
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * Returns true if no reference to a tag is set
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}

		if (getValueObject().getValueRef() <= 0) {
			return true;
		} else {
			return false;
		}
	}
    
	private void reloadValue() throws NodeException {

		tagId = new Integer(getValueObject().getValueRef());

		if (type == TYPE_PAGE) {
			tagClass = "t".equals(getValueObject().getValueText()) ? TemplateTag.class : ContentTag.class;
		} else {
			tagClass = TemplateTag.class;
		}
	}

	public Integer getPageId() throws NodeException {
		return new Integer(getValueObject().getInfo());
	}

	public boolean hasTemplate() throws NodeException {
		return false;
	}

	public Tag getLinkedTag() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		// get the linked tag
		Tag tag = t.getObject(tagClass, tagId);

		// when this is a contenttag, we maybe need to make the fallback to the templatetag (when the templatetag is not editable)
		if (type == TYPE_PAGE) {
			// get the page
			Page page = t.getObject(Page.class, getPageId());

			// when both page and tag are set, we let the page resolve the tag
			// with desired name
			if (page != null && tag != null) {
				// this might now be the templatetag
				tag = page.getTag(tag.getName());
			}
		} else if (type == TYPE_TEMPLATE && tag != null) {
			// even for templates, we get the tag by name, because this will
			// handle multichannelling fallback if necessary.
			TagContainer template = tag.getContainer();

			if (template != null) {
				tag = template.getTag(tag.getName());
			}
		}

		return tag;
	}

	public TagContainer getTagContainer() throws NodeException {
		Tag linkedTag = getLinkedTag();

		if (linkedTag != null) {
			if (type == TYPE_PAGE) {
				return (TagContainer) TransactionManager.getCurrentTransaction().getObject(Page.class, getPageId());
			} else {
				return linkedTag.getContainer();
			}
		} else {
			return null;
		}
	}

	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		Tag linkedTag = getLinkedTag();
		TagContainer tagContainer = getTagContainer();
		if (tagContainer == null) {
			// container does not exist, it was most likely deleted, so render nothing
			// but add a dependency, if container was put into wastebin
			if (renderType.doHandleDependencies()
					&& TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN)) {
				try (WastebinFilter wastebin = Wastebin.INCLUDE.set()) {
					tagContainer = getTagContainer();
					if (tagContainer != null) {
						renderType.addDependency(new DependencyObject((NodeObject) tagContainer, linkedTag), null);
					}
				}
			}

			return "";
		}

		// emergency exit, when linked tag could not be fetched
		if (linkedTag == null) {
			if (tagId == null || tagId > 0) {
				String msg = "TagPartType is null, data inconsistency - invalid link to tag {tagClass:" + tagClass + "} {tagId:" + tagId + "} {valueId:"
						+ getValueObject().getId() + "}";
				result.error(TagPartType.class, msg);
			}
			return "";
		}

		// determine whether to stop resolving from the original object (for page tags, not for template tags)
		boolean stopResolvingToSourceObject = (type == TYPE_PAGE);

		// push the container of the tag as "container" onto the resolvable
		// stack
		StackResolvable container = null;

		try {
			container = new ContainerResolver(renderType.getTopmostTagContainer());
			// container = null;
		} catch (Exception e) {// ignoring this exception since this will fail eg. when an overview
			// is rendered as a folder's object property. in this case the container
			// will remain null, which is not an issue since resolving the container
			// in a folder objprop does not make any sense at all.
		}
		if (container != null) {
			renderType.push(container);
		}

		if (stopResolvingToSourceObject) {
			// put the stop object onto the render stack (to avoid unwanted
			// fallback to tags in the including page)
			renderType.push(StackResolvable.STOP_OBJECT);
		}

		// render the result in the context of the container
		renderType.push(tagContainer);

		try {
			// eventually handle dependencies
			if (renderType.doHandleDependencies()) {
				// add the dependency to the linked tag
				renderType.addDependency(new DependencyObject((NodeObject) tagContainer, linkedTag), null);
				if (!DependencyManager.DIRECT_DEPENDENCIES) {
					// set the tag container as new dependency object
					renderType.pushDependentObject(new DependencyObject((NodeObject) tagContainer, linkedTag));
				}
			}

			int editMode = renderType.getInfo(renderType.getDepth() - 2).getEditMode();

			if (editMode == RenderType.EM_ALOHA) {
				renderType.setEditMode(RenderType.EM_ALOHA_READONLY);
			}

			String source = linkedTag.render(result);

			// as a tag is rendered completely on its own, render it with the main
			// default-renderer
			TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getInfo(0).getDefaultRenderer());

			return renderer.render(result, source);
		} finally {
			// remove the tag container from the stack
			renderType.pop();

			if (stopResolvingToSourceObject) {
				renderType.pop();
			}

			// remove the container (if on the stack)
			if (container != null) {
				renderType.pop(container);
			}

			if (renderType.doHandleDependencies()) {
				if (!DependencyManager.DIRECT_DEPENDENCIES) {
					renderType.popDependentObject();
				}
			}
		}
	}

	public Object get(String key) {
		// TODO move this to javabean getters
		if ("id".equals(key)) {
			return tagId;
		}
		if (type == TYPE_PAGE && "page_id".equals(key)) {
			try {
				return getPageId();
			} catch (NodeException e) {
				logger.error("Error while getting {" + key + "} for {" + this + "}", e);
			}
		}
		if ("tag".equals(key)) {
			try {
				return getLinkedTag();
			} catch (NodeException e) {
				logger.error("Error while getting {" + key + "} for {" + this + "}", e);
			}
		}
		if ("container".equals(key)) {
			try {
				return getTagContainer();
			} catch (NodeException e) {
				logger.error("Error while getting {" + key + "} for {" + this + "}", e);
			}
		}
		return null;
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof TagPartType) {
			return Objects.equals(getLinkedTag(), ((TagPartType) other).getLinkedTag());
		} else {
			return false;
		}
	}
}
