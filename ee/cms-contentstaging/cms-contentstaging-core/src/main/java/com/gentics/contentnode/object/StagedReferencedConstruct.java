package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.ConstructCategoryModel;
import com.gentics.contentnode.devtools.model.ConstructModel;
import com.gentics.contentnode.devtools.model.DefaultValueModel;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.rest.model.OverviewSetting;

/**
 * Staged referenced construct
 */
public class StagedReferencedConstruct extends AbstractStagedReferencedObject<Construct> {
	/**
	 * Create the wrapper
	 * @param wrapped wrapped construct
	 */
	public StagedReferencedConstruct(Construct wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		// model of the construct itself
		consumer.accept(removeI18nData(Construct.NODE2DEVTOOL.apply(wrapped, new ConstructModel())));

		// part default values
		for (Part part : wrapped.getParts()) {
			if (part.isValueless()) {
				continue;
			}
			Value value = part.getDefaultValue();
			if (value.getPartType() instanceof TextPartType) {
				consumer.accept(getStringForHashing(value.getValueText()));
			} else if (part.getPartTypeId() == Part.OVERVIEW) {
				OverviewPartSetting overviewSetting = new OverviewPartSetting(part);
				consumer.accept(OverviewPartSetting.NODE2REST.apply(overviewSetting, new OverviewSetting()));
			} else {
				consumer.accept(normalizeStrings(Value.NODE2DEVTOOL.apply(value, new DefaultValueModel())));
			}
		}
	}

	/**
	 * Remove all i18n data from the model
	 * @param model model
	 * @return modified model
	 */
	protected ConstructModel removeI18nData(ConstructModel model) {
		model.setName(null);
		model.setDescription(null);
		model.getParts().forEach(part -> {
			part.setName(null);
		});
		model.setCategory(removeI18nData(model.getCategory()));
		return model;
	}

	/**
	 * Remove all i18n data from the model
	 * @param model model
	 * @return modified model
	 */
	protected ConstructCategoryModel removeI18nData(ConstructCategoryModel model) {
		if (model != null) {
			model.setName(null);
			model.setSortOrder(0);
		}
		return model;
	}
}
