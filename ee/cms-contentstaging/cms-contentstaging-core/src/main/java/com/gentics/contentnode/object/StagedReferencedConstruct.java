package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.factory.object.OverviewPartSetting;
import com.gentics.contentnode.rest.model.OverviewSetting;

/**
 * Staged referenced construct
 */
public class StagedReferencedConstruct extends AbstractStagedReferencedObject<Construct> {
	/**
	 * Function to transform a part into the model, which is used for generation of the implementation hash
	 */
	public final static BiFunction<Part, StagedPartModel, StagedPartModel> PART2MODEL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setKeyword(from.getKeyname());
		to.setEditable(from.isEditable());
		to.setMlId(ObjectTransformer.getInt(from.getMlId(), 0));
		to.setTypeId(from.getPartTypeId());

		if (!from.isValueless()) {
			Value value = from.getDefaultValue();
			if (from.getPartTypeId() == Part.OVERVIEW) {
				OverviewPartSetting overviewSetting = new OverviewPartSetting(from);
				to.setOverviewSetting(OverviewPartSetting.NODE2REST.apply(overviewSetting, new OverviewSetting()));
			}
			to.setDefaultValue(VALUE2MODEL.apply(value, new StagedValueModel()));
		}

		return to;
	};

	/**
	 * Function to transform a construct into the model, which is used for generation of the implementation hash
	 */
	public final static BiFunction<Construct, StagedConstructModel, StagedConstructModel> CONSTRUCT2MODEL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setKeyword(from.getKeyword());

		unwrap(() -> {
			to.setParts(from.getParts().stream().map(part -> {
				return wrap(() -> PART2MODEL.apply(part, new StagedPartModel()));
			}).collect(Collectors.toList()));
		});

		return to;
	};

	/**
	 * Create the wrapper
	 * @param wrapped wrapped construct
	 */
	public StagedReferencedConstruct(Construct wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		consumer.accept(CONSTRUCT2MODEL.apply(wrapped, new StagedConstructModel()));
	}
}
