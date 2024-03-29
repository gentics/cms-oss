/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: OverviewPartType.java,v 1.22.2.1 2011-01-18 13:21:54 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueContainer;
import com.gentics.contentnode.objectsource.ObjectSourceFactory;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.etc.StringUtils;

/**
 * PartType 13 - Overview
 * <p>
 * The Overview parttype renderes overviews, which are created by the {@link ObjectSourceFactory}.
 * </p>
 */
public class OverviewPartType extends AbstractPartType {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 5466402490367442344L;

	/**
	 * Constant of the type id
	 */
	public final static int TYPE_ID = 13;

	/**
	 * overview id
	 */
	private Integer overviewId;

	/**
	 * Editable copy of the overview
	 */
	private Overview editableOverview;

	/**
	 * Flag to mark parts that have sticky channel
	 */
	private boolean stickyChannel;

	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public OverviewPartType(Value value) throws NodeException {
		super(value);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#setValue(com.gentics.contentnode.object.Value)
	 */
	public void setValue(Value value) throws NodeException {
		super.setValue(value);
		overviewId = null;
		stickyChannel = false;
		if (value != null) {
			String infoText = value.getPart().getInfoText();
			if (!ObjectTransformer.isEmpty(infoText)) {
				String[] infoTextParts = StringUtils.splitString(infoText, ';');

				if (TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
					// the possible fourth part is the flag for sticky channel
					if (infoTextParts.length >= 4) {
						stickyChannel = ObjectTransformer.getBoolean(infoTextParts[3], false);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
	 */
	public boolean isMandatoryAndNotFilledIn() throws NodeException {
		if (!isRequired()) {
			return false;
		}
        
		loadOverviewId();
		return overviewId == null;
	}
    
	/**
	 * Get the overview linked to this parttype, or null if no datasource present
	 * @return overview of this parttype or null
	 * @throws NodeException
	 */
	public Overview getOverview() throws NodeException {
		Value value = getValueObject();
		NodeObjectInfo info = value.getObjectInfo();
		Transaction t = TransactionManager.getCurrentTransaction();

		loadOverviewId();

		if (overviewId != null) {
			if (info.isEditable()) {
				if (editableOverview == null) {
					editableOverview = t.getObject(Overview.class, overviewId, true);
				}
				return editableOverview;
			} else {
				return t.getObject(Overview.class, overviewId, info.getVersionTimestamp());
			}
		} else if (info.isEditable()) {
			ValueContainer valueContainer = value.getContainer();

			if (editableOverview == null && valueContainer instanceof Tag) {
				editableOverview = t.createObject(Overview.class);
				editableOverview.setContainer((Tag) valueContainer);
				// set the default values from the default overview. Get the
				// data from part.info_text!
				Part part = value.getPart();

				String infoText = part.getInfoText();
				if (!ObjectTransformer.isEmpty(infoText)) {
					String[] infoTextParts = StringUtils.splitString(infoText, ';');
					// first part in the infoText is the list of possible target
					// types
					if (infoTextParts.length > 0) {
						int[] types = StringUtils.splitInt(infoTextParts[0], ",");
						if (types.length == 1) {
							editableOverview.setObjectClass(t.getClass(types[0]));
						}
					}
					// second part in the infoText is the list of possible
					// selection types
					if (infoTextParts.length > 1) {
						int[] selTypes = StringUtils.splitInt(infoTextParts[1], ",");
						if (selTypes.length == 1) {
							editableOverview.setSelectionType(selTypes[0]);
						}
					}
				}
			}
			return editableOverview;
		} else {
			return null;
		}
	}

	/**
	 * Set the editable overview
	 * @param overview overview
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setOverview(Overview overview) throws ReadOnlyException, NodeException {
		Value value = getValueObject();
		NodeObjectInfo info = value.getObjectInfo();

		if (!info.isEditable()) {
			throw new ReadOnlyException();
		}

		if (!Value.isEmptyId(overviewId) && ObjectTransformer.getInt(overviewId, -1) != ObjectTransformer.getInt(overview.getId(), -1)) {
			throw new NodeException("Cannot set overview, because it is already set to " + overviewId);
		}

		this.editableOverview = overview;
		overviewId = overview.getId();
	}

	/**
	 * load the overview id (if not done before)
	 * @throws NodeException
	 */
	protected void loadOverviewId() throws NodeException {
		if (overviewId == null) {
			overviewId = ObjectSourceFactory.getOverviewId(getValueObject());
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		Overview overview = getOverview();

		return overview != null ? overview.hasCodeTemplate(getValueObject()) : true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		super.render(result, template);
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		// try to find some 'mother' infos .. *crunch,crunch*
		Template tpl = null;
		Folder folder = null;

		for (int i = renderType.getDepth() - 1; i >= 0 && (folder == null || tpl == null); i--) {
			StackResolvable baseObj = renderType.getInfo(i).getLevelResolvable();

			if (baseObj == null) {
				continue;
			}

			if (baseObj instanceof Page) {
				if (tpl == null) {
					tpl = ((Page) baseObj).getTemplate();
				}
				if (folder == null) {
					folder = ((Page) baseObj).getFolder();
				}
			} else if (baseObj instanceof Folder) {
				if (folder == null) {
					folder = (Folder) baseObj;
				}
			} else if (baseObj instanceof Template) {
				if (tpl == null) {
					tpl = (Template) baseObj;
				}
			}
		}

		ContentLanguage language = renderType.getLanguage();

		Overview overview = getOverview();

		if (overview == null) {
			return "";
		}

		// currently, we always use the template from the code, not the
		// value-text
		if (!overview.hasCodeTemplate(getValueObject()) || template == null) {
			template = overview.getTemplate(getValueObject(), tpl);
		}

		List<? extends NodeObject> objs = overview.getSelectedObjects(folder, language);

		// set defaultrenderer to main renderer to translate complete code.
		renderType.push();
		try {
			renderType.setDefaultRenderer(renderType.getInfo(0).getDefaultRenderer());

			// when rendered in edit mode, set viewmode
			int editMode = renderType.getInfo(renderType.getDepth() - 2).getEditMode();

			if (editMode == RenderType.EM_ALOHA || editMode == RenderType.EM_ALOHA_READONLY) {
				renderType.setEditMode(RenderType.EM_PREVIEW);
			}
			return overview.translate(result, objs, template);
		} finally {
			renderType.pop();
		}
	}

	/**
	 * Get number of items in the overview
	 * @return number of items
	 * @throws NodeException 
	 * TODO optimize
	 */
	public int getCount() throws NodeException {
		return getItems().size();
	}

	/**
	 * get collection of items in the overview
	 * @return
	 * @throws NodeException
	 */
	public Collection getItems() throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("get items of " + getValueObject());
		}
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		Folder folder = null;

		for (int i = renderType.getDepth() - 1; i >= 0 && folder == null; i--) {
			StackResolvable baseObj = renderType.getInfo(i).getLevelResolvable();

			if (baseObj == null) {
				continue;
			}

			if (baseObj instanceof Page) {
				if (folder == null) {
					folder = ((Page) baseObj).getFolder();
					if (logger.isDebugEnabled()) {
						logger.debug("Folder: " + folder);
					}
				}
			} else if (baseObj instanceof Folder) {
				if (folder == null) {
					folder = (Folder) baseObj;
					if (logger.isDebugEnabled()) {
						logger.debug("Folder: " + folder);
					}
				}
			} else if (baseObj instanceof Template) {}
		}

		ContentLanguage language = renderType.getLanguage();

		if (language != null && logger.isDebugEnabled()) {
			logger.debug("Language: " + language);
		}

		Overview overview = getOverview();

		if (overview == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Overview is null, returning empty list");
			}
			return Collections.EMPTY_LIST;
		} else {
			List result = overview.getSelectedObjects(folder, language);
			if (logger.isDebugEnabled()) {
				logger.debug("Returning " + result.size() + " objects");
			}
			return result;
		}
	}
    
	/**
	 * Returns a list of rendered items. The selected items of the overview are rendered separately with the overview 
	 * template and the rendered code is returned in a list of strings.
	 * @return List of strings - the separately rendered items of the overview
	 * @throws Exception
	 */
	public Collection getRendereditems() throws NodeException {
		Collection items = getItems();
		Iterator iter = items.iterator();
		Vector result = new Vector();
		int i = 0;

		while (iter.hasNext()) {
			Object item = iter.next();
			Overview overview = getOverview();

			if (item instanceof StackResolvable) {
				result.add(overview.translate(new RenderResult(), (StackResolvable) item, overview.getTemplate(), ++i, items.size()));
			} else {
				logger.warn("Encountered strange object {" + item + "} in the overview {" + overviewId + "}.");
			}
		}
		return result;
	}

	/**
	 * Check whether the part has sticky channel
	 * @return true for sticky channel
	 */
	public boolean isStickyChannel() {
		return stickyChannel;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#dirtCache()
	 */
	public void dirtCache() throws NodeException {
		// dirt the cache for the overview
		try {
			loadOverviewId();
		} catch (InconsistentDataException e) {// we ignore this exception
		}

		if (overviewId != null) {
			TransactionManager.getCurrentTransaction().dirtObjectCache(Overview.class, overviewId, false);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#copyFrom(com.gentics.contentnode.object.parttype.PartType)
	 */
	public <T extends PartType> void copyFrom(T original) throws ReadOnlyException, NodeException {
		super.copyFrom(original);
		OverviewPartType oPartType = (OverviewPartType) original;
		Overview thisOverview = getOverview();
		Overview originalOverview = oPartType.getOverview();

		thisOverview.copyFrom(originalOverview);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		Overview overview = getOverview();

		if (overview != null) {
			return overview.getEffectiveUdate();
		} else {
			return -1;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#postSave()
	 */
	public boolean postSave() throws NodeException {
		ValueContainer valueContainer = getValueObject().getContainer();
		Overview overview = getOverview();
		boolean modified = false;

		if (overview != null && valueContainer instanceof Tag) {
			overview.setContainer((Tag) valueContainer);

			//save the overview if it is not undefined
			if (!overview.isUndefined()) {
				modified |= overview.save();
			}
		}

		return modified;
	}

	@Override
	public Type getPropertyType() {
		return Type.OVERVIEW;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		Value nodeValue = getValueObject();
		if (nodeValue.getContainer() instanceof Construct) {
			// value_text of the default value contains the template
			property.setStringValue(nodeValue.getValueText());
			// info of the default value contains the "changeable" flag
			property.setBooleanValue(nodeValue.getInfo() == 1);
		}

		Overview overview = getOverview();

		if (null != overview) {
			property.setOverview(ModelBuilder.getOverview(overview));
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ModelBuilder.fillRestOverview2Node(t, property, getValueObject());
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof OverviewPartType) {
			OverviewPartType otherOverviewPT = (OverviewPartType) other;
			Overview overview = getOverview();
			if (overview == null) {
				return otherOverviewPT.getOverview() == null;
			} else if (otherOverviewPT.getOverview() == null) {
				return false;
			} else {
				return overview.hasSameContent(otherOverviewPT.getOverview());
			}
		} else {
			return false;
		}
	}

	@Override
	public Object get(String key) {
		try {
			if ("listType".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? Overview.getListType(overview.getObjectClass()) : ListType.UNDEFINED;
			} else if ("selectType".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? Overview.getSelectType(overview.getSelectionType()) : SelectType.UNDEFINED;
			} else if ("orderDirection".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? Overview.getOrderDirection(overview.getOrderWay()) : OrderDirection.UNDEFINED;
			} else if ("orderBy".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? Overview.getOrderBy(overview.getOrderKind()) : OrderBy.UNDEFINED;
			} else if ("maxItems".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? overview.getMaxObjects() : 0;
			} else if ("recursive".equals(key)) {
				Overview overview = getOverview();
				return overview != null ? overview.doRecursion() : false;
			}
		} catch (NodeException e) {
			logger.error("Error while getting " + key + " from " + this, e);
			return null;
		}
		return super.get(key);
	}
}
