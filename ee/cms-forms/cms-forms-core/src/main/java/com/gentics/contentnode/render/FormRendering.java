package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher;

/**
 * Helper class containing functionality for rendering forms common to velocity and handlebars
 */
public class FormRendering {
	/**
	 * Name of the portal helper which renders a form
	 */
	public final static String FORMS_HELPER_NAME = "renderForm";

	/**
	 * Name of the HTML class to mark the root element, which is used for the form preview
	 */
	public final static String FORMS_PREVIEW_CLASS = "gcn-form-preview";

	/**
	 * Render the helper code, which will render the form in the portal
	 * @param form form to be rendered
	 * @param options options
	 * @param writer writer to render to
	 * @return true
	 * @throws NodeException
	 * @throws IOException
	 */
	public static boolean render(Form form, Options options, Writer writer) throws NodeException, IOException {
		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

		if (form != null) {
			if (renderType.getEditMode() == RenderType.EM_PUBLISH) {
				if (form.isOnline()) {
					// render in publish mode
					writer.write(options.getType().render(form, options));
				} else {
					writer.write(translate(options.getNoForm()));
				}
				renderType.addDependency(form, "online");
			} else {
				// determine page language
				String language = "en";
				Page page = renderType.getStack().getTopmost(Page.class);
				if (page != null) {
					ContentLanguage pageLanguage = page.getLanguage();
					if (pageLanguage != null) {
						language = pageLanguage.getCode();
					}
				}

				// render the preview tag
				writer.write(String.format(
					"<%s data-gcn-formid=\"%d\" data-gcn-formlanguage=\"%s\" class=\"%s\"></%s>",
					options.getPreviewRoot(),
					form.getId(),
					language,
					FORMS_PREVIEW_CLASS,
					options.getPreviewRoot()));
			}
		} else {
			writer.write(translate(options.getNoForm()));
		}
		return true;
	}

	/**
	 * Determine whether the form should be rendered
	 * @param form form
	 * @return true when the form should be rendered
	 * @throws NodeException
	 */
	public static boolean doRender(Form form) throws NodeException {
		if (form == null) {
			return false;
		}

		RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
		if (renderType.getEditMode() == RenderType.EM_PUBLISH) {
			return form.isOnline();
		} else {
			return true;
		}
	}

	/**
	 * Get the translation of "noForm" in the page's language (or do fallback with the node's languages)
	 * @param noForm no form definition
	 * @return translation
	 * @throws NodeException
	 */
	protected static String translate(JsonNode noForm) throws NodeException {
		if (noForm == null) {
			return "";
		} else if (noForm.isNull()) {
			return "";
		} else if (noForm.isTextual()) {
			return noForm.asText();
		} else {
			List<String> codes = RenderUtils.getLanguages();

			// get the first available translation
			for (String code : codes) {
				JsonNode langNode = noForm.path(code);
				if (langNode.isTextual()) {
					return langNode.asText();
				}
			}

			return "";
		}
	}

	/**
	 * Get the plugin name for the given form or throw an exception, if plugin name not known
	 * @param form form
	 * @return plugin name
	 * @throws NodeException
	 */
	protected static String getPluginName(Form form) throws NodeException {
		return Optional.ofNullable(FormConfigProvider.getConfig().get(form.getFormType()))
				.map(config -> config.pluginName)
				.orElseThrow(() -> new NodeException("Error while getting pluginName for form %s: Unknown formType %s."
						.formatted(form, form.getFormType())));
	}

	/**
	 * Options for the form directive
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Options {
		/**
		 * Render type
		 */
		private Type type = Type.java;

		/**
		 * Preview root tag
		 */
		private String previewRoot = "div";

		/**
		 * Replacement, if no form was found
		 */
		private JsonNode noForm = TextNode.valueOf("");

		/**
		 * Whether to show results of poll form, whether use has submitted a vote or not
		 */
		private boolean showResults = false;

		/**
		 * Render type
		 * @return type
		 */
		public Type getType() {
			return type;
		}

		/**
		 * Set the render type
		 * @param type type
		 */
		public void setType(Type type) {
			this.type = type;
		}

		/**
		 * Preview root tag
		 * @return root tag
		 */
		public String getPreviewRoot() {
			return previewRoot;
		}

		/**
		 * Set the preview root tag
		 * @param previewRoot root tag
		 */
		public void setPreviewRoot(String previewRoot) {
			this.previewRoot = previewRoot;
		}

		/**
		 * Replacement when no form was found
		 * @return replacement
		 */
		public JsonNode getNoForm() {
			return noForm;
		}

		/**
		 * Set replacement when no form was found
		 * @param noForm replacement
		 */
		public void setNoForm(JsonNode noForm) {
			this.noForm = noForm;
		}

		/**
		 * Whether to show results of poll form, whether use has submitted a vote or not
		 * @return showResults
		 */
		public boolean isShowResults() {
			return showResults;
		}

		/**
		 * Additional path segment for handlebars templates to render a form
		 * @param showResults
		 */
		public void setShowResults(boolean showResults) {
			this.showResults = showResults;
		}
	}

	/**
	 * Enum for render types (for backend rendering)
	 */
	public static enum Type {
		/**
		 * Render for Gentics Portal | php
		 */
		php((form, options) -> {
			String showResults = options.isShowResults() ? ", 'showResults' => true" : "";

			return String.format(
				"@%s('%s', [$formsCookie['key'] => $formsCookie['value'], 'pluginName' => '%s', 'language' => $node['language']%s])",
				FORMS_HELPER_NAME,
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				getPluginName(form),
				showResults);
		}),

		/**
		 * Render for Gentics Portal | java
		 */
		java((form, options) -> {
			String showResults = options.isShowResults() ? " showResults=true" : "";

			return String.format(
				"<eval>{ %s \"%s\" pluginName=\"%s\" language=data.node.language%s }</eval>",
				FORMS_HELPER_NAME,
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				getPluginName(form),
				showResults);
		});

		/**
		 * Renderer
		 */
		private final BiFunction<Form, Options, String> renderer;

		/**
		 * Create an instance
		 * @param renderer renderer
		 */
		Type(BiFunction<Form, Options, String> renderer) {
			this.renderer = renderer;
		}

		/**
		 * Render the form
		 * @param form form
		 * @return rendered form
		 * @throws NodeException
		 */
		public String render(Form form, Options options) throws NodeException {
			return renderer.apply(form, options);
		}
	}
}
