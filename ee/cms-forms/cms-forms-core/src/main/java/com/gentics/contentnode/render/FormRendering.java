package com.gentics.contentnode.render;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.FormGenProvider;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.forms.FormTransformer.TransformationTarget;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.*;
import com.gentics.contentnode.publish.FilePublisher;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Helper class containing functionality for rendering forms common to velocity and handlebars
 */
public class FormRendering {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(FormRendering.class);

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
			} else if (renderType.getEditMode() == RenderType.EM_ALOHA) {
				// Forms cannot be shown live/interactively while Aloha is editing the page, show a placeholder instead.
				writer.write(I18NHelper.get("form.editmode.placeholder"));
			} else {
				return renderFormPreview(form, options, writer, renderType);
			}
		} else {
			writer.write(translate(options.getNoForm()));
		}
		return true;
	}

	private static boolean renderFormPreview(Form form, Options options, Writer writer, RenderType renderType) throws NodeException, IOException {
		// determine page language
		String language = "en";
		Page page = renderType.getStack().getTopmost(Page.class);
		if (page != null) {
			ContentLanguage pageLanguage = page.getLanguage();
			if (pageLanguage != null) {
				language = pageLanguage.getCode();
			}
		}

		ObjectNode formJson = new FormTransformer().withTransformationTarget(TransformationTarget.preview).transform(form);
		formJson.put("language", language);

		// load the form type configuration
		Optional<FormTypeConfiguration> optConfig = Optional.ofNullable(FormConfigProvider.getConfig()).map(m -> m.get(form.getFormType()));
		String pluginName = optConfig.map(c -> c.pluginName).orElse("");

		// add plugin name and flows
		formJson.put("pluginName", pluginName);
		optConfig.map(c -> c.flows).ifPresent(flows -> {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode flowsJsonNode = mapper.valueToTree(flows);
			formJson.set("flows", flowsJsonNode);
		});

		FormPreviewFiles previewFiles = getFormPreviewResources(page, formJson);
		if (previewFiles == null) {
			return false;
		}

		StringBuilder preview = new StringBuilder();

		preview.append("\n<script>window.formPreviewData = %s;\n</script>".formatted(formJson.toPrettyString()));

		for (String cssPath : previewFiles.getCssFiles()) {
			preview.append("\n<link rel=\"stylesheet\" href=\"%s\"/>".formatted(cssPath));
		}

		String formId = MeshPublisher.getMeshUuid(form);

		preview.append(
				"\n<div id=\"root\" tabindex=\"0\" data-formID=\"%s\" data-culture=\"%s\" data-plugin-name=\"%s\"></div>"
						.formatted(formId, language, pluginName));

		for (String jsPath : previewFiles.getJsFiles()) {
			preview.append("\n<script type=\"module\" src=\"%s\"></script>".formatted(jsPath));
		}

		// The preview has to be loaded into an own iframe, so it doesn't clash with aloha, or any other apps on the page.
		String iframeContent = "<!DOCTYPE html>" +
				"<html>" +
				"<head>" +
				"<meta charset=\"utf-8\">" +
				"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
				"</head>" +
				"<body>" +
				preview +
				"</body>" +
				"</html>";

		// In order to use it inline in the 'srcdoc' attribute, we have to escape the iframe-content so it doesn't break out of the attribute-value.
		String escapedIframeContent = iframeContent
				.replace("'", "&apos;")
				.replace("\"", "&quot;")
				.replace(">", "&gt;")
				.replace("<", "&lt;");

		String wrapper = "<iframe class=\"formgen-preview-frame\" sandbox=\"allow-scripts allow-same-origin\" referrerpolicy=\"same-origin\" srcdoc=\"" +
				escapedIframeContent +
				"\"></iframe>";

		writer.write(wrapper);

		return true;
	}

	private static FormPreviewFiles getFormPreviewResources(Page page, ObjectNode formData) throws NodeException, IOException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
			return null;
		}

		// check whether the node publishes into a Mesh CR
		Node node = page.getFolder().getNode();
		Node masterNode = node.getMaster();
		if (!masterNode.doPublishContentmap()) {
			return null;
		}
		ContentRepository cr = masterNode.getContentRepository();
		String meshPreviewUrl = masterNode.getEffectiveMeshPreviewUrl();

		if (cr == null
			|| cr.getCrType() != ContentRepositoryModel.Type.mesh
			|| StringUtils.isEmpty(meshPreviewUrl)) {
			return null;
		}
		String previewUrl = FilePublisher.getPath(false, true, meshPreviewUrl, node.getPublishDir(), ObjectTransformer.getString(page.getFolder().get("path"), null));

		try (
				CloseableHttpClient httpClient = RenderUtils.getHttpClient(node.isInsecurePreviewUrl());
				MeshPublisher mp = new MeshPublisher(cr, false)
		) {
			NodeResponse meshNode = new NodeResponse();
			String language = MeshPublisher.getMeshLanguage(page);
			meshNode.setUuid(MeshPublisher.getMeshUuid(page));
			meshNode.setLanguage(language);
			meshNode.setParentNode(new NodeReference().setUuid(MeshPublisher.getMeshUuid(page.getFolder())).setSchema(new SchemaReferenceImpl().setName(mp.getSchemaName(Folder.TYPE_FOLDER))));
			meshNode.setSchema(new SchemaReferenceImpl().setName(mp.getSchemaName(Page.TYPE_PAGE)));
			meshNode.setFields(new FieldMapImpl());
			mp.handleRenderedEntries(true, masterNode.getId(), page.getId(), page.getTType(), () -> meshNode.getFields(), mp.render(mp.getEntries(Page.TYPE_PAGE), null, language, false), null, null, null, null, null);

			HttpPost postMethod = new HttpPost(RenderUtils.getPreviewUrl(previewUrl + "/form", "preview", node));

			postMethod.setEntity(new StringEntity(JsonUtil.toJson(meshNode), ContentType.create("application/json", StandardCharsets.UTF_8)));

			if (logger.isInfoEnabled()) {
				logger.info(String.format("Preview Request to URL %s", postMethod.getURI().toString()));
				logger.info("Request Headers:");
				Stream.of(postMethod.getAllHeaders())
						.forEach(header -> logger.info(String.format("%s: %s", header.getName(), header.getValue())));
				if (logger.isDebugEnabled()) {
					logger.debug("Request Body:");
					logger.debug(JsonUtil.toJson(meshNode));
				}
			}

			HttpResponse response = httpClient.execute(postMethod);
			int responseCode = response.getStatusLine().getStatusCode();

			if (responseCode == 200) {
				String rawResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
				ObjectMapper mapper = new ObjectMapper();
				FormPreviewFiles previewFiles = mapper.convertValue(rawResponse, FormPreviewFiles.class);

				if (logger.isInfoEnabled()) {
					logger.info(String.format("Response code %d", responseCode));
					logger.info("Response Headers:");
					Stream.of(response.getAllHeaders())
							.forEach(header -> logger.info(String.format("%s: %s", header.getName(), header.getValue())));
					if (logger.isDebugEnabled()) {
						logger.debug("Response body:");
						logger.debug(rawResponse);
					}
				}

				return normalizePreviewFiles(previewFiles, masterNode);
			} else {
				logger.error(
						String.format("Request to render preview with portal by posting to %s returned response code %d",
								previewUrl,
								responseCode));
			}
		} catch (Exception e) {
			logger.error(String.format("Error while rendering preview with portal by posting to %s", previewUrl), e);
		}

		return null;
	}

	/**
	 * Rewrite relative js/css asset URLs of the given preview files to absolute URLs pointing to the
	 * {@code NodePreviewProxyResource} routes of the given node, so the assets are loaded through the CMS backend
	 * instead of directly from the (potentially unreachable from the browser, e.g. internal-only) preview target.
	 * Already absolute URLs are left untouched.
	 * @param previewFiles preview files to normalize
	 * @param node node whose preview proxy routes should be used
	 * @return the given previewFiles, with normalized URLs
	 * @throws NodeException
	 */
	private static FormPreviewFiles normalizePreviewFiles(FormPreviewFiles previewFiles, Node node) throws NodeException {
		previewFiles.setJsFiles(normalizeUrls(previewFiles.getJsFiles(), node));
		previewFiles.setCssFiles(normalizeUrls(previewFiles.getCssFiles(), node));
		return previewFiles;
	}

	/**
	 * Normalize the given list of URLs, see {@link #normalizePreviewFiles(FormPreviewFiles, Node)}
	 * @param urls URLs to normalize (may be null)
	 * @param node node whose preview proxy routes should be used
	 * @return normalized URLs (or null, if urls was null)
	 * @throws NodeException
	 */
	private static List<String> normalizeUrls(List<String> urls, Node node) throws NodeException {
		if (urls == null) {
			return null;
		}

		List<String> normalized = new ArrayList<>(urls.size());
		for (String url : urls) {
			normalized.add(normalizeUrl(url, node));
		}
		return normalized;
	}

	/**
	 * Normalize the given URL, see {@link #normalizePreviewFiles(FormPreviewFiles, Node)}
	 * @param url URL to normalize
	 * @param node node whose preview proxy routes should be used
	 * @return normalized URL
	 * @throws NodeException
	 */
	private static String normalizeUrl(String url, Node node) throws NodeException {
		if (StringUtils.isEmpty(url) || url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
			return url;
		}

		String relativePath = url.startsWith("/") ? url.substring(1) : url;
		return String.format("/rest/node/%s/preview/proxy/%s", node.getId(), relativePath);
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
			renderType.addDependency(form, "online");
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

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class FormPreviewFiles {
		protected List<String> jsFiles;

		protected List<String> cssFiles;

		public List<String> getJsFiles() {
			return jsFiles;
		}

		public void setJsFiles(List<String> jsFiles) {
			this.jsFiles = jsFiles;
		}

		public List<String> getCssFiles() {
			return cssFiles;
		}

		public void setCssFiles(List<String> cssFiles) {
			this.cssFiles = cssFiles;
		}
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
			String externalId = Optional.ofNullable(form.getExternalId()).map(id -> ", 'externalId' => '%s'".formatted(id)).orElse("");

			return String.format(
				"@%s('%s', [$formsCookie['key'] => $formsCookie['value'], 'pluginName' => '%s', 'language' => $node['language']%s%s])",
				FORMS_HELPER_NAME,
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				getPluginName(form),
				showResults,
				externalId);
		}),

		/**
		 * Render for Gentics Portal | java
		 */
		java((form, options) -> {
			String showResults = options.isShowResults() ? " showResults=true" : "";
			String externalId = Optional.ofNullable(form.getExternalId()).map(id -> " externalId=\"%s\"".formatted(id)).orElse("");

			return String.format(
				"<eval>{ %s \"%s\" pluginName=\"%s\" language=data.node.language%s%s }</eval>",
				FORMS_HELPER_NAME,
				MeshPublisher.toMeshUuid(form.getGlobalId().toString()),
				getPluginName(form),
				showResults,
				externalId);
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
