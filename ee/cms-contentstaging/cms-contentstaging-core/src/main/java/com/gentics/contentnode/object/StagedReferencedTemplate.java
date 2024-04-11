package com.gentics.contentnode.object;

import static com.gentics.contentnode.rest.util.MiscUtils.unwrap;
import static com.gentics.contentnode.rest.util.MiscUtils.wrap;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;

/**
 * Staged referenced template
 */
public class StagedReferencedTemplate extends AbstractStagedReferencedObject<Template> {
	/**
	 * Function that transforms a template tag into the model, which is used to calculate the implementation hash
	 */
	public final static BiFunction<TemplateTag, StagedTemplateTagModel, StagedTemplateTagModel> TEMPLATETAG2MODEL = (from, to) -> {
		TemplateTag.NODE2DEVTOOL.apply(from, to);

		unwrap(() -> {
			to.setValues(from.getValues().stream().map(value -> {
				return wrap(() -> VALUE2MODEL.apply(value, new StagedValueModel()));
			}).collect(Collectors.toList()));
		});
		return to;
	};

	/**
	 * Function that transforms a template into the model, which is used to calculate the implementation hash
	 */
	public final static BiFunction<Template, StagedTemplateModel, StagedTemplateModel> TEMPLATE2MODEL = (from, to) -> {
		to.setGlobalId(from.getGlobalId().toString());
		to.setType(from.getMarkupLanguage().getName());
		to.setSource(getStringForHashing(from.getSource()));

		unwrap(() -> {
			Map<String, TemplateTag> orderedTemplateTags = new TreeMap<>(from.getTemplateTags());
			to.setTemplateTags(orderedTemplateTags.values().stream().map(templateTag -> {
				return wrap(() -> TEMPLATETAG2MODEL.apply(templateTag, new StagedTemplateTagModel()));
			}).collect(Collectors.toList()));
		});

		return to;
	};

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
		consumer.accept(TEMPLATE2MODEL.apply(wrapped, new StagedTemplateModel()));
	}
}
