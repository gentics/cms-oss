/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ContentTag.java,v 1.12.6.1 2011-03-07 16:36:43 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.regex.Pattern;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.parttype.TablePartType;
import com.gentics.contentnode.render.RenderType;

/**
 * A contenttag is a tag which is linked to a {@link Content}.
 */
@TType(Tag.TYPE_CONTENTTAG)
public abstract class ContentTag extends Tag {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2907587543144744976L;

	/**
	 * Pattern for finding table cell tags
	 */
	protected final static Pattern TABLE_CELL_TAG_NAME = Pattern.compile(".+\\.[A-Z]+[0-9]+");

	protected ContentTag(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	public String getTypeKeyword() {
		return "contenttag";
	}

	/**
	 * get the content which contains this tag.
	 * @return the content with . 
	 * @throws NodeException 
	 */
	protected abstract Content getContent() throws NodeException;

	/**
	 * Set the content id
	 * @param contentId new content id
	 * @return old content id
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public Integer setContentId(Integer contentId) throws ReadOnlyException {
		failReadOnly();
		return null;
	}

	/**
	 * Set the content
	 * @param content new content
	 * @throws NodeException
	 */
	public void setContent(Content content) throws NodeException {
		if (content == null) {
			throw new NodeException("Cannot set content to null");
		}
		setContentId(content.getId());
	}

	public TagContainer getContainer() throws NodeException {
		return getContent();
	}

	public String getStackHashKey() {
		return "cntag:" + getHashKey();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isEditable()
	 */
	public boolean isEditable() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		int tagNestLevel = renderType.getStack().countInstances(Tag.class, this);

		// if the tag is the only tag on the stack, it is always considered editable
		if (tagNestLevel == 0) {
			return true;
		}

		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		// when rendering for aloha editor, there are some tags that probably need to be migrated to aloha editor.
		// migration means that those tags are rendered as if they were not editable at all (they render only their regular output).
		// in this way, when the page is saved, the tags' html will be made part of the contenteditable code
		if (renderType.getEditMode() == RenderType.EM_ALOHA || renderType.getEditMode() == RenderType.EM_ALOHA_READONLY) {
			// the following will find table cell tags that are inline editable
			if (TABLE_CELL_TAG_NAME.matcher(getName()).matches() && isInlineEditable()) {
				return false;
			}

			// table tags will always be migrated
			for (Value value : getValues()) {
				if (value.getPartType() instanceof TablePartType) {
					return false;
				}
			}

			// some other tagtypes may have been configured to be migrated
			String[] migratedTagtypes = prefs.getProperties("aloha_migrate_tagtypes");
			if (!ObjectTransformer.isEmpty(migratedTagtypes)) {
				Construct construct = getConstruct();
				for (int i = 0; i < migratedTagtypes.length; i++) {
					if (construct.getKeyword().equals(migratedTagtypes[i]) || ObjectTransformer.getString(construct.getId(), "-1").equals(migratedTagtypes[i])) {
						return false;
					}
				}
			}
		}

		// all other contenttags are always editable
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.tag.ParserTag#isInlineEditable()
	 */
	public boolean isInlineEditable() throws NodeException {
		return getConstruct().isInlineEditable();
	}

	@Override
	public Integer getTType() {
		return Tag.TYPE_CONTENTTAG;
	}

	/**
	 * Check whether the contenttag comes from a templatetag
	 * @return true iff contenttag comes from templatetag
	 */
	public abstract boolean comesFromTemplate();

	/**
	 * Make this contenttag (which must be new) a clone of the given templatetag
	 * @param tag templatetag
	 * @throws NodeException
	 */
	public void clone(TemplateTag tag) throws NodeException {
		assertEditable();
	}
}
