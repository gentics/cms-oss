package com.gentics.contentnode.publish.mesh;

import static com.gentics.contentnode.publish.mesh.MeshPublishUtils.ifNotFound;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.events.Dependency;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.factory.object.LanguageFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;
import com.gentics.contentnode.rest.model.formtype.BaseSetting;
import com.gentics.contentnode.rest.model.formtype.BlockConfig;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

/**
 * Service implementation for publishing forms to Mesh
 */
public class FormsPublisherService implements MeshPublisherService {
	/**
	 * Name of the form gridp options attribute
	 */
	public final static String FORM_GRID_OPTIONS = "formGridOptions";

	protected final static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Get the form URL
	 * @param meshProject mesh project
	 * @param pluginName plugin name
	 * @param uuid form uuid
	 * @return URL
	 */
	public static String getFormUrl(MeshProject meshProject, String pluginName, String uuid) {
		return "/%s/plugins/%s/%s/%s".formatted(encodeSegment(meshProject.name), encodeSegment(pluginName), "forms", uuid);
	}

	/**
	 * Get the URL to the form online status
	 * @param meshProject mesh project
	 * @param pluginName plugin name
	 * @param uuid form uuid
	 * @return URL
	 */
	public static String getFormOnlineUrl(MeshProject meshProject, String pluginName, String uuid) {
		return "%s/online".formatted(getFormUrl(meshProject, pluginName, uuid));
	}

	@Override
	public boolean handles(Class<? extends NodeObject> clazz) {
		return Form.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(MeshPublisher publisher, MeshProject meshProject, String currentProjectName, boolean repair, AtomicBoolean success)
			throws NodeException {
		if (meshProject.node != null && NodeConfigRuntimeConfiguration.isFeature(Feature.FORMS, meshProject.node)) {
			Set<String> activeFormTypes = FormFactory.getFormTypeAssignment(meshProject.node.getId());

			Map<String, ObjectNode> configurationPerPlugin = new HashMap<>();

			for (String formType : activeFormTypes) {
				FormTypeConfiguration typeConfiguration = FormConfigProvider.getConfig().get(formType);
				if (typeConfiguration == null) {
					// TODO log
				} else {
					ObjectNode pluginConfiguration = Optional.ofNullable(typeConfiguration.pluginConfiguration)
							.orElseGet(() -> mapper.createObjectNode());
					configurationPerPlugin
							.computeIfAbsent(typeConfiguration.pluginName, key -> mapper.createObjectNode())
							.set(formType, pluginConfiguration);
				}
			}

			for (Map.Entry<String, ObjectNode> entry : configurationPerPlugin.entrySet()) {
				checkPluginActivation(publisher, currentProjectName, entry.getKey(), entry.getValue(), repair, success);
			}
		}
	}

	@Override
	public AbstractWriteTask createWriteTask(MeshPublisher publisher, MeshProject project, NodeObject object,
			int nodeId, boolean publishProcess, boolean reportToPublishQueue) throws NodeException {
		if (object instanceof Form form) {
			FormTypeConfiguration config = getConfig(form);

			try (LangTrx lTrx = new LangTrx("en");
					RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_PUBLISH, form, publishProcess, publishProcess,
							publishProcess)) {

				ObjectNode data = (ObjectNode) form.getData();
				JsonNode downloadUrl = data == null ? null : data.get("downloadBaseUrl");

				if (downloadUrl == null || !downloadUrl.isTextual() || StringUtils.isEmpty(downloadUrl.asText())) {
					NodePreferences prefs = NodeConfigRuntimeConfiguration.getPreferences();
					String downloadBaseUrl = String.format("%s/editor/#proxy%srest/form/%d/data",
							prefs.getProperty("cn_external_server"), prefs.getProperty("portletapp_prefix"),
							form.getId());

					data.put("downloadBaseUrl", downloadBaseUrl);
				}

				FormWriteTask task = new FormWriteTask(config.pluginName, form, nodeId, publisher);
				// task.project is the current project of the node, not necessarily the desired
				// project
				task.project = project;
				// target project is the desired project
				task.targetProject = project;
				task.reportToPublishQueue = reportToPublishQueue;

				MeshPublisher.logger.debug(String.format("Put task %s into queue", task));
				return task;
			}
		} else {
			return null;
		}
	}

	@Override
	public void takeOffline(MeshPublisher publisher, MeshProject project, NodeObject object) throws NodeException {
		if (object instanceof Form form) {
			FormTypeConfiguration config = getConfig(form);
			String formOnlineUrl = getFormOnlineUrl(project, config.pluginName, MeshPublisher.getMeshUuid(object));

			publisher.client.deleteEmpty(formOnlineUrl).toCompletable().onErrorResumeNext(throwable -> {
				return ifNotFound(throwable, () -> Completable.complete());
			}).blockingAwait();
		}
	}

	@Override
	public void delete(MeshPublisher publisher, MeshProject project, String meshUuid, Map<String, String> additionalData) throws NodeException {
		String formType = Optional.ofNullable(additionalData).map(map -> map.get("formType"))
				.orElseThrow(() -> new NodeException("No formType given"));
		FormTypeConfiguration formTypeConfiguration = getConfig(formType);

		String formUrl = getFormUrl(project, formTypeConfiguration.pluginName, meshUuid);

		publisher.client.deleteEmpty(formUrl).toCompletable().onErrorResumeNext(throwable -> {
			return ifNotFound(throwable, () -> Completable.complete());
		}).blockingAwait();
	}

	/**
	 * Check the plugin activation/configuration.
	 * @param publisher publisher instance
	 * @param currentProjectName current project name
	 * @param pluginName plugin name
	 * @param configuration configuration to be written to the plugin
	 * @param repair true to repair
	 * @param success success collector
	 * @throws NodeException
	 */
	protected void checkPluginActivation(MeshPublisher publisher, String currentProjectName, String pluginName,
			JsonNode configuration, boolean repair, AtomicBoolean success) throws NodeException {
		publisher.info("Checking %s plugin for project %s", pluginName, currentProjectName);

		String pluginUrl = "/%s/plugins/%s".formatted(encodeSegment(currentProjectName), encodeSegment(pluginName));
		String activationUrl = "%s/active".formatted(pluginUrl);
		String formtypeConfigUrl = "%s/formtypeconfigs".formatted(pluginUrl);

		FormsPluginStatusResponse formsStatus = publisher.client.get(activationUrl, FormsPluginStatusResponse.class)
				.toSingle().onErrorReturn(t -> {
					return ifNotFound(t, () -> {
						publisher.error("Could not check %s plugin. Maybe plugin is not deployed or incompatible."
								.formatted(pluginName));
						FormsPluginStatusResponse response = new FormsPluginStatusResponse();
						response.setActive(false);
						return response;
					});
				}).blockingGet();
		if (formsStatus.getActive() == Boolean.TRUE) {
			publisher.info("%s plugin is active for project %s", pluginName, currentProjectName);

			publisher.info("Writing form type configurations to %s plugin for project %s", pluginName, currentProjectName);
			publisher.client.post(formtypeConfigUrl, new JsonObject(configuration.toString())).blockingAwait();
		} else {
			if (repair) {
				publisher.info("Activating %s plugin for project %s", pluginName, currentProjectName);
				formsStatus = publisher.client.put(activationUrl, FormsPluginStatusResponse.class).blockingGet();
				if (formsStatus.getActive() == Boolean.TRUE) {
					publisher.info("Activated %s plugin for project %s", pluginName, currentProjectName);

					publisher.info("Writing form type configurations to %s plugin for project %s", pluginName, currentProjectName);
					publisher.client.post(formtypeConfigUrl, new JsonObject(configuration.toString()))
							.blockingAwait();
				} else {
					publisher.error("Could not activate %s plugin for project %s", pluginName, currentProjectName);
					success.set(false);
				}
			} else {
				publisher.error("%s plugin is not active for project %s", pluginName, currentProjectName);
				success.set(false);
			}
		}
	}

	/**
	 * Get the form type configuration for the given form or throw an exception if not found
	 * @param form form
	 * @return form type configuration
	 * @throws NodeException
	 */
	protected FormTypeConfiguration getConfig(Form form) throws NodeException {
		return Optional.ofNullable(FormConfigProvider.getConfig()).map(map -> map.get(form.getFormType())).orElseThrow(
				() -> new NodeException("Could not find form type configuration for form %s".formatted(form)));
	}

	/**
	 * Get the form type configuration for the given form type or throw an exception if not found
	 * @param formType form type
	 * @return form type configuration
	 * @throws NodeException
	 */
	protected FormTypeConfiguration getConfig(String formType) throws NodeException {
		return Optional.ofNullable(FormConfigProvider.getConfig()).map(map -> map.get(formType)).orElseThrow(
				() -> new NodeException("Could not find form type configuration for form type %s".formatted(formType)));
	}

	/**
	 * Transform the given form to the JSON object, which will be sent to the plugin
	 * @param form form
	 * @return JSON object
	 * @throws NodeException
	 */
	public static ObjectNode transform(Form form) throws NodeException {
		String name = I18NHelper.getName(form);
		FormTypeConfiguration typeConfig = Optional.ofNullable(FormConfigProvider.getConfig()).map(config -> config.get(form.getFormType())).orElseThrow(
				() -> new NodeException("Error while publishing form %s: Could not find configuration for form type %s"
						.formatted(name, form.getFormType())));

		ObjectNode copy = form.getData().deepCopy();
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

			transformAllReferenceSettings(typeConfig, copy, languages);

			// TODO transform links in richtext

			transformPageReference(copy, "successPageId", "successNodeId", p -> renderUrl(copy, "successUrl", p, languages));
			transformPageReference(copy, "adminEmailPageId", "adminEmailNodeId", p -> renderPage(copy, "adminEmailSource", p, languages));

			// take over the collected dependencies
			if (handleDependencies && storeDependencies) {
				for (Dependency dep : rTrx.get().getDependencies()) {
					renderType.addDependency(dep.getSource(), dep.getSourceProperty());
				}
			}
		}

		copy.put("name", form.getName());
		copy.put("cms_id", form.getId());
		return copy;
	}

	/**
	 * Transform a page reference stored in two separate properties
	 * @param data data possibly holding the reference
	 * @param pageIdProperty name of the pageId property
	 * @param nodeIdProperty name of the nodeId property
	 * @param pageConsumer consumer handling the transformation for the given page
	 * @throws NodeException
	 */
	protected static void transformPageReference(ObjectNode data, String pageIdProperty, String nodeIdProperty,
			Consumer<Page> pageConsumer) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		int pageId = data.optional(pageIdProperty).filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);
		int nodeId = data.optional(nodeIdProperty).filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);

		if (pageId > 0) {
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				Page referencedPage = t.getObject(Page.class, pageId);
				if (referencedPage != null) {
					pageConsumer.accept(referencedPage);
				}
			}
		}

		data.remove(pageIdProperty);
		data.remove(nodeIdProperty);
	}

	/**
	 * Transform all reference settings in the form
	 * @param typeConfig type configuration
	 * @param data form data
	 * @param languages languages of the form
	 * @throws NodeException
	 */
	protected static void transformAllReferenceSettings(FormTypeConfiguration typeConfig, JsonNode data, List<String> languages) throws NodeException {
		// iterate over the schema.properties
		for (Iterator<JsonNode> propertyIt = schemaProperties(data); propertyIt.hasNext();) {
			JsonNode property = propertyIt.next();
			transformReferences(typeConfig, property.optional("type").map(JsonNode::asText).orElse("<unknown>"),
					property, languages);
		}

		// iterate over uischema.pages.elements
		for (Iterator<JsonNode> pagesIt = uiSchemaPages(data); pagesIt.hasNext();) {
			JsonNode page = pagesIt.next();
			for (Iterator<JsonNode> elementsIt = pageElements(page); elementsIt.hasNext();) {
				JsonNode element = elementsIt.next();

				transformReferences(typeConfig,
						element.optional(FORM_GRID_OPTIONS).flatMap(formGridOptions -> formGridOptions.optional("type"))
								.map(JsonNode::asText).orElse("<unknown>"),
						element, languages);
			}
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
	protected static void transformReferences(FormTypeConfiguration typeConfig, String elementType,
			JsonNode propertyOrElement, List<String> languages) throws NodeException {
		Optional<JsonNode> optFormGridOptions = propertyOrElement.optional(FORM_GRID_OPTIONS);

		if (optFormGridOptions.isPresent()) {
			Optional<ControlConfig> optControlConfig = Optional.ofNullable(typeConfig.controls.get(elementType));
			Optional<BlockConfig> optBlockConfig = Optional.ofNullable(typeConfig.blocks.get(elementType));
			for (BaseSetting setting : optControlConfig.map(c -> c.settings).orElse(Collections.emptyList())) {
				if (setting instanceof ReferenceSetting referenceSetting) {
					transformReference(referenceSetting, optFormGridOptions.get(), languages);
				}
			}
			for (BaseSetting setting : optBlockConfig.map(c -> c.settings).orElse(Collections.emptyList())) {
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
	protected static void transformReference(ReferenceSetting referenceSetting, JsonNode formGridOptions, List<String> languages)
			throws NodeException {
		Optional<JsonNode> optData = formGridOptions.optional(referenceSetting.id);
		if (optData.isPresent()) {
			JsonNode referenceData = optData.get();
			Transaction t = TransactionManager.getCurrentTransaction();

			int itemId = referenceData.optional("id").filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);
			int nodeId = referenceData.optional("nodeId").filter(JsonNode::isInt).map(JsonNode::asInt).orElse(0);
			String refTypeString = referenceData.optional("type").filter(JsonNode::isTextual).map(JsonNode::asText)
					.orElse("");
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				ReferenceType refType = ReferenceType.valueOf(refTypeString);
				NodeObject referenced = null;
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
					switch (referenceSetting.publishAs) {
					case content:
						if (referenced instanceof Page page) {
							renderPage(formGridOptions, referenceSetting.id, page, languages);
						}
						break;
					case meshlink:
						renderMeshlink(formGridOptions, refTypeString, referenced, languages);
						break;
					case url:
						renderUrl(formGridOptions, refTypeString, referenced, languages);
						break;
					}
				}
			} catch (IllegalArgumentException e) {
				// ignored
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
	 * Render the given object as meshlink and store as translated property in the node with given name
	 * @param node node where to store the mesh links
	 * @param name property name
	 * @param object object to be linked
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderMeshlink(JsonNode node, String name, NodeObject object,
			List<String> languages) throws NodeException {
		// TODO
	}

	/**
	 * Render the URL to the given object and store as translated property in the node with given name
	 * @param node node where to store the URLs
	 * @param name property name
	 * @param object object for which the URL shall be rendered
	 * @param languages form languages
	 * @throws NodeException
	 */
	protected static void renderUrl(JsonNode node, String name, NodeObject object, List<String> languages)
			throws NodeException {
		ObjectNode translatedUrls = node.withObjectProperty(name).removeAll();

		// generate the url factory with a static host link
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_HOST, RenderUrl.LINK_HOST, null);

		// we don't want the linkway to be changed
		urlFactory.setAllowAutoDetection(false);

		// disable link management
		urlFactory.setLinkManagement(LinkManagement.OFF);

		for (String language : languages) {
			NodeObject langObject = languageFallback(object, language);
			if (langObject != null) {
				try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, langObject, false, false,
						false)) {
					// Render the URL and set it as live URL
					RenderUrl renderUrl = urlFactory.createRenderUrl(langObject.getObjectInfo().getObjectClass(),
							langObject.getId());
					translatedUrls.put(language, renderUrl.toString());
				}
			}
		}
	}

	/**
	 * Get an iterator over the schema properties
	 * @param data form data
	 * @return iterator
	 */
	protected static Iterator<JsonNode> schemaProperties(JsonNode data) {
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
	protected static Iterator<JsonNode> uiSchemaPages(JsonNode data) {
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
	protected static Iterator<JsonNode> pageElements(JsonNode page) {
		return page
			.optional("elements")
			.map(elements -> elements.values()).orElse(Collections.emptyIterator());
	}
}
