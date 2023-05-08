package com.gentics.contentnode.object;

import java.util.Map;
import java.util.TreeMap;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.DefaultValueModel;
import com.gentics.contentnode.devtools.model.TemplateModel;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.object.parttype.TextPartType;

/**
 * Staged referenced template
 */
public class StagedReferencedTemplate extends AbstractStagedReferencedObject<Template> {
	/**
	 * Create the wrapper
	 * @param wrapped template
	 */
	public StagedReferencedTemplate(Template wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		// template model
		consumer.accept(Template.NODE2DEVTOOL.apply(wrapped, new TemplateModel()));
		// template source
		consumer.accept(getStringForHashing(wrapped.getSource()));
		// tag values (ordered by name)
		Map<String, TemplateTag> tags = new TreeMap<>(wrapped.getTags());
		for (TemplateTag tag : tags.values()) {
			for (Value value : tag.getValues()) {
				if (value.getPartType() instanceof TextPartType) {
					consumer.accept(getStringForHashing(value.getValueText()));
				} else {
					consumer.accept(normalizeStrings(Value.NODE2DEVTOOL.apply(value, new DefaultValueModel())));
				}
			}
		}
	}
}
