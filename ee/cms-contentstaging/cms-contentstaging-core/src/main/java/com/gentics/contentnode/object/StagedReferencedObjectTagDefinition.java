package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.ObjectTagDefinitionTypeModel;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;

/**
 * Staged referenced object tag definition
 */
public class StagedReferencedObjectTagDefinition extends AbstractStagedReferencedObject<ObjectTagDefinition> {
	/**
	 * Function that transforms na object tag definition into the model, which is used to calculate the implementation hash
	 */
	public final static BiFunction<ObjectTagDefinition, StagedObjectTagDefinitionModel, StagedObjectTagDefinitionModel> DEF2MODEL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setType(ObjectTagDefinitionTypeModel.fromValue(from.getObjectTag().getObjType()));
		to.setKeyword(from.getObjectTag().getName());
		to.setConstructId(from.getObjectTag().getConstruct().getGlobalId().toString());

		return to;
	};

	/**
	 * Create the wrapper
	 * @param wrapped wrapper
	 */
	public StagedReferencedObjectTagDefinition(ObjectTagDefinition wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		consumer.accept(DEF2MODEL.apply(wrapped, new StagedObjectTagDefinitionModel()));
	}
}
