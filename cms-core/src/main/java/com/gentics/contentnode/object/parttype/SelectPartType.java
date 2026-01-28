/*
 * @author Stefan Hepp
 * @date 06.12.2005
 * @version $Id: SelectPartType.java,v 1.14.10.1 2011-03-08 12:28:11 norbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils.BatchUpdater;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.SelectOption;

/**
 * the select parttypes render select-boxes which uses ObjectSources for the selected values and rendering.
 */
public abstract class SelectPartType extends DatasourcePartType {
    
	protected HashMap<String, String> selection;

	/**
	 * this string is use to delimit various entries in a selection
	 */
	protected final static String SPLIT_KEY = "\\|\\-\\|"; 
    
	/**
	 * Create an instance of the parttype
	 * @param value value of the parttype
	 * @throws NodeException
	 */
	public SelectPartType(Value value) throws NodeException {
		super(value);
		readSelection(false);
	}

	/**
	 * Read the selection from the value object
	 * @param force to reload, even if read before
	 */
	protected synchronized void readSelection(boolean force) {
		if (force) {
			selection = null;
		}
		if (selection == null) {
			Value value = getValueObject();

			if (value != null && value.getValueText() != null && !value.getValueText().equals("")) {
				String selText = value.getValueText();
				String[] sel = selText.split(SPLIT_KEY);

				selection = new HashMap<String, String>(sel.length);
				for (int i = 0; i < sel.length; i++) {
					selection.put(sel[i], sel[i]);
				}
			} else {
				selection = new HashMap<String, String>(0);
			}
		}
	}

	/**
	 * Get the selection as collection of Strings
	 * @return selection
	 */
	public Collection<String> getStringSelection() {
		readSelection(false);
		return selection.values();
	}

	/*
	 *  (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#isSelected(com.gentics.contentnode.object.DatasourceEntry)
	 */
	public boolean isSelected(DatasourceEntry entry) {
		if (entry.getDsid() == -1) {
			if (selection.containsKey(entry.getValue())) {
				return true;
			}
		} else {
			if (selection.containsKey(ObjectTransformer.getString(new Integer(entry.getDsid()), ""))) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.TemplateRenderer#render(com.gentics.lib.render.RenderResult, java.lang.String)
	 */
	public String render(RenderResult result, String template) throws NodeException {
		return super.render(result, template);
	}
    
	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#getDatasourceId()
	 */
	public int getDatasourceId() throws NodeException {
		return getValueObject().getPart().getInfoInt();
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#getDatasource()
	 */
	public Datasource getDatasource() throws NodeException {
		// never get an editable version of the datasource here
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(Datasource.class, getDatasourceId());
}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#copyDatasource()
	 */
	protected void copy(DatasourcePartType original) throws NodeException {
		// do not copy the datasource
	}

	@Override
	public boolean preSave(BatchUpdater batchUpdater) throws NodeException {
		// Datasource is static and will not be saved with the Value
		return false;
	}

	@Override
	public boolean postSave(BatchUpdater batchUpdater) throws NodeException {
		// Datasource is static and will not be saved with the Value
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#delete()
	 */
	public void delete() throws NodeException {
		// datasource is static and will not be deleted
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.object.parttype.DatasourcePartType#dirtCache()
	 */
	public void dirtCache() throws NodeException {
		// datasource is static and will not be cleared from cache
	}

	@Override
	protected void fillProperty(Property property) throws NodeException {
		property.setDatasourceId(ObjectTransformer.getInteger(getDatasourceId(), null));

		Datasource datasource = getDatasource();
		if (datasource != null) {
			List<? extends DatasourceEntry> entries = datasource.getEntries();
			List<SelectOption> options = new Vector<SelectOption>(entries.size());
			List<SelectOption> selectedOptions = new Vector<SelectOption>();

			for (DatasourceEntry entry : entries) {
				SelectOption selectOption = DatasourceEntry.TRANSFORM2REST_SELECTOPTION.apply(entry);
				options.add(selectOption);

				if (isSelected(entry)) {
					selectedOptions.add(selectOption);
				}
			}

			property.setOptions(options);
			property.setSelectedOptions(selectedOptions);
		}
	}

	@Override
	public boolean hasSameContent(PartType other) throws NodeException {
		if (other instanceof SelectPartType) {
			SelectPartType otherSelectPT = (SelectPartType) other;
			return Objects.equals(getDatasourceId(), otherSelectPT.getDatasourceId())
					&& Objects.equals(getValueObject().getValueText(), other.getValueObject().getValueText());
		} else {
			return false;
		}
	}
}
