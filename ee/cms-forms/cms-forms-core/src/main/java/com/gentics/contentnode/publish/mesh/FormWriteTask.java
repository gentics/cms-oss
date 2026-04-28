package com.gentics.contentnode.publish.mesh;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.forms.FormTransformer;
import com.gentics.contentnode.forms.FormTransformer.TransformationTarget;
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
	 * Transformed form data, which will be written to Mesh
	 */
	protected String transformedFormData;

	/**
	 * Name of the plugin to write to
	 */
	protected String pluginName;

	/**
	 * Create an instance for the form
	 * @param pluginName plugin name
	 * @param form form
	 * @param nodeId node ID
	 * @param publisher publisher
	 * @throws NodeException
	 */
	public FormWriteTask(String pluginName, Form form, int nodeId, MeshPublisher publisher) throws NodeException {
		this.pluginName = pluginName;
		this.objType = Form.TYPE_FORM;
		this.objId = form.getId();
		this.description = form.toString();
		this.nodeId = nodeId;
		this.uuid = MeshPublisher.getMeshUuid(form);
		this.publisher = publisher;

		this.transformedFormData = new FormTransformer().withTransformationTarget(TransformationTarget.publish)
				.transform(form).toString();
	}

	@Override
	public void perform(boolean withSemaphore) throws NodeException {
		publish(transformedFormData).doOnError(t -> {
			publisher.errorHandler.accept(new NodeException(
					String.format("Error while performing task '%s' for '%s'", this, publisher.cr.getName()), t));
		}).doOnSubscribe(disp -> {
			MeshPublisher.logger.debug(String.format("Start publishing form %d as %s", objId, uuid));
		}).doOnComplete(() -> {
			if (publisher.renderResult != null) {
				try {
					publisher.renderResult.info(MeshPublisher.class,
							String.format("written %d.%d into {%s} for node %d", objType, objId, publisher.cr.getName(), nodeId));
				} catch (NodeException e) {
				}
			}

			reportDone();
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
	 * @return completable
	 * @throws NodeException 
	 */
	protected Completable publish(String data) throws NodeException {
		String formUrl = FormsPublisherService.getFormUrl(project, pluginName, uuid);
		String formOnlineUrl = FormsPublisherService.getFormOnlineUrl(project, pluginName, uuid);

		Completable saveForm = publisher.client.post(formUrl, new JsonObject(data)).toCompletable()
				.doOnSubscribe(disp -> {
					MeshPublisher.logger
							.debug("Saving form %s, json: %s".formatted(uuid, data));
				}).doOnComplete(() -> {
					MeshPublisher.logger.debug("Saved form %s".formatted(uuid));
				});

		Completable publishForm = publisher.client.put(formOnlineUrl).toCompletable()
				.doOnSubscribe(disp -> {
					MeshPublisher.logger.debug("Setting form %s online".formatted(uuid));
				}).doOnComplete(() -> {
					MeshPublisher.logger.debug("Form %s was set online".formatted(uuid));
				});

		return saveForm.andThen(publishForm);
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
