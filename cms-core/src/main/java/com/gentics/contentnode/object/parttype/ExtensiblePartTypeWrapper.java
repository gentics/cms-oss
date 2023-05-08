/*
 * @author norbert
 * @date 07.03.2007
 * @version $Id: ExtensiblePartTypeWrapper.java,v 1.6 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.contentnode.parttype.ExtensiblePartType;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.NodeObjectResolverContext;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;

/**
 * Wrapper for extensible parttypes
 */
public class ExtensiblePartTypeWrapper extends AbstractPartType {
	private ExtensiblePartType wrappedPartType;

	/**
	 * Create an instance
	 * @param value value
	 * @param wrappedPartType
	 * @throws NodeException
	 */
	public ExtensiblePartTypeWrapper(Value value, ExtensiblePartType wrappedPartType) throws NodeException {
		super(value);
		this.wrappedPartType = wrappedPartType;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}
    
	/**
	 * ExtensiblePartType can't have a value so it's always filled and will return false.
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		super.render(renderResult, template);
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		// create a cms resolver
		renderType.createCMSResolver();

		try {
			// The reason we save and restore the current render result and replace it
			// temporarily with the given render result, is that when a Velocity tag is rendered in
			// Aloha mode, all nested block tags must be added to the given render result,
			// so that nested blocks are rendered correctly by the gcn-plugin when a veolcity
			// tag with nested blocks is reloaded.
			RenderResult previousRenderResult = t.getRenderResult();

			t.setRenderResult(renderResult);
			String renderedContent = wrappedPartType.render();

			if (null != previousRenderResult) {
				t.setRenderResult(previousRenderResult);
			}
			return renderedContent;
		} finally {
			renderType.popCMSResolver();
			wrappedPartType.cleanAfterRender();
		}
	}

	public Object get(String key) {
		if (wrappedPartType instanceof Resolvable) {
			RenderType renderType = null;
			Tag tag = null;

			try {
				renderType = TransactionManager.getCurrentTransaction().getRenderType();
				tag = NodeObjectResolverContext.getNodeObject(Tag.class);
				// TODO: the render stack may be empty, which will result in tag to be null under certain
				// circumstances. Happens when a custom part type is used in an Aloha page. This is probably
				// a bug somewhere else.
				if (null != tag) {
					renderType.push(tag);
				}
				Object value = ((Resolvable) wrappedPartType).getProperty(key);

				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				return null;
			} finally {
				if (renderType != null && tag != null) {
					renderType.pop(tag);
				}
			}
		}
		return super.get(key);
	}

	@Override
	public Type getPropertyType() {
		if (wrappedPartType instanceof TransformablePartType) {
			return ((TransformablePartType) wrappedPartType).getPropertyType();
		} else {
			return Type.UNKNOWN;
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
	}

	@Override
	public boolean hasSameContent(PartType other) {
		return other instanceof ExtensiblePartTypeWrapper;
	}
}
