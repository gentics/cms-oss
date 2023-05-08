package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Assert for GenericResponses
 */
public class GenericResponseAssert extends AbstractAssert<GenericResponseAssert, GenericResponse> {
	/**
	 * Create an instance
	 * @param actual actual item
	 */
	protected GenericResponseAssert(GenericResponse actual) {
		super(actual, GenericResponseAssert.class);
	}

	/**
	 * Assert the given response code
	 * @param code code
	 * @return fluent API
	 */
	public GenericResponseAssert hasCode(ResponseCode code) {
		assertThat(actual.getResponseInfo()).as(String.format("%s response info", descriptionText())).isNotNull();
		assertThat(actual.getResponseInfo().getResponseCode()).as(String.format("%s response code", descriptionText())).isEqualTo(code);
		return myself;
	}

	/**
	 * Assert the given translated message
	 * @param type message type
	 * @param key translation key
	 * @param param parameters
	 * @return fluent API
	 * @throws NodeException
	 */
	public GenericResponseAssert containsMessage(Message.Type type, String key, String...param) throws NodeException {
		String msgString = Trx.supply(() -> {
			try (LangTrx lTrx = new LangTrx("de")) {
				CNI18nString msg = new CNI18nString(key);
				msg.addParameters(param);
				return msg.toString();
			}
		});

		assertThat(actual.getMessages()).as(String.format("%s response messages", descriptionText())).usingElementComparatorOnFields("type", "message")
				.contains(new Message(type, msgString));
		assertThat(msgString).as("Translated message").isNotEqualTo(key);
		return myself;
	}
}
