/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: Overview.java,v 1.32.4.1.2.1.2.1 2011-03-08 12:28:11 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.devtools.model.OverviewItemModel;
import com.gentics.contentnode.devtools.model.OverviewModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FieldGetter;
import com.gentics.contentnode.factory.FieldSetter;
import com.gentics.contentnode.factory.LightWeightPageList;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Folder.FileSearch;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.Folder.ReductionType;
import com.gentics.contentnode.object.parttype.ContainerResolver;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.utility.FileComparator;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.objectsource.ObjectSource;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Flowable;

/**
 * Overview implementation. Used Features: dsfallback nodirectselection
 * contentgroup3 ds_empty_cs dssource
 */
public abstract class Overview extends AbstractContentObject implements ObjectSource {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 3931679351510247469L;

	/**
	* Order not defined.
	*/
	public static final int ORDERWAY_UNDEFINED = 0;

	/**
	 * Order ascending.
	 */
	public static final int ORDERWAY_ASC = 1;

	/**
	 * Order descending.
	 */
	public static final int ORDERWAY_DESC = 2;

	/**
	 * Way of selecting objects not defined.
	 */
	public static final int SELECTIONTYPE_UNDEFINED = 0;

	/**
	 * Select objects by folders.
	 */
	public static final int SELECTIONTYPE_FOLDER = 1;

	/**
	 * Select objects by selecting them one-by-one.
	 */
	public static final int SELECTIONTYPE_SINGLE = 2;

	/**
	 * Select objects from the same folder.
	 */
	public static final int SELECTIONTYPE_PARENT = 3;

	/**
	 * Order undefined.
	 */
	public static final int ORDER_UNDEFINED = 0;

	/**
	 * Order by name.
	 */
	public static final int ORDER_NAME = 1;

	/**
	 * Order by creation date.
	 */
	public static final int ORDER_CDATE = 2;

	/**
	 * Order by last edit date.
	 */
	public static final int ORDER_EDATE = 3;

	/**
	 * Order by publish date.
	 */
	public static final int ORDER_PDATE = 4;

	/**
	 * Order by filesize.
	 */
	public static final int ORDER_FILESIZE = 5;

	/**
	 * Order manually.
	 */
	public static final int ORDER_SELECT = 6;

	/**
	 * Order by priority.
	 */
	public static final int ORDER_PRIORITY = 7;

	/**
	 * name of the (special telekom) feature, that allows resolving of <node url> in overviews over pages to <node seite.url>
	 */
	public final static String URL_RESOLVING_FEATURE = "resolve_pageurl_in_overview";

	/**
	 * Function to convert the object to the devtools model
	 */
	public final static BiFunction<com.gentics.contentnode.rest.model.Overview, OverviewModel, OverviewModel> REST2DEVTOOL = (from, to) -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		if (from != null) {
			to.setListType(from.getListType());
			to.setMaxItems(from.getMaxItems());
			to.setOrderBy(from.getOrderBy());
			to.setOrderDirection(from.getOrderDirection());
			to.setRecursive(from.isRecursive());
			to.setSelectType(from.getSelectType());
			to.setSource(from.getSource());

			Class<? extends NodeObject> clazz = getSelectionClass(from.getSelectType(), from.getListType());

			if (from.getSelectedItemIds() != null) {
				to.setSelection(Flowable.fromIterable(from.getSelectedItemIds()).map(item -> {
					String globalId = t.getGlobalId(clazz, item);
					return new OverviewItemModel().setId(globalId);
				}).toList().blockingGet());
			} else if (from.getSelectedNodeItemIds() != null) {
				to.setSelection(Flowable.fromIterable(from.getSelectedNodeItemIds()).map(item -> {
					String globalNodeId = t.getGlobalId(Node.class, item.getNodeId());
					String globalId = t.getGlobalId(clazz, item.getObjectId());
					return new OverviewItemModel().setNodeId(globalNodeId).setId(globalId);
				}).toList().blockingGet());
			}
		}
		return to;
	};

	/**
	 * Get the class of objects, which are selected in an overview (not necessarily the objects, which are listed in the overview)
	 * @param selectType select type
	 * @param listType list type
	 * @return object class (may be null)
	 */
	public final static Class<? extends NodeObject> getSelectionClass(SelectType selectType, ListType listType) {
		// allow for no selection type to be set
		// this can be on purpose (i.e. selection type should only be determined when editing a tag in a page)
		// or if the tag has just been added to a template
		if (selectType == null) return null;

		switch (selectType) {
		case FOLDER:
			return Folder.class;
		case MANUAL:
			// allow for no listType setting
			if (listType == null) return null;
			
			switch (listType) {
			case FILE:
				return com.gentics.contentnode.object.File.class;
			case FOLDER:
				return Folder.class;
			case IMAGE:
				return ImageFile.class;
			case PAGE:
				return Page.class;
			default:
				return null;
			}
		default:
			return null;
		}
	}

	/**
	 * Get the select type for the given type ID
	 * @param type type ID
	 * @return select type
	 */
	public final static SelectType getSelectType(int type) {
		switch (type) {
		case com.gentics.contentnode.object.Overview.SELECTIONTYPE_FOLDER:
			return SelectType.FOLDER;

		case com.gentics.contentnode.object.Overview.SELECTIONTYPE_PARENT:
			return SelectType.AUTO;

		case com.gentics.contentnode.object.Overview.SELECTIONTYPE_SINGLE:
			return SelectType.MANUAL;

		default:
			return SelectType.UNDEFINED;
		}
	}

	/**
	 * Get the list type for the given object class
	 * @param objectClass object class
	 * @return list type
	 */
	public final static ListType getListType(Class<? extends NodeObject> objectClass) {
		if (objectClass == null) {
			return ListType.UNDEFINED;
		} else if (com.gentics.contentnode.object.Page.class.isAssignableFrom(objectClass)) {
			return ListType.PAGE;
		} else if (com.gentics.contentnode.object.Folder.class.isAssignableFrom(objectClass)) {
			return ListType.FOLDER;
		} else if (ImageFile.class.isAssignableFrom(objectClass)) {
			return ListType.IMAGE;
		} else if (File.class.isAssignableFrom(objectClass)) {
			return ListType.FILE;
		} else {
			return ListType.UNDEFINED;
		}
	}

	/**
	 * Get the order type from the given order kind
	 * @param orderKind order kind
	 * @return order by
	 */
	public final static OrderBy getOrderBy(int orderKind) {
		switch (orderKind) {
		case com.gentics.contentnode.object.Overview.ORDER_CDATE:
			return OrderBy.CDATE;

		case com.gentics.contentnode.object.Overview.ORDER_EDATE:
			return OrderBy.EDATE;

		case com.gentics.contentnode.object.Overview.ORDER_FILESIZE:
			return OrderBy.FILESIZE;

		case com.gentics.contentnode.object.Overview.ORDER_NAME:
			return OrderBy.ALPHABETICALLY;

		case com.gentics.contentnode.object.Overview.ORDER_PDATE:
			return OrderBy.PDATE;

		case com.gentics.contentnode.object.Overview.ORDER_PRIORITY:
			return OrderBy.PRIORITY;

		case com.gentics.contentnode.object.Overview.ORDER_SELECT:
			return OrderBy.SELF;

		default:
			return OrderBy.UNDEFINED;
		}
	}

	/**
	 * Get the order direction from the given order way
	 * @param orderWay order way
	 * @return order direction
	 */
	public final static OrderDirection getOrderDirection(int orderWay) {
		switch (orderWay) {
		case com.gentics.contentnode.object.Overview.ORDERWAY_ASC:
			return OrderDirection.ASC;

		case com.gentics.contentnode.object.Overview.ORDERWAY_DESC:
			return OrderDirection.DESC;

		default:
			return OrderDirection.ASC;
		}
	}

	protected Overview(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	/**
	 * Get the object selection type.
	 * @return the selection type as one of the SELECTIONTYPE constants.
	 */
	@FieldGetter("is_folder")
	public abstract int getSelectionType();

	/**
	 * Set the given selection type
	 * @param selectionType selection type
	 * @throws ReadOnlyException
	 */
	@FieldSetter("is_folder")
	public void setSelectionType(int selectionType) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the class of the objects which this overview contains.
	 * @return the class of the selected objects.
	 */
	public abstract Class<? extends NodeObject> getObjectClass();

	/**
	 * Set the class of objects which this overview contains
	 * @param clazz class of selected objects
	 * @throws ReadOnlyException
	 */
	public void setObjectClass(Class<? extends NodeObject> clazz) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the ttype of the objects shown in the overview
	 * @return ttype of the shown objects
	 */
	@FieldGetter("o_type")
	public abstract int getObjectType() throws NodeException;

	/**
	 * Set the ttype of the objects shown in the overview
	 * @param ttype ttype if the objects
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	@FieldSetter("o_type")
	public void setObjectType(int ttype) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the attrbute by which the elements should be sorted.
	 * @return the attribute type to sort the elements as one of the ORDER
	 *         constants.
	 */
	@FieldGetter("orderkind")
	public abstract int getOrderKind();

	/**
	 * Set the attribute, by which the elements should be sorted
	 * @param orderKind attribute type to sort as one of the ORDER constants
	 * @throws ReadOnlyException
	 */
	@FieldSetter("orderkind")
	public void setOrderKind(int orderKind) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the sort direction.
	 * @return the direction to sort the elements, as one of the ORDERBY
	 *         constants.
	 */
	@FieldGetter("orderway")
	public abstract int getOrderWay();

	/**
	 * Set the sort direction
	 * @param orderWay direction to sort the elements as one of the ORDERBY constants
	 * @throws ReadOnlyException
	 */
	@FieldSetter("orderway")
	public void setOrderWay(int orderWay) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * get the maximum number of elements to display.
	 * @return the maximum number of elements in this overview, or 0 if
	 *         unlimited.
	 */
	@FieldGetter("max_obj")
	public abstract int getMaxObjects();

	/**
	 * Set the maximum number of elements to display, 0 for unlimited
	 * @param maxObjects maximum number of elements to display, 0 for unlimited
	 * @throws ReadOnlyException
	 */
	@FieldSetter("max_obj")
	public void setMaxObjects(int maxObjects) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * check, if elements from sub-folders should be included.
	 * @return true, if the elements of sub-folders should also be retrieved.
	 */
	@FieldGetter("recursion")
	public abstract boolean doRecursion();

	/**
	 * Set the recursion flag
	 * @param recursion true if elements of sub-folders should alse be retrieved, false if not
	 * @throws ReadOnlyException
	 */
	@FieldSetter("recursion")
	public void setRecursion(boolean recursion) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the container tag which holds this overview. TODO check fallback if
	 * ds is defined in construct/template and not editable.
	 * @return the tag which holds this tag.
	 * @throws NodeException
	 */
	public abstract Tag getContainer() throws NodeException;

	/**
	 * Set the container tag
	 * @param container container tag
	 * @throws ReadOnlyException
	 */
	public void setContainer(Tag container) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Small hack: guess the value for this datasource.
	 * @return the value object of this ds, null if not found.
	 * @throws NodeException
	 */
	public abstract Value getValue() throws NodeException;

	/**
	 * Check whether the overview has sticky channel
	 * @return true for sticky channel
	 * @throws NodeException
	 */
	public boolean isStickyChannel() throws NodeException {
		Value value = getValue();
		if (value == null) {
			return false;
		}
		return ((OverviewPartType)value.getPartType()).isStickyChannel();
	}

	/**
	 * Get all selection entries of this overview in the correct order. Note
	 * when modifying this list (adding/removing/reordering), the objectorder of
	 * each overview entry must be set correctly.
	 * 
	 * @return all selected elements of this overview as {@link OverviewEntry}.
	 * @throws NodeException
	 */
	public abstract List<OverviewEntry> getOverviewEntries() throws NodeException;

	/**
	 * Get the template code for the entries of this overview.
	 * @return the template, stored in the value of this overview.
	 * @throws NodeException
	 */
	public String getTemplate() throws NodeException {
		return getTemplate(getValue());
	}

	/**
	 * get the template of an overview, using the given value object.
	 * @param value the value object where the template is stored.
	 * @return the template for the entries.
	 */
	public String getTemplate(Value value) throws NodeException {
		return getTemplate(value, null);
	}

	/**
	 * check, if the user can define a template in the code for this datasource.
	 * @return true, if the user can define a template in the code.
	 */
	public boolean hasCodeTemplate() throws NodeException {
		return hasCodeTemplate(getValue());
	}

	/**
	 * check, if the overview settings can be edited.
	 * @return true, if the overview settings are modifiable.
	 */
	public boolean isChangeable() throws NodeException {
		return isChangeable(getValue());
	}

	/**
	 * get the template code from a value, using a template to fallback to the
	 * templatetag value code.
	 * @param value the value where the code is stored.
	 * @param template the template which contains the matching templatetag.
	 * @return the template code for this overview.
	 */
	public String getTemplate(Value value, Template template) throws NodeException {

		String tpl;

		if (hasCodeTemplate(value)) {

			Value tplValue = getTemplateValue(template, value);

			// get template from template ;) fallback to value itself if
			// templatetag is not found.
			tpl = tplValue != null ? tplValue.getValueText() : value.getValueText();

		} else if (!isChangeable()) {
			tpl = "";
			Value defaultValue = value.getPart().getDefaultValue();

			if (defaultValue != null) {
				tpl = defaultValue.getValueText();
			}
			if ("".equals(tpl)) {
				Value tplValue = getTemplateValue(template, value);

				if (tplValue != null) {
					tpl = tplValue.getValueText();
				}
			}
		} else {

			tpl = value.getValueText();

			// get template from templatetag-value if not set for current obj.
			if ("".equals(tpl)) {
				Value tplValue = getTemplateValue(template, value);

				if (tplValue != null) {
					tpl = tplValue.getValueText();
				}
			}

			// get value from construct tpl-code not available
			if ("".equals(tpl)) {
				tpl = value.getPart().getDefaultValue().getValueText();
			}

		}

		return tpl;
	}

	/**
	 * check, if the overview settings can be edited.
	 * @param value the value where the overview values are stored.
	 * @return true, if the overview settings are modifiable.
	 */
	public boolean isChangeable(Value value) throws NodeException {
		NodePreferences prefs = getObjectInfo().getConfiguration().getDefaultPreferences();
		Value defaultValue = value.getPart().getDefaultValue();

		return prefs.getFeature("dssource") && (defaultValue != null && defaultValue.getInfo() > 0);
	}

	/**
	 * This method returns true if the overview if the SelectionType the
	 * OrderKind, the OrderWay, the ObjectClass and the OverviewEntries are
	 * undefined.
	 * 
	 * @return true if the Overview is yet undefined
	 * @throws NodeException
	 *             delegated
	 */
	public boolean isUndefined() throws NodeException {
		boolean isUndefined = true;

		isUndefined = getSelectionType() == SELECTIONTYPE_UNDEFINED 
				&& getOrderKind() == ORDER_UNDEFINED 
				&& getOrderWay() == ORDERWAY_UNDEFINED
				&& getObjectClass() == null 
				&& (getOverviewEntries() == null || getOverviewEntries().size() == 0);

		return isUndefined;

	}

	/**
	 * check, if the user can define a template in the code for this datasource.
	 * @param value the value where the overview values are stored.
	 * @return true, if the user can define a template in the code.
	 */
	public boolean hasCodeTemplate(Value value) throws NodeException {
		NodePreferences prefs = getObjectInfo().getConfiguration().getDefaultPreferences();
		Value defaultValue = value.getPart().getDefaultValue();
		String defaultTpl = defaultValue != null ? defaultValue.getValueText() : "";
		boolean hasDefaultTpl = defaultTpl != null && !"".equals(defaultTpl);

		return !prefs.getFeature("dssource") || (!isChangeable(value) && !hasDefaultTpl);
	}

	public List<? extends NodeObject> getSelectedObjects() throws NodeException {
		return getSelectedObjects(null, null);
	}

	/**
	 * get a list of all selected objects of this overview in the correct order.
	 * @param root the current root folder to use, or null if the folder of the
	 *        selected objects should be used.
	 * @param language get only objects in this contentlanguage, or null if all
	 *        objects should be returned.
	 * @return a list of all selected objects, with the class of
	 *         {@link #getObjectClass()}.
	 */
	public List<? extends NodeObject> getSelectedObjects(Folder root, ContentLanguage language) throws NodeException {

		NodePreferences prefs = getObjectInfo().getConfiguration().getDefaultPreferences();

		if (getObjectClass() != null){
			if (Page.class.isAssignableFrom(getObjectClass())) {
				return getMCPageSelection(root, language);
			}
			if (File.class.isAssignableFrom(getObjectClass()) && prefs.getFeature("dsfile")) {
				return getMCFileSelection(root, ImageFile.class.equals(getObjectClass()));
			}
			if (Folder.class.equals(getObjectClass()) && prefs.getFeature("ds_folder")) {
				return getMCFolderSelection(root);
			}
		}

		return Collections.emptyList();
	}
    
	/**
	 * Translates the template of a single item from an overview.
	 * @param renderResult
	 * @param object The object that should be rendered
	 * @param template The overview template
	 * @param nr Number of the item (this is important for resolving <node ds.nr>)
	 * @param totalCount Total number of items of the overview (important for <node ds.count>) 
	 * @return The rendered template of the item
	 * @throws NodeException
	 */
	public String translate(RenderResult renderResult, StackResolvable object, String template, int nr, int totalCount) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		StringBuffer code = new StringBuffer();

		TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

		// push the container of the tag as "container" onto the resolvable
		// stack
		StackResolvable container = null;

		try {
			container = new ContainerResolver(renderType.getTopmostTagContainer());
		} catch (Exception e) {// ignoring this exception since this will fail eg. when an overview
			// is rendered as a folder's object property. in this case the container
			// will remain null, which is not an issue since resolving the container
			// in a folder objprop does not make any sense at all.
		}
		if (container != null) {
			renderType.push(container);
		}

		DsResolver dsResolver = new DsResolver(container, getContainer(), totalCount);

		renderType.push((StackResolvable) dsResolver);

		try {
			renderType.push(object);
    
			dsResolver.setNr(nr);
    
			try {
				code.append(renderer.render(renderResult, template));
			} catch (Exception e) {
				renderResult.error("Overview", "Could not render object in overview, skipping", e.getMessage());
			} finally {
				renderType.pop((StackResolvable) object);
			}
		} finally {
			renderType.pop(dsResolver);
			if (container != null) {
				renderType.pop(container);
			}
		}

		return code.toString();
	}

	public String translate(RenderResult renderResult, List<? extends NodeObject> objects, String template) throws NodeException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		StringBuffer code = new StringBuffer();

		TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());

		// push the stop object onto the stack
		renderType.push(StackResolvable.STOP_OBJECT);

		// push the container of the tag as "container" onto the resolvable
		// stack
		StackResolvable container = null;

		try {
			container = new ContainerResolver(renderType.getTopmostTagContainer());
		} catch (Exception e) {// ignoring this exception since this will fail eg. when an overview
			// is rendered as a folder's object property. in this case the container
			// will remain null, which is not an issue since resolving the container
			// in a folder objprop does not make any sense at all.
		}
		if (container != null) {
			renderType.push(container);
		}

		DsResolver dsResolver = new DsResolver(container, getContainer(), objects.size());

		renderType.push((StackResolvable) dsResolver);

		// check the special url resolving feature
		boolean urlResolvingFeature = TransactionManager.getCurrentTransaction().getRenderType().getPreferences().getFeature(URL_RESOLVING_FEATURE);

		try {
			for (int i = 0; i < objects.size(); i++) {
				Object object = objects.get(i);

				if (object instanceof StackResolvable) {
					// check whether the special url resolving shall be used
					boolean useUrlResolver = urlResolvingFeature && (object instanceof Page);
					StackResolvable urlResolver = null;

					renderType.push((StackResolvable) object);

					if (useUrlResolver) {
						urlResolver = new UrlResolver((Page) object);
						renderType.push(urlResolver);
					}

					dsResolver.setNr(i + 1);

					try {
						code.append(renderer.render(renderResult, template));
					} catch (Exception e) {
						renderResult.error("Overview", "Could not render object in overview, skipping", e.getMessage());
					} finally {
						if (useUrlResolver) {
							renderType.pop(urlResolver);
						}
						renderType.pop((StackResolvable) object);
					}
				} else {
					getLogger().warn("Unhandled object " + object + " found in overview");
				}
			}
		} finally {
			renderType.pop(dsResolver);
			if (container != null) {
				renderType.pop(container);
			}
			renderType.pop(StackResolvable.STOP_OBJECT);
		}

		return code.toString();
	}

	/**
	 * Get the class of the overview entries (depending on the selection type)
	 * @return class of overview entries (may be null)
	 */
	public Class<? extends NodeObject> getEntriesClass() {
		switch (getSelectionType()) {
		case SELECTIONTYPE_FOLDER:
			return Folder.class;
		case SELECTIONTYPE_SINGLE:
			return getObjectClass();
		default:
			return null;
		}
	}

	private Value getTemplateValue(Template template, Value value) throws NodeException {

		if (template == null || value == null) {
			return null;
		}

		Value tplValue = null;

		ValueContainer container = value.getContainer();

		if (container instanceof Tag) {
			String name = ((Tag) container).getName();
			TemplateTag tplTag = template.getTemplateTag(name);

			if (tplTag == null) {
				return null;
			}

			ValueList values = tplTag.getValues();

			tplValue = values.getByPartId(value.getPartId());

			// not the same construct! get value of first ds-part
			if (tplValue == null) {
				final int partTypeId = value.getPart().getPartTypeId();

				for (Value v : values) {
					if (v.getPart().getPartTypeId() == partTypeId) {
						tplValue = v;
						break;
					}
				}
			}

		}

		return tplValue;
	}

	/**
	 * Add dependencies on the folders
	 * @param folders list of the folders
	 * @param recursive true when selecting objects recursively
	 * @param objectClass class of the selected objects
	 * @throws NodeException
	 */
	private void addDependencies(List<Folder> folders, boolean recursive, Class objectClass) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		Class depObjClass = objectClass;

		if (renderType.doHandleDependencies()) {
			String property = null;

			if (objectClass == Page.class) {
				property = Folder.PAGES_PROPERTY;
			} else if (objectClass == Folder.class) {
				property = Folder.FOLDERS_PROPERTY;
			} else if (objectClass == File.class) {
				property = Folder.FILES_PROPERTY;
			} else if (objectClass == ImageFile.class) {
				property = Folder.IMAGES_PROPERTY;
				// from the class, we do not distinguish between files and
				// images
				depObjClass = File.class;
			} else {
				return;
			}
			// add dependencies on all objects in the folders (orderby property)
			String orderedProperty = getOrderByProperty();

			for (Folder folder : folders) {
				Integer folderId = folder.getId();

				if (orderedProperty != null) {
					renderType.addDependency(new DependencyObject(Folder.class, folderId, depObjClass, null), orderedProperty);
				}
				// for recursive folder selection, we also have a dependency on
				// creation on "folders"
				if (recursive) {
					renderType.addDependency(new DependencyObject(Folder.class, folderId, null, null), Folder.FOLDERS_PROPERTY);

					// Add dependencies of child folders recursively
					addDependencies(folder.getChildFolders(), true, objectClass);
				}
				// and we have a dependency on the property
				renderType.addDependency(new DependencyObject(Folder.class, folderId, null, null), property);
			}
		}
	}

	/**
	 * Get the list of pages in the overview for multichannelling
	 * @param root root folder
	 * @param language current language (for language fallback)
	 * @return list of pages
	 * @throws NodeException
	 */
	private List<Page> getMCPageSelection(Folder root, ContentLanguage language) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean debugLog = logger.isDebugEnabled();
		if (debugLog) {
			logger.debug("Get page selection");
			if (t.usePublishablePages()) {
				logger.debug("Using PublishablePage instances");
			}
		}
		List<Page> pages = t.usePublishablePages() ? new LightWeightPageList(isStickyChannel()) : new ArrayList<Page>();
		NodePreferences prefs = getObjectInfo().getConfiguration().getDefaultPreferences();
		List<OverviewEntry> entries = getOverviewEntries();

		if (debugLog) {
			logger.debug("Found " + entries.size() + " entries");
		}

		PageSearch search = PageSearch.create();
		boolean updateLanguages = prefs.getFeature("contentgroup3") && language != null;

		// first get all selected pages (independent of their language)
		switch (getSelectionType()) {
		case SELECTIONTYPE_SINGLE:
			if (debugLog) {
				logger.debug("Selection type is 'single'");
			}
			// get exactly the selected pages (with MC fallback)
			pages.addAll(getSelectedNodeObjects(Page.class, entries));

			break;

		case SELECTIONTYPE_FOLDER:
			if (debugLog) {
				logger.debug("Selection type is 'folder'");
			}
			List<Folder> folders = getSelectedNodeObjects(Folder.class, entries);

			// if objects shall be fetched recursively, we remove folders that
			// are subfolders of other selected folders
			if (doRecursion()) {
				folders = Folder.reduceFolders(folders, ReductionType.PARENT);
			}

			// get the pages from the selected folders (with MC fallback)
			search.setRecursive(doRecursion()).setOnline(true);
			for (Folder folder : folders) {
				pages.addAll(folder.getPages(search));
			}

			addDependencies(folders, doRecursion(), Page.class);
			break;

		case SELECTIONTYPE_PARENT:
			if (debugLog) {
				logger.debug("Selection type is 'parent'");
			}

			// get the pages from the root folder (with MC fallback)
			search.setRecursive(doRecursion()).setOnline(true);
			if (root != null) {
				pages.addAll(root.getPages(search));
				addDependencies(Collections.singletonList(root), doRecursion(), Page.class);
			}
			break;

		default:
			return Collections.emptyList();
		}

		// now we filter the pages, according to the following criteria
		// 1. if no language is given, just add the page as it is (if its online)
		// 2. if a language is given, and the selected page has no language, only add it if feature "ds_empty_cs" is true (and page is online)
		// 3. if a language is given, and the selected page has this language and is online, add it
		// 4. otherwise, add all online language variants to the fallback list
		boolean dsFallback = prefs.getFeature("dsfallback");
		boolean emptyCS = prefs.getFeature("ds_empty_cs");

		if (debugLog) {
			logger.debug("Selected " + pages.size() + " pages");
			logger.debug("dsfallback: " + dsFallback);
			logger.debug("ds_empty_cs: " + emptyCS);
		}

		if (!updateLanguages) {
			// no language was given, just filter out the pages not online
			for (Iterator<Page> iterator = pages.iterator(); iterator.hasNext();) {
				Page page = iterator.next();

				if (!page.isOnline()) {
					if (debugLog) {
						logger.debug("Remove " + page + " (not online)");
					}
					iterator.remove();
				}
			}
		} else {
			if (debugLog) {
				logger.debug("Doing language fallback");
			}
			// when we need to check for languages, we will use the fallback list
			PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(language, root.getNode(), t.usePublishablePages(), isStickyChannel());

			for (Page page : pages) {
				boolean online = page.isOnline();

				if (page.getLanguage() == null) {
					// pages without language are only added, if ds_empty_cs is on and the page is online
					if (emptyCS && online) {
						if (debugLog) {
							logger.debug(page + " has no language, adding");
						}
						fallbackList.addPage(page);
					}
				} else if (page.getLanguage().equals(language) && online) {
					if (debugLog) {
						logger.debug(page + " has target language, adding");
					}
					// the page has the correct language and is online, so we add it
					fallbackList.addPage(page);
				} else if (dsFallback) {
					if (debugLog) {
						logger.debug("Doing language fallback for " + page);
					}
					// the page has the wrong language or is offline, but we shall do language fallback, so fetch all online language variants and add them
					Collection<Page> languageVariants = page.getLanguages();

					for (Page languageVariant : languageVariants) {
						if (languageVariant.isOnline()) {
							if (debugLog) {
								logger.debug("Adding lang variant " + languageVariant);
							}
							fallbackList.addPage(languageVariant);
						}
					}
				} else {
					// the selected page has wrong language or is offline and we shall not do language fallback.
					// get the correct language variant and add it, if it is online
					Page languageVariant = page.getLanguageVariant(language.getCode());

					if (languageVariant != null && languageVariant.isOnline()) {
						if (debugLog) {
							logger.debug("Add " + languageVariant + " instead of " + page);
						}
						fallbackList.addPage(languageVariant);
					}
				}
			}

			pages = fallbackList.getPages();
			if (debugLog) {
				logger.debug("After language fallback: " + pages.size() + " pages");
			}
		}

		// create page comparator for sorting
		PageComparator comp = null;
		String orderWay = getOrderWay() == ORDERWAY_DESC ? "desc" : "asc";

		switch (getOrderKind()) {
		case ORDER_NAME:
			comp = new PageComparator("name", orderWay);
			break;

		case ORDER_CDATE:
			comp = new PageComparator("customordefaultcdate", orderWay);
			break;

		case ORDER_EDATE:
			comp = new PageComparator("customordefaultedate", orderWay);
			break;

		case ORDER_PDATE:
			comp = new PageComparator("pdate", orderWay);
			break;

		case ORDER_FILESIZE:
			comp = new PageComparator("size", orderWay);
			break;

		case ORDER_PRIORITY:
			comp = new PageComparator("priority", orderWay);
			break;

		case ORDER_SELECT:
			break;

		default:
			comp = new PageComparator("name", orderWay);
			break;
		}

		// sort pages
		if (comp != null) {
			// disable publish cache while sorting. Otherwise, all PublishablePage objects would be loaded into memory
			// which could easily lead to OOM
			try (PublishCacheTrx trx = new PublishCacheTrx(false)) {
				Collections.sort(pages, comp);
			}
			if (debugLog) {
				logger.debug("Sorted pages");
			}
		}

		// restrict result list
		int maxObjects = getMaxObjects();

		if (maxObjects > 0 && !ObjectTransformer.isEmpty(pages)) {
			int origSize = pages.size();
			pages = pages.subList(0, Math.min(pages.size(), maxObjects));
			if (debugLog) {
				logger.debug("Reduced from " + origSize + " to " + pages.size() + " pages");
			}
		}

		return pages;
	}

	private String getOrderByProperty() {
		switch (getOrderKind()) {
		case ORDER_NAME:
			return "name";

		case ORDER_CDATE:
			return "cdate";

		case ORDER_EDATE:
			return "edate";

		case ORDER_PDATE:
			return "pdate";

		case ORDER_FILESIZE:
			return "filesize";

		case ORDER_PRIORITY:
			return "priority";

		default:
			return null;
		}
	}

	/**
	 * Get the list of files in the overview for multichannelling
	 * @param root root folder
	 * @param isImage true if images shall be fetched, false for files
	 * @return list of files
	 * @throws NodeException
	 */
	private List<File> getMCFileSelection(Folder root, boolean isImage) throws NodeException {
		List<File> files = new Vector<File>();
		List<OverviewEntry> entries = getOverviewEntries();
		FileSearch search = FileSearch.create();

		switch (getSelectionType()) {
		case SELECTIONTYPE_SINGLE:
			Class<? extends File> clazz = isImage ? ImageFile.class : File.class;
			files.addAll(getSelectedNodeObjects(clazz, entries));

			break;

		case SELECTIONTYPE_FOLDER:
			List<Folder> folders = getSelectedNodeObjects(Folder.class, entries);

			// if objects shall be fetched recursively, we remove folders that
			// are subfolders of other selected folders
			if (doRecursion()) {
				folders = Folder.reduceFolders(folders, ReductionType.PARENT);
			}

			// get the files from the selected folders (with MC fallback)
			search.setRecursive(doRecursion());
			for (Folder folder : folders) {
				if (isImage) {
					files.addAll(folder.getImages(search));
				} else {
					files.addAll(folder.getFiles(search));
				}
			}

			addDependencies(folders, doRecursion(), isImage ? ImageFile.class : File.class);
			break;

		case SELECTIONTYPE_PARENT:
			// get the files from the root folder (with MC fallback)
			search.setRecursive(doRecursion());
			if (root != null) {
				if (isImage) {
					files.addAll(root.getImages(search));
				} else {
					files.addAll(root.getFiles(search));
				}
				addDependencies(Collections.singletonList(root), doRecursion(), isImage ? ImageFile.class : File.class);
			}
			break;

		default:
			return Collections.emptyList();
		}

		// create file comparator for sorting
		FileComparator comp = null;
		String orderWay = getOrderWay() == ORDERWAY_DESC ? "desc" : "asc";

		switch (getOrderKind()) {
		case ORDER_NAME:
			comp = new FileComparator("name", orderWay);
			break;

		case ORDER_CDATE:
			comp = new FileComparator("customordefaultcdate", orderWay);
			break;

		case ORDER_EDATE:
			comp = new FileComparator("customordefaultedate", orderWay);
			break;

		case ORDER_FILESIZE:
			comp = new FileComparator("size", orderWay);
			break;

		case ORDER_SELECT:
			break;

		default:
			comp = new FileComparator("name", orderWay);
			break;
		}

		// sort files
		if (comp != null) {
			Collections.sort(files, comp);
		}

		// restrict result list
		int maxObjects = getMaxObjects();

		if (maxObjects > 0 && !ObjectTransformer.isEmpty(files)) {
			files = files.subList(0, Math.min(files.size(), maxObjects));
		}

		return files;
	}

	/**
	 * Recursive function to collect all children (and grand-children, ...) in the given collection
	 * @param f folder
	 * @param allChildren collection which will contain all children
	 * @throws NodeException
	 */
	private void getAllChildFolders(Folder f, List<Folder> allChildren) throws NodeException {
		List<Folder> children = new ArrayList<Folder>(f.getChildFolders());

		// first remove the children, that are already added to the given list
		children.removeAll(allChildren);
		// add the remaining children to the given list
		allChildren.addAll(children);
		for (Folder folder : children) {
			getAllChildFolders(folder, allChildren);
		}
	}

	/**
	 * Get the list of folders in the overview for multichannelling
	 * @param root root folder
	 * @return list of folders
	 * @throws NodeException
	 */
	private List<Folder> getMCFolderSelection(Folder root) throws NodeException {
		List<Folder> folders = new Vector<Folder>();
		List<OverviewEntry> entries = getOverviewEntries();

		switch (getSelectionType()) {
		case SELECTIONTYPE_SINGLE:
			folders.addAll(getSelectedNodeObjects(Folder.class, entries));

			if (doRecursion()) {
				List<Folder> containers = new ArrayList<Folder>(folders);
				for (Folder container : containers) {
					getAllChildFolders(container, folders);
				}

				addDependencies(containers, doRecursion(), Folder.class);
			}

			break;

		case SELECTIONTYPE_FOLDER:
			List<Folder> containers = getSelectedNodeObjects(Folder.class, entries);

			if (doRecursion()) {
				// if objects shall be fetched recursively, we remove folders that
				// are subfolders of other selected folders
				containers = Folder.reduceFolders(containers, ReductionType.PARENT);

				// now get all children of container folders
				for (Folder container : containers) {
					getAllChildFolders(container, folders);
				}
			} else {
				// now get all children of container folders
				for (Folder container : containers) {
					folders.addAll(container.getChildFolders());
				}
			}

			addDependencies(containers, doRecursion(), Folder.class);
			break;

		case SELECTIONTYPE_PARENT:
			// get the files from the root folder (with MC fallback)
			if (root != null) {
				if (doRecursion()) {
					getAllChildFolders(root, folders);
				} else {
					folders.addAll(root.getChildFolders());
				}
				addDependencies(Collections.singletonList(root), doRecursion(), Folder.class);
			}
			break;

		default:
			return Collections.emptyList();
		}

		// create folder comparator for sorting
		FolderComparator comp = null;
		String orderWay = getOrderWay() == ORDERWAY_DESC ? "desc" : "asc";

		switch (getOrderKind()) {
		case ORDER_NAME:
			comp = new FolderComparator("name", orderWay);
			break;

		case ORDER_CDATE:
			comp = new FolderComparator("cdate", orderWay);
			break;

		case ORDER_EDATE:
			comp = new FolderComparator("edate", orderWay);
			break;

		case ORDER_SELECT:
			break;

		default:
			comp = new FolderComparator("name", orderWay);
			break;
		}

		// sort folders
		if (comp != null) {
			Collections.sort(folders, comp);
		}

		// restrict result list
		int maxObjects = getMaxObjects();

		if (maxObjects > 0 && !ObjectTransformer.isEmpty(folders)) {
			folders = folders.subList(0, Math.min(folders.size(), maxObjects));
		}

		return folders;
	}

	/**
	 * Get selected node objects for the given entries. If the overview has sticky channel, the returned objects will be proxies
	 * @param clazz object class
	 * @param entries list of entries
	 * @return returned entries
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	private <T extends NodeObject> List<T> getSelectedNodeObjects(Class<T> clazz, List<OverviewEntry> entries) throws NodeException {
		if (isStickyChannel()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			RenderType renderType = t.getRenderType();
			boolean checkWastebin = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN) && renderType.doHandleDependencies();
			List<T> objects = new ArrayList<>();

			for (OverviewEntry entry : entries) {
				int nodeId = entry.getNodeId();
				Integer objectId = entry.getObjectId();
				try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
					T object = t.getObject(clazz, objectId);
					if (object == null && checkWastebin) {
						try (WastebinFilter filter = Wastebin.INCLUDE.set(); PublishCacheTrx pCacheTrx = new PublishCacheTrx(false)) {
							object = t.getObject(clazz, objectId);
							if (object != null && object.isDeleted()) {
								renderType.addDependency(object, "online");
							}
						}
					} else if (object != null) {
						objects.add((T) ChannelTrxInvocationHandler.wrap(nodeId, object));
					}
				}
			}

			return objects;
		} else {
			return getSelectedNodeObjectsFromIds(clazz, entries.stream().map(OverviewEntry::getObjectId).collect(Collectors.toList()));
		}
	}

	/**
	 * Get selected objects. If any of the selected objects are in the wastebin and dependencies need to be handled
	 * a dependency is added on every object in the wastebin.
	 * @param ids set of selected objects Ids
	 * @return list of selected objects
	 * @throws NodeException
	 */
	private <T extends NodeObject> List<T> getSelectedNodeObjectsFromIds(Class<T> clazz, List<Integer> ids) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();

		List<T> objects = t.getObjects(clazz, ids);

		// check which objects were not found and whether they are in the wastebin
		if (t.getNodeConfig().getDefaultPreferences().isFeature(Feature.WASTEBIN) && objects.size() != ids.size() && renderType.doHandleDependencies()) {
			try (WastebinFilter filter = Wastebin.INCLUDE.set(); PublishCacheTrx pCacheTrx = new PublishCacheTrx(false)) {
				for (NodeObject object : t.getObjects(clazz, ids)) {
					if (object.isDeleted()) {
						renderType.addDependency(object, "online");
					}
				}
			}
		}

		return objects;
	}

	private NodeLogger getLogger() {
		return NodeLogger.getNodeLogger(getClass());
	}

	/**
	 * Clone this overview from the given overview
	 * @param overview overview to clone
	 * @throws NodeException
	 */
	public void cloneFrom(Overview overview) throws NodeException {
		assertEditable();
		if (overview == null) {
			return;
		}
		setMaxObjects(overview.getMaxObjects());
		setObjectClass(overview.getObjectClass());
		setOrderKind(overview.getOrderKind());
		setOrderWay(overview.getOrderWay());
		setRecursion(overview.doRecursion());
		setSelectionType(overview.getSelectionType());

		List<OverviewEntry> origEntries = overview.getOverviewEntries();
		List<OverviewEntry> entries = getOverviewEntries();

		entries.clear();
		for (OverviewEntry origEntry : origEntries) {
			OverviewEntry newEntry = (OverviewEntry) origEntry.copy();

			newEntry.setOverview(this);
			entries.add(newEntry);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.object.NodeObject#getEffectiveUdate()
	 */
	public int getEffectiveUdate() throws NodeException {
		// get the overview's udate
		int udate = getUdate();
		// check the overview entries
		List<OverviewEntry> entries = getOverviewEntries();

		for (OverviewEntry entry : entries) {
			udate = Math.max(udate, entry.getEffectiveUdate());
		}
		return udate;
	}

	/**
	 * Check whether this overview has the same content as the other
	 * @param other other overview
	 * @return true iff the overview have the same content
	 * @throws NodeException
	 */
	public boolean hasSameContent(Overview other) throws NodeException {
		return Objects.equals(getMaxObjects(), other.getMaxObjects())
				&& Objects.equals(getObjectClass(), other.getObjectClass())
				&& Objects.equals(getOrderKind(), other.getOrderKind())
				&& Objects.equals(getOrderWay(), other.getOrderWay())
				&& Objects.equals(doRecursion(), other.doRecursion())
				&& Objects.equals(getSelectionType(), other.getSelectionType())
				&& MiscUtils.equals(getOverviewEntries(), other.getOverviewEntries(), (e1, e2) -> {
					return Objects.equals(e1.getObject(), e2.getObject())
							&& Objects.equals(e1.getNodeId(), e2.getNodeId());
				});
	}

	/**
	 * handles ds keywords which may be used inside overviews for ds.nr and
	 * ds.count
	 */
	protected class DsResolver implements StackResolvable, Resolvable {

		/**
		 * contains the current index
		 */
		protected int nr = 0;

		/**
		 * count of objects that will be cycled
		 */
		protected int count = 0;

		/**
		 * container reference
		 */
		protected StackResolvable container;

		/**
		 * reference to the overviewTag
		 */
		protected Tag overviewTag;

		/**
		 * resolver for various ds keywords
		 * @param container the page or template which contains this overview
		 * @param overviewTag reference to the overview tag itself, to be able
		 *        to resolve it's name
		 * @param count item count
		 */
		public DsResolver(StackResolvable container, Tag overviewTag, int count) {
			setContainer(container);
			setOverviewTag(overviewTag);
			setCount(count);
		}

		public String[] getStackKeywords() {
			return new String[] { "ds", getOverviewTag().getName()};
		}

		public Resolvable getKeywordResolvable(String keyword) throws NodeException {
			if ("ds".equals(keyword) || getOverviewTag().getName().equals(keyword)) {
				return this;
			}
			return null;
		}

		public Resolvable getShortcutResolvable() throws NodeException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getStackHashKey() {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getProperty(String key) {
			return get(key);
		}

		public Object get(String key) {
			if ("nr".equals(key)) {
				return new Integer(getNr());
			} else if ("count".equals(key)) {
				return new Integer(getCount());
			} else if ("container".equals(key)) {
				return getContainer();
			} else if ("tag".equals(key)) {
				return getOverviewTag();
			}
			return null;
		}

		public boolean canResolve() {
			return true;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public int getNr() {
			return nr;
		}

		public void setNr(int nr) {
			this.nr = nr;
		}

		public StackResolvable getContainer() {
			return container;
		}

		public void setContainer(StackResolvable container) {
			this.container = container;
		}

		public Tag getOverviewTag() {
			return overviewTag;
		}

		public void setOverviewTag(Tag overviewTag) {
			this.overviewTag = overviewTag;
		}
	}

	protected static class UrlResolver implements StackResolvable, Resolvable {
		protected Page page;

		protected final static String[] KEYWORDS = new String[0];

		/**
		 * Create an instance of the url resolver for the given paga
		 * @param page page for which the url can be resolved
		 */
		public UrlResolver(Page page) {
			this.page = page;
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
			return this;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.StackResolvable#getStackHashKey()
		 */
		public String getStackHashKey() {
			return null;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.StackResolvable#getStackKeywords()
		 */
		public String[] getStackKeywords() {
			return KEYWORDS;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			if ("url".equals(key)) {
				return page.get("url");
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
		 */
		public Object getProperty(String key) {
			return get(key);
		}
	}

	/**
	 * Inner helper class to store information necessary to correctly sort pages. It will store the id and contentSetId of a page. {@link #hashCode()} and
	 * {@link #equals(Object)} are overwritten to identify pages by either their channelSetId (if not 0) or their channelSetId (if not 0) or id.
	 */
	protected class PageSortInfo {

		/**
		 * ID of the page (language variant)
		 */
		protected int id;

		/**
		 * Contentset of the page (may be 0)
		 */
		protected int contentSetId;

		/**
		 * Channelset id of the page (may be 0)
		 */
		protected int channelSetId;

		/**
		 * Create an instance for a page
		 * @param page page
		 * @throws NodeException 
		 */
		public PageSortInfo(Page page) throws NodeException {
			this.id = ObjectTransformer.getInt(page.getId(), 0);
			this.contentSetId = ObjectTransformer.getInt(page.getContentsetId(), 0);
			this.channelSetId = ObjectTransformer.getInt(page.getChannelSetId(), 0);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PageSortInfo) {
				PageSortInfo psi = (PageSortInfo) obj;

				// check if a contentSetId was set
				if (contentSetId == 0) {
					if (channelSetId == 0) {
						// no contentSetId and no channelSetId set, so the page ids must be equal
						return id == psi.id;
					} else {
						return channelSetId == psi.channelSetId;
					}
				} else {
					// all pages of the same contentset are treated as equal
					return contentSetId == psi.contentSetId;
				}
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// we use the contentSetId as hashCode. This fulfills the contract of hash code, because equal pages have the same contentSetId.
			return contentSetId;
		}
	}
}
