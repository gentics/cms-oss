package com.gentics.contentnode.factory.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.DatasourceEntry;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.parttype.MultiSelectPartType;
import com.gentics.contentnode.object.parttype.SelectPartType;
import com.gentics.contentnode.object.parttype.SingleSelectPartType;
import com.gentics.contentnode.rest.model.SelectOption;
import com.gentics.contentnode.rest.model.SelectSetting;

/**
 * Settings of Part of type {@link SelectPartType} or {@link MultiSelectPartType}
 */
public class SelectPartSetting {
	/**
	 * Consumer that transforms the node model into the given rest model
	 */
	public final static BiFunction<SelectPartSetting, SelectSetting, SelectSetting> NODE2REST = (
			nodeModel, restModel) -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		restModel.setDatasourceId(nodeModel.getDatasourceId());
		restModel.setTemplate(nodeModel.getTemplate());
		Datasource datasource = t.getObject(Datasource.class, nodeModel.getDatasourceId());
		if (datasource != null) {
			List<SelectOption> options = new ArrayList<>();
			for (DatasourceEntry entry : datasource.getEntries()) {
				options.add(DatasourceEntry.TRANSFORM2REST_SELECTOPTION.apply(entry));
			}
			restModel.setOptions(options);
		}
		return restModel;
	};

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<SelectPartSetting, SelectSetting> TRANSFORM2REST = nodeModel -> {
		return NODE2REST.apply(nodeModel, new SelectSetting());
	};

	/**
	 * Function to transform the rest model into the node model
	 */
	public final static BiFunction<SelectSetting, SelectPartSetting, SelectPartSetting> REST2NODE = (from, to) -> {
		if (from.getDatasourceId() != 0) {
			to.setDatasourceId(from.getDatasourceId());
		}
		if (from.getTemplate() != null) {
			to.setTemplate(from.getTemplate());
		}
		return to;
	};

	/**
	 * Supported part types
	 */
	protected final static List<Integer> TYPES = Arrays.asList(SingleSelectPartType.TYPE_ID, MultiSelectPartType.TYPE_ID);

	/**
	 * Datasource ID
	 */
	protected int datasourceId;

	/**
	 * Datasource template
	 */
	protected String template;

	/**
	 * Create empty instance
	 */
	public SelectPartSetting() {
	}

	/**
	 * Check whether the part is a select part
	 * @param part part to check
	 * @return true for select parts
	 */
	public static boolean isSelectPart(Part part) {
		return TYPES.contains(part.getPartTypeId());
	}

	/**
	 * Create settings for given part
	 * @param part part
	 * @throws NodeException
	 */
	public SelectPartSetting(Part part) throws NodeException {
		assertPart(part);
		datasourceId = part.getInfoInt();
		template = part.getInfoText();
	}

	/**
	 * Get datasource ID
	 * @return datasource ID
	 */
	public int getDatasourceId() {
		return datasourceId;
	}

	/**
	 * Set datasource ID
	 * @param datasourceId datasource ID
	 */
	public void setDatasourceId(int datasourceId) {
		this.datasourceId = datasourceId;
	}

	/**
	 * Get template
	 * @return template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Set template
	 * @param template template
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Set the select part settings into the given part
	 * @param part
	 * @throws NodeException
	 */
	public void setTo(Part part) throws NodeException {
		assertPart(part);

		part.setInfoInt(getDatasourceId());
		part.setInfoText(getTemplate());
	}

	/**
	 * Assert that the part is not null and has the correct type (select)
	 * @param part part
	 * @throws NodeException
	 */
	protected void assertPart(Part part) throws NodeException {
		if (part == null) {
			throw new NodeException("Cannot handle select settings for null part");
		}
		if (!isSelectPart(part)) {
			throw new NodeException("Cannot handle select settings for part of type " + part.getPartTypeId() + ". Type must be one of " + TYPES);
		}
	}
}
