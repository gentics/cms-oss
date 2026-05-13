package com.gentics.contentnode.publish.mesh;

import static com.gentics.contentnode.publish.mesh.MeshPublishUtils.ifNotFound;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.publish.mesh.MeshPublisher.MeshProject;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.rest.model.formtype.FormGenTranslations;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

/**
 * Service implementation for publishing forms to Mesh
 */
public class FormsPublisherService implements MeshPublisherService {

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

			FormGenTranslationsModel translations = getFormGenTranslations(activeFormTypes);

			for (Map.Entry<String, ObjectNode> entry : configurationPerPlugin.entrySet()) {
				checkPluginActivation(publisher, currentProjectName, entry.getKey(), entry.getValue(), translations,
						repair, success);
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
	 * @param translations translations to be written to the plugin
	 * @param repair true to repair
	 * @param success success collector
	 * @throws NodeException
	 */
	protected void checkPluginActivation(MeshPublisher publisher, String currentProjectName, String pluginName,
			JsonNode configuration, FormGenTranslationsModel translations, boolean repair, AtomicBoolean success) throws NodeException {
		publisher.info("Checking %s plugin for project %s", pluginName, currentProjectName);

		String pluginUrl = "/%s/plugins/%s".formatted(encodeSegment(currentProjectName), encodeSegment(pluginName));
		String activationUrl = "%s/active".formatted(pluginUrl);
		String formtypeConfigUrl = "%s/formtypeconfigs".formatted(pluginUrl);
		String translationsUrl = "%s/translations".formatted(pluginUrl);

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

			publisher.info("Writing form type configurations and translations to %s plugin for project %s", pluginName, currentProjectName);
			publisher.client.post(formtypeConfigUrl, new JsonObject(configuration.toString())).blockingAwait();
			publisher.client.post(translationsUrl, translations, JsonObject.class).blockingAwait();
		} else {
			if (repair) {
				publisher.info("Activating %s plugin for project %s", pluginName, currentProjectName);
				formsStatus = publisher.client.put(activationUrl, FormsPluginStatusResponse.class).blockingGet();
				if (formsStatus.getActive() == Boolean.TRUE) {
					publisher.info("Activated %s plugin for project %s", pluginName, currentProjectName);

					publisher.info("Writing form type configurations and translations to %s plugin for project %s", pluginName, currentProjectName);
					publisher.client.post(formtypeConfigUrl, new JsonObject(configuration.toString()))
							.blockingAwait();
					publisher.client.post(translationsUrl, translations, JsonObject.class).blockingAwait();
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
	 * Get the formGen translations model for the given set of active form types
	 * @param activeFormTypes set of active form types
	 * @return formGen translations model
	 */
	protected FormGenTranslationsModel getFormGenTranslations(Set<String> activeFormTypes) {
		FormGenTranslationsModel model = new FormGenTranslationsModel();
		model._default = transform(FormConfigProvider.getFormGenTranslations());
		model.types = new LinkedHashMap<>();

		for (String formType : activeFormTypes) {
			model.types.put(formType, transform(FormConfigProvider.getFormGenTranslations(formType)));
		}

		return model;
	}

	/**
	 * Transform the formGen translations from the config into the model sent to the plugin
	 * @param formGenTranslations configuration
	 * @return model
	 */
	protected TranslationsModel transform(FormGenTranslations formGenTranslations) {
		TranslationsModel transformed = new TranslationsModel();

		for (Entry<String, Map<String, String>> entry : formGenTranslations.entrySet()) {
			String key = entry.getKey();

			for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
				String language = entry2.getKey();
				String translation = entry2.getValue();

				transformed.computeIfAbsent(language, k -> new LinkedHashMap<>()).put(key, translation);
			}
		}

		return transformed;
	}
}
