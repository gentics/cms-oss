package com.gentics.contentnode.tests.utils;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Optional;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.publish.mesh.FormsPluginOnlineStatusResponse;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;

/**
 * Static helper class for use in Form tests
 */
public class FormRESTUtils {

	/**
	 * Get a form resource
	 * @return form resource
	 * @throws NodeException
	 */
	public static FormResourceImpl getFormResource() throws NodeException {
		return new FormResourceImpl();
	}

	/**
	 * Assert a form in Mesh
	 * @param client mesh rest client
	 * @param projectName project name
	 * @param form form in question
	 * @param expectExistance true if the form is expected to exist, false if it is expected to not exist
	 * @param expectOnline true if the form is expected to be online, false if it is expected to be offline, null for no assertion
	 * @param asserters optional additional asserters
	 * @throws NodeException
	 */
	@SafeVarargs
	public static void assertForm(MeshRestClient client, String projectName, Form form, boolean expectExistance,
			Boolean expectOnline, Consumer<NodeResponse>... asserters) throws NodeException {
		String formType = form.getFormType();
		FormTypeConfiguration formTypeConfiguration = FormConfigProvider.getConfig().get(formType);
		assertThat(formTypeConfiguration).as("Form Type configuration for %s".formatted(formType)).isNotNull();
		String pluginName = formTypeConfiguration.pluginName;
		String meshUuid = supply(() -> MeshPublisher.getMeshUuid(form));

		String formUrl = "/%s/plugins/%s/forms/%s".formatted(encodeSegment(projectName), pluginName, meshUuid);

		Optional<NodeResponse> optResponse = client
				.get(formUrl, NodeResponse.class).toSingle()
				.map(response -> {
					return Optional.ofNullable(response);
				}).onErrorResumeNext(t -> {
					if (t instanceof MeshRestClientMessageException) {
						MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
						if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
							return Single.just(Optional.empty());
						}
					}
					throw new RuntimeException(t);
				}).blockingGet();

		String crDescription = "Mesh CR for project %s".formatted(projectName);

		if (expectExistance && !optResponse.isPresent()) {
			fail("Did not find expected %s in %s".formatted(form, crDescription));
		} else if (!expectExistance && optResponse.isPresent()) {
			fail("Found unexpected %s in %s".formatted(form, crDescription));
		}

		if (expectExistance && expectOnline != null) {
			FormsPluginOnlineStatusResponse onlineStatus = client
					.get("%s/online".formatted(formUrl), FormsPluginOnlineStatusResponse.class).toSingle()
					.onErrorResumeNext(t -> {
						if (t instanceof MeshRestClientMessageException) {
							MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
							if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
								return Single.just(new FormsPluginOnlineStatusResponse().setOnline(false));
							}
						}
						throw new RuntimeException(t);
					}).blockingGet();
			assertThat(onlineStatus.getOnline()).as("Online status of form %s".formatted(form)).isEqualTo(expectOnline);
		}

		if (expectExistance && asserters.length > 0) {
			for (Consumer<NodeResponse> asserter : asserters) {
				asserter.accept(optResponse.get());
			}
		}
	}
}
