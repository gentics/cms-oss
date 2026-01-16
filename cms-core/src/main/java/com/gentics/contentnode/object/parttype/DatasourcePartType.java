/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: DatasourcePartType.java,v 1.16 2010-09-28 17:01:29 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.parser.tag.ContentTagRenderer;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.render.TemplateRenderer;
import com.gentics.contentnode.resolving.ResolvableGetter;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Property.Type;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.lib.etc.StringUtils;

/**
 * PartType 32 - Datasource
 * <p>
 * The datasource parttype uses the ObjectSource factory to get objectsources and uses
 * them to render a template.
 * </p>
 * <p>
 * Also serves as base class for the derived PartTypes 29 and 30
 * </p>
 */
public class DatasourcePartType extends AbstractPartType {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -495550913389775496L;

	/**
	 * this one is used to register the renderer at the RendererFactory
	 */
	private static final String RENDERER_TYPE = "tagds";
    
	/**
	 * keyname for the ContentTagRenderer
	 */
	private static final String CONTENT_TAG_RENDERER_KEYNAME = "ds"; 

	/**
	 * register our ds ContentTagRenderer at the RendererFactory
	 */
	static {
		RendererFactory.registerRenderer(RENDERER_TYPE, new ContentTagRenderer(CONTENT_TAG_RENDERER_KEYNAME));
	}

	/**
	 * Editable version of the datasource, if the parttype instance belongs to an editable value
	 */
	private Datasource editableDatasource;

	/**
	 * Create instance of this parttype
	 * @param value value
	 * @throws NodeException
	 */
	public DatasourcePartType(Value value) throws NodeException {
		super(value);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#isEmpty()
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
    
	/**
	 * Get the datasource id
	 * @return datasource id
	 * @throws NodeException
	 */
	public int getDatasourceId() throws NodeException {
		return getValueObject().getValueRef();
	}

	/**
	 * Get the datasource object or null. If the value object is editable, the datasource will be editable as well.
	 * @return datasource object
	 * @throws NodeException
	 */
	public Datasource getDatasource() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		int datasourceId = getDatasourceId();
		NodeObjectInfo info = getValueObject().getObjectInfo();

		if (datasourceId > 0) {
			if (info.isEditable()) {
				if (editableDatasource == null) {
					editableDatasource = t.getObject(Datasource.class, datasourceId, true);
				}
				return editableDatasource;
			} else {
				return t.getObject(Datasource.class, datasourceId, info.getVersionTimestamp());
			}
		} else if (info.isEditable()) {
			if (editableDatasource == null) {
				editableDatasource = t.createObject(Datasource.class);
				editableDatasource.setSourceType(SourceType.staticDS);
			}
			return editableDatasource;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.PartType#hasTemplate()
	 */
	public boolean hasTemplate() throws NodeException {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult renderResult, String template) throws NodeException {
		super.render(renderResult, template);
		List<DatasourceEntry> entries = getItems();
		StringBuffer code = new StringBuffer();
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		TemplateRenderer renderer = RendererFactory.getRenderer(renderType.getDefaultRenderer());
		TemplateRenderer dsRenderer = RendererFactory.getRenderer("tagds");
        
		if (StringUtils.isEmpty(template)) {
			template = getTemplate();
		}
        
		String codePart;
		int nr = 1;

		for (DatasourceEntry entry : entries) {
			if (!isSelected(entry)) {
				continue;
			}
			// it is important to make a selection copy of the entry here (otherwise the nr would not be correct)
			renderType.push(entry.getSelectionCopy(nr++));

			try {
				codePart = dsRenderer.render(renderResult, template);
			} finally {
				renderType.pop();
			}
			code.append(renderer.render(renderResult, codePart));
		}
		return code.toString();
	}

	/**
	 * Get all items selectable from the datasource
	 * @return list of all items
	 * @throws NodeException
	 */
	@ResolvableGetter
	public List<DatasourceEntry> getItems() throws NodeException {
		Datasource datasource = getDatasource();

		if (datasource != null) {
			return datasource.getEntries();
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Get selected items
	 * @return list of selected items
	 * @throws NodeException
	 */
	@ResolvableGetter
	public List<DatasourceEntry> getSelection() throws NodeException {
		List<DatasourceEntry> items = getItems();
		List<DatasourceEntry> selection = new ArrayList<DatasourceEntry>(items.size());
		int nr = 1;

		for (DatasourceEntry entry : items) {
			if (!isSelected(entry)) {
				continue;
			}
			// it is important to make a selection copy of the entry here
			selection.add(entry.getSelectionCopy(nr++));
		}

		return selection;
	}

	/**
	 * Get selected values
	 * @return list of selected values
	 * @throws NodeException
	 */
	@ResolvableGetter
	public List<String> getValues() throws NodeException {
		List<DatasourceEntry> selection = getSelection();
		List<String> values = new ArrayList<String>();

		for (DatasourceEntry entry : selection) {
			values.add(entry.getValue());
		}

		return values;
	}

	/**
	 * Get selected values
	 * @return list of selected values
	 * @throws NodeException
	 */
	@ResolvableGetter
	public List<String> getKeys() throws NodeException {
		List<DatasourceEntry> selection = getSelection();
		List<String> keys = new ArrayList<String>();

		for (DatasourceEntry entry : selection) {
			keys.add(entry.getKey());
		}

		return keys;
	}

	/**
	 * Get the value of the first selected object or empty string if nothing selected
	 * @return value of the first selected object
	 * @throws NodeException
	 */
	@ResolvableGetter
	public String getValue() throws NodeException {
		List<DatasourceEntry> selection = getSelection();

		if (selection.size() > 0) {
			return selection.get(0).getValue();
		} else {
			return "";
		}
	}

	/**
	 * Get the key of the first selected object or empty string if nothing selected
	 * @return key of the first selected object
	 * @throws NodeException
	 */
	@ResolvableGetter
	public String getKey() throws NodeException {
		List<DatasourceEntry> selection = getSelection();

		if (selection.size() > 0) {
			return selection.get(0).getKey();
		} else {
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		Object value = super.get(key);

		if (value == null) {
			// check whether the key is actually a (positive) number
			int number = ObjectTransformer.getInt(key, -1);

			if (number > 0) {
				try {
					List<DatasourceEntry> selection = getSelection();

					if (selection.size() >= number) {
						return selection.get(number - 1);
					}
				} catch (NodeException e) {
					logger.error("Error while resolving {" + key + "} for {" + this + "}", e);
				}
			}
		}

		return value;
	}

	/**
	 * retrieve the datasource template
	 * @return the template
	 * @throws NodeException
	 */
	public String getTemplate() throws NodeException {
		String template = getValueObject().getPart().getInfoText();

		return StringUtils.isEmpty(template) ? "<ds value>" : template;
	}

	/**
	 * check if a specific DatasourceEntry is selected
	 * @param entry the entry to be checked for
	 * @return true if it is in selection, false otherwise
	 */
	public boolean isSelected(DatasourceEntry entry) {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#copyFrom(com.gentics.contentnode.object.parttype.PartType)
	 */
	public <T extends PartType> void copyFrom(T original) throws ReadOnlyException, NodeException {
		super.copyFrom(original);
		copy((DatasourcePartType)original);
	}

	/**
	 * Copy the datasource (if the datasource belongs to the value exclusively)
	 * @param original original parttype
	 * @throws NodeException
	 */
	protected void copy(DatasourcePartType original) throws NodeException {
		if (getDatasourceId() == original.getDatasourceId()) {
			// we want tp generate a new datasource, so first set the reference to 0
			getValueObject().setValueRef(0);
		}
		Datasource thisDatasource = getDatasource();
		Datasource originalDatasource = original.getDatasource();
		if (originalDatasource != null) {
			thisDatasource.copyFrom(originalDatasource);
		}
	}

	@Override
	public boolean preSave(BatchUpdater batchUpdater) throws NodeException {
		Datasource datasource = getDatasource();
		boolean modified = false;
		if (datasource != null) {
			modified |= datasource.saveBatch(batchUpdater);

			getValueObject().setValueRef(ObjectTransformer.getInt(datasource.getId(), 0));
		}

		return modified;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#delete()
	 */
	public void delete() throws NodeException {
		Datasource datasource = getDatasource();
		if (datasource != null) {
			datasource.delete();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.AbstractPartType#dirtCache()
	 */
	public void dirtCache() throws NodeException {
		int datasourceId = getDatasourceId();
		if (datasourceId > 0) {
			TransactionManager.getCurrentTransaction().dirtObjectCache(Datasource.class, datasourceId);
		}
	}

	@Override
	public Type getPropertyType() {
		return Type.DATASOURCE;
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setDatasourceId(ObjectTransformer.getInteger(getDatasourceId(), null));

		Datasource datasource = getDatasource();
		if (datasource != null) {
			List<? extends DatasourceEntry> entries = datasource.getEntries();
			List<SelectOption> options = new Vector<SelectOption>(entries.size());

			for (DatasourceEntry entry : entries) {
				SelectOption selectOption = new SelectOption();

				selectOption.setId(entry.getDsid());
				selectOption.setKey(entry.getKey());
				selectOption.setValue(entry.getValue());
				options.add(selectOption);
			}

			property.setOptions(options);
		}
	}

	@Override
	public void fromProperty(Property property) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<DatasourceEntry> items = getItems();
		Map<Integer, DatasourceEntry> itemMap = items.stream().collect(Collectors.toMap(entry -> entry.getDsid(), Function.identity()));
		AtomicInteger maxDsId = new AtomicInteger(items.stream().mapToInt(DatasourceEntry::getDsid).max().orElse(0));
		items.clear();

		if (property.getOptions() != null) {
			for (SelectOption option : property.getOptions()) {
				Integer dsId = option.getId();
				DatasourceEntry item = null;

				// either find existing item or create new one
				if (dsId != null && itemMap.containsKey(dsId)) {
					item = itemMap.get(dsId);
				} else {
					item = t.createObject(DatasourceEntry.class);
					item.setDsid(maxDsId.incrementAndGet());
				}
				item.setKey(option.getKey());
				item.setValue(option.getValue());

				items.add(item);
			}
		}
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof DatasourcePartType && !(other instanceof SelectPartType)) {
			return getDatasource().hasSameContent(((DatasourcePartType)other).getDatasource());
		} else {
			return false;
		}
	}
}
