package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionCategoryModel;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionModel;
import com.gentics.contentnode.etc.Consumer;

/**
 * Staged referenced object tag definition
 */
public class StagedReferencedObjectTagDefinition extends AbstractStagedReferencedObject<ObjectTagDefinition> {
	/**
	 * Create the wrapper
	 * @param wrapped wrapper
	 */
	public StagedReferencedObjectTagDefinition(ObjectTagDefinition wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		ObjectTagDefinitionModel stagedModel = ObjectTagDefinition.NODE2DEVTOOL_FEATURE_CHECK.apply(wrapped, new ObjectTagDefinitionModel(), false);

		stagedModel.setNodeIds(null);

		consumer.accept(removeI18nData(stagedModel));
	}

	/**
	 * Remove all i18n data from the model
	 * @param model model
	 * @return modified model
	 */
	protected ObjectTagDefinitionModel removeI18nData(ObjectTagDefinitionModel model) {
		model.setName(null);
		model.setDescription(null);
		model.setCategory(removeI18nData(model.getCategory()));
		return model;
	}

	/**
	 * Remove all i18n data from the model
	 * @param model model
	 * @return modified model
	 */
	protected ObjectTagDefinitionCategoryModel removeI18nData(ObjectTagDefinitionCategoryModel model) {
		if (model != null) {
			model.setName(null);
		}
		return model;
	}
}
