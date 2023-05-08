package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.contentnode.etc.BiFunction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;

/**
 * Form Directive
 */
public class FormDirective extends Directive {
	/**
	 * First argument must be the form
	 */
	public final static int ARGS_FORM = 0;

	/**
	 * Second argument must be the options
	 */
	public final static int ARGS_OPTIONS = 1;

	/**
	 * Name of the HTML class to mark the root element, which is used for the form preview
	 */
	public final static String FORMS_PREVIEW_CLASS = "gcn-form-preview";

	@Override
	public String getName() {
		return "gtx_form";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			Form form = getForm(context, node);
			Options options = RenderUtils.getVtlDirectiveObject(Options.class, context, node, ARGS_OPTIONS, Options::new);

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
		} catch (NodeException e) {
			throw new MethodInvocationException("Error while rendering object", e, getName(), context.getCurrentTemplateName(), node.jjtGetChild(0)
					.getLine(), node.jjtGetChild(0).getColumn());
		}
	}

	/**
	 * Get the form to render
	 * @param node node
	 * @return form or null, if not found
	 * @throws NodeException
	 */
	protected Form getForm(InternalContextAdapter context, Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		Object formChild = unwrapRenderableResolvable(node.jjtGetChild(ARGS_FORM).value(context));
		if (formChild instanceof Form) {
			return (Form) formChild;
		}
		if (formChild instanceof Value) {
			PartType partType = ((Value)formChild).getPartType();
			if (partType instanceof CmsFormPartType) {
				return ((CmsFormPartType)partType).getTarget();
			}
		}
		if (formChild != null) {
			return t.getObject(Form.class, ObjectTransformer.getInt(formChild, 0));
		}
		return null;
	}

	/**
	 * Unwrap instances of {@link RenderableResolvable} (recursively)
	 * @param object object to unwrap
	 * @return unwrapped object
	 */
	protected Object unwrapRenderableResolvable(Object object) {
		if (object instanceof RenderableResolvable) {
			return unwrapRenderableResolvable(((RenderableResolvable)object).getWrappedObject());
		} else {
			return object;
		}
	}

	/**
	 * Get the translation of "noForm" in the page's language (or do fallback with the node's languages)
	 * @param noForm no form definition
	 * @return translation
	 * @throws NodeException
	 */
	protected String translate(JsonNode noForm) throws NodeException {
		if (noForm == null) {
			return "";
		} else if (noForm.isNull()) {
			return "";
		} else if (noForm.isTextual()) {
			return noForm.asText();
		} else {
			List<String> codes = new ArrayList<>();
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			Page page = renderType.getStack().getTopmost(Page.class);
			if (page != null) {
				// add the page's language first
				ContentLanguage pageLanguage = page.getLanguage();
				if (pageLanguage != null) {
					codes.add(pageLanguage.getCode());
				}

				// then add all node languages
				List<ContentLanguage> langs = page.getOwningNode().getLanguages();
				for (ContentLanguage lang : langs) {
					if (!codes.contains(lang.getCode())) {
						codes.add(lang.getCode());
					}
				}
			}

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
	 * Options for the form directive
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected static class Options {
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
		 * Additional path segment for handlebars templates to render a form
		 */
		private String templateContext = "";

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

		public String getTemplateContext() {
			if (templateContext == null) {
				templateContext = "";
			}

			return templateContext;
		}

		public void setTemplateContext(String templateContext) {
			this.templateContext = templateContext;
		}
	}

	/**
	 * Enum for render types (for backend rendering)
	 */
	protected enum Type {
		/**
		 * Render for Gentics Portal | php
		 */
		php((form, options) -> {
			String showResults = options.isShowResults() ? ", 'showResults' => true" : "";
			String templateContext = options.getTemplateContext();

			if (StringUtils.isNotBlank(templateContext)) {
				templateContext = String.format(", 'templateContext' => '%s'", templateContext);
			}

			return String.format(
				"@renderForm('%s', [$formsCookie['key'] => $formsCookie['value'], 'language' => $node['language']%s%s])",
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				showResults,
				templateContext);
		}),

		/**
		 * Render for Gentics Portal | java
		 */
		java((form, options) -> {
			String showResults = options.isShowResults() ? " showResults=true" : "";
			String templateContext = options.getTemplateContext();

			if (StringUtils.isNotBlank(templateContext)) {
				templateContext = String.format(" templateContext=\"%s\"", templateContext);
			}

			return String.format(
				"<eval>{ renderForm \"%s\" language=data.node.language%s%s }</eval>",
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				showResults,
				templateContext);
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
