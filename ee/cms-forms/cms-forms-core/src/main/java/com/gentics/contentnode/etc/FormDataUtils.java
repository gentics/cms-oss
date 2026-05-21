package com.gentics.contentnode.etc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.ElementConfig;

/**
 * Utility class for handling form data
 */
public class FormDataUtils {
	/**
	 * Name of the form gridp options attribute
	 */
	public final static String FORM_GRID_OPTIONS = "formGridOptions";

	/**
	 * Name of the schema property
	 */
	public final static String SCHEMA = "schema";

	/**
	 * Name of the ui-schema property
	 */
	public final static String UI_SCHEMA = "ui-schema";

	/**
	 * Name of the old-ui-schema property
	 */
	public final static String OLD_UI_SCHEMA = "old-ui-schema";

	/**
	 * Name of the formname property
	 */
	public final static String FORMNAME = "formname";

	/**
	 * Name of the pages property
	 */
	public final static String PAGES = "pages";

	/**
	 * Name of the pagename property
	 */
	public final static String PAGENAME = "pagename";

	/**
	 * Name of the elements property
	 */
	public final static String ELEMENTS = "elements";

	/**
	 * Name of the properties property
	 */
	public final static String PROPERTIES = "properties";
	/**
	 * Name of the name property
	 */
	public final static String NAME = "name";

	/**
	 * Name of the type property
	 */
	public final static String TYPE = "type";

	/**
	 * Name of the type property
	 */
	public final static String LABEL = "label";

	/**
	 * Name of the type property
	 */
	public final static String DESCRIPTION = "description";

	/**
	 * Name of the submitOnce property
	 */
	public final static String SUBMIT_ONCE = "submitOnce";

	/**
	 * Handle elements found in the schema or ui-schema of the given data by calling the given handler
	 * @param typeConfig type config
	 * @param data form data
	 * @param handler optional handler for handling found elements with known element types
	 * @param unknownTypeHandler optional handler for handling found elements with unknown element types
	 * @throws NodeException
	 */
	public static void handleElements(FormTypeConfiguration typeConfig, JsonNode data,
			TriConsumer<JsonNode, String, ElementConfig> handler, BiConsumer<JsonNode, String> unknownTypeHandler) throws NodeException {
		Map<String, String> elementTypePerId = new HashMap<>();

		// iterate over the schema.properties
		for (Iterator<Entry<String, JsonNode>> propertyIt = schemaProperties(data); propertyIt.hasNext();) {
			Entry<String, JsonNode> entry = propertyIt.next();
			String propertyId = entry.getKey();
			JsonNode property = entry.getValue();
			String elementType = property.optional(TYPE).map(JsonNode::asText).orElse("<unknown>");
			elementTypePerId.put(propertyId, elementType);

			Optional<ControlConfig> optControlConfig = Optional.ofNullable(typeConfig.controls).map(c -> c.get(elementType));
			Optional<BlockConfig> optBlockConfig = Optional.ofNullable(typeConfig.blocks).map(b -> b.get(elementType));

			if (optControlConfig.isPresent()) {
				if (handler != null) {
					handler.accept(property, elementType, optControlConfig.get());
				}
			} else if (optBlockConfig.isPresent()) {
				if (handler != null) {
					handler.accept(property, elementType, optBlockConfig.get());
				}
			} else {
				if (unknownTypeHandler != null) {
					unknownTypeHandler.accept(property, elementType);
				}
			}
		}

		// iterate over uischema.pages.elements
		for (Iterator<JsonNode> pagesIt = uiSchemaPages(data); pagesIt.hasNext();) {
			JsonNode page = pagesIt.next();
			for (Iterator<JsonNode> elementsIt = pageElements(page); elementsIt.hasNext();) {
				JsonNode element = elementsIt.next();
				String propertyId = element.path("id").asText("<unknown>");
				String elementType = elementTypePerId.getOrDefault(propertyId,
						element.optional(FORM_GRID_OPTIONS).flatMap(formGridOptions -> formGridOptions.optional(TYPE))
								.map(JsonNode::asText).orElse("<unknown>"));

				Optional<ControlConfig> optControlConfig = Optional.ofNullable(typeConfig.controls).map(c -> c.get(elementType));
				Optional<BlockConfig> optBlockConfig = Optional.ofNullable(typeConfig.blocks).map(b -> b.get(elementType));

				if (optControlConfig.isPresent()) {
					if (handler != null) {
						handler.accept(element, elementType, optControlConfig.get());
					}
				} else if (optBlockConfig.isPresent()) {
					if (handler != null) {
						handler.accept(element, elementType, optBlockConfig.get());
					}
				} else {
					if (unknownTypeHandler != null) {
						unknownTypeHandler.accept(element, elementType);
					}
				}
			}
		}
	}

	/**
	 * Get an iterator over the schema properties
	 * @param data form data
	 * @return iterator
	 */
	public static Iterator<Entry<String, JsonNode>> schemaProperties(JsonNode data) {
		return data.path(SCHEMA).path(PROPERTIES).propertyStream().flatMap(FormDataUtils::withSubProperties).iterator();
	}

	/**
	 * From the given property, create a stream containing the property and all sub-properties
	 * @param property initial property
	 * @return stream
	 */
	protected static Stream<Entry<String, JsonNode>> withSubProperties(Entry<String, JsonNode> property) {
		return Stream.concat(Stream.of(property),
				property.getValue().path(PROPERTIES).propertyStream().flatMap(FormDataUtils::withSubProperties));
	}

	/**
	 * Get an iterator over the ui-schema.pages
	 * @param data form data
	 * @return iterator
	 */
	public static Iterator<JsonNode> uiSchemaPages(JsonNode data) {
		return data.path(UI_SCHEMA).path(PAGES).values();
	}

	/**
	 * Get an iterator over the elements in a page
	 * @param page page
	 * @return iterator
	 */
	public static Iterator<JsonNode> pageElements(JsonNode page) {
		return page.path(ELEMENTS).valueStream().flatMap(FormDataUtils::withSubelements).iterator();
	}

	/**
	 * For the given element, create a stream containing the element and all sub-elements
	 * @param element initial element
	 * @return stream
	 */
	protected static Stream<JsonNode> withSubelements(JsonNode element) {
		return Stream.concat(Stream.of(element), element.path(ELEMENTS).valueStream().flatMap(FormDataUtils::withSubelements));
	}
}
