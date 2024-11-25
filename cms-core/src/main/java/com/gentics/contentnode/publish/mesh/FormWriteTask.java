package com.gentics.contentnode.publish.mesh;

import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.PublishAction;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

/**
 * Task for writing a form to Mesh
 *
 */
class FormWriteTask extends AbstractWriteTask {
	/**
	 * Map of language code -&gt; language specific form data
	 */
	protected Map<String, String> dataMap = new HashMap<>();

	/**
	 * Create an instance for the form
	 * @param form form
	 * @param nodeId node ID
	 * @param publisher publisher
	 * @throws NodeException
	 */
	public FormWriteTask(Form form, int nodeId, MeshPublisher publisher) throws NodeException {
		this.objType = Form.TYPE_FORM;
		this.objId = form.getId();
		this.description = form.toString();
		this.nodeId = nodeId;
		this.uuid = MeshPublisher.getMeshUuid(form);
		this.publisher = publisher;

		for (String language : form.getLanguages()) {
			dataMap.put(language, form.getData(language).toString());
		}
	}

	@Override
	public void perform(boolean withSemaphore) throws NodeException {
		List<Completable> observableList = new ArrayList<>();
		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			String language = entry.getKey();
			String data = entry.getValue();
			observableList.add(publish(data, language));
		}

		publisher.getExistingFormLanguages(project, uuid).flatMapCompletable(langSet -> {
			// add completables which will remove languages from Mesh, which are not used any more
			langSet.stream().filter(lang -> !dataMap.keySet().contains(lang)).forEach(lang -> {
				observableList.add(remove(lang));
			});

			return Completable.concat(observableList).doOnError(t -> {
				publisher.errorHandler.accept(new NodeException(
						String.format("Error while performing task '%s' for '%s'", this, publisher.cr.getName()), t));
			}).doOnSubscribe(disp -> {
				MeshPublisher.logger.debug(String.format("Start publishing form %d as %s in languages %s", objId, uuid, dataMap.keySet()));
			}).doOnComplete(() -> {
				if (publisher.renderResult != null) {
					try {
						publisher.renderResult.info(MeshPublisher.class,
								String.format("written %d.%d into {%s} for node %d", objType, objId, publisher.cr.getName(), nodeId));
					} catch (NodeException e) {
					}
				}

				reportDone();
			});
		}).blockingAwait();
	}

	@Override
	public void reportDone() {
		MeshPublisher.logger.debug(String.format("Set %d.%d to be done", objType, objId));
		if (publisher.controller.publishProcess) {
			if (reportToPublishQueue) {
				try {
					PublishQueue.reportPublishActionDone(MeshPublisher.normalizeObjType(objType), objId, nodeId, PublishAction.WRITE_CR);
				} catch (NodeException e) {
				}
			}

			if (publisher.publishInfo != null) {
				MBeanRegistry.getPublisherInfo().publishedForm(nodeId);
				publisher.publishInfo.formRendered();
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Publish form %s (uuid %s)", description, uuid);
	}

	/**
	 * Publish the given form data as language variant of the form
	 * @param data form data
	 * @param language language
	 * @return completable
	 * @throws NodeException 
	 */
	protected Completable publish(String data, String language) throws NodeException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode form;
		try {
			form = objectMapper.readTree(data);
		} catch (Exception e) {
			throw new NodeException(e);
		}
		JsonNode renderedData = MeshPublisher.renderFormTextUrls(form, language);
		return publisher.client.post(String.format("/%s/plugins/forms/forms/%s", encodeSegment(project.name), uuid),
				new JsonObject(renderedData.toString())).toCompletable().doOnSubscribe(disp -> {
					MeshPublisher.logger
							.debug(String.format("Saving form %s, language %s, json: %s", uuid, language, renderedData));
				}).doOnComplete(() -> {
					MeshPublisher.logger
							.debug(String.format("Saved form %s, language %s", uuid, language));
				});
	}

	/**
	 * Remove the given language variant from the form
	 * @param language language
	 * @return completable
	 */
	protected Completable remove(String language) {
		return publisher.client.deleteNode(project.name, uuid, language).toCompletable().doOnSubscribe(disp -> {
			MeshPublisher.logger.debug(String.format("Removing language %s from form %s", language, uuid));
		});
	}
}
