package com.gentics.contentnode.forms;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.BiConsumer;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.FormDataUtils;
import com.gentics.contentnode.etc.Operator;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.factory.AnyChannelTrx;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher.UrlEscapeUtils;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.ElementConfig;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.formtype.StringSetting;
import com.gentics.contentnode.rest.model.formtype.TranslationSetting;
import com.gentics.lib.log.NodeLogger;

/**
 * Instances of this class will transform the data of a given form
 */
public class FormTransformer {
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(FormTransformer.class);

	/**
	 * Dummy ReferenceSetting for transforming the "successUrl" node/page reference
	 */
	protected final static ReferenceSetting SUCCESS_PAGE_REFERENCE = new ReferenceSetting("successUrl",
			Collections.emptyMap(), Collections.singleton(ReferenceType.page), PublishAsType.url);

	/**
	 * Dummy ReferenceSetting for transforming the "adminEmailSource" node/page reference
	 */
	protected final static ReferenceSetting ADMIN_MAIL_SOURCE_REFERENCE = new ReferenceSetting("adminEmailSource",
			Collections.emptyMap(), Collections.singleton(ReferenceType.page), PublishAsType.content);

	/**
	 * Transformation target, which defines how the references should be transformed
	 */
	protected TransformationTarget transformationTarget = TransformationTarget.publish;

	/**
	 * Callback, which will be called when unknown references are handled
	 */
	protected Operator unknownReferenceCallback = () -> {};

	/**
	 * Set the transformation target
	 * @param transformationTarget target
	 * @return fluent API
	 */
	public FormTransformer withTransformationTarget(TransformationTarget transformationTarget) {
		this.transformationTarget = transformationTarget;
		return this;
	}

	/**
	 * Set the callback for unknown references
	 * @param unknownReferenceCallback callback
	 * @return fluent API
	 */
	public FormTransformer withUnknownReferenceCallback(Operator unknownReferenceCallback) {
		this.unknownReferenceCallback = unknownReferenceCallback;
		return this;
	}

	/**
	 * Transform the given form to the JSON object
	 * @param form form
	 * @return JSON object
	 * @throws NodeException
	 */
	public ObjectNode transform(Form form) throws NodeException {
		return transform(form, form.getData().deepCopy());
	}

	/**
	 * Transform the given data for the form to the JSON object
	 * @param form form
	 * @param data form data to transform
	 * @return JSON object
	 * @throws NodeException
	 */
	public ObjectNode transform(Form form, ObjectNode data) throws NodeException {
		String name = I18NHelper.getName(form);
		FormTypeConfiguration typeConfig = Optional.ofNullable(FormConfigProvider.getConfig()).map(config -> config.get(form.getFormType())).orElseThrow(
				() -> new NodeException("Error while transforming form %s: Could not find configuration for form type %s"
						.formatted(name, form.getFormType())));

		List<String> languages = form.getLanguages();

		Transaction t = TransactionManager.getCurrentTransaction();
		RenderType renderType = t.getRenderType();
		boolean handleDependencies = renderType != null ? renderType.doHandleDependencies() : false;
		boolean storeDependencies = renderType != null ? renderType.isStoreDependencies() : false;

		try (PropertyTrx linkWayTrx = new PropertyTrx("contentnode.linkway", "host");
				PropertyTrx fileLinkWayTrx = new PropertyTrx("contentnode.linkway_file", "host");
				PublishCacheTrx pcTrx = new PublishCacheTrx(false);
				RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, form, handleDependencies, false, false)) {
			// for the StaticUrlFactory, forbid auto detection of the linkway
			RenderUrlFactory renderUrlFactory = rTrx.get().getRenderUrlFactory();
			if (renderUrlFactory instanceof StaticUrlFactory) {
				((StaticUrlFactory) renderUrlFactory).setAllowAutoDetection(false);
			}

			FormDataUtils.handleElements(typeConfig, data, (element, elementType, elementConfig) -> {
				transformReferences(typeConfig, elementConfig, element, languages);
				handleLinks(typeConfig, elementConfig, element, languages);
			}, null);

			if (!typeConfig.external) {
				transformPageReference(data, "successPageId", "successNodeId", (n, p) -> {
					if (p != null) {
						transformationTarget.transform(SUCCESS_PAGE_REFERENCE, data, languages, n, p,
								Optional.of(Pair.of("successPageId", "successNodeId")));
					}
				});
				transformPageReference(data, "adminEmailPageId", "adminEmailNodeId", (n, p) -> {
					if (p != null) {
						transformationTarget.transform(ADMIN_MAIL_SOURCE_REFERENCE, data, languages, n,
								p, Optional.of(Pair.of("adminEmailPageId", "adminEmailNodeId")));
					}
				});
			}

			transformationTarget.transform(typeConfig, form, data);

			// take over the collected dependencies
			if (handleDependencies && storeDependencies) {
				for (Dependency dep : rTrx.get().getDependencies()) {
					renderType.addDependency(dep.getSource(), dep.getSourceProperty());
				}
			}
		}

		return data;
	}

	/**
	 * Merge new UI-Schema with old FormgridSchema. Old Logic from customtool.
	 *
	 * @param config form type configuration
	 * @param oldUISchema old ui schema, sent by the external system the last time
	 * @param newUISchema new ui schema, sent by the external system
	 * @param oldFormGridSchema old ui schema, modified by formgrid
	 * @return mergedUI-Schema
	 */
	public ObjectNode mergeUiSchemaAndFormGrid(FormTypeConfiguration config, ObjectNode oldUISchema,
			ObjectNode newUISchema, ObjectNode oldFormGridSchema) {
		logger.debug("mergeUiSchemaAndFormGrid");

		if (oldFormGridSchema == null) {
			return null;
		}

		if (logger.isEnabled(Level.TRACE)) {
			logger.log(Level.TRACE, "oldUISchema: %s".formatted(oldUISchema.toPrettyString()));
			logger.log(Level.TRACE, "newUISchema: %s".formatted(newUISchema.toPrettyString()));
			logger.log(Level.TRACE, "oldFormGridSchema: %s".formatted(oldFormGridSchema.toPrettyString()));
		}

		ArrayNode newFormGridPages = newUISchema.arrayNode();
		ArrayNode newUISchemaPages = newUISchema.withArrayProperty("pages");

		ArrayNode oldFormGridPages = oldFormGridSchema.withArrayProperty("pages");
		ArrayNode newFormGridElements = newUISchema.arrayNode();

		int index = 0;
		for (JsonNode page : oldFormGridPages) {
			ArrayNode formGridPageElements = page.withArrayProperty("elements");

			for (JsonNode element : formGridPageElements) {
				String elementId = element.path("id").asText();
				JsonNode oldElement = oldUISchema.withArrayProperty("pages").valueStream()
						.flatMap(p -> p.withArrayProperty("elements").valueStream())
						.filter(e -> Strings.CI.equals(e.path("id").asText(), elementId)).findFirst().orElse(null);

				ObjectNode mergedElement = getMergedElement(config, (ObjectNode) oldElement, (ObjectNode) element,
						newUISchemaPages, index);
				if (mergedElement != null) {
					newFormGridElements.add(mergedElement);
				}
			}
			index++;
		}
		// set up the pages according to the uiSchema
		for (JsonNode uiSchemaPage : newUISchemaPages) {
			ObjectNode newPage = newFormGridPages.objectNode();
			newPage.set("pagename", uiSchemaPage.get("pagename"));
			newPage.withArrayProperty("elements");
			newFormGridPages.add(newPage);
		}
		// add the elements to the pages
		for (JsonNode element : newFormGridElements) {
			int uiSchemaPage = element.optional("uiSchemaPage").filter(JsonNode::isIntegralNumber).map(JsonNode::asInt)
					.orElse(0);
			if (uiSchemaPage <= newUISchema.withArrayProperty("pages").size()) {
				newFormGridPages.get(uiSchemaPage).withArrayProperty("elements").add(element);
			}
		}
		// add Elements that are only present in uiSchema
		// add Elements at the end of the page
		// if there are fewer pages in formgrid than add to the last page
		index = 0;
		for (JsonNode uiPage : newUISchemaPages) {
			ArrayNode uiPageElements = uiPage.withArrayProperty("elements");
			for (JsonNode uiElement : uiPageElements) {
				ObjectNode foundElement = searchFormGridElement(uiElement.path("id").asText(), newFormGridPages);
				if (foundElement == null || !foundElement.hasNonNull("element")) {
					if (newFormGridPages.get(index) != null) {
						newFormGridPages.get(index).withArrayProperty("elements").add(uiElement);
					} else {
						newFormGridPages.get(newFormGridPages.size() - 1).withArrayProperty("elements").add(uiElement);
					}
				}
			}
			index++;
		}

		newUISchema.set("pages", newFormGridPages);

		// take all values from old-FormGridSchema
		// except a blacklist, containing already merged things (pages) or attributes
		// which should be taken from ActaNova
		List<String> mergeBlackList = Arrays.asList("key", "version", "formname", "formdescription", "pages");

		oldFormGridSchema.forEachEntry((key, value) -> {
			// Your filter condition
			if (!mergeBlackList.contains(key) && value != null) {
				newUISchema.set(key, value);
			}
		});

		if (logger.isEnabled(Level.TRACE)) {
			logger.log(Level.TRACE, "Result UI-Schema: %s".formatted(newUISchema.toPrettyString()));
		}
		return newUISchema;
	}

	/**
	 * Convert the given languages list by transforming internal language codes to external codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param languages languages list (will be modified)
	 * @return transformed languages
	 */
	public static List<String> transformToExternalLanguageCodes(FormTypeConfiguration config, List<String> languages) {
		if (MapUtils.isNotEmpty(config.languageMapping) && CollectionUtils.isNotEmpty(languages)) {
			return transformLanguageCodes(languages, MapUtils.invertMap(config.languageMapping));
		} else {
			return languages;
		}
	}

	/**
	 * Convert the given languages list by transforming external language codes to internal codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param languages languages list (will be modified)
	 * @return transformed languages
	 */
	public static List<String> transformToInternalLanguageCodes(FormTypeConfiguration config, List<String> languages) {
		if (MapUtils.isNotEmpty(config.languageMapping) && CollectionUtils.isNotEmpty(languages)) {
			return transformLanguageCodes(languages, config.languageMapping);
		} else {
			return languages;
		}
	}

	/**
	 * Convert the languages with the given transformation map
	 * @param languages languages (will be modified)
	 * @param transformationMap transformation map
	 * @return transformed languages
	 */
	private static List<String> transformLanguageCodes(List<String> languages, Map<String, String> transformationMap) {
		for (Entry<String, String> entry : transformationMap.entrySet()) {
			String oldKey = entry.getKey();
			String newKey = entry.getValue();

			int index = languages.indexOf(oldKey);
			if (index >= 0) {
				languages.add(index, newKey);
				languages.removeIf(e -> Strings.CI.equals(e, oldKey));
			}
		}
		return languages;
	}

	/**
	 * Convert the given data by transforming internal language codes to external codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param data data (will be modified)
	 * @return transformed data
	 * @throws NodeException
	 */
	public static JsonNode transformToExternalLanguageCodes(FormTypeConfiguration config, JsonNode data) throws NodeException {
		if (MapUtils.isNotEmpty(config.languageMapping) && data instanceof ObjectNode dataObject) {
			return transformLanguageCodes(config, dataObject, MapUtils.invertMap(config.languageMapping));
		} else {
			return data;
		}
	}

	/**
	 * Convert the given data by transforming external language codes to internal codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param data data (will be modified)
	 * @return transformed data
	 * @throws NodeException
	 */
	public static JsonNode transformToInternalLanguageCodes(FormTypeConfiguration config, JsonNode data) throws NodeException {
		if (MapUtils.isNotEmpty(config.languageMapping) && data instanceof ObjectNode dataObject) {
			return transformLanguageCodes(config, dataObject, config.languageMapping);
		} else {
			return data;
		}
	}

	/**
	 * Convert the given data with the transformation map
	 * @param config configuration
	 * @param data data (will be modified)
	 * @param transformationMap transformation map
	 * @return transformed data
	 * @throws NodeException
	 */
	private static ObjectNode transformLanguageCodes(FormTypeConfiguration config, ObjectNode data,
			Map<String, String> transformationMap) throws NodeException {
		transformLanguageCodes(data.path(FormDataUtils.UI_SCHEMA).path(FormDataUtils.FORMNAME), transformationMap);
		data.path(FormDataUtils.UI_SCHEMA).path(FormDataUtils.PAGES).valueStream().forEach(page -> {
			transformLanguageCodes(page.path(FormDataUtils.PAGENAME), transformationMap);
		});
		FormDataUtils.handleElements(config, data, (element, elementType, elementConfig) -> {
			transformLanguageCodes(element.path(FormDataUtils.LABEL), transformationMap);
			transformLanguageCodes(element.path(FormDataUtils.DESCRIPTION), transformationMap);

			if (CollectionUtils.isNotEmpty(elementConfig.settings)) {
				for (BaseSetting setting : elementConfig.settings) {
					if (setting instanceof TranslationSetting) {
						if (StringUtils.isNotBlank(setting.propertyPath)) {
							JsonNode resolved = element;
							for (String segment : StringUtils.split(setting.propertyPath, '.')) {
								resolved = resolved.path(segment);
							}
							transformLanguageCodes(resolved, transformationMap);
						} else {
							transformLanguageCodes(element.path(FormDataUtils.FORM_GRID_OPTIONS).path(setting.id),
									transformationMap);
						}
					}
				}
			}
		}, null);
		return data;
	}

	/**
	 * Convert the given translations by transforming internal language codes to external codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param translations translations (will be modified)
	 * @return transformed translations
	 * @throws NodeException
	 */
	public static FormGenTranslations transformToExternalLanguageCodes(FormTypeConfiguration config, FormGenTranslations translations) throws NodeException {
		if (MapUtils.isNotEmpty(config.languageMapping) && translations != null) {
			return transformLanguageCodes(config, translations, MapUtils.invertMap(config.languageMapping));
		} else {
			return translations;
		}
	}

	/**
	 * Convert the given translations by transforming external language codes to internal codes, if a mapping exists in the configuration
	 * @param config configuration
	 * @param translations translations (will be modified)
	 * @return transformed translations
	 * @throws NodeException
	 */
	public static FormGenTranslations transformToInternalLanguageCodes(FormTypeConfiguration config, FormGenTranslations translations) throws NodeException {
		if (MapUtils.isNotEmpty(config.languageMapping) && translations != null) {
			return transformLanguageCodes(config, translations, config.languageMapping);
		} else {
			return translations;
		}
	}

	/**
	 * Convert the given translations with the transformation map
	 * @param config configuration
	 * @param translations translations (will be modified)
	 * @param transformationMap transformation map
	 * @return transformed translations
	 * @throws NodeException
	 */
	private static FormGenTranslations transformLanguageCodes(FormTypeConfiguration config, FormGenTranslations translations,
			Map<String, String> transformationMap) throws NodeException {
		translations.entrySet().forEach(entry -> {
			transformLanguageCodes(entry.getValue(), transformationMap);
		});
		return translations;
	}

	/**
	 * Transform the language codes in the given property
	 * @param property property (will be modified)
	 * @param transformationMap transformation map
	 */
	private static void transformLanguageCodes(JsonNode property, Map<String, String> transformationMap) {
		if (property instanceof ObjectNode propertyObject) {
			for (Entry<String, String> entry : transformationMap.entrySet()) {
				String oldKey = entry.getKey();
				String newKey = entry.getValue();

				if (propertyObject.hasNonNull(oldKey)) {
					JsonNode value = propertyObject.get(oldKey);
					propertyObject.remove(oldKey);
					propertyObject.set(newKey, value);
				}
			}
		}
	}

	/**
	 * Transform the language codes in the given translation map
	 * @param translationMap translation map (will be modified)
	 * @param transformationMap transformation map
	 */
	private static void transformLanguageCodes(Map<String, String> translationMap, Map<String, String> transformationMap) {
		for (Entry<String, String> entry : transformationMap.entrySet()) {
			String oldKey = entry.getKey();
			String newKey = entry.getValue();

			if (translationMap.get(oldKey) != null) {
				String value = translationMap.get(oldKey);
				translationMap.remove(oldKey);
				translationMap.put(newKey, value);
			}
		}
	}

	/**
	 * uiElement = oldFormGridElement
	 * gridPages = new UI SchemaPages
	 *
	 * @param config form type configuration
	 * @param oldUiSchemaElement old UI schema element
	 * @param oldFormGridElement
	 * @param newUISchemaPages
	 * @param pageIndex
	 * @return
	 */
	private static ObjectNode getMergedElement(FormTypeConfiguration config, ObjectNode oldUiSchemaElement, ObjectNode oldFormGridElement, ArrayNode newUISchemaPages, int pageIndex) {
		int numberOfUiSchemaPages = newUISchemaPages.size();
		if (isBlock(config, oldFormGridElement)) {
			// only keep text and spacer formgrid elements if they are on a page, that still
			// exists in the uiSchema
			if (numberOfUiSchemaPages >= (pageIndex - 1)) {
				oldFormGridElement.put("uiSchemaPage", pageIndex);
				return oldFormGridElement;
			}

			return null;
		}

		//
		ObjectNode foundNewUISchemaEntry = searchFormGridElement(oldFormGridElement.path("id").asText(""), newUISchemaPages);
		JsonNode foundNewUISchemaElement = foundNewUISchemaEntry.path("element");

		if (foundNewUISchemaElement.isNull() || foundNewUISchemaElement.isMissingNode()) {
			return null;
		}

		// use everything from found element
		// except formGridOptions and uiSchemaPage
		ObjectNode newElement = foundNewUISchemaEntry.path("element").deepCopy();
		newElement.set("formGridOptions", oldFormGridElement.get("formGridOptions"));
		newElement.put("uiSchemaPage", foundNewUISchemaEntry.path("pageInUiSchema").asInt(0));

		// if label or description has been changed in formgrid, we restore this values
		if (oldUiSchemaElement != null) {
			overwriteModifiedTranslations("label", oldUiSchemaElement, oldFormGridElement, newElement);
			overwriteModifiedTranslations("description", oldUiSchemaElement, oldFormGridElement, newElement);
		}

		String olfFormGridElementFormgridElementType = oldFormGridElement.path("formGridOptions").path("type").asText("");
		if (olfFormGridElementFormgridElementType.equals("aggregate") || oldFormGridElement.path("type").asText("").equals("aggregate")) {
			ArrayNode newAggElements = newElement.arrayNode();
			//formgrid AggElements
			ArrayNode oldFormGridAggElements = oldFormGridElement.optional("elements").filter(JsonNode::isArray)
					.map(n -> (ArrayNode) n).orElseGet(() -> oldFormGridElement.arrayNode());
			//newUI AggElements
			ArrayNode foundNewUISchemaAggElements = foundNewUISchemaElement.withArrayProperty("elements");

			for (JsonNode aggUiElement : oldFormGridAggElements) {
				// add the element if it is a text or spacer
				if (isBlock(config, aggUiElement)) {
					//aggregate elements will not switch pages - they will stay with their parent
					((ObjectNode) aggUiElement).put("uiSchemaPage", 0);
					newAggElements.add(aggUiElement);
				} else {
					// find element in aggregate by id
					JsonNode aggUiSchemaElement = foundNewUISchemaAggElements.valueStream()
							.filter(n -> n.path("id").asText("").equals(aggUiElement.path("id").asText(""))).findFirst()
							.orElse(null);

					// add the element if it still exists in the newAggElements
					if (aggUiSchemaElement != null) {
						// use data from uiSchema
						// except for formGridOptions - use formGrid data
						ObjectNode deepCopy = aggUiSchemaElement.deepCopy();
						deepCopy.set("formGridOptions", aggUiElement.get("formGridOptions"));
						// aggregate elements will not switch pages - they will stay with their parent
						deepCopy.put("uiSchemaPage", 0);

						// find the aggregate element in the old UI schema element
						if (oldUiSchemaElement != null) {
							String elementId = deepCopy.path("id").asText();
							JsonNode oldSchemaAggElement = oldUiSchemaElement.withArrayProperty("elements").valueStream()
									.filter(e -> Strings.CI.equals(e.path("id").asText(), elementId)).findFirst()
									.orElse(null);
							if (oldSchemaAggElement instanceof ObjectNode oldSchemaAggElementObject) {
								overwriteModifiedTranslations("label", oldSchemaAggElementObject,
										(ObjectNode) aggUiElement, deepCopy);
								overwriteModifiedTranslations("description", oldSchemaAggElementObject,
										(ObjectNode) aggUiElement, deepCopy);
							}
						}

						newAggElements.add(deepCopy);
					}
				}
			}

			// add Elements that are only present in newUiSchema - new Elements
			// add Elements at the end
			for (JsonNode newUISchemaAggElement : foundNewUISchemaAggElements) {
				boolean found = newAggElements.valueStream().filter(aggElement -> aggElement.path("id").asText("")
						.equals(newUISchemaAggElement.path("id").asText("")) || !newUISchemaAggElement.has("id")).findFirst().isPresent();
				if(!found) {
					ObjectNode deepCopy = newUISchemaAggElement.deepCopy();
					deepCopy.put("uiSchemaPage", 0);
					newAggElements.add(deepCopy);
				}
			}

			newElement.set("elements", newAggElements);
		}
		return newElement;
	}

	private static ObjectNode searchFormGridElement(String elementId, ArrayNode gridPages) {
		JsonNode foundElement = null;
		// define i outside of loop scope to use later
		int i = 0;
		for (; i < gridPages.size(); i++) {
			JsonNode page = gridPages.get(i);
			for (JsonNode element : page.path("elements")) {
				if (element.path("id").asText().equals(elementId)) {
					foundElement = element;
					break;
				}
			}
			if (foundElement != null) {
				break;
			}
		}

		return gridPages.objectNode()
			.put("pageInUiSchema", i)
			.set("element", foundElement);
	}

	private static boolean isBlock(FormTypeConfiguration config, JsonNode element) {
		if (element == null) {
			return false;
		}
		String formGridOptionsType = element.path("formGridOptions").path("type").asText("");
		Set<String> blockTypes = Optional.ofNullable(config.blocks).map(Map::keySet).orElse(Collections.emptySet());
		return blockTypes.contains(formGridOptionsType);
	}

	/**
	 * Overwrite translated entries
	 * @param propertyName
	 * @param oldUiSchemaElement
	 * @param oldFormGridElement
	 * @param newElement
	 */
	private static void overwriteModifiedTranslations(String propertyName, ObjectNode oldUiSchemaElement,
			ObjectNode oldFormGridElement, ObjectNode newElement) {
		newElement.optional(propertyName).filter(JsonNode::isObject).map(n -> (ObjectNode) n).ifPresent(prop -> {
			prop.propertyStream().forEach(entry -> {
				String language = entry.getKey();

				String oldUiTranslation = oldUiSchemaElement.path(propertyName).path(language).asText(null);
				String oldFormGridTranslation = oldFormGridElement.path(propertyName).path(language).asText(null);

				if (StringUtils.isNotBlank(oldFormGridTranslation) && !Strings.CS.equals(oldFormGridTranslation, oldUiTranslation)) {
					entry.setValue(newElement.textNode(oldFormGridTranslation));
				}
			});
		});
	}

	/**
	 * Transform a page reference stored in two separate properties
	 * @param data data possibly holding the reference
	 * @param pageIdProperty name of the pageId property
	 * @param nodeIdProperty name of the nodeId property
	 * @param pageConsumer consumer handling the transformation for the given page
	 * @throws NodeException
	 */
	protected void transformPageReference(ObjectNode data, String pageIdProperty, String nodeIdProperty,
			BiConsumer<Node, Page> pageConsumer) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		String pageId = data.optional(pageIdProperty).map(JsonNode::asText).orElse("");
		String nodeId = data.optional(nodeIdProperty).map(JsonNode::asText).orElse("");

		data.remove(pageIdProperty);
		data.remove(nodeIdProperty);

		Page referencedPage = null;
		int iNodeId = 0;

		Node node = t.getObject(Node.class, nodeId);
		if (!StringUtils.isBlank(pageId) && !Strings.CI.equals(pageId, "0")) {
			iNodeId = Optional.ofNullable(node).map(Node::getId).orElse(0);
			try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
				referencedPage = t.getObject(Page.class, pageId);
			}
		}

		try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
			pageConsumer.accept(node, referencedPage);
		}
	}

	/**
	 * Transform reference settings of the given element or property
	 * @param typeConfig type configuration
	 * @param elementType element type
	 * @param propertyOrElement property or element holding the settings
	 * @param languages languages of the form
	 * @throws NodeException
	 */
	protected void transformReferences(FormTypeConfiguration typeConfig, ElementConfig elementConfig,
			JsonNode propertyOrElement, List<String> languages) throws NodeException {
		Optional<JsonNode> optFormGridOptions = propertyOrElement.optional(FormDataUtils.FORM_GRID_OPTIONS);

		if (optFormGridOptions.isPresent() && elementConfig.settings != null) {
			for (BaseSetting setting : elementConfig.settings) {
				if (setting instanceof ReferenceSetting referenceSetting) {
					transformReference(referenceSetting, optFormGridOptions.get(), languages);
				}
			}
		}
	}

	/**
	 * Transform a single reference
	 * @param referenceSetting reference setting configuration
	 * @param formGridOptions JSON node holding the formGridOptions
	 * @param languages languages of the form
	 * @throws NodeException
	 */
	protected void transformReference(ReferenceSetting referenceSetting, JsonNode formGridOptions, List<String> languages)
			throws NodeException {
		Optional<JsonNode> optData = formGridOptions.optional(referenceSetting.id);
		if (optData.isPresent()) {
			JsonNode referenceData = optData.get();
			Transaction t = TransactionManager.getCurrentTransaction();

			String itemId = referenceData.optional("id").map(JsonNode::asText).orElse("");
			String nodeId = referenceData.optional("nodeId").map(JsonNode::asText).orElse("");
			String refTypeString = referenceData.optional("type").filter(JsonNode::isTextual).map(JsonNode::asText)
					.orElse("");
			if (!StringUtils.isBlank(itemId) && !Strings.CI.equals(itemId, "0")) {
				Node node = t.getObject(Node.class, nodeId);
				int iNodeId = Optional.ofNullable(node).map(Node::getId).orElse(0);
				try (ChannelTrx cTrx = new ChannelTrx(iNodeId)) {
					ReferenceType refType = ReferenceType.valueOf(refTypeString);
					LocalizableNodeObject<? extends NodeObject> referenced = null;
					switch (refType) {
					case file:
						referenced = t.getObject(File.class, itemId);
						break;
					case folder:
						referenced = t.getObject(Folder.class, itemId);
						break;
					case image:
						referenced = t.getObject(ImageFile.class, itemId);
						break;
					case page:
						referenced = t.getObject(Page.class, itemId);
						break;
					default:
						break;
					}

					if (referenced != null) {
						transformationTarget.transform(referenceSetting, (ObjectNode) formGridOptions, languages, node,
								referenced);
					} else {
						unknownReferenceCallback.operate();
					}
				} catch (IllegalArgumentException e) {
					// ignored
				}
			}
		}
	}

	/**
	 * Handle links in settings that allow richtext
	 * @param typeConfig type configuration
	 * @param elementConfig element configuration
	 * @param propertyOrElement property or element
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected void handleLinks(FormTypeConfiguration typeConfig, ElementConfig elementConfig,
			JsonNode propertyOrElement, List<String> languages) throws NodeException {
		Optional<JsonNode> optFormGridOptions = propertyOrElement.optional(FormDataUtils.FORM_GRID_OPTIONS);

		if (optFormGridOptions.isPresent() && elementConfig.settings != null) {
			for (BaseSetting setting : elementConfig.settings) {
				if (setting instanceof StringSetting stringSetting) {
					if (stringSetting.richContent) {
						transformationTarget.transform(stringSetting, (ObjectNode) optFormGridOptions.get(), languages);
					}
				} else if (setting instanceof TranslationSetting translationSetting) {
					if (translationSetting.richContent) {
						transformationTarget.transform(translationSetting, (ObjectNode) optFormGridOptions.get(), languages);
					}
				}
			}
		}
	}

	/**
	 * Do language fallback for the given object (if it is a page)
	 * @param <T> type of the object
	 * @param object object
	 * @param language target language
	 * @return target object
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends NodeObject> T languageFallback(T object, String language) throws NodeException {
		if (object instanceof Page page) {
			return (T) PageLanguageFallbackList.doFallback(page, LanguageFactory.get(language), page.getOwningNode());
		} else {
			return object;
		}
	}

	/**
	 * Render the page in all form languages and store the content as translated property in the node with given name
	 * @param node node where to store the content
	 * @param name property name
	 * @param page page to be rendered
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderPage(JsonNode node, String name, Page page, List<String> languages)
			throws NodeException {
		ObjectNode translatedRenderedPages = node.withObjectProperty(name).removeAll();
		for (String language : languages) {
			Page langPage = languageFallback(page, language);

			if (langPage != null) {
				translatedRenderedPages.put(language, langPage.render());
			}
		}
	}

	/**
	 * Render the URL to the given object and store as translated property in the node with given name
	 * @param node node where to store the URLs
	 * @param name property name
	 * @param object object for which the URL shall be rendered
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderUrl(JsonNode node, String name, NodeObject object, List<String> languages, int linkWay)
			throws NodeException {
		ObjectNode translatedUrls = node.withObjectProperty(name).removeAll();

		// generate the url factory with a static host link
		StaticUrlFactory urlFactory = new StaticUrlFactory(linkWay, linkWay, null);

		// we don't want the linkway to be changed
		urlFactory.setAllowAutoDetection(false);

		// disable link management
		urlFactory.setLinkManagement(LinkManagement.OFF);

		for (String language : languages) {
			Optional.ofNullable(renderUrl(urlFactory, object, language))
					.ifPresent(url -> translatedUrls.put(language, url));
		}
	}

	protected static String renderUrl(StaticUrlFactory urlFactory, NodeObject object, String language)
			throws NodeException {
		NodeObject langObject = languageFallback(object, language);
		if (langObject != null) {
			try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, langObject, false, false,
					false)) {
				RenderUrl renderUrl = urlFactory.createRenderUrl(langObject.getObjectInfo().getObjectClass(),
						langObject.getId());
				return renderUrl.toString();
			}
		}

		return null;
	}

	/**
	 * Transform the reference to global IDs
	 * @param jsonNode node holding the reference
	 * @param name name of the reference property
	 * @param node target node of the reference
	 * @param object target object of the reference
	 * @param idNames optional pair of names holding the reference IDs
	 * @throws NodeException
	 */
	protected static void toGlobalId(ObjectNode jsonNode, String name, Node node,
			LocalizableNodeObject<? extends NodeObject> object, Optional<Pair<String, String>> idNames)
			throws NodeException {
		ObjectNode base = idNames.map(ids -> jsonNode).orElseGet(() -> jsonNode.withObjectProperty(name));
		String itemIdName = idNames.map(Pair::getLeft).orElse("id");
		String nodeIdName = idNames.map(Pair::getRight).orElse("nodeId");

		if (object != null) {
			base.put(itemIdName, object.getMaster().getGlobalId().toString());
		} else {
			base.putNull(itemIdName);
		}
		if (node != null) {
			base.put(nodeIdName, node.getGlobalId().toString());
		} else {
			base.putNull(nodeIdName);
		}
	}

	/**
	 * Transform the reference to local IDs
	 * @param jsonNode node holding the reference
	 * @param name name of the reference property
	 * @param node target node of the reference
	 * @param object target object of the reference
	 * @param idNames optional pair of names holding the reference IDs
	 * @throws NodeException
	 */
	protected static void toLocalId(ObjectNode jsonNode, String name, Node node,
			LocalizableNodeObject<? extends NodeObject> object, Optional<Pair<String, String>> idNames)
			throws NodeException {
		ObjectNode base = idNames.map(ids -> jsonNode).orElseGet(() -> jsonNode.withObjectProperty(name));
		String itemIdName = idNames.map(Pair::getLeft).orElse("id");
		String nodeIdName = idNames.map(Pair::getRight).orElse("nodeId");

		if (object != null) {
			base.put(itemIdName, object.getMaster().getId());
		} else {
			base.putNull(itemIdName);
		}
		if (node != null) {
			base.put(nodeIdName, node.getId());
		} else {
			base.putNull(nodeIdName);
		}
	}

	/**
	 * Render URLS in text, if any.
	 * 
	 * @param text
	 * @param language
	 * @return
	 * @throws NodeException
	 */
	protected static final String renderUrls(String text, String language) throws NodeException {
		if (text == null) {
			// TODO check allow_links if possible
			return text;
		}

		// generate the url factory with a static host link
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_HOST, RenderUrl.LINK_HOST, null);

		// we don't want the linkway to be changed
		urlFactory.setAllowAutoDetection(false);

		// disable link management
		urlFactory.setLinkManagement(LinkManagement.OFF);

		String linkPattern = "\\{\\{"
				+ "[\\s]*(LINK)[\\s]*"
				+ "[|]"
				+ "[\\s]*(?<linkType>(PAGE|FILE|URL))"
				+ "[:]("
					+ "?:(?<=URL:)(?<url>[^|]*)|(?<!URL:)"
					+ "(?<nodeId>[a-zA-Z0-9\\-\\.]+)"
					+ "[:]"
					+ "(?<itemId>[a-zA-Z0-9\\-\\.]+)"
					+ "(?:[:](?<langCode>[a-zA-Z]{2}))?"
				+ ")[\\s]*[|][\\s]*"
				+ "(?<displayText>[^|]*)"
				+ "[\\s]*(?:[|][\\s]*(?<target>(_blank|_self|_top|_unfencedTop|_parent)))?[\\s]*"
				+ "\\}\\}";
		Pattern pattern = Pattern.compile(linkPattern);
		Matcher matcher = pattern.matcher(text);
		try (AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Deque<String> matches = new ArrayDeque<>(1);
			Transaction t = TransactionManager.getCurrentTransaction();
			while (matcher.find()) {
				String linkType = matcher.group("linkType");
				String nodeId = matcher.group("nodeId");
				String itemId = matcher.group("itemId");
				String displayText = matcher.group("displayText");
				Optional<String> maybeUrl = Optional.ofNullable(matcher.group("url"));
				Optional<String> maybeLangCode = Optional.ofNullable(matcher.group("langCode")).or(() -> Optional.ofNullable(language));
				Optional<String> maybeTarget = Optional.ofNullable(matcher.group("target"));
				Optional<Node> maybeChannelOrNode = Optional.ofNullable(t.getObject(Node.class, nodeId));
				Optional<Disinheritable<?>> maybeRenderedObject = Optional.empty();
				if ("FILE".equals(linkType)) {
					maybeRenderedObject = Optional.ofNullable(t.getObject(File.class, itemId));
					if (maybeRenderedObject.isEmpty()) {
						maybeRenderedObject = Optional.ofNullable(t.getObject(ImageFile.class, itemId));
					}
				} else if ("PAGE".equals(linkType)) {
					maybeRenderedObject = Optional.ofNullable(t.getObject(Page.class, itemId));
				} else if ("URL".equals(linkType) && maybeUrl.isEmpty()) {
					throw new NodeException("No URL provided for " + matcher.toString());
				}
				String url;
				if ("URL".equals(linkType)) {
					url = maybeUrl.get();
				} else {
					try (ChannelTrx cTrx = new ChannelTrx(maybeChannelOrNode.orElse(null))) {
						url = Optional.ofNullable(renderUrl(urlFactory, maybeRenderedObject.orElse(null), maybeLangCode.orElse(null))).orElse("#");
					}
				}
				matches.addLast(String.format("<a href='%s' %s>%s</a>", 
						Matcher.quoteReplacement(UrlEscapeUtils.unescapeUrl(url)), 
						maybeTarget.map(target -> "target='" + target + "'").orElse(org.apache.commons.lang3.StringUtils.EMPTY), 
						Matcher.quoteReplacement(UrlEscapeUtils.unescapeUrl(displayText))
					));
			}
			String link;
			while ((link = matches.poll()) != null) {
				text = text.replaceFirst(linkPattern, link);
			}
		}
		return text;
	}

	/**
	 * Enum of possible transformation targets
	 */
	public static enum TransformationTarget {
		/**
		 * Transform the form for publishing. This will transform references defined by
		 * their "publishAs" settings
		 */
		publish,

		/**
		 * Transform the form for preview
		 */
		preview,

		/**
		 * Transform the form for exporting it into a devtool package. This will
		 * transform references to global IDs
		 */
		toContentPackage,

		/**
		 * Transform the form for importing it from a devtool package. This will
		 * transform references to local IDs
		 */
		fromContentPackage;

		/**
		 * Transform the base form data
		 * @param typeConfig type configuration
		 * @param form form
		 * @param data form data
		 * @throws NodeException
		 */
		public void transform(FormTypeConfiguration typeConfig, Form form, ObjectNode data) throws NodeException {
			switch (this) {
			case publish:
				data.put("cms_id", form.getId());
				data.put(FormDataUtils.NAME, form.getName());
				data.put(FormDataUtils.TYPE, form.getFormType());
				data.put(FormDataUtils.SUBMIT_ONCE, typeConfig.submitOnce);
				// schema.properties is required in the plugin
				data.withObjectProperty(FormDataUtils.SCHEMA).withObjectProperty(FormDataUtils.PROPERTIES);
				// ui-schema.formname is required
				ObjectNode formName = data.withObjectProperty(FormDataUtils.UI_SCHEMA)
						.withObjectProperty(FormDataUtils.FORMNAME);
				if (formName.isEmpty()) {
					for (String language : form.getLanguages()) {
						formName.put(language, form.getName());
					}
				}
				data.without(FormDataUtils.OLD_UI_SCHEMA);
				transformToExternalLanguageCodes(typeConfig, data);
				break;
			case preview:
				data.put("formId", form.getId());
				data.put("formType", form.getFormType());
				data.set("uiSchema", data.get(FormDataUtils.UI_SCHEMA));
				data.without(FormDataUtils.UI_SCHEMA);
				data.put("cmsSid", TransactionManager.getCurrentTransaction().getSession().getSessionId());
				break;
			case fromContentPackage:
			case toContentPackage:
				if (!typeConfig.external) {
					for (String property : Arrays.asList("successPageId", "successNodeId", "adminEmailPageId",
							"adminEmailNodeId")) {
						if (!data.has(property)) {
							data.putNull(property);
						}
					}
				}
				break;
			default:
				break;
			}
		}

		/**
		 * Transform the reference
		 * @param referenceSetting reference setting
		 * @param referenceNode node holding the reference
		 * @param languages form languages
		 * @param node target node of the reference
		 * @param referenced target object of the reference
		 * @throws NodeException
		 */
		public void transform(ReferenceSetting referenceSetting, ObjectNode referenceNode, List<String> languages,
				Node node, LocalizableNodeObject<? extends NodeObject> referenced) throws NodeException {
			transform(referenceSetting, referenceNode, languages, node, referenced, Optional.empty());
		}

		/**
		 * Transform the reference
		 * @param referenceSetting reference setting
		 * @param referenceNode node holding the reference
		 * @param languages form languages
		 * @param node target node of the reference
		 * @param referenced target object of the reference
		 * @param idNames optional pair of ID names
		 * @throws NodeException
		 */
		public void transform(ReferenceSetting referenceSetting, ObjectNode referenceNode, List<String> languages,
				Node node, LocalizableNodeObject<? extends NodeObject> referenced,
				Optional<Pair<String, String>> idNames) throws NodeException {
			switch (this) {
			case toContentPackage:
				toGlobalId(referenceNode, referenceSetting.id, node, referenced, idNames);
				break;
			case fromContentPackage:
				toLocalId(referenceNode, referenceSetting.id, node, referenced, idNames);
				break;
			case publish:
				switch (referenceSetting.publishAs) {
				case content:
					if (referenced instanceof Page page) {
						renderPage(referenceNode, referenceSetting.id, page, languages);
					}
					break;
				case meshlink:
					renderUrl(referenceNode, referenceSetting.id, referenced, languages, RenderUrl.LINK_PORTAL);
					break;
				case url:
					renderUrl(referenceNode, referenceSetting.id, referenced, languages, RenderUrl.LINK_HOST);
					break;
				}
				break;
			default:
				break;
			}
		}

		/**
		 * Transform URLs embedded in the string setting
		 * @param stringSetting
		 * @param node
		 * @param languages
		 * @throws NodeException
		 */
		public void transform(StringSetting stringSetting, ObjectNode node, List<String> languages)
				throws NodeException {
			switch(this) {
			case toContentPackage:
				break;
			case fromContentPackage:
				break;
			case publish:
				String text = node.path(stringSetting.id).asText("");
				if (!StringUtils.isBlank(text)) {
					text = renderUrls(text, null);
					node.put(stringSetting.id, text);
				}
				break;
			default:
				break;
			}
		}

		/**
		 * Transform URLs embedded in the translation setting
		 * @param translationSetting
		 * @param node
		 * @param languages
		 * @throws NodeException
		 */
		public void transform(TranslationSetting translationSetting, ObjectNode node, List<String> languages)
				throws NodeException {
			switch(this) {
			case toContentPackage:
				break;
			case fromContentPackage:
				break;
			case publish:
				for (Entry<String, JsonNode> entry : node.optional(translationSetting.id).filter(JsonNode::isObject)
						.map(JsonNode::properties).orElse(Collections.emptySet())) {
					String language = entry.getKey();
					if (languages.contains(language)) {
						String text = entry.getValue().asText();
						text = renderUrls(text, language);
						entry.setValue(node.textNode(text));
					}
				}
				break;
			default:
				break;
			}
		}
	}
}
