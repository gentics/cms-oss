package com.gentics.contentnode.etc;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

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
	 * Handle elements found in the schema or uiSchema of the given data by calling the given handler
	 * @param typeConfig type config
	 * @param data form data
	 * @param handler optional handler for handling found elements with known element types
	 * @param unknownTypeHandler optional handler for handling found elements with unknown element types
	 * @throws NodeException
	 */
	public static void handleElements(FormTypeConfiguration typeConfig, JsonNode data,
			TriConsumer<JsonNode, String, ElementConfig> handler, BiConsumer<JsonNode, String> unknownTypeHandler) throws NodeException {
		// iterate over the schema.properties
		for (Iterator<JsonNode> propertyIt = schemaProperties(data); propertyIt.hasNext();) {
			JsonNode property = propertyIt.next();
			String elementType = property.optional("type").map(JsonNode::asText).orElse("<unknown>");

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
				String elementType = element.optional(FORM_GRID_OPTIONS).flatMap(formGridOptions -> formGridOptions.optional("type"))
					.map(JsonNode::asText).orElse("<unknown>");

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
	public static Iterator<JsonNode> schemaProperties(JsonNode data) {
		return data
			.optional("schema")
			.flatMap(schema -> schema.optional("properties"))
			.map(properties -> properties.values())
			.orElse(Collections.emptyIterator());
	}

	/**
	 * Get an iterator over the uiSchema.pages
	 * @param data form data
	 * @return iterator
	 */
	public static Iterator<JsonNode> uiSchemaPages(JsonNode data) {
		return data
			.optional("uiSchema")
			.flatMap(uiSchema -> uiSchema.optional("pages"))
			.map(pages -> pages.values()).orElse(Collections.emptyIterator());
	}

	/**
	 * Get an iterator over the elements in a page
	 * @param page page
	 * @return iterator
	 */
	public static Iterator<JsonNode> pageElements(JsonNode page) {
		return page
			.optional("elements")
			.map(elements -> elements.values()).orElse(Collections.emptyIterator());
	}
}
